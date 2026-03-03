package com.pelletsfactory.stock_manager.enums;

public enum Cargo {
    GERENTE("Gerente"),
    SUPERVISOR("Supervisor"),
    OPERADOR("Operador"),
    ADMINISTRATIVO("Administrativo"),
    ARMAZEM("Armazém");

    private final String displayName;

    Cargo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
