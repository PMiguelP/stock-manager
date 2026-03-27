package com.pelletsfactory.stock_manager.common.enums;

public enum TipoEventoNotificacao {
    STOCK_BAIXO("Stock Baixo"),
    NOVA_ENCOMENDA("Nova Encomenda"),
    NOVA_ORDEM_PRODUCAO("Nova Ordem de Produção"),
    ORDEM_CONCLUIDA("Ordem Concluída"),
    EXPEDICAO_REALIZADA("Expedição Realizada"),
    ERRO_PRODUCAO("Erro na Produção");


    private final String displayName;

    TipoEventoNotificacao(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
