package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record AlocacaoLoteEncomendaResponseDTO(
        UUID id,
        UUID loteId,
        String codigoLote,
        UUID itemEncomendaId,
        UUID encomendaId,
        String clienteNome,
        String tipoPelletNome,
        Double quantidadeReservada
) {
}
