package com.pelletsfactory.stock_manager.common.dto.request;

import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

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
        ValidationUtils.requirePositive(valorTotal, "Valor total");
        if (moedaId == null) {
            throw new IllegalArgumentException("ID da moeda é obrigatório");
        }
    }
}
