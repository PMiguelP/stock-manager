package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

public record AlocacaoOrdemEncomendaRequestDTO(
        UUID ordemId,
        UUID encomendaClienteId,
        Double quantidadeReservada
) {
    public AlocacaoOrdemEncomendaRequestDTO {
        if (ordemId == null) {
            throw new IllegalArgumentException("O ID da ordem é obrigatório");
        }
        if (encomendaClienteId == null) {
            throw new IllegalArgumentException("O ID da encomenda de cliente é obrigatório");
        }
        if (quantidadeReservada == null || quantidadeReservada <= 0) {
            throw new IllegalArgumentException("A quantidade reservada deve ser superior a zero");
        }
    }
}