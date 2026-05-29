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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

    @Transactional
    public int ajustarStock() {
        throw new UnsupportedOperationException("Use adicionar/subtrair stock indicando o item e a quantidade");
    }

    public int verificarAlertasStock() {
        return Math.toIntExact(contarMateriasAbaixoMinimo() + tipoPelletRepo.countBelowMinimumStock());
    }

    public long contarMateriasAbaixoMinimo() {
        return materiaPrimaRepo.countBelowMinimumStock();
    }

    public double calcularStockPelletsAtual() {
        return tipoPelletRepo.findAll().stream()
                .mapToDouble(tipo -> valorStock(tipo.getStockAtual()))
                .sum();
    }

    public double calcularStockPelletsMinimo() {
        return tipoPelletRepo.findAll().stream()
                .mapToDouble(tipo -> valorStock(tipo.getStockMinimo()))
                .sum();
    }

    public double calcularStockMateriasPrimasAtual() {
        return materiaPrimaRepo.findAll().stream()
                .mapToDouble(materia -> valorStock(materia.getStockAtual()))
                .sum();
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
        MateriaPrima materiaPrima = buscarMateriaPrimaPorId(materiaPrimaId);
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
        MateriaPrima materiaPrima = buscarMateriaPrimaPorId(materiaPrimaId);
        materiaPrima.setStockAtual(valorStock(materiaPrima.getStockAtual()) + quantidade);
        return materiaPrimaRepo.save(materiaPrima);
    }

    @Transactional
    public TipoPellet atualizarStockPellet(UUID tipoPelletId, Double quantidade, boolean isAdicao) {
        validarQuantidadePositiva(quantidade);
        TipoPellet tipoPellet = buscarTipoPelletPorId(tipoPelletId);
        double stockAtual = valorStock(tipoPellet.getStockAtual());
        double novoStock = isAdicao ? stockAtual + quantidade : stockAtual - quantidade;
        if (novoStock < 0) {
            throw new IllegalArgumentException("Stock insuficiente para o tipo de pellet");
        }
        tipoPellet.setStockAtual(novoStock);
        return tipoPelletRepo.save(tipoPellet);
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

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "nome";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

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

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "nome";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

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
    }

    private void normalizarStockTipoPellet(TipoPellet tipoPellet) {
        if (tipoPellet.getStockAtual() == null) {
            tipoPellet.setStockAtual(0.0);
        }
        if (tipoPellet.getStockMinimo() == null) {
            tipoPellet.setStockMinimo(0.0);
        }
    }

    private void validarQuantidadePositiva(Double quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
    }

    private double valorStock(Double valor) {
        return valor != null ? valor : 0.0;
    }
}
