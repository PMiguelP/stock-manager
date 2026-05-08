package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record ItemEncomendaFornecedorResponseDTO(
        UUID id,
        UUID encomendaId,
        UUID materiaPrimaId,
        String materiaPrimaNome,
        String unidade,
        Double quantidade,
        Double precoUnitarioNet,
        Double taxaIva,
        Double valorIvaCalculado
) {
}

