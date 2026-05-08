package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record ComposicaoPelletSimpleDTO(
        UUID id,
        String materiaPrimaNome,
        String unidade,
        Double quantidadePorKg
) {
}

