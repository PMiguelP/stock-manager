package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

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
        if (totalLiquido == null || totalLiquido < 0) {
            throw new IllegalArgumentException("Total líquido não pode ser negativo");
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

