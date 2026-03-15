package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import org.springframework.stereotype.Component;

@Component
public class MainController {
    private final NavigationService navigationService;

    @FXML private BorderPane contentArea;

    public MainController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        navigationService.setContentArea(contentArea);

        navigationService.navigateTo("/dashboard");
    }
}
