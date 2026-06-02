package com.pelletsfactory.stock_manager.common.dto.request;

import java.math.BigDecimal;
import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

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

        if (diametroMm != null) {
            ValidationUtils.requirePositive(diametroMm, "Diâmetro");
        }
        if (poderCalorifico != null) {
            ValidationUtils.requirePositive(poderCalorifico, "Poder calorífico");
        }
        if (stockAtual != null) {
            ValidationUtils.requireNonNegative(stockAtual, "Stock atual");
        }
        if (stockMinimo != null) {
            ValidationUtils.requireNonNegative(stockMinimo, "Stock mínimo");
        }
    }
}
