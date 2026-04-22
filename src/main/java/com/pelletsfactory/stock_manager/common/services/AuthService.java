package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final FuncionarioRepository funcionarioRepo;

    public AuthService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepo = funcionarioRepository;
    }

    public Funcionario login(Integer numeroFuncionario, String pin) {
        Funcionario funcionario = funcionarioRepo
                .findByNumeroFuncionario(numeroFuncionario)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (!verificarPin(pin, funcionario.getPinHash())) {
            throw new RuntimeException("PIN inválido");
        } else {
            SessaoFuncionario.login(funcionario);
            return funcionario;
        }
    }
    public void logout() {
        SessaoFuncionario.logout();
    }

    public String gerarHashPin(String pinPuro) {
        return BCrypt.hashpw(pinPuro, BCrypt.gensalt());
    }

    public boolean verificarPin(String pinPuro, String pinHash) {
        if (pinPuro == null || pinHash == null) {
            return false;
        }
        return BCrypt.checkpw(pinPuro, pinHash);
    }
}
