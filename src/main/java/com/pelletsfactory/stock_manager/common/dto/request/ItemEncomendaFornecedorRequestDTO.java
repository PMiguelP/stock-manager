package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

public record ItemEncomendaFornecedorRequestDTO(
        UUID encomendaId,
        UUID materiaPrimaId,
        Double quantidade,
        Double precoUnitarioNet,
        Double taxaIva
) {
    public ItemEncomendaFornecedorRequestDTO {
        if (encomendaId == null) {
            throw new IllegalArgumentException("ID da encomenda é obrigatório");
        }
        if (materiaPrimaId == null) {
            throw new IllegalArgumentException("ID da matéria-prima é obrigatório");
        }
        ValidationUtils.requirePositive(quantidade, "Quantidade");
        ValidationUtils.requirePositive(precoUnitarioNet, "Preço unitário");
        ValidationUtils.requireNonNegative(taxaIva, "Taxa IVA");
        if (taxaIva > 100) {
            throw new IllegalArgumentException("Taxa IVA não pode ser superior a 100%");
        }
    }
}
