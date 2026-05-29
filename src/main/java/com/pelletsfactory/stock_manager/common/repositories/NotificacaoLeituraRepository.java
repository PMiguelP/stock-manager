package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.NotificacaoLeitura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificacaoLeituraRepository extends JpaRepository<NotificacaoLeitura, UUID> {
    boolean existsByNotificacaoIdAndFuncionarioId(UUID notificacaoId, UUID funcionarioId);
}
