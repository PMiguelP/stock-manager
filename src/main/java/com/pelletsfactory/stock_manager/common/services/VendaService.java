package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.AlocacaoOrdemEncomendaResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.*;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.mapper.AlocacaoOrdemEncomendaMapper;
import com.pelletsfactory.stock_manager.common.mapper.EncomendaClienteMapper;
import com.pelletsfactory.stock_manager.common.mapper.ItemEncomendaClienteMapper;
import com.pelletsfactory.stock_manager.common.repositories.*;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class VendaService {
    private final ClienteService clienteService;
    private final EncomendaClienteRepository encomendaClienteRepo;
    private final ItemEncomendaClienteRepository itemEncomendaClienteRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final StockService stockService;
    private final FinanceiroService financeiroService;
    private final NotificacaoService notificacaoService;
    private final MoedaRepository moedaRepo;
    private final AlocacaoOrdemEncomendaRepository alocacaoRepo;
    private final EncomendaClienteMapper encomendaClienteMapper;
    private final ItemEncomendaClienteMapper itemEncomendaClienteMapper;
    private final AlocacaoOrdemEncomendaMapper alocacaoOrdemEncomendaMapper;

    public VendaService(
            ClienteService clienteService,
            EncomendaClienteRepository encomendaClienteRepo,
            ItemEncomendaClienteRepository itemEncomendaClienteRepo,
            TipoPelletRepository tipoPelletRepo,
            StockService stockService,
            FinanceiroService financeiroService,
            NotificacaoService notificacaoService,
            MoedaRepository moedaRepo,
            AlocacaoOrdemEncomendaRepository alocacaoRepo,
            EncomendaClienteMapper encomendaClienteMapper,
            ItemEncomendaClienteMapper itemEncomendaClienteMapper,
            AlocacaoOrdemEncomendaMapper alocacaoOrdemEncomendaMapper) {
        this.clienteService = clienteService;
        this.encomendaClienteRepo = encomendaClienteRepo;
        this.itemEncomendaClienteRepo = itemEncomendaClienteRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.stockService = stockService;
        this.financeiroService = financeiroService;
        this.notificacaoService = notificacaoService;
        this.moedaRepo = moedaRepo;
        this.alocacaoRepo = alocacaoRepo;
        this.encomendaClienteMapper = encomendaClienteMapper;
        this.itemEncomendaClienteMapper = itemEncomendaClienteMapper;
        this.alocacaoOrdemEncomendaMapper = alocacaoOrdemEncomendaMapper;
    }

    /**
     * CreateSalesOrder: Criar encomenda de cliente
     * Apenas ASSISTENTE_COMERCIAL
     */
    @Transactional
    public EncomendaClienteResponseDTO criarPedidoVenda(UUID clienteId, UUID moedaId) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL);

        Cliente cliente = clienteService.buscarClientePorId(clienteId);

        Moeda moeda = moedaRepo.findById(moedaId)
                .orElseThrow(() -> new EntityNotFoundException("Moeda não encontrada"));

        EncomendaCliente encomenda = new EncomendaCliente();
        encomenda.setCliente(cliente);
        encomenda.setMoeda(moeda);
        encomenda.setData(LocalDate.now());
        encomenda.setEstado(EstadoEncomendaCliente.PENDENTE);
        encomenda.setTotalNet(0.0);
        encomenda.setTotalIva(0.0);
        encomenda.setTotalFinal(0.0);

        return encomendaClienteMapper.toResponseDTO(encomendaClienteRepo.save(encomenda));
    }

    /**
     * Adicionar item à encomenda
     * Com cálculo automático de IVA
     */
    @Transactional
    public void adicionarItemEncomenda(UUID encomendaId, UUID tipoPelletId, Double quantidadeKg, Double precoUnitarioNet, Double taxaIva) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL);

        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);

        // Validar que está em PENDENTE
        if (!EstadoEncomendaCliente.PENDENTE.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas pendentes podem ser modificadas");
        }

        TipoPellet tipoPellet = tipoPelletRepo.findById(tipoPelletId)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de pellet não encontrado"));

        // Calcular IVA automaticamente
        Double valorSubtotal = precoUnitarioNet * quantidadeKg;
        Double valorIvaCalculado = valorSubtotal * (taxaIva / 100.0);

        ItemEncomendaCliente item = new ItemEncomendaCliente();
        item.setEncomenda(encomenda);
        item.setTipoPellet(tipoPellet);
        item.setQuantidadeKg(quantidadeKg);
        item.setPrecoUnitarioNet(precoUnitarioNet);
        item.setTaxaIva(taxaIva);
        item.setValorIvaCalculado(valorIvaCalculado);

        itemEncomendaClienteRepo.save(item);

        // Recalcular totais
        recalcularTotaisEncomenda(encomendaId);
    }

    /**
     * Confirmar encomenda (mudar para CONFIRMADA)
     * Apenas ADMINISTRADOR ou ASSISTENTE_COMERCIAL
     */
    @Transactional
    public EncomendaClienteResponseDTO confirmarEncomenda(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR, Cargo.ASSISTENTE_COMERCIAL);

        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);

        if (!EstadoEncomendaCliente.PENDENTE.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas pendentes podem ser confirmadas");
        }

        if (encomenda.getItens() == null || encomenda.getItens().isEmpty()) {
            throw new RuntimeException("Encomenda deve ter pelo menos um item");
        }

        encomenda.setEstado(EstadoEncomendaCliente.CONFIRMADA);
        return encomendaClienteMapper.toResponseDTO(encomendaClienteRepo.save(encomenda));
    }

    /**
     * ReleaseStock: Cancelar encomenda e liberar stock reservado
     * Remove todas as alocações e marca como CANCELADA
     */
    @Transactional
    public EncomendaClienteResponseDTO cancelarEncomenda(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR, Cargo.ASSISTENTE_COMERCIAL);

        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);

        if (EstadoEncomendaCliente.EXPEDIDA.equals(encomenda.getEstado())) {
            throw new RuntimeException("Não é possível cancelar encomenda já expedida");
        }

        // Liberar todas as alocações (ReleaseStock)
        List<AlocacaoOrdemEncomenda> alocacoes = alocacaoRepo.findByEncomendaClienteId(encomendaId);
        for (AlocacaoOrdemEncomenda alocacao : alocacoes) {
            alocacaoRepo.delete(alocacao);
        }

        encomenda.setEstado(EstadoEncomendaCliente.CANCELADA);
        return encomendaClienteMapper.toResponseDTO(encomendaClienteRepo.save(encomenda));
    }

    /**
     * ShipmentManifest: Expedir encomenda
     * 1. Validar que tem codigo_tracking
     * 2. Validar que todo o stock está alocado
     * 3. Mover para EXPEDIDA
     * 4. Criar MovimentoFinanceiro de ENTRADA
     */
    @Transactional
    public EncomendaClienteResponseDTO expedir(UUID encomendaId, String codigoTracking) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_LOGISTICA);

        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);

        // Validar estado (deve estar CONFIRMADA ou EM_PRODUCAO)
        if (!EstadoEncomendaCliente.CONFIRMADA.equals(encomenda.getEstado()) &&
            !EstadoEncomendaCliente.EM_PRODUCAO.equals(encomenda.getEstado()) &&
            !EstadoEncomendaCliente.PRONTA.equals(encomenda.getEstado())) {
            throw new RuntimeException("Encomenda deve estar confirmada ou pronta para ser expedida");
        }

        // Validar codigo_tracking
        if (codigoTracking == null || codigoTracking.isEmpty()) {
            throw new RuntimeException("Código de tracking é obrigatório");
        }

        encomenda.setCodigoTracking(codigoTracking);
        encomenda.setEstado(EstadoEncomendaCliente.EXPEDIDA);

        EncomendaCliente updated = encomendaClienteRepo.save(encomenda);
        financeiroService.registarEntrada(encomenda, encomenda.getTotalFinal());

        return encomendaClienteMapper.toResponseDTO(updated);
    }

    /**
     * Mudar para EM_PRODUCAO (quando começa a produzir)
     */
    @Transactional
    public EncomendaClienteResponseDTO mudarParaEmProducao(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);

        if (!EstadoEncomendaCliente.CONFIRMADA.equals(encomenda.getEstado())) {
            throw new RuntimeException("Encomenda deve estar confirmada");
        }

        encomenda.setEstado(EstadoEncomendaCliente.EM_PRODUCAO);
        return encomendaClienteMapper.toResponseDTO(encomendaClienteRepo.save(encomenda));
    }

    /**
     * Mudar para PRONTA (quando pronta para expedir)
     */
    @Transactional
    public EncomendaClienteResponseDTO marcarComoPronta(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_LOGISTICA);

        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);

        if (!EstadoEncomendaCliente.EM_PRODUCAO.equals(encomenda.getEstado())) {
            throw new RuntimeException("Encomenda deve estar em produção");
        }

        encomenda.setEstado(EstadoEncomendaCliente.PRONTA);
        return encomendaClienteMapper.toResponseDTO(encomendaClienteRepo.save(encomenda));
    }

    /**
     * Listar encomendas de cliente com paginação
     */
    public Page<EncomendaClienteResponseDTO> listarEncomendasCliente(
            UUID clienteId,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "data";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

        Page<EncomendaCliente> pageResult = encomendaClienteRepo.findByClienteId(clienteId, pageable);
        return pageResult.map(encomendaClienteMapper::toResponseDTO);
    }

    /**
     * Obter encomenda por ID
     */
    public EncomendaClienteResponseDTO buscarEncomendaClienteDTO(UUID id) {
        return encomendaClienteMapper.toResponseDTO(buscarEncomendaOuFalhar(id));
    }

    /**
     * Obter encomenda por ID (uso interno)
     */
    public EncomendaCliente getEncomendaClienteById(UUID id) {
        return buscarEncomendaOuFalhar(id);
    }

    /**
     * Buscar por ID ou falhar
     */
    private EncomendaCliente buscarEncomendaOuFalhar(UUID id) {
        return encomendaClienteRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encomenda não encontrada com o ID: " + id));
    }

    /**
     * Recalcular totais da encomenda
     */
    private void recalcularTotaisEncomenda(UUID encomendaId) {
        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);
        List<ItemEncomendaCliente> itens = encomenda.getItens();

        Double totalNet = itens.stream()
                .mapToDouble(item -> item.getPrecoUnitarioNet() * item.getQuantidadeKg())
                .sum();

        Double totalIva = itens.stream()
                .mapToDouble(ItemEncomendaCliente::getValorIvaCalculado)
                .sum();

        Double totalFinal = totalNet + totalIva;

        encomenda.setTotalNet(totalNet);
        encomenda.setTotalIva(totalIva);
        encomenda.setTotalFinal(totalFinal);

        encomendaClienteRepo.save(encomenda);
    }

    /**
     * Listar encomendas com filtros (SimpleDTO)
     */
    public Page<EncomendaClienteSimpleDTO> listarEncomendasComFiltrosSimples(
            UUID clienteId,
            EstadoEncomendaCliente estado,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "data";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

        return encomendaClienteRepo.findByFiltros(clienteId, estado, pageable)
                .map(encomendaClienteMapper::toSimpleDTO);
    }

    public EncomendaClienteDetailsDTO obterDetalhesEncomendaCliente(UUID encomendaId) {
        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);

        List<ItemEncomendaClienteResponseDTO> itens = itemEncomendaClienteRepo.findByEncomendaId(encomendaId).stream()
                .map(itemEncomendaClienteMapper::toResponseDTO)
                .toList();

        List<AlocacaoOrdemEncomendaResponseDTO> alocacoes = alocacaoRepo.findByEncomendaClienteId(encomendaId).stream()
                .map(alocacaoOrdemEncomendaMapper::toResponseDTO)
                .toList();

        return new EncomendaClienteDetailsDTO(
                encomenda.getId(),
                encomenda.getCliente() != null ? encomenda.getCliente().getId() : null,
                encomenda.getCliente() != null ? encomenda.getCliente().getNome() : null,
                encomenda.getData(),
                encomenda.getEstado(),
                encomenda.getTotalNet(),
                encomenda.getTotalIva(),
                encomenda.getTotalFinal(),
                encomenda.getMoeda() != null ? encomenda.getMoeda().getId() : null,
                encomenda.getMoeda() != null ? encomenda.getMoeda().getCodigo() : null,
                encomenda.getCodigoTracking(),
                itens,
                alocacoes,
                encomenda.getCreatedAt(),
                encomenda.getUpdatedAt()
        );
    }
}
