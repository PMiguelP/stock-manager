package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.MateriaPrimaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.mapper.MateriaPrimaMapper;
import com.pelletsfactory.stock_manager.common.mapper.TipoPelletMapper;
import jakarta.persistence.EntityNotFoundException;
import com.pelletsfactory.stock_manager.common.repositories.MateriaPrimaRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ProductionUnitUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

@Service
public class StockService {
    private final MateriaPrimaRepository materiaPrimaRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final MateriaPrimaMapper materiaPrimaMapper;
    private final TipoPelletMapper tipoPelletMapper;

    public StockService(MateriaPrimaRepository materiaPrimaRepo, TipoPelletRepository tipoPelletRepo,
                        MateriaPrimaMapper materiaPrimaMapper, TipoPelletMapper tipoPelletMapper) {
        this.materiaPrimaRepo = materiaPrimaRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.materiaPrimaMapper = materiaPrimaMapper;
        this.tipoPelletMapper = tipoPelletMapper;
    }

    @Transactional
    public MateriaPrima registarMateriaPrima(MateriaPrima materiaPrima) {
        if (materiaPrima == null) {
            throw new IllegalArgumentException("Matéria-prima é obrigatória");
        }
        normalizarStockMateriaPrima(materiaPrima);
        return materiaPrimaRepo.save(materiaPrima);
    }

    @Transactional
    public MateriaPrimaDetailsDTO criarMateriaPrima(MateriaPrimaRequestDTO dto) {
        MateriaPrima entity = materiaPrimaMapper.toEntity(dto);
        normalizarStockMateriaPrima(entity);
        MateriaPrima saved = materiaPrimaRepo.save(entity);
        return new MateriaPrimaDetailsDTO(
                saved.getId(),
                saved.getNome(),
                saved.getUnidade(),
                saved.getStockAtual(),
                saved.getStockMinimo(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    @Transactional
    public TipoPellet configurarTipoPellet(TipoPellet tipoPellet) {
        if (tipoPellet == null) {
            throw new IllegalArgumentException("Tipo de pellet é obrigatório");
        }
        normalizarStockTipoPellet(tipoPellet);
        return tipoPelletRepo.save(tipoPellet);
    }

    public int verificarAlertasStock() {
        return Math.toIntExact(contarMateriasAbaixoMinimo() + tipoPelletRepo.countBelowMinimumStock());
    }

    public boolean existemPelletsAbaixoMinimo() {
        return tipoPelletRepo.countBelowMinimumStock() > 0;
    }

    public long contarMateriasAbaixoMinimo() {
        return materiaPrimaRepo.countBelowMinimumStock();
    }

    public double calcularStockPelletsAtual() {
        return valorStock(tipoPelletRepo.sumCurrentStock());
    }

    public double calcularStockPelletsMinimo() {
        return valorStock(tipoPelletRepo.sumMinimumStock());
    }

    @Deprecated
    public double calcularStockMateriasPrimasAtual() {
        return calcularStockMateriasPrimasKg();
    }

    public double calcularStockMateriasPrimasKg() {
        return valorStock(materiaPrimaRepo.sumCurrentStockByUnit(ProductionUnitUtils.KILOGRAM));
    }


    public MateriaPrima buscarMateriaPrimaPorId(UUID id) {
        return materiaPrimaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Matéria-prima não encontrada"));
    }

    public TipoPellet buscarTipoPelletPorId(UUID id) {
        return tipoPelletRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de pellet não encontrado"));
    }

    @Transactional
    public MateriaPrima subtrairStockMateriaPrima(UUID materiaPrimaId, Double quantidade) {
        validarQuantidadePositiva(quantidade);
        MateriaPrima materiaPrima = buscarMateriaPrimaParaAtualizar(materiaPrimaId);
        double novoStock = valorStock(materiaPrima.getStockAtual()) - quantidade;
        if (novoStock < 0) {
            throw new IllegalArgumentException("Stock insuficiente para a matéria-prima");
        }
        materiaPrima.setStockAtual(novoStock);
        return materiaPrimaRepo.save(materiaPrima);
    }

    @Transactional
    public MateriaPrima adicionarStockMateriaPrima(UUID materiaPrimaId, Double quantidade) {
        validarQuantidadePositiva(quantidade);
        MateriaPrima materiaPrima = buscarMateriaPrimaParaAtualizar(materiaPrimaId);
        materiaPrima.setStockAtual(valorStock(materiaPrima.getStockAtual()) + quantidade);
        return materiaPrimaRepo.save(materiaPrima);
    }

    @Transactional
    private TipoPellet atualizarStockPellet(UUID tipoPelletId, Double quantidade, boolean isAdicao) {
        validarQuantidadePositiva(quantidade);
        TipoPellet tipoPellet = buscarTipoPelletParaAtualizar(tipoPelletId);
        double stockAtual = valorStock(tipoPellet.getStockAtual());
        double novoStock = isAdicao ? stockAtual + quantidade : stockAtual - quantidade;
        if (novoStock < 0) {
            throw new IllegalArgumentException("Stock insuficiente para o tipo de pellet");
        }
        tipoPellet.setStockAtual(novoStock);
        return tipoPelletRepo.save(tipoPellet);
    }

    /**
     * Ajusta stock de pellets sem permitir valores negativos.
     */
    @Transactional
    public TipoPellet adicionarStockPellet(UUID tipoPelletId, Double quantidade) {
        return atualizarStockPellet(tipoPelletId, quantidade, true);
    }

    @Transactional
    public TipoPellet subtrairStockPellet(UUID tipoPelletId, Double quantidade) {
        return atualizarStockPellet(tipoPelletId, quantidade, false);
    }

    /**
     * Listar matérias-primas com paginação e filtros (SimpleDTO)
     */
    public Page<MateriaPrimaSimpleDTO> listarMateriasPrimasComFiltros(
            int page,
            int pageSize,
            String nome,
            String unidade,
            String sortBy,
            String direction) {

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "nome");

        return materiaPrimaRepo.findByFiltros(nome, unidade, pageable)
                .map(materiaPrimaMapper::toSimpleDTO);
    }

    /**
     * Listar tipos de pellet com paginação e filtros (SimpleDTO)
     */
    public Page<TipoPelletSimpleDTO> listarTiposPelletComFiltros(
            int page,
            int pageSize,
            String nome,
            Double diametroMm,
            String sortBy,
            String direction) {

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "nome");

        return tipoPelletRepo.findByFiltros(nome, diametroMm, pageable)
                .map(tipoPelletMapper::toSimpleDTO);
    }

    public MateriaPrimaDetailsDTO obterDetalhesMateriaPrima(UUID materiaPrimaId) {
        MateriaPrima materiaPrima = materiaPrimaRepo.findById(materiaPrimaId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Matéria-prima não encontrada"));

        return new MateriaPrimaDetailsDTO(
                materiaPrima.getId(),
                materiaPrima.getNome(),
                materiaPrima.getUnidade(),
                materiaPrima.getStockAtual(),
                materiaPrima.getStockMinimo(),
                materiaPrima.getCreatedAt(),
                materiaPrima.getUpdatedAt()
        );
    }

    public TipoPelletDetailsDTO obterDetalhesTipoPellet(UUID tipoPelletId) {
        TipoPellet tipoPellet = tipoPelletRepo.findById(tipoPelletId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tipo de pellet não encontrado"));

        return new TipoPelletDetailsDTO(
                tipoPellet.getId(),
                tipoPellet.getNome(),
                tipoPellet.getDiametroMm(),
                tipoPellet.getPoderCalorifico(),
                tipoPellet.getStockAtual(),
                tipoPellet.getStockMinimo(),
                tipoPellet.getCustoAtualPorKg(),
                tipoPellet.getMoeda() != null ? tipoPellet.getMoeda().getId() : null,
                tipoPellet.getMoeda() != null ? tipoPellet.getMoeda().getCodigo() : null,
                tipoPellet.getCreatedAt(),
                tipoPellet.getUpdatedAt()
        );
    }

    private void normalizarStockMateriaPrima(MateriaPrima materiaPrima) {
        if (materiaPrima.getStockAtual() == null) {
            materiaPrima.setStockAtual(0.0);
        }
        if (materiaPrima.getStockMinimo() == null) {
            materiaPrima.setStockMinimo(0.0);
        }
        validarQuantidadeNaoNegativa(materiaPrima.getStockAtual(), "Stock atual");
        validarQuantidadeNaoNegativa(materiaPrima.getStockMinimo(), "Stock mínimo");
    }

    private void normalizarStockTipoPellet(TipoPellet tipoPellet) {
        if (tipoPellet.getStockAtual() == null) {
            tipoPellet.setStockAtual(0.0);
        }
        if (tipoPellet.getStockMinimo() == null) {
            tipoPellet.setStockMinimo(0.0);
        }
        validarQuantidadeNaoNegativa(tipoPellet.getStockAtual(), "Stock atual");
        validarQuantidadeNaoNegativa(tipoPellet.getStockMinimo(), "Stock mínimo");
    }

    private void validarQuantidadePositiva(Double quantidade) {
        ValidationUtils.requirePositive(quantidade, "Quantidade");
    }

    private void validarQuantidadeNaoNegativa(Double quantidade, String campo) {
        ValidationUtils.requireNonNegative(quantidade, campo);
    }

    /**
     * Bloqueia o registo enquanto altera o stock para evitar que duas operações
     * concorrentes se sobreponham e percam quantidades.
     */
    private MateriaPrima buscarMateriaPrimaParaAtualizar(UUID id) {
        return materiaPrimaRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Matéria-prima não encontrada"));
    }

    private TipoPellet buscarTipoPelletParaAtualizar(UUID id) {
        return tipoPelletRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de pellet não encontrado"));
    }

    private double valorStock(Double valor) {
        return valor != null ? valor : 0.0;
    }
}
