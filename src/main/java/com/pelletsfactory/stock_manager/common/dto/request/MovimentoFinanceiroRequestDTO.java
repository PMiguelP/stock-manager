package com.pelletsfactory.stock_manager.common.dto.request;

import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import java.util.UUID;

public record MovimentoFinanceiroRequestDTO(
        TipoMovimento tipoMovimento,
        Double valorTotal,
        UUID moedaId,
        UUID idEncomendaCliente,
        UUID idEncomendaFornecedor
) {
    public MovimentoFinanceiroRequestDTO {
        if (tipoMovimento == null) {
            throw new IllegalArgumentException("Tipo de movimento é obrigatório");
        }
        if (valorTotal == null || valorTotal <= 0) {
            throw new IllegalArgumentException("Valor total deve ser superior a zero");
        }
        if (moedaId == null) {
            throw new IllegalArgumentException("ID da moeda é obrigatório");
        }
    }
}

