package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Fornecedor;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.mapper.MateriaPrimaMapper;
import com.pelletsfactory.stock_manager.common.mapper.TipoPelletMapper;
import com.pelletsfactory.stock_manager.common.repositories.FornecedorRepository;
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
    private final FornecedorRepository forncedorRepo;
    private final MateriaPrimaMapper materiaPrimaMapper;
    private final TipoPelletMapper tipoPelletMapper;

    public StockService(MateriaPrimaRepository materiaPrimaRepo, TipoPelletRepository tipoPelletRepo, FornecedorRepository forncedorRepo,
                        MateriaPrimaMapper materiaPrimaMapper, TipoPelletMapper tipoPelletMapper) {
        this.materiaPrimaRepo = materiaPrimaRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.forncedorRepo = forncedorRepo;
        this.materiaPrimaMapper = materiaPrimaMapper;
        this.tipoPelletMapper = tipoPelletMapper;
    }

    @Transactional
    public MateriaPrima registarMateriaPrima(MateriaPrima materiaPrima) {
        // TODO: Regista nova matéria-prima no sistema e retorna a MateriaPrima criada
        return new MateriaPrima();
    }

    @Transactional
    public Fornecedor registarFornecedor(Fornecedor fornecedor) {
        // TODO: Regista novo fornecedor e retorna o Fornecedor criado
        return new Fornecedor();
    }


    @Transactional
    public TipoPellet configurarTipoPellet(TipoPellet tipoPellet) {
        // TODO: Define novo tipo de pellet (produto final) e retorna o TipoPellet criado
        return new TipoPellet();
    }

    @Transactional
    public int ajustarStock() {
        return 0;
    }

    public int verificarAlertasStock() {
        return 0; //TODO vai retornar uma lista com o stock abaixo do minimo
    }


    public MateriaPrima buscarMateriaPrimaPorId(UUID id) {
        // TODO: Busca matéria-prima por ID. Usado pelo ProducaoService e CompraService. Retorna MateriaPrima ou lança exceção
        return new MateriaPrima();
    }

    public TipoPellet buscarTipoPelletPorId(UUID id) {
        // TODO: Busca tipo de pellet por ID. Usado pelo VendaService e ProducaoService. Retorna TipoPellet ou lança exceção
        return new TipoPellet();
    }

    @Transactional
    public MateriaPrima subtrairStockMateriaPrima(UUID materiaPrimaId, Double quantidade) {
        // TODO: @Transactional - Reduz stock de matéria-prima e verifica se ficou abaixo do mínimo. Retorna MateriaPrima atualizada
        return new MateriaPrima();
    }

    @Transactional
    public MateriaPrima adicionarStockMateriaPrima(UUID materiaPrimaId, Double quantidade) {
        // TODO: Adiciona stock quando encomenda de fornecedor é recebida. Retorna MateriaPrima atualizada
        return new MateriaPrima();
    }

    @Transactional
    public TipoPellet atualizarStockPellet(UUID tipoPelletId, Double quantidade, boolean isAdicao) {
        // TODO: @Transactional - Gere entrada (produção) ou saída (venda) de pellets. Se isAdicao=true adiciona, senão subtrai. Retorna TipoPellet atualizado
        return new TipoPellet();
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
}
