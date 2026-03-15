package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class FuncionarioService {
    private final FuncionarioRepository funcRepo;
    private final AuthService authService;

    public FuncionarioService(FuncionarioRepository funcRepo, AuthService authService)   {
        this.funcRepo = funcRepo;
        this.authService = authService;
    }

    @Transactional
    public Funcionario adicionarFuncionario(Funcionario funcionario) {
        validarPermissaoAdmin();
        //TODO: mudar as funcoes de validcao para outro ficheiro
        validarDadosFuncionario(funcionario);
        if (funcRepo.existsByNif(funcionario.getNif())) {
            throw new RuntimeException("Já existe um funcionário registado com este NIF: " + funcionario.getNif());
        }

        Integer ultimoNumero = funcRepo.findMaxNumeroFuncionario();
        Integer proximoNumero = (ultimoNumero == null) ? 1000 : ultimoNumero + 1;
        funcionario.setNumeroFuncionario(proximoNumero);

        String pinInicial = String.valueOf(proximoNumero);
        //Veiricar porque estou a usar a funcao do auth service veer se funcionou igual
        String hashFinal = authService.gerarHashPin(pinInicial);
        funcionario.setPinHash(hashFinal);

        if (funcionario.getDataAdmissao() == null) {
            funcionario.setDataAdmissao(LocalDate.now());
        }
        return funcRepo.save(funcionario);
    }

    @Transactional
    public void atualizarFuncionario(Funcionario f) {
        validarPermissaoAdmin();
        Funcionario existente = buscarPorId(f.getId());
        validarDadosFuncionario(f);

        if (!existente.getNif().equals(f.getNif()) && funcRepo.existsByNif(f.getNif())) {
            throw new RuntimeException("Este NIF já está em uso por outro funcionário.");
        }

        existente.setNome(f.getNome());
        existente.setNif(f.getNif());
        existente.setContacto(f.getContacto());
        existente.setCargo(f.getCargo());
        existente.setDataAdmissao(f.getDataAdmissao());
        funcRepo.save(existente);
    }
    @Transactional
    public void apagarFuncionario(UUID id) {
        validarPermissaoAdmin();

        Funcionario f = buscarPorId(id);

        if (f.getId().equals(SessaoFuncionario.getFuncionarioLogado().getId())) {
            throw new RuntimeException("Não pode remover a sua própria conta de administrador.");
        }

        funcRepo.delete(f);
    }

    public List<Funcionario> listarTodos() {
        return funcRepo.findAll();
    }

    public Funcionario buscarPorId(UUID id) {
        // TODO: CRÍTICO - Busca funcionário por UUID e retorna entidade completa. Usado por TODOS os outros serviços para validar autor de ações
        return funcRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Funcionário não encontrado com o ID: " + id));
    }


    public boolean validarCargo(UUID funcionarioId, Cargo cargoEsperado){
        // TODO: Verifica se o funcionário tem permissão para executar operação baseado no cargo esperado. Retorna
        //  true/false nao sei se ainda e necessario tenho que ver melhor
        return true;
    }

    //Funcao auxiliar de validacao do formulario para criar um funcionario
    private void validarDadosFuncionario(Funcionario f) {
        if (f.getNome() == null || f.getNome().trim().length() < 3) {
            throw new RuntimeException("O nome deve ter pelo menos 3 caracteres.");
        }

        if (f.getNif() == null || !f.getNif().matches("\\d{9}")) {
            throw new RuntimeException("NIF inválido. Deve conter exatamente 9 dígitos numéricos.");
        }
        if (f.getContacto() == null || !f.getContacto().matches("[2789]\\d{8}")) {
            throw new RuntimeException("Contacto telefónico inválido.");
        }
    }

    //TODO: remover isto para outro arquivo vai ser muito utilizado
    private void validarPermissaoAdmin() {
        Funcionario userLogado = SessaoFuncionario.getFuncionarioLogado();

        if (userLogado == null) {
            throw new RuntimeException("Utilizador não autenticado. Por favor, faça login.");
        }

        if (userLogado.getCargo() != Cargo.ADMINISTRADOR) {
            throw new RuntimeException("Acesso negado: Operação exclusiva para Administradores.");
        }
    }
}
