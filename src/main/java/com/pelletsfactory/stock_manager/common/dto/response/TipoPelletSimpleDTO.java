package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record TipoPelletSimpleDTO(
        UUID id,
        String nome,
        Double diametroMm,
        Double stockAtual,
        Double stockMinimo,
        String moedaCodigo
) {
}

