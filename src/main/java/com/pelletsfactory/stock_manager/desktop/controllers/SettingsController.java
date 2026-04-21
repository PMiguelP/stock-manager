package com.pelletsfactory.stock_manager.desktop.controllers;

import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class SettingsController {

    // Adicionei o tema Dracula que estava no teu FXML mas faltava no controller
    @FXML
    private void changeToDracula() {
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
    }

    @FXML
    private void changeToPrimerLight() {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }

    @FXML
    private void changeToPrimerDark() {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
    }

    @FXML
    private void changeToNordLight() {
        Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
    }

    @FXML
    private void changeToNordDark() {
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
    }

    @FXML
    private void changeToCupertinoLight() {
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
    }

    @FXML
    private void changeToCupertinoDark() {
        Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
    }

    // Podes adicionar aqui as ações dos botões de Save Changes mais tarde
    @FXML
    private void handleSaveChanges() {
        System.out.println("Configurações de perfil guardadas!");
    }
}