package com.pelletsfactory.stock_manager.common.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClienteDetailsDTO(
        UUID id,
        String nome,
        String nif,
        String contacto,
        String email,
        List<EncomendaClienteSimpleDTO> encomendas,
        Instant createdAt,
        Instant updatedAt
) {
}

