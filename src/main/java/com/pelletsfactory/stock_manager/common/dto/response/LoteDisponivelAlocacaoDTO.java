package com.pelletsfactory.stock_manager.common.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LoteDisponivelAlocacaoDTO(
        UUID loteId,
        String codigoLote,
        UUID tipoPelletId,
        String tipoPelletNome,
        String localizacaoArmazem,
        Instant dataProducao,
        Double quantidadeTotal,
        Double quantidadeReservada,
        Double quantidadeDisponivel
) {
}
