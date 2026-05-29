package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import java.time.Instant;
import java.util.UUID;

public record NotificacaoSimpleDTO(
        UUID id,
        String titulo,
        TipoEventoNotificacao tipoEvento,
        Cargo cargoAlvo,
        Boolean lida,
        Boolean requerAcao,
        Boolean concluida,
        String concluidaPorNome,
        Instant concluidaEm,
        Instant createdAt
) {
}
