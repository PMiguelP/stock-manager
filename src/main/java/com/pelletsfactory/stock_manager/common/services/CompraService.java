package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.EncomendaFornecedorDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaFornecedorResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaFornecedorSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaFornecedorResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.*;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.mapper.EncomendaFornecedorMapper;
import com.pelletsfactory.stock_manager.common.mapper.ItemEncomendaFornecedorMapper;
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
import java.util.UUID;

@Service
public class CompraService {
    private final EncomendaFornecedorRepository encomendaFornecedorRepo;
    private final ItemEncomendaFornecedorRepository itemEncomendaFornecedorRepo;
    private final FornecedorService fornecedorService;
    private final MateriaPrimaRepository matPrimaRepo;
    private final StockService stockService;
    private final FinanceiroService financeiroService;
    private final MoedaRepository moedaRepo;
    private final EncomendaFornecedorMapper encomendaFornecedorMapper;
    private final ItemEncomendaFornecedorMapper itemEncomendaFornecedorMapper;

    public CompraService(
            EncomendaFornecedorRepository encomendaFornecedorRepo,
            ItemEncomendaFornecedorRepository itemEncomendaFornecedorRepo,
            FornecedorService fornecedorService,
            MateriaPrimaRepository matPrimaRepo,
            StockService stockService,
            FinanceiroService financeiroService,
            MoedaRepository moedaRepo,
            EncomendaFornecedorMapper encomendaFornecedorMapper,
            ItemEncomendaFornecedorMapper itemEncomendaFornecedorMapper) {
        this.encomendaFornecedorRepo = encomendaFornecedorRepo;
        this.itemEncomendaFornecedorRepo = itemEncomendaFornecedorRepo;
        this.fornecedorService = fornecedorService;
        this.matPrimaRepo = matPrimaRepo;
        this.stockService = stockService;
        this.financeiroService = financeiroService;
        this.moedaRepo = moedaRepo;
        this.encomendaFornecedorMapper = encomendaFornecedorMapper;
        this.itemEncomendaFornecedorMapper = itemEncomendaFornecedorMapper;
    }

    /**
     * Cria uma encomenda de fornecedor em rascunho.
     */
    @Transactional
    public EncomendaFornecedorResponseDTO gerarEncomendaRascunho(UUID fornecedorId, UUID moedaId) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL);

        Fornecedor fornecedor = fornecedorService.buscarFornecedorPorId(fornecedorId);

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
     * Adiciona um item ao rascunho e recalcula os totais com arredondamento monetário.
     */
    @Transactional
    public void adicionarItemEncomenda(UUID encomendaId, UUID materiaPrimaId, Double quantidade, Double precoUnitarioNet, Double taxaIva) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL);

        EncomendaFornecedor encomenda = buscarParaAtualizarOuFalhar(encomendaId);

        // Validar que está em RASCUNHO
        if (!EstadoEncomendaFornecedor.RASCUNHO.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas em rascunho podem ser modificadas");
        }

        MateriaPrima matPrima = matPrimaRepo.findById(materiaPrimaId)
                .orElseThrow(() -> new EntityNotFoundException("Matéria-prima não encontrada"));

        CalculationUtils.requirePositive(quantidade, "Quantidade");
        CalculationUtils.requirePositive(precoUnitarioNet, "Preço unitário");
        CalculationUtils.requirePercentage(taxaIva, "Taxa de IVA");
        Double valorSubtotal = CalculationUtils.subtotal(quantidade, precoUnitarioNet);
        Double valorIvaCalculado = CalculationUtils.vat(valorSubtotal, taxaIva);

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

        EncomendaFornecedor encomenda = buscarParaAtualizarOuFalhar(encomendaId);

        if (!EstadoEncomendaFornecedor.RASCUNHO.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas em rascunho podem ser confirmadas");
        }

        if (itemEncomendaFornecedorRepo.findByEncomendaId(encomendaId).isEmpty()) {
            throw new RuntimeException("Encomenda deve ter pelo menos um item");
        }

        encomenda.setEstado(EstadoEncomendaFornecedor.EFETIVA);
        return encomendaFornecedorMapper.toResponseDTO(encomendaFornecedorRepo.save(encomenda));
    }

    /**
     * Confirma o recebimento, adiciona as matérias-primas ao stock e regista a saída financeira.
     */
    @Transactional
    public EncomendaFornecedorResponseDTO confirmarRecebimento(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR, Cargo.RESPONSAVEL_LOGISTICA);

        EncomendaFornecedor encomenda = buscarParaAtualizarOuFalhar(encomendaId);

        if (!EstadoEncomendaFornecedor.EFETIVA.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas efetivas podem ser recebidas");
        }

        for (ItemEncomendaFornecedor item : itemEncomendaFornecedorRepo.findByEncomendaId(encomendaId)) {
            stockService.adicionarStockMateriaPrima(item.getMateriaPrima().getId(), item.getQuantidade());
        }

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

        EncomendaFornecedor encomenda = buscarParaAtualizarOuFalhar(encomendaId);

        if (!EstadoEncomendaFornecedor.RASCUNHO.equals(encomenda.getEstado())) {
            throw new RuntimeException("Apenas encomendas em rascunho podem ser atualizadas");
        }

        Fornecedor fornecedor = fornecedorService.buscarFornecedorPorId(fornecedorId);

        encomenda.setFornecedor(fornecedor);
        return encomendaFornecedorMapper.toResponseDTO(encomendaFornecedorRepo.save(encomenda));
    }

    @Transactional
    public void apagarEncomendaRascunho(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR, Cargo.ASSISTENTE_COMERCIAL);
        EncomendaFornecedor encomenda = buscarParaAtualizarOuFalhar(encomendaId);
        if (!EstadoEncomendaFornecedor.RASCUNHO.equals(encomenda.getEstado())) {
            throw new IllegalStateException("Só é possível eliminar encomendas em rascunho");
        }
        encomendaFornecedorRepo.deleteById(encomendaId);
    }

    /**
     * Anular encomenda
     */
    @Transactional
    public EncomendaFornecedorResponseDTO anumarEncomenda(UUID encomendaId) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);

        EncomendaFornecedor encomenda = buscarParaAtualizarOuFalhar(encomendaId);

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

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "data");

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

    private EncomendaFornecedor buscarParaAtualizarOuFalhar(UUID id) {
        return encomendaFornecedorRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Encomenda não encontrada com o ID: " + id));
    }

    /**
     * Recalcular totais da encomenda (somatório de itens)
     */
    private void recalcularTotaisEncomenda(UUID encomendaId) {
        EncomendaFornecedor encomenda = buscarPorIdOuFalhar(encomendaId);
        List<ItemEncomendaFornecedor> itens = itemEncomendaFornecedorRepo.findByEncomendaId(encomendaId);

        Double totalLiquido = CalculationUtils.money(itens.stream()
                .mapToDouble(item -> CalculationUtils.subtotal(item.getQuantidade(), item.getPrecoUnitarioNet()))
                .sum());

        Double totalIva = CalculationUtils.money(itens.stream()
                .mapToDouble(ItemEncomendaFornecedor::getValorIvaCalculado)
                .sum());

        Double totalFinal = CalculationUtils.total(totalLiquido, totalIva);

        encomenda.setTotalLiquido(totalLiquido);
        encomenda.setTotalIva(totalIva);
        encomenda.setTotalFinal(totalFinal);

        encomendaFornecedorRepo.save(encomenda);
    }

    /**
     * Listar encomendas com filtros (SimpleDTO)
     */
    public Page<EncomendaFornecedorSimpleDTO> listarEncomendasComFiltrosSimples(
            UUID fornecedorId,
            EstadoEncomendaFornecedor estado,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "data");

        return encomendaFornecedorRepo.findByFiltros(fornecedorId, estado, pageable)
                .map(encomendaFornecedorMapper::toSimpleDTO);
    }

    public EncomendaFornecedorDetailsDTO obterDetalhesEncomendaFornecedor(UUID encomendaId) {
        EncomendaFornecedor encomenda = buscarPorIdOuFalhar(encomendaId);

        List<ItemEncomendaFornecedorResponseDTO> itens = itemEncomendaFornecedorRepo.findByEncomendaId(encomendaId).stream()
                .map(itemEncomendaFornecedorMapper::toResponseDTO)
                .toList();

        return new EncomendaFornecedorDetailsDTO(
                encomenda.getId(),
                encomenda.getFornecedor() != null ? encomenda.getFornecedor().getId() : null,
                encomenda.getFornecedor() != null ? encomenda.getFornecedor().getNome() : null,
                encomenda.getData(),
                encomenda.getEstado(),
                encomenda.getTotalLiquido(),
                encomenda.getTotalIva(),
                encomenda.getTotalFinal(),
                encomenda.getMoeda() != null ? encomenda.getMoeda().getId() : null,
                encomenda.getMoeda() != null ? encomenda.getMoeda().getCodigo() : null,
                itens,
                encomenda.getCreatedAt(),
                encomenda.getUpdatedAt()
        );
    }
}
