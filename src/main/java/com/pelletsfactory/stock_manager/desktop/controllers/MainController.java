package com.pelletsfactory.stock_manager.desktop.controllers;

import atlantafx.base.controls.ModalPane;
import com.pelletsfactory.stock_manager.desktop.controllers.components.HeaderController;
import com.pelletsfactory.stock_manager.desktop.controllers.components.SidebarController;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import org.springframework.stereotype.Component;

@Component
public class MainController {
    private final NavigationService navigationService;

    @FXML private BorderPane contentArea;

    // O modalPane vem daqui! O @FXML faz a ligação com o ID no main-view.fxml
    @FXML private ModalPane modalPane;

    @FXML private SidebarController sidebarIncludeController;
    @FXML private HeaderController headerIncludeController;

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


    public void MapsTo(String fxmlFile, ToggleButton sidebarBtn, String... breadcrumbPath) {
        Node view = navigationService.loadExternalView(fxmlFile);
        if (view != null) {
            contentArea.setCenter(view);
        }

        if (sidebarBtn != null && sidebarIncludeController != null) {
            if (sidebarBtn.getToggleGroup() == null) {
                sidebarBtn.setToggleGroup(sidebarIncludeController.getNavigationGroup());
            }
            sidebarBtn.setSelected(true);
            sidebarIncludeController.setSelectedButton(sidebarBtn);
        }

        if (headerIncludeController != null && breadcrumbPath != null && breadcrumbPath.length > 0) {
            headerIncludeController.updateBreadcrumbPath(breadcrumbPath);
        }
    }
}