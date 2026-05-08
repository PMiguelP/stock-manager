package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record ConsumoSimpleDTO(
        UUID id,
        UUID ordemProducaoId,
        String materiaPrimaNome,
        String unidade,
        Double quantidadeConsumidaReal
) {
}

