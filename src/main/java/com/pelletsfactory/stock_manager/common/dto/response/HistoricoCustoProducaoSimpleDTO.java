package com.pelletsfactory.stock_manager.common.dto.response;

import java.time.Instant;
import java.util.UUID;

public record HistoricoCustoProducaoSimpleDTO(
        UUID id,
        String tipoPelletNome,
        Double custoBasePorKg,
        Instant dataInicio,
        Instant dataFim
) {
}

