package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.LotePelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LotePelletResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LotePelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.mapper.LotePelletMapper;
import com.pelletsfactory.stock_manager.common.repositories.LotePelletRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class LotePelletService {

    private final LotePelletRepository loteRepo;
    private final OrdemProducaoRepository ordemRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final LotePelletMapper mapper;

    public LotePelletService(
            LotePelletRepository loteRepo,
            OrdemProducaoRepository ordemRepo,
            TipoPelletRepository tipoPelletRepo,
            LotePelletMapper mapper) {
        this.loteRepo = loteRepo;
        this.ordemRepo = ordemRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.mapper = mapper;
    }

    /**
     * Criar novo lote de pellets
     */
    @Transactional
    public LotePelletResponseDTO criarLote(LotePelletRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        // Validar código único
        if (loteRepo.existsByCodigoLote(dto.codigoLote())) {
            throw new IllegalArgumentException(
                    "Já existe um lote com o código: " + dto.codigoLote()
            );
        }

        // Buscar ordem e tipo de pellet
        OrdemProducao ordem = ordemRepo.findById(dto.ordemProducaoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ordem de produção não encontrada"
                ));

        TipoPellet tipoPellet = tipoPelletRepo.findById(dto.tipoPelletId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tipo de pellet não encontrado"
                ));

        // Validar que o tipo de pellet corresponde à ordem
        if (!ordem.getTipoPellet().getId().equals(tipoPellet.getId())) {
            throw new IllegalArgumentException(
                    "Tipo de pellet não corresponde à ordem de produção"
            );
        }

        // Criar lote
        LotePellet lote = mapper.toEntity(dto);
        lote.setOrdem(ordem);
        lote.setTipoPellet(tipoPellet);
        lote.setDataProducao(Instant.now());

        // Incrementar stock do tipo de pellet
        tipoPellet.setStockAtual(tipoPellet.getStockAtual() + dto.quantidadeKg());

        LotePellet saved = loteRepo.save(lote);
        tipoPelletRepo.save(tipoPellet);

        return mapper.toResponseDTO(saved);
    }

    /**
     * Atualizar lote
     */
    @Transactional
    public LotePelletResponseDTO atualizarLote(UUID id, LotePelletRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        LotePellet lote = buscarPorIdOuFalhar(id);

        // Validar código único (se mudou)
        if (!lote.getCodigoLote().equals(dto.codigoLote()) && 
            loteRepo.existsByCodigoLote(dto.codigoLote())) {
            throw new IllegalArgumentException(
                    "Já existe um lote com o código: " + dto.codigoLote()
            );
        }

        mapper.updateEntityFromDTO(dto, lote);

        LotePellet updated = loteRepo.save(lote);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Eliminar lote
     */
    @Transactional
    public void apagarLote(UUID id) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        LotePellet lote = buscarPorIdOuFalhar(id);

        // Descrementar stock
        TipoPellet tipoPellet = lote.getTipoPellet();
        tipoPellet.setStockAtual(tipoPellet.getStockAtual() - lote.getQuantidadeKg());

        loteRepo.delete(lote);
        tipoPelletRepo.save(tipoPellet);
    }

    /**
     * Listar com paginação e filtros
     */
    public Page<LotePelletSimpleDTO> listarLotesComFiltros(
            int page,
            int pageSize,
            String codigoLote,
            UUID tipoPelletId,
            UUID ordemId,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "dataProducao";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

        Page<LotePellet> lotesPage = loteRepo.findByFiltros(codigoLote, tipoPelletId, ordemId, pageable);

        return lotesPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar lotes de uma ordem
     */
    public Page<LotePelletSimpleDTO> listarLotesDaOrdem(UUID ordemId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("dataProducao").descending());
        Page<LotePellet> lotesPage = loteRepo.findByOrdemId(ordemId, pageable);
        return lotesPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar todos (para combobox/dropdown)
     */
    public java.util.List<LotePelletSimpleDTO> listarTodosSimples() {
        return loteRepo.findAll()
                .stream()
                .map(mapper::toSimpleDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Verifica se código já existe
     */
    public boolean existsByCodigoLote(String codigoLote) {
        return loteRepo.existsByCodigoLote(codigoLote);
    }

    /**
     * Obter por ID
     */
    public LotePelletResponseDTO buscarPorId(UUID id) {
        LotePellet lote = buscarPorIdOuFalhar(id);
        return mapper.toResponseDTO(lote);
    }

    /**
     * Buscar entidade ou falhar
     */
    public LotePellet buscarPorIdOuFalhar(UUID id) {
        return loteRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lote de pellet não encontrado com ID: " + id
                ));
    }
}
