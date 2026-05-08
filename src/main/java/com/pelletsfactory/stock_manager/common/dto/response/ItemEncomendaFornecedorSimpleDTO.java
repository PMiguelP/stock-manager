package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record ItemEncomendaFornecedorSimpleDTO(
        UUID id,
        String materiaPrimaNome,
        String unidade,
        Double quantidade,
        Double precoUnitarioNet,
        Double taxaIva
) {
}

