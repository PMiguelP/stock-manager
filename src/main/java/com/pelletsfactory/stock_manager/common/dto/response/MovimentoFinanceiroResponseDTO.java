package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import java.time.Instant;
import java.util.UUID;

public record MovimentoFinanceiroResponseDTO(
        UUID id,
        TipoMovimento tipoMovimento,
        Double valorTotal,
        UUID moedaId,
        String moedaCodigo,
        UUID idEncomendaCliente,
        UUID idEncomendaFornecedor,
        Instant createdAt
) {
}

