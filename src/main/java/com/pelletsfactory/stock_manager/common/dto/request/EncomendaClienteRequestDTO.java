package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

public record EncomendaClienteRequestDTO(
        UUID clienteId,
        String data,
        Double totalNet,
        Double totalIva,
        Double totalFinal,
        UUID moedaId,
        String codigoTracking
) {
    public EncomendaClienteRequestDTO {
        if (clienteId == null) {
            throw new IllegalArgumentException("ID do cliente é obrigatório");
        }
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Data é obrigatória");
        }
        if (totalNet == null || totalNet < 0) {
            throw new IllegalArgumentException("Total net não pode ser negativo");
        }
        if (totalIva == null || totalIva < 0) {
            throw new IllegalArgumentException("Total IVA não pode ser negativo");
        }
        if (totalFinal == null || totalFinal < 0) {
            throw new IllegalArgumentException("Total final não pode ser negativo");
        }
        if (moedaId == null) {
            throw new IllegalArgumentException("ID da moeda é obrigatório");
        }
    }
}

