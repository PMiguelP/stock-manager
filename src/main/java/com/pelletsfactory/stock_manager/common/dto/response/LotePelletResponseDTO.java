package com.pelletsfactory.stock_manager.common.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record LotePelletResponseDTO(
    UUID id,
    UUID ordemProducaoId,
    UUID tipoPelletId,
    String tipoPelletNome,
    String codigoLote,
    Double quantidadeKg,
    LocalDateTime dataProducao,
    String localizacaoArmazem
) {
}

