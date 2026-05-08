package com.pelletsfactory.stock_manager.common.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record TipoPelletRequestDTO(
        String nome,
        Double diametroMm,
        Double poderCalorifico,
        Double stockAtual,
        Double stockMinimo,
        BigDecimal custoAtualPorKg,
        UUID moedaId
) {
    public TipoPelletRequestDTO {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do tipo de pellet é obrigatório");
        }
        if (nome.length() > 100) {
            throw new IllegalArgumentException("Nome não pode exceder 100 caracteres");
        }

        if (stockAtual != null && stockAtual < 0) {
            throw new IllegalArgumentException("Stock atual não pode ser negativo");
        }
        if (stockMinimo != null && stockMinimo < 0) {
            throw new IllegalArgumentException("Stock mínimo não pode ser negativo");
        }
    }
}

