package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.FormulaProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FormulaProducaoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FormulaSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.mapper.FormulaProducaoMapper;
import com.pelletsfactory.stock_manager.common.repositories.FormulaProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FormulaProducaoService {

    private final FormulaProducaoRepository formulaRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final FormulaProducaoMapper mapper;

    public FormulaProducaoService(
            FormulaProducaoRepository formulaRepo,
            TipoPelletRepository tipoPelletRepo,
            FormulaProducaoMapper mapper) {
        this.formulaRepo = formulaRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.mapper = mapper;
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

    /**
     * Atualizar fórmula
     */
    @Transactional
    public FormulaProducaoResponseDTO atualizarFormula(UUID id, FormulaProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        FormulaProducao formula = buscarPorIdOuFalhar(id);

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

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "nome";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

        Page<FormulaProducao> formulasPage = formulaRepo.findByFiltros(nome, ativa, tipoPelletId, pageable);

        return formulasPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar apenas fórmulas ativas
     */
    public Page<FormulaSimpleDTO> listarFormulasAtivas(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<FormulaProducao> formulasPage = formulaRepo.findAllAtivas(pageable);
        return formulasPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar fórmulas de um tipo de pellet
     */
    public Page<FormulaSimpleDTO> listarFormulasPorTipoPellet(UUID tipoPelletId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("nome").ascending());
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
}

