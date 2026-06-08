package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.LotePelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LotePelletResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LotePelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import com.pelletsfactory.stock_manager.common.mapper.LotePelletMapper;
import com.pelletsfactory.stock_manager.common.repositories.LotePelletRepository;
import com.pelletsfactory.stock_manager.common.repositories.AlocacaoLoteEncomendaRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
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
    private final StockService stockService;
    private final AlocacaoLoteEncomendaRepository alocacaoRepo;
    private final LotePelletMapper mapper;

    public LotePelletService(
            LotePelletRepository loteRepo,
            OrdemProducaoRepository ordemRepo,
            TipoPelletRepository tipoPelletRepo,
            StockService stockService,
            AlocacaoLoteEncomendaRepository alocacaoRepo,
            LotePelletMapper mapper) {
        this.loteRepo = loteRepo;
        this.ordemRepo = ordemRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.stockService = stockService;
        this.alocacaoRepo = alocacaoRepo;
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
        OrdemProducao ordem = ordemRepo.findByIdForUpdate(dto.ordemProducaoId())
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
        validarOrdemPermiteLotes(ordem);
        validarQuantidadeProduzidaDisponivel(ordem, dto.quantidadeKg(), null);

        // Criar lote
        LotePellet lote = mapper.toEntity(dto);
        lote.setOrdem(ordem);
        lote.setTipoPellet(tipoPellet);
        lote.setDataProducao(Instant.now());

        // Incrementar stock do tipo de pellet
        LotePellet saved = loteRepo.save(lote);
        stockService.adicionarStockPellet(tipoPellet.getId(), dto.quantidadeKg());

        return mapper.toResponseDTO(saved);
    }

    /**
     * Atualizar lote
     */
    @Transactional
    public LotePelletResponseDTO atualizarLote(UUID id, LotePelletRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        LotePellet lote = loteRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Lote de pellet não encontrado"));
        OrdemProducao ordem = ordemRepo.findByIdForUpdate(lote.getOrdem().getId())
                .orElseThrow(() -> new EntityNotFoundException("Ordem de produção não encontrada"));
        if (!lote.getOrdem().getId().equals(dto.ordemProducaoId())
                || !lote.getTipoPellet().getId().equals(dto.tipoPelletId())) {
            throw new IllegalArgumentException("Não é possível trocar a ordem ou o tipo de pellet de um lote existente");
        }

        // Validar código único (se mudou)
        if (!lote.getCodigoLote().equals(dto.codigoLote()) && 
            loteRepo.existsByCodigoLote(dto.codigoLote())) {
            throw new IllegalArgumentException(
                    "Já existe um lote com o código: " + dto.codigoLote()
            );
        }

        validarOrdemPermiteLotes(ordem);
        validarQuantidadeProduzidaDisponivel(ordem, dto.quantidadeKg(), id);
        double diferenca = dto.quantidadeKg() - lote.getQuantidadeKg();
        validarQuantidadeNaoFicaAbaixoDasReservas(lote.getId(), dto.quantidadeKg());
        if (diferenca > 0) {
            stockService.adicionarStockPellet(lote.getTipoPellet().getId(), diferenca);
        } else if (diferenca < 0) {
            stockService.subtrairStockPellet(lote.getTipoPellet().getId(), -diferenca);
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

        LotePellet lote = loteRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Lote de pellet não encontrado"));
        ordemRepo.findByIdForUpdate(lote.getOrdem().getId())
                .orElseThrow(() -> new EntityNotFoundException("Ordem de produção não encontrada"));
        validarQuantidadeNaoFicaAbaixoDasReservas(lote.getId(), 0.0);

        stockService.subtrairStockPellet(lote.getTipoPellet().getId(), lote.getQuantidadeKg());
        loteRepo.delete(lote);
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

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "dataProducao");

        Page<LotePellet> lotesPage = loteRepo.findByFiltros(codigoLote, tipoPelletId, ordemId, pageable);

        return lotesPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar lotes de uma ordem
     */
    public Page<LotePelletSimpleDTO> listarLotesDaOrdem(UUID ordemId, int page, int pageSize) {
        Pageable pageable = PageableUtils.create(page, pageSize, "dataProducao", "DESC", "dataProducao");
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
     * Gera o próximo código de lote disponível no formato LOT-YYYY-NNN.
     */
    public String gerarProximoCodigo() {
        String year = String.valueOf(java.time.Year.now().getValue());
        String prefix = "LOT-" + year + "-";
        java.util.List<String> tops = loteRepo.findTopCodigosComPrefix(prefix, PageRequest.of(0, 1));
        if (tops.isEmpty()) {
            return prefix + "001";
        }
        try {
            int seq = Integer.parseInt(tops.get(0).substring(prefix.length()));
            return prefix + String.format("%03d", seq + 1);
        } catch (NumberFormatException e) {
            return prefix + String.format("%03d", loteRepo.findTopCodigosComPrefix(prefix, PageRequest.of(0, Integer.MAX_VALUE)).size() + 1);
        }
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

    /**
     * Lotes representam produto acabado real e apenas podem nascer após o início da produção.
     */
    private void validarOrdemPermiteLotes(OrdemProducao ordem) {
        if (ordem.getEstado() != EstadoOrdemProducao.EM_PRODUCAO
                && ordem.getEstado() != EstadoOrdemProducao.CONCLUIDA) {
            throw new IllegalStateException("A ordem deve estar em produção ou concluída para registar lotes");
        }
    }

    private void validarQuantidadeProduzidaDisponivel(OrdemProducao ordem, Double novaQuantidade, UUID loteIgnoradoId) {
        if (novaQuantidade == null || !Double.isFinite(novaQuantidade) || novaQuantidade <= 0) {
            throw new IllegalArgumentException("Quantidade do lote deve ser maior que zero");
        }
        double jaRegistado = loteRepo.findByOrdemId(ordem.getId()).stream()
                .filter(lote -> !lote.getId().equals(loteIgnoradoId))
                .mapToDouble(LotePellet::getQuantidadeKg)
                .sum();
        double produzido = ordem.getQuantidadeProduzidaReal() != null ? ordem.getQuantidadeProduzidaReal() : 0.0;
        if (jaRegistado + novaQuantidade > produzido) {
            throw new IllegalArgumentException("A quantidade dos lotes não pode exceder a produção real registada");
        }
    }

    private void validarQuantidadeNaoFicaAbaixoDasReservas(UUID loteId, Double novaQuantidade) {
        Double reservado = alocacaoRepo.sumQuantidadeReservadaByLoteId(loteId);
        if (reservado != null && reservado > novaQuantidade + 0.000001) {
            throw new IllegalStateException("O lote não pode ficar abaixo da quantidade reservada para encomendas");
        }
    }
}
