package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;
@Component
public class DashboardController {

    @FXML private Label lblTotalFuncionarios;
    @FXML private Label lblFuncionariosAtivos;
    @FXML private Label lblProducaoMes;
    @FXML private Label lblStockAtual;

    @FXML
    public void initialize() {
        carregarDadosEstaticos();
    }

    //E so para teste de momento depois vamos retirar isto tudo
    private void carregarDadosEstaticos() {
        if (lblTotalFuncionarios != null) {
            lblTotalFuncionarios.setText("42");
        }

        if (lblFuncionariosAtivos != null) {
            lblFuncionariosAtivos.setText("38");
        }

        if (lblProducaoMes != null) {
            lblProducaoMes.setText("15.340 ton");
        }

        if (lblStockAtual != null) {
            lblStockAtual.setText("2.850 ton");
        }
    }
    @FXML
    private void handleAtualizar() {
        System.out.println("Atualizando dashboard...");
        carregarDadosEstaticos();
    }
}
