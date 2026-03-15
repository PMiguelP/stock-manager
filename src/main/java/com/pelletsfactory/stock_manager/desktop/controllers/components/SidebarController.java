package com.pelletsfactory.stock_manager.desktop.controllers.components;

import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.springframework.stereotype.Component;
@Component
public class SidebarController {

    private final NavigationService navigationService;

    @FXML private Button btnDashboard;
    @FXML private Button btnFuncionarios;

    private Button currentActiveButton;

    public SidebarController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        setActiveButton(btnDashboard);
    }

    @FXML
    private void handleDashboard() {
        navigationService.navigateTo("/dashboard");
        setActiveButton(btnDashboard);
    }

    //CArrega funcionario
    @FXML
    private void handleFuncionarios() {
        navigationService.navigateTo("/funcionarios");
        setActiveButton(btnFuncionarios);
    }

    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("accent");
        }

        if (button != null && !button.getStyleClass().contains("accent")) {
            button.getStyleClass().add("accent");
        }

        currentActiveButton = button;
    }

    //Logout ainda por fazer
    @FXML
    private void handleSair() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/login-view.fxml")
            );

            javafx.stage.Stage stage = (javafx.stage.Stage) btnDashboard.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 450, 600);
            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (Exception e) {
            System.err.println("Erro ao fazer logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
