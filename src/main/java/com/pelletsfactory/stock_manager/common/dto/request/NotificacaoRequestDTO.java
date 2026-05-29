package com.pelletsfactory.stock_manager.common.dto.request;

import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import java.util.UUID;

public record NotificacaoRequestDTO(
        String titulo,
        String mensagem,
        TipoEventoNotificacao tipoEvento,
        Cargo cargoAlvo,
        Boolean requerAcao,
        UUID linkReferencia
) {
    public NotificacaoRequestDTO {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("O título da notificação é obrigatório");
        }
        if (titulo.trim().length() > 200) {
            throw new IllegalArgumentException("O título não pode exceder 200 caracteres");
        }

        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException("A mensagem da notificação é obrigatória");
        }

        if (tipoEvento == null) {
            throw new IllegalArgumentException("O tipo de evento da notificação é obrigatório");
        }
    }
}
