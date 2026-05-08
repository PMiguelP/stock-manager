package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record AlocacaoOrdemEncomendaResponseDTO(
    UUID id,
    UUID ordemId,
    String ordemCodigo,
    UUID encomendaClienteId,
    String nomeCliente,
    Double quantidadeReservada
) {
}

