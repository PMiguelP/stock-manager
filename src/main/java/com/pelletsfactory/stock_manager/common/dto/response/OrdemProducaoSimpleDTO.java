package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import java.time.Instant;
import java.util.UUID;

public record OrdemProducaoSimpleDTO(
    UUID id,
    String tipoPelletNome,
    String funcionarioNome,
    Double quantidadePlaneada,
    Double quantidadeProduzidaReal,
    Instant dataInicio,
    EstadoOrdemProducao estado
) {
}
