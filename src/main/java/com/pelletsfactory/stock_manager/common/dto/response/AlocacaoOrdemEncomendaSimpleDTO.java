package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record AlocacaoOrdemEncomendaSimpleDTO(
        UUID id,
        String ordemCodigo,
        String nomeCliente,
        Double quantidadeReservada
) {
}

