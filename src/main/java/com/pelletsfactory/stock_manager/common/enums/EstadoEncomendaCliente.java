package com.pelletsfactory.stock_manager.common.enums;

public enum EstadoEncomendaCliente {
    PENDENTE("Pendente"),
    CONFIRMADA("Confirmada"),
    EM_PRODUCAO("Em Produção"),
    PRONTA("Pronta"),
    EXPEDIDA("Expedida"),
    CANCELADA("Cancelada");

    private final String displayName;

    EstadoEncomendaCliente(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}