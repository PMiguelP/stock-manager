package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record ConsumoResponseDTO(
    UUID id,
    UUID ordemProducaoId,
    UUID materiaPrimaId,
    String materiaPrimaNome,
    String unidade,
    Double quantidadeConsumidaReal
) {
}

