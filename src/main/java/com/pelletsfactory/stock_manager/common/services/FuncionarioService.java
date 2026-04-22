package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.FuncionarioRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import com.pelletsfactory.stock_manager.common.mapper.FuncionarioMapper;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
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
import java.util.stream.Collectors;

@Service
public class FuncionarioService {
    private final FuncionarioRepository funcRepo;
    private final OrdemProducaoRepository ordemProducaoRepo;
    private final FuncionarioMapper funcMapper;
    private final AuthService authService;

    public FuncionarioService(FuncionarioRepository funcRepo, AuthService authService, FuncionarioMapper funcMapper,
     OrdemProducaoRepository ordemProducaoRepo                         )   {
        this.funcRepo = funcRepo;
        this.authService = authService;
        this.funcMapper = funcMapper;
        this.ordemProducaoRepo = ordemProducaoRepo;
    }

    /**
     * Criar novo funcionário
     */
    @Transactional
    public FuncionarioResponseDTO criarFuncionario(FuncionarioRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);

        // Validar NIF único
        if (funcRepo.existsByNif(dto.nif())) {
            throw new RuntimeException("Já existe um funcionário registado com este NIF: " + dto.nif());
        }

        // Criar entidade
        Funcionario funcionario = funcMapper.toEntity(dto);

        // Gerar número de funcionário automaticamente
        Integer ultimoNumero = funcRepo.findMaxNumeroFuncionario();
        Integer proximoNumero = (ultimoNumero == null) ? 1000 : ultimoNumero + 1;
        funcionario.setNumeroFuncionario(proximoNumero);

        // Gerar PIN inicial (mesmo número do funcionário)
        String pinInicial = String.valueOf(proximoNumero);
        String hashFinal = authService.gerarHashPin(pinInicial);
        funcionario.setPinHash(hashFinal);

        // Data de admissão = hoje
        funcionario.setDataAdmissao(LocalDate.now());

        Funcionario saved = funcRepo.save(funcionario);
        return funcMapper.toResponseDTO(saved);
    }

    /**
     * Atualizar funcionário existente
     */
    @Transactional
    public FuncionarioResponseDTO atualizarFuncionario(UUID id, FuncionarioRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);
        Funcionario existente = buscarPorIdOuFalhar(id);

        // Validar NIF único (se mudou)
        if (!existente.getNif().equals(dto.nif()) && funcRepo.existsByNif(dto.nif())) {
            throw new RuntimeException("Este NIF já está em uso por outro funcionário.");
        }

        // Atualizar campos
        funcMapper.updateEntityFromDTO(dto, existente);

        Funcionario atualizado = funcRepo.save(existente);
        return funcMapper.toResponseDTO(atualizado);
    }

    /**
     * Eliminar funcionário
     */
    @Transactional
    public void apagarFuncionario(UUID id) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);

        Funcionario funcionario = buscarPorIdOuFalhar(id);

        // Não pode eliminar a si próprio
        if (funcionario.getId().equals(SessaoFuncionario.getFuncionarioLogado().getId())) {
            throw new RuntimeException("Não pode remover a sua própria conta.");
        }

        Long ordensEmCurso = ordemProducaoRepo.countByFuncionarioIdAndEstado(
                id, EstadoOrdemProducao.EM_PRODUCAO
        );
        if (ordensEmCurso > 0) {
            throw new RuntimeException(
                    "Não é possível eliminar funcionário com ordens de produção em curso."
            );
        }

        funcRepo.delete(funcionario);
    }

    /**
     * Listar com paginação e filtros (retorna SimpleDTO)
     */
    public Page<FuncionarioSimpleDTO> listarFuncionarios(
            int page,
            int pageSize,
            String nome,
            String nif,
            Cargo cargo,
            Integer numeroFuncionario,
            String sortBy,
            String direction) {

        // Default sort
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "dataAdmissao";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

        Page<Funcionario> funcionariosPage = funcRepo.findByFiltros(
                nome, nif, cargo, numeroFuncionario, pageable
        );

        return funcionariosPage.map(funcMapper::toSimpleDTO);
    }

    /**
     * Listar todos (versão simples para dropdowns)
     */
    public List<FuncionarioSimpleDTO> listarTodosSimples() {
        return funcRepo.findAll()
                .stream()
                .map(funcMapper::toSimpleDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obter detalhes completos (com estatísticas de produção)
     */
    public FuncionarioDetailsDTO obterDetalhes(UUID id) {
        Funcionario funcionario = buscarPorIdOuFalhar(id);
        return funcMapper.toDetailsDTO(funcionario);
    }


    /**
     * Verifica se NIF já existe
     */
    public boolean existeNif(String nif) {
        return funcRepo.existsByNif(nif);
    }

    /**
     * Verifica se número de funcionário já existe
     */
    public boolean existeNumeroFuncionario(Integer numero) {
        return funcRepo.existsByNumeroFuncionario(numero);
    }

    /**
     * Buscar por ID (retorna DTO básico)
     */
    public FuncionarioResponseDTO buscarPorId(UUID id) {
        Funcionario funcionario = buscarPorIdOuFalhar(id);
        return funcMapper.toResponseDTO(funcionario);
    }

    /**
     * Buscar entidade por ID ou lançar exceção
     */
    public Funcionario buscarPorIdOuFalhar(UUID id) {
        return funcRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Funcionário não encontrado com o ID: " + id
                ));
    }

    @Transactional
    public void alterarPin(UUID id, String pinAntigo, String pinNovo) {
        Funcionario logged = SessaoFuncionario.getFuncionarioLogado();
        if (logged == null) {
            throw new RuntimeException("Sessão expirada. Faça login novamente.");
        }

        if (!logged.getId().equals(id) && logged.getCargo() != Cargo.ADMINISTRADOR) {
            throw new RuntimeException("Não tem permissão para alterar este PIN.");
        }

        Funcionario funcionario = buscarPorIdOuFalhar(id);

        if (!authService.verificarPin(pinAntigo, funcionario.getPinHash())) {
            throw new RuntimeException("PIN atual incorreto.");
        }

        if (pinNovo == null || !pinNovo.matches("\\d{4}")) {
            throw new RuntimeException("O novo PIN deve conter exatamente 4 dígitos.");
        }

        funcionario.setPinHash(authService.gerarHashPin(pinNovo));
        Funcionario atualizado = funcRepo.save(funcionario);

        if (logged.getId().equals(atualizado.getId())) {
            SessaoFuncionario.login(atualizado);
        }
    }
}
