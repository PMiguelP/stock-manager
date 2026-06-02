package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.AlocacaoLoteEncomendaResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.*;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.mapper.EncomendaClienteMapper;
import com.pelletsfactory.stock_manager.common.mapper.ItemEncomendaClienteMapper;
import com.pelletsfactory.stock_manager.common.repositories.*;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import com.pelletsfactory.stock_manager.common.utils.CalculationUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VendaService {
    private final ClienteService clienteService;
    private final EncomendaClienteRepository encomendaClienteRepo;
    private final ItemEncomendaClienteRepository itemEncomendaClienteRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final StockService stockService;
    private final FinanceiroService financeiroService;
    private final MoedaRepository moedaRepo;
    private final AlocacaoLoteEncomendaRepository alocacaoLoteRepo;
    private final EncomendaClienteMapper encomendaClienteMapper;
    private final ItemEncomendaClienteMapper itemEncomendaClienteMapper;
    private final AlocacaoLoteEncomendaService alocacaoLoteService;

    public VendaService(
            ClienteService clienteService,
            EncomendaClienteRepository encomendaClienteRepo,
            ItemEncomendaClienteRepository itemEncomendaClienteRepo,
            TipoPelletRepository tipoPelletRepo,
            StockService stockService,
            FinanceiroService financeiroService,
            MoedaRepository moedaRepo,
            AlocacaoLoteEncomendaRepository alocacaoLoteRepo,
            EncomendaClienteMapper encomendaClienteMapper,
            ItemEncomendaClienteMapper itemEncomendaClienteMapper,
            AlocacaoLoteEncomendaService alocacaoLoteService) {
        this.clienteService = clienteService;
        this.encomendaClienteRepo = encomendaClienteRepo;
        this.itemEncomendaClienteRepo = itemEncomendaClienteRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.stockService = stockService;
        this.financeiroService = financeiroService;
        this.moedaRepo = moedaRepo;
        this.alocacaoLoteRepo = alocacaoLoteRepo;
        this.encomendaClienteMapper = encomendaClienteMapper;
        this.itemEncomendaClienteMapper = itemEncomendaClienteMapper;
        this.alocacaoLoteService = alocacaoLoteService;
    }

    /**
     * Cria uma encomenda de cliente pendente.
     */
    @Transactional
    public EncomendaClienteResponseDTO criarPedidoVenda(UUID clienteId, UUID moedaId) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL, Cargo .ADMINISTRADOR);

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
     * Adiciona um item pendente e recalcula os totais com arredondamento monetário.
     */
    @Transactional
    public void adicionarItemEncomenda(UUID encomendaId, UUID tipoPelletId, Double quantidadeKg, Double precoUnitarioNet, Double taxaIva) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL);

        EncomendaCliente encomenda = buscarEncomendaParaAtualizarOuFalhar(encomendaId);

        // Validar que está em PENDENTE
        if (!EstadoEncomendaCliente.PENDENTE.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas pendentes podem ser modificadas");
        }

        TipoPellet tipoPellet = tipoPelletRepo.findById(tipoPelletId)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de pellet não encontrado"));

        CalculationUtils.requirePositive(quantidadeKg, "Quantidade");
        CalculationUtils.requirePositive(precoUnitarioNet, "Preço unitário");
        CalculationUtils.requirePercentage(taxaIva, "Taxa de IVA");
        Double valorSubtotal = CalculationUtils.subtotal(quantidadeKg, precoUnitarioNet);
        Double valorIvaCalculado = CalculationUtils.vat(valorSubtotal, taxaIva);

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

        EncomendaCliente encomenda = buscarEncomendaParaAtualizarOuFalhar(encomendaId);

        if (!EstadoEncomendaCliente.PENDENTE.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas pendentes podem ser confirmadas");
        }

        if (itemEncomendaClienteRepo.findByEncomendaId(encomendaId).isEmpty()) {
            throw new RuntimeException("Encomenda deve ter pelo menos um item");
        }

        encomenda.setEstado(EstadoEncomendaCliente.CONFIRMADA);
        return encomendaClienteMapper.toResponseDTO(encomendaClienteRepo.save(encomenda));
    }

    /**
     * Cancela a encomenda e liberta as alocações associadas.
     */
    @Transactional
    public EncomendaClienteResponseDTO cancelarEncomenda(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR, Cargo.ASSISTENTE_COMERCIAL);

        EncomendaCliente encomenda = buscarEncomendaParaAtualizarOuFalhar(encomendaId);

        if (EstadoEncomendaCliente.EXPEDIDA.equals(encomenda.getEstado())
                || EstadoEncomendaCliente.CANCELADA.equals(encomenda.getEstado())) {
            throw new RuntimeException("Não é possível cancelar uma encomenda finalizada");
        }

        alocacaoLoteRepo.deleteAll(alocacaoLoteRepo.findByItemEncomendaEncomendaId(encomendaId));

        encomenda.setEstado(EstadoEncomendaCliente.CANCELADA);
        return encomendaClienteMapper.toResponseDTO(encomendaClienteRepo.save(encomenda));
    }

    /**
     * Expede uma encomenda pronta: valida tracking, alocação e stock, desconta pellets
     * e regista a receita uma única vez.
     */
    @Transactional
    public EncomendaClienteResponseDTO expedir(UUID encomendaId, String codigoTracking) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_LOGISTICA);

        EncomendaCliente encomenda = buscarEncomendaParaAtualizarOuFalhar(encomendaId);

        if (!EstadoEncomendaCliente.PRONTA.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas prontas podem ser expedidas");
        }

        if (codigoTracking == null || codigoTracking.isBlank()) {
            throw new RuntimeException("Código de tracking é obrigatório");
        }
        String trackingNormalizado = codigoTracking.trim();
        if (encomendaClienteRepo.existsByCodigoTrackingIgnoreCase(trackingNormalizado)) {
            throw new RuntimeException("Código de tracking já está associado a outra encomenda");
        }

        List<ItemEncomendaCliente> itens = itemEncomendaClienteRepo.findByEncomendaId(encomendaId);
        validarAlocacaoCompleta(encomendaId, itens);
        for (ItemEncomendaCliente item : itens) {
            stockService.subtrairStockPellet(item.getTipoPellet().getId(), item.getQuantidadeKg());
        }

        encomenda.setCodigoTracking(trackingNormalizado);
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

        EncomendaCliente encomenda = buscarEncomendaParaAtualizarOuFalhar(encomendaId);

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

        EncomendaCliente encomenda = buscarEncomendaParaAtualizarOuFalhar(encomendaId);

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

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "data");

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

    private EncomendaCliente buscarEncomendaParaAtualizarOuFalhar(UUID id) {
        return encomendaClienteRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Encomenda não encontrada com o ID: " + id));
    }

    /**
     * Recalcular totais da encomenda
     */
    private void recalcularTotaisEncomenda(UUID encomendaId) {
        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);
        List<ItemEncomendaCliente> itens = itemEncomendaClienteRepo.findByEncomendaId(encomendaId);

        Double totalNet = CalculationUtils.money(itens.stream()
                .mapToDouble(item -> CalculationUtils.subtotal(item.getQuantidadeKg(), item.getPrecoUnitarioNet()))
                .sum());

        Double totalIva = CalculationUtils.money(itens.stream()
                .mapToDouble(ItemEncomendaCliente::getValorIvaCalculado)
                .sum());

        Double totalFinal = CalculationUtils.total(totalNet, totalIva);

        encomenda.setTotalNet(totalNet);
        encomenda.setTotalIva(totalIva);
        encomenda.setTotalFinal(totalFinal);

        encomendaClienteRepo.save(encomenda);
    }

    private void validarAlocacaoCompleta(UUID encomendaId, List<ItemEncomendaCliente> itens) {
        if (itens.isEmpty()) {
            throw new IllegalStateException("Encomenda sem itens não pode ser expedida");
        }

        Map<UUID, Double> necessarioPorPellet = itens.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getTipoPellet().getId(),
                        Collectors.summingDouble(ItemEncomendaCliente::getQuantidadeKg)
                ));
        Map<UUID, Double> reservadoPorPellet = alocacaoLoteRepo.findByItemEncomendaEncomendaId(encomendaId).stream()
                .collect(Collectors.groupingBy(
                        alocacao -> alocacao.getItemEncomenda().getTipoPellet().getId(),
                        Collectors.summingDouble(AlocacaoLoteEncomenda::getQuantidadeReservada)
                ));

        necessarioPorPellet.forEach((tipoPelletId, quantidadeNecessaria) -> {
            double quantidadeReservada = reservadoPorPellet.getOrDefault(tipoPelletId, 0.0);
            if (quantidadeReservada < quantidadeNecessaria) {
                throw new IllegalStateException("A encomenda ainda não tem produção totalmente alocada");
            }
        });
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

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "data");

        return encomendaClienteRepo.findByFiltros(clienteId, estado, pageable)
                .map(encomendaClienteMapper::toSimpleDTO);
    }

    public EncomendaClienteDetailsDTO obterDetalhesEncomendaCliente(UUID encomendaId) {
        EncomendaCliente encomenda = buscarEncomendaOuFalhar(encomendaId);

        List<ItemEncomendaClienteResponseDTO> itens = itemEncomendaClienteRepo.findByEncomendaId(encomendaId).stream()
                .map(itemEncomendaClienteMapper::toResponseDTO)
                .toList();

        List<AlocacaoLoteEncomendaResponseDTO> alocacoes = alocacaoLoteService.listarPorEncomenda(encomendaId);

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
