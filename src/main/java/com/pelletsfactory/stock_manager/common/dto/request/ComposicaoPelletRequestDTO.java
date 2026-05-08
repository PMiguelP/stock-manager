package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

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
        if (quantidadePorKg == null || quantidadePorKg <= 0) {
            throw new IllegalArgumentException("A quantidade por kg deve ser superior a zero (ex: 0.01)");
        }
    }
}