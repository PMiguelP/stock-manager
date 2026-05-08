package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import java.time.LocalDate;
import java.util.UUID;

public record EncomendaFornecedorResponseDTO(
        UUID id,
        UUID fornecedorId,
        String fornecedorNome,
        LocalDate data,
        EstadoEncomendaFornecedor estado,
        Double totalLiquido,
        Double totalIva,
        Double totalFinal,
        UUID moedaId,
        String moedaCodigo
) {
}

