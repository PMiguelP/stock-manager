package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

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
        ValidationUtils.requireNonNegative(totalNet, "Total net");
        ValidationUtils.requireNonNegative(totalIva, "Total IVA");
        ValidationUtils.requireNonNegative(totalFinal, "Total final");
        if (moedaId == null) {
            throw new IllegalArgumentException("ID da moeda é obrigatório");
        }
    }
}
