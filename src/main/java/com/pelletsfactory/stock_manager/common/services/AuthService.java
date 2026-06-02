package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final FuncionarioRepository funcionarioRepo;
    private final Map<Integer, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public AuthService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepo = funcionarioRepository;
    }

    public Funcionario login(Integer numeroFuncionario, String pin) {
        validarBloqueio(numeroFuncionario);
        Funcionario funcionario = funcionarioRepo
                .findByNumeroFuncionario(numeroFuncionario)
                .orElseThrow(() -> credenciaisInvalidas(numeroFuncionario));

        if (!verificarPin(pin, funcionario.getPinHash())) {
            throw credenciaisInvalidas(numeroFuncionario);
        } else {
            attempts.remove(numeroFuncionario);
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

    private void validarBloqueio(Integer numeroFuncionario) {
        if (numeroFuncionario == null) {
            throw new RuntimeException("Credenciais inválidas");
        }
        LoginAttempt attempt = attempts.get(numeroFuncionario);
        if (attempt != null && attempt.lockedUntil() != null && Instant.now().isBefore(attempt.lockedUntil())) {
            throw new RuntimeException("Demasiadas tentativas. Tente novamente dentro de alguns minutos.");
        }
        if (attempt != null && attempt.lockedUntil() != null) {
            attempts.remove(numeroFuncionario);
        }
    }

    private RuntimeException credenciaisInvalidas(Integer numeroFuncionario) {
        if (numeroFuncionario != null) {
            attempts.compute(numeroFuncionario, (key, current) -> {
                int failedAttempts = current == null ? 1 : current.failedAttempts() + 1;
                Instant lockedUntil = failedAttempts >= MAX_FAILED_ATTEMPTS
                        ? Instant.now().plus(LOCK_DURATION)
                        : null;
                return new LoginAttempt(failedAttempts, lockedUntil);
            });
        }
        return new RuntimeException("Credenciais inválidas");
    }

    private record LoginAttempt(int failedAttempts, Instant lockedUntil) {
    }
}
