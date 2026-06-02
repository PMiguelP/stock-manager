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
import com.pelletsfactory.stock_manager.common.utils.ProductionUnitUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class OrdemProducaoService {

    private final OrdemProducaoRepository ordemRepo;
    private final FuncionarioRepository funcRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final FormulaProducaoRepository formulaRepo;
    private final ConsumoProducaoRepository consumoRepo;
    private final ComposicaoPelletRepository composicaoRepo;
    private final LotePelletRepository loteRepo;
    private final MateriaPrimaRepository materiaPrimaRepo;
    private final OrdemProducaoMapper mapper;

    public OrdemProducaoService(
            OrdemProducaoRepository ordemRepo,
            FuncionarioRepository funcRepo,
            TipoPelletRepository tipoPelletRepo,
            FormulaProducaoRepository formulaRepo,
            ConsumoProducaoRepository consumoRepo,
            ComposicaoPelletRepository composicaoRepo,
            LotePelletRepository loteRepo,
            MateriaPrimaRepository materiaPrimaRepo,
            OrdemProducaoMapper mapper) {
        this.ordemRepo = ordemRepo;
        this.funcRepo = funcRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.formulaRepo = formulaRepo;
        this.consumoRepo = consumoRepo;
        this.composicaoRepo = composicaoRepo;
        this.loteRepo = loteRepo;
        this.materiaPrimaRepo = materiaPrimaRepo;
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

        validarFormulaProntaParaProducao(formula);
        validarStockMateriasParaProducao(formula, dto.quantidadePlaneada(), null);

        // Criar entidade
        OrdemProducao ordem = mapper.toEntity(dto);
        ordem.setTipoPellet(tipoPellet);
        ordem.setFuncionario(funcionario);
        ordem.setFormula(formula);
        ordem.setEstado(EstadoOrdemProducao.PENDENTE);
        ordem.setQuantidadeProduzidaReal(0.0);

        OrdemProducao saved = ordemRepo.save(ordem);
        return mapper.toResponseDTO(saved);
    }

    /**
     * Atualizar ordem existente
     */
    @Transactional
    public OrdemProducaoResponseDTO atualizarOrdem(UUID id, OrdemProducaoRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        OrdemProducao ordem = buscarPorIdParaAtualizarOuFalhar(id);

        if (ordem.getEstado() != EstadoOrdemProducao.PENDENTE) {
            throw new IllegalArgumentException("Apenas ordens pendentes podem ser atualizadas");
        }

        Funcionario funcionario = buscarFuncionarioOuFalhar(dto.funcionarioId());
        TipoPellet tipoPellet = buscarTipoPelletOuFalhar(dto.tipoPelletId());
        FormulaProducao formula = buscarFormulaOuFalhar(dto.formulaId());

        if (!formula.getTipoPellet().getId().equals(tipoPellet.getId())) {
            throw new IllegalArgumentException(
                    "A fórmula selecionada não corresponde ao tipo de pellet"
            );
        }

        validarFormulaProntaParaProducao(formula);
        validarStockMateriasParaProducao(formula, dto.quantidadePlaneada(), ordem.getId());

        EstadoOrdemProducao estadoOriginal = ordem.getEstado();
        Double quantidadeProduzidaOriginal = ordem.getQuantidadeProduzidaReal();
        ordem.setTipoPellet(tipoPellet);
        ordem.setFuncionario(funcionario);
        ordem.setFormula(formula);
        mapper.updateEntityFromDTO(dto, ordem);
        ordem.setEstado(estadoOriginal);
        ordem.setQuantidadeProduzidaReal(quantidadeProduzidaOriginal);
        OrdemProducao updated = ordemRepo.save(ordem);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Mudar estado da ordem (PENDENTE -> EM_PRODUCAO -> CONCLUIDA)
     */
    @Transactional
    public OrdemProducaoResponseDTO mudarEstado(UUID id, String novoEstado) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        OrdemProducao ordem = buscarPorIdParaAtualizarOuFalhar(id);
        EstadoOrdemProducao estado = EstadoOrdemProducao.valueOf(novoEstado);

        // Validar transição de estado
        validarTransicaoEstado(ordem.getEstado(), estado);

        if (estado == EstadoOrdemProducao.EM_PRODUCAO) {
            validarStockMateriasParaProducao(ordem.getFormula(), ordem.getQuantidadePlaneada(), ordem.getId());
            ordem.setDataInicio(Instant.now());
        } else if (estado == EstadoOrdemProducao.CONCLUIDA) {
            validarConclusao(ordem);
            ordem.setDataFim(Instant.now());
        } else if (estado == EstadoOrdemProducao.ANULADA) {
            validarAnulacao(ordem);
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

        OrdemProducao ordem = buscarPorIdParaAtualizarOuFalhar(id);

        if (ordem.getEstado() != EstadoOrdemProducao.EM_PRODUCAO) {
            throw new IllegalArgumentException(
                    "Apenas ordens em produção podem registar quantidade real"
            );
        }

        if (quantidadeProduzida == null || !Double.isFinite(quantidadeProduzida) || quantidadeProduzida < 0) {
            throw new IllegalArgumentException("Quantidade produzida deve ser um valor não negativo");
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

        OrdemProducao ordem = buscarPorIdParaAtualizarOuFalhar(id);

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

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "dataInicio");

        Page<OrdemProducao> ordensPage = ordemRepo.findByFiltros(estado, tipoPelletId, funcionarioId, pageable);

        return ordensPage.map(mapper::toSimpleDTO);
    }

    /**
     * Obter detalhes completos
     */
    @Transactional
    public OrdemProducaoDetailsDTO obterDetalhes(UUID id) {
        OrdemProducao ordem = ordemRepo.findByIdWithDetalhes(id)
                .orElse(buscarPorIdOuFalhar(id));
        return mapper.toDetailsDTO(ordem);
    }

    /**
     * Obter por ID
     */
    public OrdemProducaoResponseDTO buscarPorId(UUID id) {
        OrdemProducao ordem = buscarPorIdParaAtualizarOuFalhar(id);
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
     * Pausa uma ordem que está em produção.
     */
    @Transactional
    public OrdemProducaoResponseDTO pausarProducao(UUID id) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        OrdemProducao ordem = buscarPorIdParaAtualizarOuFalhar(id);

        if (ordem.getEstado() != EstadoOrdemProducao.EM_PRODUCAO) {
            throw new RuntimeException("Apenas ordens em produção podem ser pausadas");
        }

        ordem.setEstado(EstadoOrdemProducao.PAUSADA);
        OrdemProducao updated = ordemRepo.save(ordem);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Retoma uma ordem pausada se as matérias-primas continuarem disponíveis.
     */
    @Transactional
    public OrdemProducaoResponseDTO retomar(UUID id) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_PRODUCAO);

        OrdemProducao ordem = buscarPorIdParaAtualizarOuFalhar(id);

        if (ordem.getEstado() != EstadoOrdemProducao.PAUSADA) {
            throw new RuntimeException("Apenas ordens pausadas podem ser retomadas");
        }

        validarStockMateriasParaProducao(ordem.getFormula(), ordem.getQuantidadePlaneada(), ordem.getId());

        ordem.setEstado(EstadoOrdemProducao.EM_PRODUCAO);

        OrdemProducao updated = ordemRepo.save(ordem);
        return mapper.toResponseDTO(updated);
    }

    private Funcionario buscarFuncionarioOuFalhar(UUID id) {
        return funcRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Funcionário não encontrado com ID: " + id
                ));
    }

    private OrdemProducao buscarPorIdParaAtualizarOuFalhar(UUID id) {
        return ordemRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ordem de produção não encontrada com ID: " + id
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

    /**
     * Considera o consumo esperado ainda não realizado das restantes ordens abertas.
     * Isto evita prometer a mesma matéria-prima a várias ordens em simultâneo.
     */
    private void validarStockMateriasParaProducao(FormulaProducao formula, Double quantidade, UUID ordemIgnoradaId) {
        if (quantidade == null || !Double.isFinite(quantidade) || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade planeada deve ser maior que zero");
        }
        var composicoes = composicaoRepo.findByFormulaId(formula.getId());

        composicoes.stream()
                .map(ComposicaoPellet::getMateriaPrima)
                .sorted(Comparator.comparing(materia -> materia.getId().toString()))
                .forEach(materia -> materiaPrimaRepo.findByIdForUpdate(materia.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Matéria-prima não encontrada")));

        for (ComposicaoPellet comp : composicoes) {
            ProductionUnitUtils.requireKilograms(comp.getMateriaPrima());
            Double necessario = comp.getQuantidadePorKg() * quantidade;
            Double stockAtual = comp.getMateriaPrima().getStockAtual();
            double comprometido = ordemRepo.findCompromissosMateriaPrima(
                            comp.getMateriaPrima().getId(),
                            ordemIgnoradaId,
                            EnumSet.of(
                                    EstadoOrdemProducao.PENDENTE,
                                    EstadoOrdemProducao.EM_PRODUCAO,
                                    EstadoOrdemProducao.PAUSADA
                            ))
                    .stream()
                    .mapToDouble(compromisso -> Math.max(
                            compromisso.getQuantidadeEsperada() - compromisso.getQuantidadeConsumida(),
                            0.0))
                    .sum();

            if (stockAtual - comprometido < necessario) {
                throw new IllegalArgumentException(
                        "Stock insuficiente de " + comp.getMateriaPrima().getNome() +
                                ". Necessário: " + necessario + "kg, Disponível: " + (stockAtual - comprometido) + "kg"
                );
            }
        }
    }

    private void validarFormulaProntaParaProducao(FormulaProducao formula) {
        if (!Boolean.TRUE.equals(formula.getAtiva())) {
            throw new IllegalArgumentException("A fórmula selecionada não está ativa");
        }
        var composicoes = composicaoRepo.findByFormulaId(formula.getId());
        composicoes.forEach(comp -> ProductionUnitUtils.requireKilograms(comp.getMateriaPrima()));
        double totalPorKg = composicoes.stream().mapToDouble(ComposicaoPellet::getQuantidadePorKg).sum();
        if (Math.abs(totalPorKg - 1.0) > 0.000001) {
            throw new IllegalArgumentException("A composição da fórmula deve totalizar exatamente 1 kg");
        }
    }

    private void validarConclusao(OrdemProducao ordem) {
        double produzido = ordem.getQuantidadeProduzidaReal() != null ? ordem.getQuantidadeProduzidaReal() : 0.0;
        if (produzido <= 0) {
            throw new IllegalStateException("Registe a quantidade produzida antes de concluir a ordem");
        }
        var consumos = consumoRepo.findByOrdemId(ordem.getId());
        if (consumos.isEmpty()) {
            throw new IllegalStateException("Registe os consumos antes de concluir a ordem");
        }
        Map<UUID, Double> consumoPorMateria = consumos.stream()
                .collect(Collectors.groupingBy(
                        consumo -> consumo.getMateriaPrima().getId(),
                        Collectors.summingDouble(ConsumoProducao::getQuantidadeConsumidaReal)
                ));
        boolean faltaAlgumaMateria = composicaoRepo.findByFormulaId(ordem.getFormula().getId()).stream()
                .map(ComposicaoPellet::getMateriaPrima)
                .map(MateriaPrima::getId)
                .anyMatch(materiaId -> consumoPorMateria.getOrDefault(materiaId, 0.0) <= 0);
        if (faltaAlgumaMateria) {
            throw new IllegalStateException("Registe o consumo de todas as matérias-primas da fórmula antes de concluir");
        }
        double quantidadeEmLotes = loteRepo.findByOrdemId(ordem.getId()).stream()
                .mapToDouble(LotePellet::getQuantidadeKg)
                .sum();
        if (Math.abs(quantidadeEmLotes - produzido) > 0.000001) {
            throw new IllegalStateException("A quantidade dos lotes deve coincidir com a produção real");
        }
    }

    private void validarAnulacao(OrdemProducao ordem) {
        if (!loteRepo.findByOrdemId(ordem.getId()).isEmpty()) {
            throw new IllegalStateException("Não é possível anular uma ordem com lotes registados");
        }
        if (!consumoRepo.findByOrdemId(ordem.getId()).isEmpty()) {
            throw new IllegalStateException("Não é possível anular uma ordem com consumos registados");
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
