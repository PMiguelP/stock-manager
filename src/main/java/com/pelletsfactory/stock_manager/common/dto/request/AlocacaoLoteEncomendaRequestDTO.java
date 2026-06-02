package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

public record AlocacaoLoteEncomendaRequestDTO(
        UUID loteId,
        UUID itemEncomendaId,
        Double quantidadeReservada
) {
    public AlocacaoLoteEncomendaRequestDTO {
        if (loteId == null) {
            throw new IllegalArgumentException("O lote é obrigatório");
        }
        if (itemEncomendaId == null) {
            throw new IllegalArgumentException("O item da encomenda é obrigatório");
        }
        if (quantidadeReservada == null || !Double.isFinite(quantidadeReservada) || quantidadeReservada <= 0) {
            throw new IllegalArgumentException("A quantidade reservada deve ser superior a zero");
        }
    }
}
