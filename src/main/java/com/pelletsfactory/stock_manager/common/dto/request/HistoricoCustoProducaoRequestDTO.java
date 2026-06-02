package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

public record HistoricoCustoProducaoRequestDTO(
        UUID tipoPelletId,
        Double custoBasePorKg
) {
    public HistoricoCustoProducaoRequestDTO {
        if (tipoPelletId == null) {
            throw new IllegalArgumentException("ID do tipo de pellet é obrigatório");
        }
        ValidationUtils.requirePositive(custoBasePorKg, "Custo base por kg");
    }
}
