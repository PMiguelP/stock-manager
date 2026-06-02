package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

public record ConsumoProducaoRequestDTO(
        UUID ordemProducaoId,
        UUID materiaPrimaId,
        Double quantidadeConsumidaReal
) {
    public ConsumoProducaoRequestDTO {
        if (ordemProducaoId == null) {
            throw new IllegalArgumentException("O ID da ordem de produção é obrigatório");
        }
        if (materiaPrimaId == null) {
            throw new IllegalArgumentException("O ID da matéria-prima é obrigatório");
        }
        ValidationUtils.requirePositive(quantidadeConsumidaReal, "A quantidade consumida real");
    }
}
