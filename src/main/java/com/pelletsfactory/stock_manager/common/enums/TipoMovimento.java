package com.pelletsfactory.stock_manager.common.enums;

public enum TipoMovimento {
    ENTRADA("Entrada"),
    SAIDA("Saída");

    private final String displayName;
    TipoMovimento(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}