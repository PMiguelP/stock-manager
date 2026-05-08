package com.pelletsfactory.stock_manager.common.dto.request;

public record MoedaRequestDTO(
        String codigo,
        String simbolo
) {
    public MoedaRequestDTO {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O código da moeda é obrigatório");
        }
        // Validação de 3 letras maiúsculas (ISO 4217)
        if (!codigo.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("O código deve ter exatamente 3 letras maiúsculas (ex: EUR, USD)");
        }

        if (simbolo == null || simbolo.isBlank()) {
            throw new IllegalArgumentException("O símbolo da moeda é obrigatório");
        }
        if (simbolo.trim().length() < 1 || simbolo.trim().length() > 5) {
            throw new IllegalArgumentException("O símbolo deve ter entre 1 e 5 caracteres");
        }
    }
}