package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

public record ItemEncomendaClienteRequestDTO(
        UUID encomendaId,
        UUID tipoPelletId,
        Double quantidadeKg,
        Double precoUnitarioNet,
        Double taxaIva
) {
    public ItemEncomendaClienteRequestDTO {
        if (encomendaId == null) {
            throw new IllegalArgumentException("ID da encomenda é obrigatório");
        }
        if (tipoPelletId == null) {
            throw new IllegalArgumentException("ID do tipo de pellet é obrigatório");
        }
        ValidationUtils.requirePositive(quantidadeKg, "Quantidade em kg");
        ValidationUtils.requirePositive(precoUnitarioNet, "Preço unitário");
        ValidationUtils.requireNonNegative(taxaIva, "Taxa IVA");
        if (taxaIva > 100) {
            throw new IllegalArgumentException("Taxa IVA não pode ser superior a 100%");
        }
    }
}
