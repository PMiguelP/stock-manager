package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import java.time.LocalDate;
import java.util.UUID;

public record EncomendaClienteSimpleDTO(
        UUID id,
        String clienteNome,
        LocalDate data,
        EstadoEncomendaCliente estado,
        Double totalFinal,
        String moedaCodigo,
        String codigoTracking
) {
}

