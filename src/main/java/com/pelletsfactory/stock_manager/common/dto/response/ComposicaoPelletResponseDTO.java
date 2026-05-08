package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record ComposicaoPelletResponseDTO(
    UUID id,
    UUID formulaId,
    UUID materiaPrimaId,
    String materiaPrimaNome,
    String unidade,
    Double quantidadePorKg
) {
}

