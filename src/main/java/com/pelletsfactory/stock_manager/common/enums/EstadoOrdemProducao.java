package com.pelletsfactory.stock_manager.common.enums;

public enum EstadoOrdemProducao {
    PENDENTE("Pendente", "warning"),
    EM_PRODUCAO("Em Produção", "primary"),
    CONCLUIDA("Concluída", "success"),
    PAUSADA("Pausada", "info"),
    CANCELADA("Cancelada", "danger");

    private final String displayName;
    private final String corEstilo;

    EstadoOrdemProducao(String displayName, String corEstilo) {
        this.displayName = displayName;
        this.corEstilo = corEstilo;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCorEstilo() {
        return corEstilo;
    }

    public boolean isFinalizado() {
        return this == CONCLUIDA || this == CANCELADA;
    }

    public boolean podeEditar() {
        return this == PENDENTE;
    }
}