package com.pelletsfactory.stock_manager.common.enums;

public enum EstadoOrdemProducao {
    PENDENTE("Pendente"),
    EM_PRODUCAO("Em Produção"),
    CONCLUIDA("Concluída"),
    PAUSADA("Pausada"),
    ANULADA("Cancelada");

    private final String displayName;

    EstadoOrdemProducao(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFinalizado() {
        return this == CONCLUIDA || this == ANULADA;
    }

    public boolean podeEditar() {
        return this == PENDENTE;
    }
}