package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record ItemEncomendaClienteResponseDTO(
        UUID id,
        UUID encomendaId,
        UUID tipoPelletId,
        String tipoPelletNome,
        Double quantidadeKg,
        Double precoUnitarioNet,
        Double taxaIva,
        Double valorIvaCalculado
) {
}

