package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.OrdemProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.OrdemProducaoDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.OrdemProducaoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.OrdemProducaoSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.*;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import com.pelletsfactory.stock_manager.common.mapper.OrdemProducaoMapper;
import com.pelletsfactory.stock_manager.common.repositories.*;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrdemProducaoService {
    
    private final OrdemProducaoRepository ordemRepo;
    private final FuncionarioRepository funcRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final FormulaProducaoRepository formulaRepo;
    private final ConsumoProducaoRepository consumoRepo;
    private final ComposicaoPelletRepository composicaoRepo;
    private final OrdemProducaoMapper mapper;

    public OrdemProducaoService(
            OrdemProducaoRepository ordemRepo,
            FuncionarioRepository funcRepo,
            TipoPelletRepository tipoPelletRepo,
            FormulaProducaoRepository formulaRepo,
            ConsumoProducaoRepository consumoRepo,
            ComposicaoPelletRepository composicaoRepo,
            OrdemProducaoMapper mapper) {
        this.ordemRepo = ordemRepo;
        this.funcRepo = funcRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.formulaRepo = formulaRepo;
        this.consumoRepo = consumoRepo;
        this.composicaoRepo = composicaoRepo;
        this.mapper = mapper;
    }

    /**
     * Criar nova ordem de produção
     */
    @Transactional
    public OrdemProducaoResponseDTO criarOrdem(OrdemProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO, Cargo.ADMINISTRADOR);

        // Validar entidades referenciadas
        Funcionario funcionario = buscarFuncionarioOuFalhar(dto.funcionarioId());
        TipoPellet tipoPellet = buscarTipoPelletOuFalhar(dto.tipoPelletId());
        FormulaProducao formula = buscarFormulaOuFalhar(dto.formulaId());

        // Validar que a fórmula corresponde ao tipo de pellet
        if (!formula.getTipoPellet().getId().equals(tipoPellet.getId())) {
            throw new IllegalArgumentException(
                    "A fórmula selecionada não corresponde ao tipo de pellet"
            );
        }

        // Validar stock de matérias-primas
        validarStockMateriasParaProducao(formula, dto.quantidadePlaneada());

        // Criar entidade
        OrdemProducao ordem = mapper.toEntity(dto);
        ordem.setTipoPellet(tipoPellet);
        ordem.setFuncionario(funcionario);
        ordem.setFormula(formula);

        OrdemProducao saved = ordemRepo.save(ordem);
        return mapper.toResponseDTO(saved);
    }

    /**
     * Atualizar ordem existente
     */
    @Transactional
    public OrdemProducaoResponseDTO atualizarOrdem(UUID id, OrdemProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        OrdemProducao ordem = buscarPorIdOuFalhar(id);

        // Não pode atualizar ordens já concluídas
        if (ordem.getEstado() == EstadoOrdemProducao.CONCLUIDA) {
            throw new IllegalArgumentException("Não é possível atualizar uma ordem concluída");
        }

        mapper.updateEntityFromDTO(dto, ordem);
        OrdemProducao updated = ordemRepo.save(ordem);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Mudar estado da ordem (PENDENTE -> EM_PRODUCAO -> CONCLUIDA)
     */
    @Transactional
    public OrdemProducaoResponseDTO mudarEstado(UUID id, String novoEstado) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        OrdemProducao ordem = buscarPorIdOuFalhar(id);
        EstadoOrdemProducao estado = EstadoOrdemProducao.valueOf(novoEstado);

        // Validar transição de estado
        validarTransicaoEstado(ordem.getEstado(), estado);

        if (estado == EstadoOrdemProducao.EM_PRODUCAO) {
            ordem.setDataInicio(Instant.now());
        } else if (estado == EstadoOrdemProducao.CONCLUIDA) {
            ordem.setDataFim(Instant.now());
        }

        ordem.setEstado(estado);
        OrdemProducao updated = ordemRepo.save(ordem);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Registar quantidade real produzida
     */
    @Transactional
    public OrdemProducaoResponseDTO registarProducaoReal(UUID id, Double quantidadeProduzida) {
        SecurityUtils.checkPermission(Cargo.OPERADOR_PRODUCAO);

        OrdemProducao ordem = buscarPorIdOuFalhar(id);

        if (ordem.getEstado() != EstadoOrdemProducao.EM_PRODUCAO) {
            throw new IllegalArgumentException(
                    "Apenas ordens em produção podem registar quantidade real"
            );
        }

        if (quantidadeProduzida > ordem.getQuantidadePlaneada()) {
            throw new IllegalArgumentException(
                    "Quantidade produzida não pode exceder quantidade planeada"
            );
        }

        ordem.setQuantidadeProduzidaReal(quantidadeProduzida);
        OrdemProducao updated = ordemRepo.save(ordem);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Eliminar ordem (apenas se PENDENTE)
     */
    @Transactional
    public void apagarOrdem(UUID id) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        OrdemProducao ordem = buscarPorIdOuFalhar(id);

        if (ordem.getEstado() != EstadoOrdemProducao.PENDENTE) {
            throw new IllegalArgumentException(
                    "Apenas ordens pendentes podem ser eliminadas"
            );
        }

        ordemRepo.delete(ordem);
    }

    /**
     * Listar com paginação e filtros
     */
    public Page<OrdemProducaoSimpleDTO> listarOrdensComFiltros(
            int page,
            int pageSize,
            EstadoOrdemProducao estado,
            UUID tipoPelletId,
            UUID funcionarioId,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "dataInicio";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

        Page<OrdemProducao> ordensPage = ordemRepo.findByFiltros(estado, tipoPelletId, funcionarioId, pageable);

        return ordensPage.map(mapper::toSimpleDTO);
    }

    /**
     * Obter detalhes completos
     */
    public OrdemProducaoDetailsDTO obterDetalhes(UUID id) {
        OrdemProducao ordem = ordemRepo.findByIdWithDetalhes(id)
                .orElse(buscarPorIdOuFalhar(id));
        return mapper.toDetailsDTO(ordem);
    }

    /**
     * Obter por ID
     */
    public OrdemProducaoResponseDTO buscarPorId(UUID id) {
        OrdemProducao ordem = buscarPorIdOuFalhar(id);
        return mapper.toResponseDTO(ordem);
    }

    /**
     * Buscar entidade ou falhar
     */
    public OrdemProducao buscarPorIdOuFalhar(UUID id) {
        return ordemRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ordem de produção não encontrada com ID: " + id
                ));
    }

    /**
     * Contar ordens por estado
     */
    public Long contarPorEstado(EstadoOrdemProducao estado) {
        return ordemRepo.countByEstado(estado);
    }

    /**
     * Listar todos (para combobox)
     */
    public java.util.List<OrdemProducaoSimpleDTO> listarTodosSimples() {
        return ordemRepo.findAll()
                .stream()
                .map(mapper::toSimpleDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * PauseProduction: Pausar ordem de produção
     * Apenas RESPONSAVEL_PRODUCAO
     */
    @Transactional
    public OrdemProducaoResponseDTO pausarProducao(UUID id, String motivo) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        OrdemProducao ordem = buscarPorIdOuFalhar(id);

        if (ordem.getEstado() != EstadoOrdemProducao.EM_PRODUCAO) {
            throw new RuntimeException("Apenas ordens em produção podem ser pausadas");
        }

        ordem.setEstado(EstadoOrdemProducao.PAUSADA);
        // Opcional: registar motivo em um campo (se existir na DB)

        OrdemProducao updated = ordemRepo.save(ordem);
        return mapper.toResponseDTO(updated);
    }

    /**
     * ResumeProduction: Retomar ordem pausada
     * Apenas RESPONSAVEL_PRODUCAO
     */
    @Transactional
    public OrdemProducaoResponseDTO retomar(UUID id) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        OrdemProducao ordem = buscarPorIdOuFalhar(id);

        if (ordem.getEstado() != EstadoOrdemProducao.PAUSADA) {
            throw new RuntimeException("Apenas ordens pausadas podem ser retomadas");
        }

        // Validar que ainda há stock disponível
        validarStockMateriasParaProducao(ordem.getFormula(), ordem.getQuantidadePlaneada());

        ordem.setEstado(EstadoOrdemProducao.EM_PRODUCAO);

        OrdemProducao updated = ordemRepo.save(ordem);
        return mapper.toResponseDTO(updated);
    }

    // ===== MÉTODOS PRIVADOS =====

    private Funcionario buscarFuncionarioOuFalhar(UUID id) {
        return funcRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Funcionário não encontrado com ID: " + id
                ));
    }

    private TipoPellet buscarTipoPelletOuFalhar(UUID id) {
        return tipoPelletRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tipo de pellet não encontrado com ID: " + id
                ));
    }

    private FormulaProducao buscarFormulaOuFalhar(UUID id) {
        return formulaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Fórmula de produção não encontrada com ID: " + id
                ));
    }

    private void validarStockMateriasParaProducao(FormulaProducao formula, Double quantidade) {
        // Obter composição
        var composicoes = composicaoRepo.findByFormulaId(formula.getId());

        // Para cada matéria-prima na fórmula, validar stock
        for (ComposicaoPellet comp : composicoes) {
            Double necessario = comp.getQuantidadePorKg() * quantidade;
            Double stockAtual = comp.getMateriaPrima().getStockAtual();

            if (stockAtual < necessario) {
                throw new IllegalArgumentException(
                        "Stock insuficiente de " + comp.getMateriaPrima().getNome() +
                                ". Necessário: " + necessario + "kg, Disponível: " + stockAtual + "kg"
                );
            }
        }
    }

    private void validarTransicaoEstado(EstadoOrdemProducao estadoAtual, EstadoOrdemProducao novoEstado) {
        // PENDENTE -> EM_PRODUCAO, ANULADA
        // EM_PRODUCAO -> CONCLUIDA, PAUSADA, ANULADA
        // PAUSADA -> EM_PRODUCAO, ANULADA
        // CONCLUIDA, ANULADA -> não permite transição

        if (estadoAtual == EstadoOrdemProducao.CONCLUIDA || estadoAtual == EstadoOrdemProducao.ANULADA) {
            throw new IllegalArgumentException(
                    "Não é possível mudar estado de uma ordem " + estadoAtual.name()
            );
        }

        boolean transiçãoValida = switch (estadoAtual) {
            case PENDENTE -> novoEstado == EstadoOrdemProducao.EM_PRODUCAO || novoEstado == EstadoOrdemProducao.ANULADA;
            case EM_PRODUCAO -> novoEstado == EstadoOrdemProducao.CONCLUIDA || novoEstado == EstadoOrdemProducao.PAUSADA || novoEstado == EstadoOrdemProducao.ANULADA;
            case PAUSADA -> novoEstado == EstadoOrdemProducao.EM_PRODUCAO || novoEstado == EstadoOrdemProducao.ANULADA;
            default -> false;
        };

        if (!transiçãoValida) {
            throw new IllegalArgumentException(
                    "Transição de estado inválida: " + estadoAtual.name() + " -> " + novoEstado.name()
            );
        }
    }
}
