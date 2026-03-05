package com.pelletsfactory.stock_manager.common.enums;

public enum EstadoEncomendaCliente {
    PENDENTE("Pendente", "warning"),
    CONFIRMADA("Confirmada", "info"),
    EM_PRODUCAO("Em Produção", "primary"),
    PRONTA("Pronta", "success"),
    ENVIADA("Enviada", "primary"),
    ENTREGUE("Entregue", "success"),
    CANCELADA("Cancelada", "danger");

    private final String displayName;
    private final String corEstilo;

    EstadoEncomendaCliente(String displayName, String corEstilo) {
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