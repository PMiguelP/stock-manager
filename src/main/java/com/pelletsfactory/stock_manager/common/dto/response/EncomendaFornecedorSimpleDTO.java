package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import java.time.LocalDate;
import java.util.UUID;

public record EncomendaFornecedorSimpleDTO(
        UUID id,
        String fornecedorNome,
        LocalDate data,
        EstadoEncomendaFornecedor estado,
        Double totalFinal,
        String moedaCodigo
) {
}

