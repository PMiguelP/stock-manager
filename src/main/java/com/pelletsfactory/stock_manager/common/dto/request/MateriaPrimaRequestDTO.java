package com.pelletsfactory.stock_manager.common.dto.request;

import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

public record MateriaPrimaRequestDTO(
        String nome,
        String unidade,
        Double stockAtual,
        Double stockMinimo
) {
    public MateriaPrimaRequestDTO {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da matéria-prima é obrigatório");
        }
        if (unidade == null || unidade.isBlank()) {
            throw new IllegalArgumentException("Unidade da matéria-prima é obrigatória");
        }
        if (stockAtual != null) {
            ValidationUtils.requireNonNegative(stockAtual, "Stock atual");
        }
        if (stockMinimo != null) {
            ValidationUtils.requireNonNegative(stockMinimo, "Stock mínimo");
        }
    }
}
