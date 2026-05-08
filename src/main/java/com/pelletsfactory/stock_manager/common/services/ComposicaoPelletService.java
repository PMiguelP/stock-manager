package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.ComposicaoPelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ComposicaoPelletResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.ComposicaoPellet;
import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.mapper.ComposicaoPelletMapper;
import com.pelletsfactory.stock_manager.common.repositories.ComposicaoPelletRepository;
import com.pelletsfactory.stock_manager.common.repositories.FormulaProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.MateriaPrimaRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ComposicaoPelletService {

    private final ComposicaoPelletRepository composicaoRepo;
    private final FormulaProducaoRepository formulaRepo;
    private final MateriaPrimaRepository matPrimaRepo;
    private final ComposicaoPelletMapper mapper;

    public ComposicaoPelletService(
            ComposicaoPelletRepository composicaoRepo,
            FormulaProducaoRepository formulaRepo,
            MateriaPrimaRepository matPrimaRepo,
            ComposicaoPelletMapper mapper) {
        this.composicaoRepo = composicaoRepo;
        this.formulaRepo = formulaRepo;
        this.matPrimaRepo = matPrimaRepo;
        this.mapper = mapper;
    }

    /**
     * Adicionar matéria-prima à fórmula
     */
    @Transactional
    public ComposicaoPelletResponseDTO adicionarMateriaPrimaAFormula(ComposicaoPelletRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        // Buscar fórmula e matéria-prima
        FormulaProducao formula = formulaRepo.findById(dto.formulaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Fórmula não encontrada"
                ));

        MateriaPrima materiaPrima = matPrimaRepo.findById(dto.materiaPrimaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Matéria-prima não encontrada"
                ));

        // Criar composição
        ComposicaoPellet composicao = mapper.toEntity(dto);
        composicao.setFormulaProducao(formula);
        composicao.setMateriaPrima(materiaPrima);

        ComposicaoPellet saved = composicaoRepo.save(composicao);
        return mapper.toResponseDTO(saved);
    }

    /**
     * Atualizar quantidade de matéria-prima na fórmula
     */
    @Transactional
    public ComposicaoPelletResponseDTO atualizarComposicao(UUID id, ComposicaoPelletRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        ComposicaoPellet composicao = buscarPorIdOuFalhar(id);

        mapper.updateEntityFromDTO(dto, composicao);

        ComposicaoPellet updated = composicaoRepo.save(composicao);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Remover matéria-prima da fórmula
     */
    @Transactional
    public void removerMateriaPrimaDeFormula(UUID id) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        ComposicaoPellet composicao = buscarPorIdOuFalhar(id);
        composicaoRepo.delete(composicao);
    }

    /**
     * Listar composição de uma fórmula
     */
    public List<ComposicaoPelletResponseDTO> listarComposicaoDeFormula(UUID formulaId) {
        return composicaoRepo.findByFormulaId(formulaId).stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Listar composição de uma fórmula (com paginação)
     */
    public Page<ComposicaoPelletResponseDTO> listarComposicaoDeFrmulaPaginado(UUID formulaId, int page, int pageSize) {
        return listarComposicaoDeFormulaPaginado(formulaId, page, pageSize, "createdAt", "DESC");
    }

    /**
     * Listar composição de uma fórmula (com paginação e ordenação)
     */
    public Page<ComposicaoPelletResponseDTO> listarComposicaoDeFormulaPaginado(
            UUID formulaId,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        org.springframework.data.domain.Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, org.springframework.data.domain.Sort.by(dir, sortBy));
        Page<ComposicaoPellet> composicaoPage = composicaoRepo.findByFormulaId(formulaId, pageable);
        return composicaoPage.map(mapper::toResponseDTO);
    }

    /**
     * Contar composições de uma fórmula
     */
    public long contarComposicoesDaFormula(UUID formulaId) {
        return composicaoRepo.findByFormulaId(formulaId).size();
    }

    /**
     * Verificar se uma matéria-prima já está na fórmula
     */
    public boolean existeMateriaPrimaEmFormula(UUID formulaId, UUID materiaPrimaId) {
        return composicaoRepo.findByFormulaId(formulaId).stream()
                .anyMatch(c -> c.getMateriaPrima().getId().equals(materiaPrimaId));
    }

    /**
     * Obter por ID
     */
    public ComposicaoPelletResponseDTO buscarPorId(UUID id) {
        ComposicaoPellet composicao = buscarPorIdOuFalhar(id);
        return mapper.toResponseDTO(composicao);
    }

    /**
     * Buscar entidade ou falhar
     */
    public ComposicaoPellet buscarPorIdOuFalhar(UUID id) {
        return composicaoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Composição não encontrada com ID: " + id
                ));
    }
}
