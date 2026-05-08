package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.List;
import java.util.UUID;

public record FormulaProducaoResponseDTO(
    UUID id,
    UUID tipoPelletId,
    String tipoPelletNome,
    String nome,
    Boolean ativa,
    List<ComposicaoPelletResponseDTO> composicoes
) {
}

