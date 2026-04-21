package com.pelletsfactory.stock_manager.desktop.controllers.components;

import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.springframework.stereotype.Component;

@Component
public class SidebarController {

    private final NavigationService navigationService;

    // Injeção de todos os botões do FXML
    @FXML private Button btnDashboard, btnFuncionarios, btnOrders, btnProduction,
            btnStock, btnClients, btnSuppliers, btnPurchaseOrders,
            btnRawMaterials, btnPelletTypes, btnFormulas, btnBatches,
            btnSettings, btnSair;

    private Button currentActiveButton;

    public SidebarController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        // Define o Dashboard como ativo ao iniciar
        setActiveButton(btnDashboard);
    }

    // --- MÉTODOS DE NAVEGAÇÃO PRINCIPAL ---

    @FXML
    private void handleDashboard() {
        navigationService.navigateTo("/dashboard");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void handleFuncionarios() {
        navigationService.navigateTo("/funcionarios");
        setActiveButton(btnFuncionarios);
    }

    // --- NOVOS MÉTODOS (Resolvem o LoadException) ---

    @FXML
    private void handleOrders() {
        System.out.println("Navegando para Orders...");
        setActiveButton(btnOrders);
        // navigationService.navigateTo("/orders");
    }

    @FXML
    private void handleProduction() {
        setActiveButton(btnProduction);
    }

    @FXML
    private void handleStock() {
        setActiveButton(btnStock);
    }

    @FXML
    private void handleClients() {
        setActiveButton(btnClients);
    }

    @FXML
    private void handleSuppliers() {
        setActiveButton(btnSuppliers);
    }

    @FXML
    private void handlePurchaseOrders() {
        setActiveButton(btnPurchaseOrders);
    }

    @FXML
    private void handleRawMaterials() {
        setActiveButton(btnRawMaterials);
    }

    @FXML
    private void handlePelletTypes() {
        setActiveButton(btnPelletTypes);
    }

    @FXML
    private void handleFormulas() {
        setActiveButton(btnFormulas);
    }

    @FXML
    private void handleBatches() {
        setActiveButton(btnBatches);
    }

    @FXML
    private void handleSettings() {
        navigationService.navigateTo("/settings");
        setActiveButton(btnSettings);
    }

    // --- LÓGICA DE ESTILO E LOGOUT ---

    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            // Remove a classe de destaque do botão anterior
            currentActiveButton.getStyleClass().remove("accent");
            // Se usares botões flat, podes querer readicionar a classe original aqui
            if (!currentActiveButton.getStyleClass().contains("button-flat")) {
                currentActiveButton.getStyleClass().add("button-flat");
            }
        }

        if (button != null) {
            // Remove o estilo flat para aplicar o estilo accent (destaque)
            button.getStyleClass().remove("button-flat");
            if (!button.getStyleClass().contains("accent")) {
                button.getStyleClass().add("accent");
            }
            currentActiveButton = button;
        }
    }

    @FXML
    private void handleSair() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/login-view.fxml")
            );

            // Importante: Usar o Stage atual para trocar a cena
            javafx.stage.Stage stage = (javafx.stage.Stage) btnSair.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 450, 600);
            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (Exception e) {
            System.err.println("Erro ao fazer logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
}