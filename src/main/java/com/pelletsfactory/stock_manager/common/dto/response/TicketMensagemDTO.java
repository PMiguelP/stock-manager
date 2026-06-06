package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.AutorMensagemTicket;

import java.time.Instant;
import java.util.UUID;

public record TicketMensagemDTO(
        UUID id,
        AutorMensagemTicket autorTipo,
        String autorNome,
        String mensagem,
        Instant createdAt
) {
}
