package com.pelletsfactory.stock_manager.common.enums;

public enum TipoMovimento {
    ENTRADA("Entrada"),  // Entrada (Venda)
    SAIDA("Saída");    // Saída (Compra)

    private final String displayName;
    TipoMovimento(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}