package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

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
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser superior a zero");
        }
        if (precoUnitarioNet == null || precoUnitarioNet < 0) {
            throw new IllegalArgumentException("Preço unitário não pode ser negativo");
        }
        if (taxaIva == null || taxaIva < 0) {
            throw new IllegalArgumentException("Taxa IVA não pode ser negativa");
        }
    }
}

