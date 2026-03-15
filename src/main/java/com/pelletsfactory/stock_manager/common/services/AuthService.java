/*
package com.pelletsfactory.stock_manager.common.services;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final FuncionarioRepository funcionarioRepo;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepo= funcionarioRepository;
    }

    public Funcionario login(Integer numeroFuncionario, String pin) {

        Funcionario funcionario = funcionarioRepo
                .findByNumeroFuncionario(numeroFuncionario)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (!passwordEncoder.matches(pin, funcionario.getPinHash())) {
            throw new RuntimeException("PIN inválido");
        } else {
            SessaoFuncionario.login(funcionario);

            return funcionario;
        }

    }

    public void logout() {
        SessaoFuncionario.logout();
    }

    public Funcionario getFuncionarioAtual() {
        return SessaoFuncionario.getFuncionarioLogado();
    }

    public boolean isAutenticado() {
        return SessaoFuncionario.isAutenticado();
    }
}*/
