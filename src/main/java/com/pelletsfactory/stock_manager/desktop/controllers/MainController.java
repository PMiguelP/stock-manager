package com.pelletsfactory.stock_manager.desktop.controllers;

import atlantafx.base.controls.ModalPane;
import com.pelletsfactory.stock_manager.desktop.controllers.components.HeaderController;
import com.pelletsfactory.stock_manager.desktop.controllers.components.SidebarController;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class MainController {
    private final NavigationService navigationService;
    private final ToastService toastService;

    @FXML private BorderPane contentArea;

    // O modalPane vem daqui! O @FXML faz a ligação com o ID no main-view.fxml
    @FXML private ModalPane modalPane;
    @FXML private VBox toastContainer;

    @FXML private SidebarController sidebarIncludeController;
    @FXML private HeaderController headerIncludeController;

    public MainController(NavigationService navigationService, ToastService toastService) {
        this.navigationService = navigationService;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        navigationService.setContentArea(contentArea);

        // Agora o navigationService terá a referência do Modal que cobre tudo
        navigationService.setModalPane(modalPane);
        toastService.setToastContainer(toastContainer);

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