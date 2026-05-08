package com.pelletsfactory.stock_manager.common.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TipoPelletResponseDTO(
        UUID id,
        String nome,
        Double diametroMm,
        Double poderCalorifico,
        Double stockAtual,
        Double stockMinimo,
        BigDecimal custoAtualPorKg,
        UUID moedaId,
        String moedaCodigo
) {
}

