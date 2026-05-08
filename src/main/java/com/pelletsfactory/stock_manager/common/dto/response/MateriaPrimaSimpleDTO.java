package com.pelletsfactory.stock_manager.common.dto.response;

import java.util.UUID;

public record MateriaPrimaSimpleDTO(
        UUID id,
        String nome,
        String unidade,
        Double stockAtual,
        Double stockMinimo
) {
}

