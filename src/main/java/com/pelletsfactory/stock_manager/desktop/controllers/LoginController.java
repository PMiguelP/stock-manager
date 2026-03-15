package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Controller para a tela de login
 */
@Component
public class LoginController {

    private final ConfigurableApplicationContext springContext;

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblErro;

    public LoginController(ConfigurableApplicationContext springContext) {
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        if (lblErro != null) {
            lblErro.setVisible(false);
        }
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            mostrarErro("Preencha todos os campos!");
            return;
        }

        // TODO: Implementar autenticação real com banco de dados como temos para fazwera
        if (username.length() > 0 && password.length() > 0) {
            abrirTelaPrincipal();
        } else {
            mostrarErro("Credenciais inválidas!");
        }
    }

    //Se o login funcionar vair carregar a main view
    private void abrirTelaPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            loader.setControllerFactory(springContext::getBean);

            Scene scene = new Scene(loader.load(), 1400, 900);
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setTitle("Pellets Factory - Stock Manager");
            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (Exception e) {
            System.err.println("Erro ao carregar tela principal: " + e.getMessage());
            e.printStackTrace();
            mostrarErro("Erro ao carregar aplicação!");
        }
    }
    //Em caso de erro
    private void mostrarErro(String mensagem) {
        if (lblErro != null) {
            lblErro.setText(mensagem);
            lblErro.setVisible(true);

            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    javafx.application.Platform.runLater(() -> lblErro.setVisible(false));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
