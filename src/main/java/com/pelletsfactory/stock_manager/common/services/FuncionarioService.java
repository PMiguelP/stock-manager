package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.accept.ApiVersionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FuncionarioService {
    private final FuncionarioRepository funcRepo;

    public FuncionarioService(FuncionarioRepository funcRepo) {
        this.funcRepo = funcRepo;
    }

    public Funcionario autenticar(String numeroFuncionario, String pin) {
        // TODO: Valida credenciais do funcionário (número + PIN) e retorna o Funcionario se válido, null se inválido
        return new Funcionario();
    }

    @Transactional
    public Funcionario adicionarFuncionario(Funcionario funcionario) {
        // TODO: Cria novo funcionário no sistema com hash do PIN e retorna o Funcionario criado
        return new Funcionario();
    }

    @Transactional
    public int atualizarFuncionario(Funcionario f) {
        return 1;
    }

    @Transactional
    public int apagarFuncionario(Funcionario f) {
        return 1;
    }

    public List<Funcionario> listarOperadores() {
        // TODO: Retorna lista de funcionários filtrados por cargo específico (ex: OPERADOR)
        return new ArrayList<>();
    }


    public Funcionario buscarPorId(UUID id) {
        // TODO: CRÍTICO - Busca funcionário por UUID e retorna entidade completa. Usado por TODOS os outros serviços para validar autor de ações
        return funcRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Funcionário não encontrado com o ID: " + id));
    }


    public boolean validarCargo(UUID funcionarioId, Cargo cargoEsperado){
        // TODO: Verifica se o funcionário tem permissão para executar operação baseado no cargo esperado. Retorna true/false
        return true;
    }
}
