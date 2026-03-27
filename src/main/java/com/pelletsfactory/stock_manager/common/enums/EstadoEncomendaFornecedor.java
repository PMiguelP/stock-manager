package com.pelletsfactory.stock_manager.common.enums;

public enum EstadoEncomendaFornecedor {
    RASCUNHO("Rascunho"),
    EFETIVA("Efetiva"),
    RECEBIDA("Recebida"),
    ANULADA("Anulada");

    private final String displayName;

    EstadoEncomendaFornecedor(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}