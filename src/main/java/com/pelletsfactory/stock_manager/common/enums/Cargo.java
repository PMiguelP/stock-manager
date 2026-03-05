package com.pelletsfactory.stock_manager.common.enums;

public enum Cargo {
    //TODO change the roles names
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
