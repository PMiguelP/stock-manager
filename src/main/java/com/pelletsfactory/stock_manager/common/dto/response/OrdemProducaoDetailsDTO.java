package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrdemProducaoDetailsDTO(
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
        EstadoOrdemProducao estado,
        List<ConsumoResponseDTO> consumos,
        List<LotePelletSimpleDTO> lotes,
        List<AlocacaoSimpleDTO> encomendas,
        Instant createdAt,
        Instant updatedAt
) {
}