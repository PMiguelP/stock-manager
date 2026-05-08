package com.pelletsfactory.stock_manager.common.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TipoPelletDetailsDTO(
        UUID id,
        String nome,
        Double diametroMm,
        Double poderCalorifico,
        Double stockAtual,
        Double stockMinimo,
        BigDecimal custoAtualPorKg,
        UUID moedaId,
        String moedaCodigo,
        Instant createdAt,
        Instant updatedAt
) {
}

