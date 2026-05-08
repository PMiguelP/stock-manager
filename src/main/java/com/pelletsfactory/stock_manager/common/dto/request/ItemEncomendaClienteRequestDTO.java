package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

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
        if (quantidadeKg == null || quantidadeKg <= 0) {
            throw new IllegalArgumentException("Quantidade em kg deve ser superior a zero");
        }
        if (precoUnitarioNet == null || precoUnitarioNet < 0) {
            throw new IllegalArgumentException("Preço unitário não pode ser negativo");
        }
        if (taxaIva == null || taxaIva < 0) {
            throw new IllegalArgumentException("Taxa IVA não pode ser negativa");
        }
    }
}

