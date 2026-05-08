package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OrdemProducaoResponseDTO(
    UUID id,
    UUID tipoPelletId,
    String tipoPelletNome,
    UUID funcionarioId,
    String funcionarioNome,
    UUID formulaId,
    String formulaNome,
    Double quantidadePlaneada,
    Double quantidadeProduzidaReal,
    Instant dataInicio,
    Instant dataFim,
    EstadoOrdemProducao estado
) {
    public OrdemProducaoResponseDTO {
        Objects.requireNonNull(id, "ID não pode ser nulo");
        Objects.requireNonNull(estado, "Estado não pode ser nulo");
    }
}
