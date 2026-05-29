package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import java.util.UUID;

public record AlocacaoSimpleDTO(
        UUID encomendaId,
        String clienteNome,
        EstadoEncomendaCliente estadoEncomenda,
        Double quantidadeReservada,
        String codigoTracking
) {}