package com.pelletsfactory.stock_manager.common.enums;

public enum EstadoEncomendaFornecedor {
    RASCUNHO("Rascunho", "warning"),
    EFETIVA("Efetiva", "info"),
    RECEBIDA("Recebida", "success"),
    ANULADA("Anulada", "danger");

    private final String displayName;
    private final String corEstilo;

    EstadoEncomendaFornecedor(String displayName, String corEstilo) {
        this.displayName = displayName;
        this.corEstilo = corEstilo;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCorEstilo() {
        return corEstilo;
    }
}