package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import java.time.Instant;
import java.util.UUID;

public record MovimentoFinanceiroSimpleDTO(
        UUID id,
        TipoMovimento tipoMovimento,
        Double valorTotal,
        String moedaCodigo,
        Instant createdAt
) {
}

