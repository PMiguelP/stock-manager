package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record FornecedorResponseDTO(
        UUID id,
        String nome,
        String nif,
        String contacto,
        String email
) {
}

