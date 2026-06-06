package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoTicket;

import java.time.Instant;
import java.util.UUID;

public record TicketSimpleDTO(
        UUID id,
        UUID encomendaId,
        String codigoTracking,
        String clienteNome,
        String assunto,
        EstadoTicket estado,
        UUID responsavelId,
        String responsavelNome,
        Instant ultimaMensagemEm
) {
}
