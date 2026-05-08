package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

public record FormulaProducaoRequestDTO(
        UUID tipoPelletId,
        String nome,
        Boolean ativa
) {
    public FormulaProducaoRequestDTO {
        if (tipoPelletId == null) {
            throw new IllegalArgumentException("O ID do tipo de pellet é obrigatório");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da fórmula é obrigatório");
        }
        if (nome.trim().length() < 3 || nome.trim().length() > 100) {
            throw new IllegalArgumentException("O nome da fórmula deve ter entre 3 e 100 caracteres");
        }
        if (ativa == null) {
            throw new IllegalArgumentException("O estado (ativa) é obrigatório");
        }
    }
}