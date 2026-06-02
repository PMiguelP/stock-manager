package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.ConsumoProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ConsumoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ConsumoSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.ConsumoProducao;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import com.pelletsfactory.stock_manager.common.mapper.ConsumoProducaoMapper;
import com.pelletsfactory.stock_manager.common.repositories.ConsumoProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.ComposicaoPelletRepository;
import com.pelletsfactory.stock_manager.common.repositories.MateriaPrimaRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import com.pelletsfactory.stock_manager.common.utils.ProductionUnitUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsumoProducaoService {

    private final ConsumoProducaoRepository consumoRepo;
    private final OrdemProducaoRepository ordemRepo;
    private final MateriaPrimaRepository matPrimaRepo;
    private final ComposicaoPelletRepository composicaoRepo;
    private final StockService stockService;
    private final ConsumoProducaoMapper mapper;

    public ConsumoProducaoService(
            ConsumoProducaoRepository consumoRepo,
            OrdemProducaoRepository ordemRepo,
            MateriaPrimaRepository matPrimaRepo,
            ComposicaoPelletRepository composicaoRepo,
            StockService stockService,
            ConsumoProducaoMapper mapper) {
        this.consumoRepo = consumoRepo;
        this.ordemRepo = ordemRepo;
        this.matPrimaRepo = matPrimaRepo;
        this.composicaoRepo = composicaoRepo;
        this.stockService = stockService;
        this.mapper = mapper;
    }

    /**
     * Registar consumo real de matéria-prima
     */
    @Transactional
    public ConsumoResponseDTO registarConsumo(ConsumoProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        // Buscar ordem e matéria-prima
        OrdemProducao ordem = ordemRepo.findByIdForUpdate(dto.ordemProducaoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ordem de produção não encontrada"
                ));

        MateriaPrima materiaPrima = matPrimaRepo.findById(dto.materiaPrimaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Matéria-prima não encontrada"
                ));

        validarConsumoPermitido(ordem, materiaPrima);

        // Criar consumo
        ConsumoProducao consumo = mapper.toEntity(dto);
        consumo.setOrdem(ordem);
        consumo.setMateriaPrima(materiaPrima);

        ConsumoProducao saved = consumoRepo.save(consumo);
        stockService.subtrairStockMateriaPrima(materiaPrima.getId(), dto.quantidadeConsumidaReal());

        return mapper.toResponseDTO(saved);
    }

    /**
     * Atualizar consumo registado
     */
    @Transactional
    public ConsumoResponseDTO atualizarConsumo(UUID id, ConsumoProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        ConsumoProducao consumo = buscarPorIdOuFalhar(id);
        ordemRepo.findByIdForUpdate(consumo.getOrdem().getId())
                .orElseThrow(() -> new EntityNotFoundException("Ordem de produção não encontrada"));
        if (!consumo.getOrdem().getId().equals(dto.ordemProducaoId())
                || !consumo.getMateriaPrima().getId().equals(dto.materiaPrimaId())) {
            throw new IllegalArgumentException("Não é possível trocar a ordem ou a matéria-prima de um consumo existente");
        }
        validarConsumoPermitido(consumo.getOrdem(), consumo.getMateriaPrima());

        // Se a quantidade mudou, ajustar stock
        if (!consumo.getQuantidadeConsumidaReal().equals(dto.quantidadeConsumidaReal())) {
            MateriaPrima matPrima = consumo.getMateriaPrima();
            
            // Devolver quantidade anterior
            stockService.adicionarStockMateriaPrima(matPrima.getId(), consumo.getQuantidadeConsumidaReal());

            // Descontar quantidade nova
            Double novaQtd = dto.quantidadeConsumidaReal();
            stockService.subtrairStockMateriaPrima(matPrima.getId(), novaQtd);
        }

        mapper.updateEntityFromDTO(dto, consumo);

        ConsumoProducao updated = consumoRepo.save(consumo);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Eliminar consumo registado
     */
    @Transactional
    public void apagarConsumo(UUID id) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        ConsumoProducao consumo = buscarPorIdOuFalhar(id);
        ordemRepo.findByIdForUpdate(consumo.getOrdem().getId())
                .orElseThrow(() -> new EntityNotFoundException("Ordem de produção não encontrada"));
        if (consumo.getOrdem().getEstado() != EstadoOrdemProducao.EM_PRODUCAO) {
            throw new IllegalStateException("Apenas consumos de ordens em produção podem ser eliminados");
        }

        // Devolver stock
        MateriaPrima matPrima = consumo.getMateriaPrima();
        stockService.adicionarStockMateriaPrima(matPrima.getId(), consumo.getQuantidadeConsumidaReal());

        consumoRepo.delete(consumo);
    }

    /**
     * Listar consumos de uma ordem
     */
    public List<ConsumoResponseDTO> listarConsumosDeOrdem(UUID ordemId) {
        return consumoRepo.findByOrdemId(ordemId).stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Listar consumos de uma ordem (com paginação)
     */
    public Page<ConsumoResponseDTO> listarConsumosDeOrdemPaginado(UUID ordemId, int page, int pageSize) {
        return listarConsumosDeOrdemPaginado(ordemId, page, pageSize, "createdAt", "DESC");
    }

    /**
     * Listar consumos de uma ordem (com paginação e ordenação)
     */
    public Page<ConsumoResponseDTO> listarConsumosDeOrdemPaginado(
            UUID ordemId,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "createdAt");
        Page<ConsumoProducao> consumosPage = consumoRepo.findByOrdemId(ordemId, pageable);
        return consumosPage.map(mapper::toResponseDTO);
    }

    /**
     * Listar consumos de uma ordem (com paginação) - SimpleDTO
     */
    public Page<ConsumoSimpleDTO> listarConsumosDeOrdemSimples(
            UUID ordemId,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "createdAt");
        Page<ConsumoProducao> consumosPage = consumoRepo.findByOrdemId(ordemId, pageable);
        return consumosPage.map(mapper::toSimpleDTO);
    }

    /**
     * Obter por ID
     */
    public ConsumoResponseDTO buscarPorId(UUID id) {
        ConsumoProducao consumo = buscarPorIdOuFalhar(id);
        return mapper.toResponseDTO(consumo);
    }

    /**
     * Buscar entidade ou falhar
     */
    public ConsumoProducao buscarPorIdOuFalhar(UUID id) {
        return consumoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Consumo de produção não encontrado com ID: " + id
                ));
    }

    private void validarConsumoPermitido(OrdemProducao ordem, MateriaPrima materiaPrima) {
        if (ordem.getEstado() != EstadoOrdemProducao.EM_PRODUCAO) {
            throw new IllegalStateException("Apenas ordens em produção podem registar consumos");
        }
        ProductionUnitUtils.requireKilograms(materiaPrima);
        boolean pertenceAFormula = composicaoRepo.findByFormulaId(ordem.getFormula().getId()).stream()
                .anyMatch(comp -> comp.getMateriaPrima().getId().equals(materiaPrima.getId()));
        if (!pertenceAFormula) {
            throw new IllegalArgumentException("A matéria-prima não pertence à fórmula da ordem");
        }
    }
}
