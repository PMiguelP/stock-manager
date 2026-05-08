package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record MoedaResponseDTO(
    UUID id,
    String codigo,
    String simbolo
) {
}

