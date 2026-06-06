package com.pelletsfactory.stock_manager.common.enums;

public enum EstadoTicket {
    AGUARDA_EQUIPE("Aguarda equipa"),
    AGUARDA_CLIENTE("Aguarda cliente"),
    RESOLVIDO("Resolvido");

    private final String displayName;

    EstadoTicket(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
