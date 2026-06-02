package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

public record ComposicaoPelletRequestDTO(
        UUID formulaId,
        UUID materiaPrimaId,
        Double quantidadePorKg
) {
    public ComposicaoPelletRequestDTO {
        if (formulaId == null) {
            throw new IllegalArgumentException("O ID da fórmula é obrigatório");
        }
        if (materiaPrimaId == null) {
            throw new IllegalArgumentException("O ID da matéria-prima é obrigatório");
        }
        ValidationUtils.requirePositive(quantidadePorKg, "A quantidade por kg");
    }
}
