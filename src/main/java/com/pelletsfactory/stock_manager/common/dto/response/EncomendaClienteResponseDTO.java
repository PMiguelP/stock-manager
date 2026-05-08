package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import java.time.LocalDate;
import java.util.UUID;

public record EncomendaClienteResponseDTO(
        UUID id,
        UUID clienteId,
        String clienteNome,
        LocalDate data,
        EstadoEncomendaCliente estado,
        Double totalNet,
        Double totalIva,
        Double totalFinal,
        UUID moedaId,
        String moedaCodigo,
        String codigoTracking
) {
}

