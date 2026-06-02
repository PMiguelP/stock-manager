package com.pelletsfactory.stock_manager.common.dto.request;

import java.util.UUID;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;

public record LotePelletRequestDTO(
        UUID ordemProducaoId,
        UUID tipoPelletId,
        String codigoLote,
        Double quantidadeKg,
        String localizacaoArmazem
) {
    public LotePelletRequestDTO {
        if (ordemProducaoId == null) {
            throw new IllegalArgumentException("O ID da ordem de produção é obrigatório");
        }
        if (tipoPelletId == null) {
            throw new IllegalArgumentException("O ID do tipo de pellet é obrigatório");
        }
        if (codigoLote == null || codigoLote.isBlank()) {
            throw new IllegalArgumentException("O código do lote é obrigatório");
        }
        if (codigoLote.trim().length() < 3 || codigoLote.trim().length() > 50) {
            throw new IllegalArgumentException("O código do lote deve ter entre 3 e 50 caracteres");
        }
        ValidationUtils.requirePositive(quantidadeKg, "A quantidade em kg");
        // Localização é opcional na DB, mas se enviada, validamos o tamanho
        if (localizacaoArmazem != null && localizacaoArmazem.length() > 100) {
            throw new IllegalArgumentException("A localização no armazém não pode exceder 100 caracteres");
        }
    }
}
