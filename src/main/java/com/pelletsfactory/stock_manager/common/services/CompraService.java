package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.EncomendaFornecedorResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.*;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.mapper.EncomendaFornecedorMapper;
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
public class CompraService {
    private final EncomendaFornecedorRepository encomendaFornecedorRepo;
    private final ItemEncomendaFornecedorRepository itemEncomendaFornecedorRepo;
    private final FornecedorRepository fornecedorRepo;
    private final MateriaPrimaRepository matPrimaRepo;
    private final StockService stockService;
    private final FinanceiroService financeiroService;
    private final NotificacaoService notificacaoService;
    private final MoedaRepository moedaRepo;
    private final EncomendaFornecedorMapper encomendaFornecedorMapper;

    public CompraService(
            EncomendaFornecedorRepository encomendaFornecedorRepo,
            ItemEncomendaFornecedorRepository itemEncomendaFornecedorRepo,
            FornecedorRepository fornecedorRepo,
            MateriaPrimaRepository matPrimaRepo,
            StockService stockService,
            FinanceiroService financeiroService,
            NotificacaoService notificacaoService,
            MoedaRepository moedaRepo,
            EncomendaFornecedorMapper encomendaFornecedorMapper) {
        this.encomendaFornecedorRepo = encomendaFornecedorRepo;
        this.itemEncomendaFornecedorRepo = itemEncomendaFornecedorRepo;
        this.fornecedorRepo = fornecedorRepo;
        this.matPrimaRepo = matPrimaRepo;
        this.stockService = stockService;
        this.financeiroService = financeiroService;
        this.notificacaoService = notificacaoService;
        this.moedaRepo = moedaRepo;
        this.encomendaFornecedorMapper = encomendaFornecedorMapper;
    }

    /**
     * SubmitDraftOrder: Criar encomenda de fornecedor em estado RASCUNHO
     * Apenas ASSISTENTE_COMERCIAL
     */
    @Transactional
    public EncomendaFornecedorResponseDTO gerarEncomendaRascunho(UUID fornecedorId, UUID moedaId) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL);

        Fornecedor fornecedor = fornecedorRepo.findById(fornecedorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado"));

        Moeda moeda = moedaRepo.findById(moedaId)
                .orElseThrow(() -> new EntityNotFoundException("Moeda não encontrada"));

        EncomendaFornecedor encomenda = new EncomendaFornecedor();
        encomenda.setFornecedor(fornecedor);
        encomenda.setMoeda(moeda);
        encomenda.setData(LocalDate.now());
        encomenda.setEstado(EstadoEncomendaFornecedor.RASCUNHO);
        encomenda.setTotalLiquido(0.0);
        encomenda.setTotalIva(0.0);
        encomenda.setTotalFinal(0.0);

        return encomendaFornecedorMapper.toResponseDTO(encomendaFornecedorRepo.save(encomenda));
    }

    /**
     * Adicionar item à encomenda com CalculateTaxes automático
     * O IVA é calculado automaticamente: valor_iva_calculado = preco_unitario_net * quantidade * (taxa_iva / 100)
     */
    @Transactional
    public void adicionarItemEncomenda(UUID encomendaId, UUID materiaPrimaId, Double quantidade, Double precoUnitarioNet, Double taxaIva) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL);

        EncomendaFornecedor encomenda = buscarPorIdOuFalhar(encomendaId);

        // Validar que está em RASCUNHO
        if (!EstadoEncomendaFornecedor.RASCUNHO.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas em rascunho podem ser modificadas");
        }

        MateriaPrima matPrima = matPrimaRepo.findById(materiaPrimaId)
                .orElseThrow(() -> new EntityNotFoundException("Matéria-prima não encontrada"));

        // Calcular IVA automaticamente
        Double valorSubtotal = precoUnitarioNet * quantidade;
        Double valorIvaCalculado = valorSubtotal * (taxaIva / 100.0);

        ItemEncomendaFornecedor item = new ItemEncomendaFornecedor();
        item.setEncomenda(encomenda);
        item.setMateriaPrima(matPrima);
        item.setQuantidade(quantidade);
        item.setPrecoUnitarioNet(precoUnitarioNet);
        item.setTaxaIva(taxaIva);
        item.setValorIvaCalculado(valorIvaCalculado);

        itemEncomendaFornecedorRepo.save(item);

        // Recalcular totais da encomenda
        recalcularTotaisEncomenda(encomendaId);
    }

    /**
     * Confirmar encomenda (mudar para EFETIVA)
     * Apenas ADMINISTRADOR pode confirmar
     */
    @Transactional
    public EncomendaFornecedorResponseDTO confirmarEncomenda(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);

        EncomendaFornecedor encomenda = buscarPorIdOuFalhar(encomendaId);

        if (!EstadoEncomendaFornecedor.RASCUNHO.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas em rascunho podem ser confirmadas");
        }

        if (encomenda.getItens() == null || encomenda.getItens().isEmpty()) {
            throw new RuntimeException("Encomenda deve ter pelo menos um item");
        }

        encomenda.setEstado(EstadoEncomendaFornecedor.EFETIVA);
        return encomendaFornecedorMapper.toResponseDTO(encomendaFornecedorRepo.save(encomenda));
    }

    /**
     * InventoryInflow: Confirmar recebimento de encomenda
     * 1. Muda estado para RECEBIDA
     * 2. Para cada item, adiciona stock em MateriaPrima
     * 3. Cria MovimentoFinanceiro de SAIDA
     */
    @Transactional
    public EncomendaFornecedorResponseDTO confirmarRecebimento(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR, Cargo.RESPONSAVEL_LOGISTICA);

        EncomendaFornecedor encomenda = buscarPorIdOuFalhar(encomendaId);

        if (!EstadoEncomendaFornecedor.EFETIVA.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas efetivas podem ser recebidas");
        }

        // 1. Para cada item, incrementar stock da matéria-prima
        for (ItemEncomendaFornecedor item : encomenda.getItens()) {
            MateriaPrima matPrima = item.getMateriaPrima();
            matPrima.setStockAtual(matPrima.getStockAtual() + item.getQuantidade());
            matPrimaRepo.save(matPrima);

            // Verificar se stock subiu acima do mínimo (para notificações)
            if (matPrima.getStockAtual() > matPrima.getStockMinimo()) {
                // Stock voltou ao normal - poderia criar notificação
            }
        }

        // 2. Mudar estado para RECEBIDA
        encomenda.setEstado(EstadoEncomendaFornecedor.RECEBIDA);
        financeiroService.registarSaida(encomenda, encomenda.getTotalFinal());

        return encomendaFornecedorMapper.toResponseDTO(encomendaFornecedorRepo.save(encomenda));
    }

    /**
     * Atualizar encomenda (apenas se RASCUNHO)
     */
    @Transactional
    public EncomendaFornecedorResponseDTO atualizarEncomenda(UUID encomendaId, UUID fornecedorId) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL);

        EncomendaFornecedor encomenda = buscarPorIdOuFalhar(encomendaId);

        if (!EstadoEncomendaFornecedor.RASCUNHO.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas em rascunho podem ser atualizadas");
        }

        Fornecedor fornecedor = fornecedorRepo.findById(fornecedorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado"));

        encomenda.setFornecedor(fornecedor);
        return encomendaFornecedorMapper.toResponseDTO(encomendaFornecedorRepo.save(encomenda));
    }

    /**
     * Anular encomenda
     */
    @Transactional
    public EncomendaFornecedorResponseDTO anumarEncomenda(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);

        EncomendaFornecedor encomenda = buscarPorIdOuFalhar(encomendaId);

        if (EstadoEncomendaFornecedor.RECEBIDA.equals(encomenda.getEstado())) {
            throw new RuntimeException("Não é possível anular encomenda já recebida");
        }

        encomenda.setEstado(EstadoEncomendaFornecedor.ANULADA);
        return encomendaFornecedorMapper.toResponseDTO(encomendaFornecedorRepo.save(encomenda));
    }

    /**
     * Listar encomendas de um fornecedor com paginação
     */
    public Page<EncomendaFornecedorResponseDTO> listarEncomendasFornecedores(
            UUID fornecedorId,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "data";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

        Page<EncomendaFornecedor> pageResult = encomendaFornecedorRepo.findByFornecedorId(fornecedorId, pageable);
        return pageResult.map(encomendaFornecedorMapper::toResponseDTO);
    }

    /**
     * Obter encomenda por ID
     */
    public EncomendaFornecedorResponseDTO buscarEncomendaFornecedorDTO(UUID id) {
        return encomendaFornecedorMapper.toResponseDTO(buscarPorIdOuFalhar(id));
    }

    /**
     * Obter encomenda por ID (uso interno)
     */
    public EncomendaFornecedor getEncomendaFornecedorById(UUID id) {
        return buscarPorIdOuFalhar(id);
    }

    /**
     * Buscar por ID ou falhar
     */
    private EncomendaFornecedor buscarPorIdOuFalhar(UUID id) {
        return encomendaFornecedorRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encomenda não encontrada com o ID: " + id));
    }

    /**
     * Recalcular totais da encomenda (somatório de itens)
     */
    private void recalcularTotaisEncomenda(UUID encomendaId) {
        EncomendaFornecedor encomenda = buscarPorIdOuFalhar(encomendaId);
        List<ItemEncomendaFornecedor> itens = encomenda.getItens();

        Double totalLiquido = itens.stream()
                .mapToDouble(item -> item.getPrecoUnitarioNet() * item.getQuantidade())
                .sum();

        Double totalIva = itens.stream()
                .mapToDouble(ItemEncomendaFornecedor::getValorIvaCalculado)
                .sum();

        Double totalFinal = totalLiquido + totalIva;

        encomenda.setTotalLiquido(totalLiquido);
        encomenda.setTotalIva(totalIva);
        encomenda.setTotalFinal(totalFinal);

        encomendaFornecedorRepo.save(encomenda);
    }
}
