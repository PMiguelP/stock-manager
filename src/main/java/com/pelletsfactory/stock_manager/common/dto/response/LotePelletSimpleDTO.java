package com.pelletsfactory.stock_manager.common.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record LotePelletSimpleDTO(
    UUID id,
    String codigoLote,
    Double quantidadeKg,
    LocalDateTime dataProducao,
    String localizacaoArmazem
) {
}

