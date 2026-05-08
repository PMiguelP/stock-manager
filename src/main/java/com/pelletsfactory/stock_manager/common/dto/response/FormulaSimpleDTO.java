package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record FormulaSimpleDTO(
    UUID id,
    String tipoPelletNome,
    String nome,
    Boolean ativa
) {
}

