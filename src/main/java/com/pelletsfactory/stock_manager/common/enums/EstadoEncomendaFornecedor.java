package com.pelletsfactory.stock_manager.common.enums;

public enum EstadoEncomendaFornecedor {
    PENDENTE("Pendente", "warning"),
    CONFIRMADA("Confirmada", "info"),
    EM_TRANSITO("Em Trânsito", "primary"),
    RECEBIDA("Recebida", "success"),
    CANCELADA("Cancelada", "danger");

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