package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

public record HistoricoCustoProducaoRequestDTO(
        UUID tipoPelletId,
        Double custoBasePorKg
) {
    public HistoricoCustoProducaoRequestDTO {
        if (tipoPelletId == null) {
            throw new IllegalArgumentException("ID do tipo de pellet é obrigatório");
        }
        if (custoBasePorKg == null || custoBasePorKg <= 0) {
            throw new IllegalArgumentException("Custo base por kg deve ser superior a zero");
        }
    }
}

