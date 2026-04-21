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

        if (!BCrypt.checkpw(pin, funcionario.getPinHash())) {
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

//    /**
//     * Autenticar funcionário (login)
//     */
//    public FuncionarioResponseDTO autenticar(Integer numeroFuncionario, String pin) {
//        Funcionario funcionario = funcRepo.findByNumeroFuncionario(numeroFuncionario)
//                .orElseThrow(() -> new RuntimeException("Número de funcionário inválido."));
//
//        if (!authService.verificarPin(pin, funcionario.getPinHash())) {
//            throw new RuntimeException("PIN incorreto.");
//        }
//
//        return funcionarioMapper.toResponseDTO(funcionario);
//    }
//
//    /**
//     * Alterar PIN
//     */
//    @Transactional
//    public void alterarPin(UUID id, String pinAntigo, String pinNovo) {
//        Funcionario funcionario = buscarPorIdOuFalhar(id);
//
//        // Validar PIN antigo
//        if (!authService.verificarPin(pinAntigo, funcionario.getPinHash())) {
//            throw new RuntimeException("PIN antigo incorreto.");
//        }
//
//        // Validar novo PIN (4 dígitos)
//        if (!pinNovo.matches("\\d{4}")) {
//            throw new RuntimeException("O novo PIN deve ter exatamente 4 dígitos.");
//        }
//
//        // Atualizar
//        String novoHash = authService.gerarHashPin(pinNovo);
//        funcionario.setPinHash(novoHash);
//        funcRepo.save(funcionario);
}
