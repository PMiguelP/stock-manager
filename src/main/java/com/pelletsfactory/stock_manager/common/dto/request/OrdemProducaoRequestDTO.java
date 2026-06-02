package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

public record OrdemProducaoRequestDTO(
        UUID tipoPelletId,
        UUID funcionarioId,
        UUID formulaId,
        Double quantidadePlaneada,
        Double quantidadeProduzidaReal,
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

        ValidationUtils.requirePositive(quantidadePlaneada, "A quantidade planeada");

        if (quantidadeProduzidaReal != null) {
            ValidationUtils.requireNonNegative(quantidadeProduzidaReal, "A quantidade produzida real");
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
