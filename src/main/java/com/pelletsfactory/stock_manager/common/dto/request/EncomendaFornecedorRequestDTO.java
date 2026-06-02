package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

public record EncomendaFornecedorRequestDTO(
        UUID fornecedorId,
        String data,
        Double totalLiquido,
        Double totalIva,
        Double totalFinal,
        UUID moedaId
) {
    public EncomendaFornecedorRequestDTO {
        if (fornecedorId == null) {
            throw new IllegalArgumentException("ID do fornecedor é obrigatório");
        }
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Data é obrigatória");
        }
        ValidationUtils.requireNonNegative(totalLiquido, "Total líquido");
        ValidationUtils.requireNonNegative(totalIva, "Total IVA");
        ValidationUtils.requireNonNegative(totalFinal, "Total final");
        if (moedaId == null) {
            throw new IllegalArgumentException("ID da moeda é obrigatório");
        }
    }
}
