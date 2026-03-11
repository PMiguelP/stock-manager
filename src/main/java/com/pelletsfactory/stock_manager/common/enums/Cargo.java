package com.pelletsfactory.stock_manager.common.enums;

public enum Cargo {
    ADMINISTRADOR("Administrador"),
    RESPONSAVEL_PRODUCAO("Responsavel Producao"),
    OPERADOR_PRODUCAO("Operador Producao"),
    RESPONSAVEL_LOGISTICA("Responsavel Logistica"),
    ASSISTENTE_COMERCIAL("Assistente Comercial");

    private final String displayName;

    Cargo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
