package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoTicket;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TicketDetailsDTO(
        UUID id,
        UUID encomendaId,
        String codigoTracking,
        String clienteNome,
        String assunto,
        EstadoTicket estado,
        String responsavelNome,
        LocalDate dataEncomenda,
        EstadoEncomendaCliente estadoEncomenda,
        Double quantidadeTotalKg,
        Double totalFinal,
        String moedaSimbolo,
        Instant ultimaMensagemEm,
        List<TicketOrderItemDTO> itens,
        List<TicketMensagemDTO> mensagens
) {
}
