package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record MoedaSimpleDTO(
        UUID id,
        String codigo,
        String simbolo
) {
}

