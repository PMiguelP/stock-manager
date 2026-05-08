package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record ItemEncomendaClienteSimpleDTO(
        UUID id,
        String tipoPelletNome,
        Double quantidadeKg,
        Double precoUnitarioNet,
        Double taxaIva
) {
}

