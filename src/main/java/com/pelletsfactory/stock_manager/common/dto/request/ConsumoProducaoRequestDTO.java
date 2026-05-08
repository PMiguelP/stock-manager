package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

public record ConsumoProducaoRequestDTO(
        UUID ordemProducaoId,
        UUID materiaPrimaId,
        Double quantidadeConsumidaReal
) {
    public ConsumoProducaoRequestDTO {
        if (ordemProducaoId == null) {
            throw new IllegalArgumentException("O ID da ordem de produção é obrigatório");
        }
        if (materiaPrimaId == null) {
            throw new IllegalArgumentException("O ID da matéria-prima é obrigatório");
        }
        if (quantidadeConsumidaReal == null || quantidadeConsumidaReal <= 0) {
            throw new IllegalArgumentException("A quantidade consumida real deve ser superior a zero");
        }
    }
}