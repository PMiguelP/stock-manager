package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EncomendaFornecedorDetailsDTO(
        UUID id,
        UUID fornecedorId,
        String fornecedorNome,
        LocalDate data,
        EstadoEncomendaFornecedor estado,
        Double totalLiquido,
        Double totalIva,
        Double totalFinal,
        UUID moedaId,
        String moedaCodigo,
        List<ItemEncomendaFornecedorResponseDTO> itens,
        Instant createdAt,
        Instant updatedAt
) {
}

