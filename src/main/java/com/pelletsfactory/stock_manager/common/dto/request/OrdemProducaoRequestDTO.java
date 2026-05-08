package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;

public record OrdemProducaoRequestDTO(
        UUID tipoPelletId,
        UUID funcionarioId,
        UUID formulaId,
        Double quantidadePlaneada,
        String dataInicio,
        String estado
) {
    public OrdemProducaoRequestDTO {
        if (tipoPelletId == null) {
            throw new IllegalArgumentException("O ID do tipo de pellet é obrigatório");
        }
        if (funcionarioId == null) {
            throw new IllegalArgumentException("O ID do funcionário é obrigatório");
        }
        if (formulaId == null) {
            throw new IllegalArgumentException("O ID da fórmula é obrigatório");
        }

        if (quantidadePlaneada == null || quantidadePlaneada <= 0) {
            throw new IllegalArgumentException("A quantidade planeada deve ser superior a zero");
        }

        if (dataInicio == null || dataInicio.isBlank()) {
            throw new IllegalArgumentException("A data de início é obrigatória");
        }

        // Validação de Estado (Enum manual check)
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("O estado da ordem é obrigatório");
        }
        if (!estado.matches("PENDENTE|EM_PRODUCAO|CONCLUIDA|PAUSADA|ANULADA")) {
            throw new IllegalArgumentException("Estado inválido. Valores aceites: PENDENTE, EM_PRODUCAO, CONCLUIDA, PAUSADA ou ANULADA");
        }
    }
}