package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.ConsumoProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ConsumoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ConsumoSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.ConsumoProducao;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.mapper.ConsumoProducaoMapper;
import com.pelletsfactory.stock_manager.common.repositories.ConsumoProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.MateriaPrimaRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
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
    private final ConsumoProducaoMapper mapper;

    public ConsumoProducaoService(
            ConsumoProducaoRepository consumoRepo,
            OrdemProducaoRepository ordemRepo,
            MateriaPrimaRepository matPrimaRepo,
            ConsumoProducaoMapper mapper) {
        this.consumoRepo = consumoRepo;
        this.ordemRepo = ordemRepo;
        this.matPrimaRepo = matPrimaRepo;
        this.mapper = mapper;
    }

    /**
     * Registar consumo real de matéria-prima
     */
    @Transactional
    public ConsumoResponseDTO registarConsumo(ConsumoProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        // Buscar ordem e matéria-prima
        OrdemProducao ordem = ordemRepo.findById(dto.ordemProducaoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ordem de produção não encontrada"
                ));

        MateriaPrima materiaPrima = matPrimaRepo.findById(dto.materiaPrimaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Matéria-prima não encontrada"
                ));

        // Validar stock disponível
        if (materiaPrima.getStockAtual() < dto.quantidadeConsumidaReal()) {
            throw new IllegalArgumentException(
                    "Stock insuficiente de " + materiaPrima.getNome() +
                            ". Disponível: " + materiaPrima.getStockAtual() + "kg"
            );
        }

        // Criar consumo
        ConsumoProducao consumo = mapper.toEntity(dto);
        consumo.setOrdem(ordem);
        consumo.setMateriaPrima(materiaPrima);

        // Descrementar stock
        materiaPrima.setStockAtual(materiaPrima.getStockAtual() - dto.quantidadeConsumidaReal());

        ConsumoProducao saved = consumoRepo.save(consumo);
        matPrimaRepo.save(materiaPrima);

        return mapper.toResponseDTO(saved);
    }

    /**
     * Atualizar consumo registado
     */
    @Transactional
    public ConsumoResponseDTO atualizarConsumo(UUID id, ConsumoProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        ConsumoProducao consumo = buscarPorIdOuFalhar(id);

        // Se a quantidade mudou, ajustar stock
        if (!consumo.getQuantidadeConsumidaReal().equals(dto.quantidadeConsumidaReal())) {
            MateriaPrima matPrima = consumo.getMateriaPrima();
            
            // Devolver quantidade anterior
            matPrima.setStockAtual(matPrima.getStockAtual() + consumo.getQuantidadeConsumidaReal());

            // Descontar quantidade nova
            Double novaQtd = dto.quantidadeConsumidaReal();
            if (matPrima.getStockAtual() < novaQtd) {
                throw new IllegalArgumentException("Stock insuficiente");
            }
            matPrima.setStockAtual(matPrima.getStockAtual() - novaQtd);

            matPrimaRepo.save(matPrima);
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

        // Devolver stock
        MateriaPrima matPrima = consumo.getMateriaPrima();
        matPrima.setStockAtual(matPrima.getStockAtual() + consumo.getQuantidadeConsumidaReal());

        consumoRepo.delete(consumo);
        matPrimaRepo.save(matPrima);
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

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        org.springframework.data.domain.Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, org.springframework.data.domain.Sort.by(dir, sortBy));
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

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        org.springframework.data.domain.Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, org.springframework.data.domain.Sort.by(dir, sortBy));
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
}
