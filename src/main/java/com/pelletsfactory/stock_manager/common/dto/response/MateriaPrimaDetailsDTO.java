package com.pelletsfactory.stock_manager.common.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MateriaPrimaDetailsDTO(
        UUID id,
        String nome,
        String unidade,
        Double stockAtual,
        Double stockMinimo,
        Instant createdAt,
        Instant updatedAt
) {
}

