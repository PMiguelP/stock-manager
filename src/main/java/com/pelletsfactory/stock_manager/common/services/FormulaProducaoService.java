package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.FormulaProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.ComposicaoPelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FormulaProducaoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FormulaSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.mapper.FormulaProducaoMapper;
import com.pelletsfactory.stock_manager.common.repositories.FormulaProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Map;

@Service
public class FormulaProducaoService {

    private final FormulaProducaoRepository formulaRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final OrdemProducaoRepository ordemRepo;
    private final ComposicaoPelletService composicaoService;
    private final FormulaProducaoMapper mapper;

    public FormulaProducaoService(
            FormulaProducaoRepository formulaRepo,
            TipoPelletRepository tipoPelletRepo,
            OrdemProducaoRepository ordemRepo,
            ComposicaoPelletService composicaoService,
            FormulaProducaoMapper mapper) {
        this.formulaRepo = formulaRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.ordemRepo = ordemRepo;
        this.composicaoService = composicaoService;
        this.mapper = mapper;
    }

    @Transactional
    public FormulaProducaoResponseDTO criarFormula(
            FormulaProducaoRequestDTO dto,
            Map<UUID, Double> quantidadesPorMateriaPrima) {
        FormulaProducaoResponseDTO formula = criarFormula(dto);
        guardarComposicao(formula.id(), quantidadesPorMateriaPrima);
        return buscarPorId(formula.id());
    }

    /**
     * Criar nova fórmula de produção
     */
    @Transactional
    public FormulaProducaoResponseDTO criarFormula(FormulaProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        // Buscar tipo de pellet
        TipoPellet tipoPellet = tipoPelletRepo.findById(dto.tipoPelletId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tipo de pellet não encontrado"
                ));

        // Criar fórmula
        FormulaProducao formula = mapper.toEntity(dto);
        formula.setTipoPellet(tipoPellet);

        FormulaProducao saved = formulaRepo.save(formula);
        return mapper.toResponseDTO(saved);
    }

    @Transactional
    public FormulaProducaoResponseDTO atualizarFormula(
            UUID id,
            FormulaProducaoRequestDTO dto,
            Map<UUID, Double> quantidadesPorMateriaPrima) {
        atualizarFormula(id, dto);
        composicaoService.listarComposicaoDeFormula(id)
                .forEach(composicao -> composicaoService.removerMateriaPrimaDeFormula(composicao.id()));
        guardarComposicao(id, quantidadesPorMateriaPrima);
        return buscarPorId(id);
    }

    /**
     * Atualizar fórmula
     */
    @Transactional
    public FormulaProducaoResponseDTO atualizarFormula(UUID id, FormulaProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        FormulaProducao formula = buscarPorIdOuFalhar(id);
        validarFormulaSemOrdens(id);
        if (!formula.getTipoPellet().getId().equals(dto.tipoPelletId())) {
            throw new IllegalArgumentException("Não é possível trocar o tipo de pellet de uma fórmula existente");
        }

        mapper.updateEntityFromDTO(dto, formula);

        FormulaProducao updated = formulaRepo.save(formula);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Eliminar fórmula
     */
    @Transactional
    public void apagarFormula(UUID id) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        FormulaProducao formula = buscarPorIdOuFalhar(id);
        validarFormulaSemOrdens(id);
        formulaRepo.delete(formula);
    }

    /**
     * Listar com paginação e filtros
     */
    public Page<FormulaSimpleDTO> listarFormulasComFiltros(
            int page,
            int pageSize,
            String nome,
            Boolean ativa,
            UUID tipoPelletId,
            String sortBy,
            String direction) {

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "nome");

        Page<FormulaProducao> formulasPage = formulaRepo.findByFiltros(nome, ativa, tipoPelletId, pageable);

        return formulasPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar apenas fórmulas ativas
     */
    public Page<FormulaSimpleDTO> listarFormulasAtivas(int page, int pageSize) {
        Pageable pageable = PageableUtils.create(page, pageSize, "nome", "ASC", "nome");
        Page<FormulaProducao> formulasPage = formulaRepo.findAllAtivas(pageable);
        return formulasPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar fórmulas de um tipo de pellet
     */
    public Page<FormulaSimpleDTO> listarFormulasPorTipoPellet(UUID tipoPelletId, int page, int pageSize) {
        Pageable pageable = PageableUtils.create(page, pageSize, "nome", "ASC", "nome");
        Page<FormulaProducao> formulasPage = formulaRepo.findByTipoPelletId(tipoPelletId, pageable);
        return formulasPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar todos (para combobox)
     */
    public java.util.List<FormulaSimpleDTO> listarTodosSimples() {
        return formulaRepo.findAll()
                .stream()
                .map(mapper::toSimpleDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Obter por ID
     */
    public FormulaProducaoResponseDTO buscarPorId(UUID id) {
        FormulaProducao formula = formulaRepo.findByIdWithComposicao(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Fórmula de produção não encontrada com ID: " + id
                ));
        return mapper.toResponseDTO(formula);
    }

    /**
     * Buscar entidade ou falhar
     */
    public FormulaProducao buscarPorIdOuFalhar(UUID id) {
        return formulaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Fórmula de produção não encontrada com ID: " + id
                ));
    }

    private void validarFormulaSemOrdens(UUID formulaId) {
        if (ordemRepo.existsByFormulaId(formulaId)) {
            throw new IllegalStateException("Não é possível alterar uma fórmula que já foi usada numa ordem de produção");
        }
    }

    private void guardarComposicao(UUID formulaId, Map<UUID, Double> quantidadesPorMateriaPrima) {
        if (quantidadesPorMateriaPrima == null || quantidadesPorMateriaPrima.isEmpty()) {
            throw new IllegalArgumentException("A fórmula deve ter pelo menos uma matéria-prima");
        }
        double total = quantidadesPorMateriaPrima.values().stream()
                .mapToDouble(quantidade -> quantidade == null ? Double.NaN : quantidade)
                .sum();
        if (!Double.isFinite(total) || Math.abs(total - 1.0) > 0.000001) {
            throw new IllegalArgumentException("A composição total da fórmula deve ser exatamente 1 kg");
        }
        quantidadesPorMateriaPrima.forEach((materiaPrimaId, quantidade) ->
                composicaoService.adicionarMateriaPrimaAFormula(
                        new ComposicaoPelletRequestDTO(formulaId, materiaPrimaId, quantidade)
                )
        );
    }
}
