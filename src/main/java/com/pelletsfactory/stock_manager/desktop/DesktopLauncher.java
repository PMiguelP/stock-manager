package com.pelletsfactory.stock_manager.desktop;

import com.pelletsfactory.stock_manager.StockManagerApplication;
import javafx.application.Application;

/**
 * Ponto de entrada para executar o desktop fora de um runtime JavaFX modular.
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(StockManagerApplication.class, args);
    }
}
