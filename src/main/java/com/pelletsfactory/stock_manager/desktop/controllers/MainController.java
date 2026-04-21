package com.pelletsfactory.stock_manager.desktop.controllers;

import atlantafx.base.controls.ModalPane; // IMPORTANTE: Adicione este import
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import org.springframework.stereotype.Component;

@Component
public class MainController {
    private final NavigationService navigationService;

    @FXML private BorderPane contentArea;

    // O modalPane vem daqui! O @FXML faz a ligação com o ID no main-view.fxml
    @FXML private ModalPane modalPane;

    public MainController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        navigationService.setContentArea(contentArea);

        // Agora o navigationService terá a referência do Modal que cobre tudo
        navigationService.setModalPane(modalPane);

        navigationService.navigateTo("/dashboard");
    }
}