package com.pelletsfactory.stock_manager.common.dto.request;

public record MateriaPrimaRequestDTO(
        String nome,
        String unidade,
        Double stockAtual,
        Double stockMinimo
) {
    public MateriaPrimaRequestDTO {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da matéria-prima é obrigatório");
        }
        if (unidade == null || unidade.isBlank()) {
            throw new IllegalArgumentException("Unidade da matéria-prima é obrigatória");
        }
        if (stockAtual != null && stockAtual < 0) {
            throw new IllegalArgumentException("Stock atual não pode ser negativo");
        }
        if (stockMinimo != null && stockMinimo < 0) {
            throw new IllegalArgumentException("Stock mínimo não pode ser negativo");
        }
    }
}

