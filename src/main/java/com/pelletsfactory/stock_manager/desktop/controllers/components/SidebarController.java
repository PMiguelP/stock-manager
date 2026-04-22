package com.pelletsfactory.stock_manager.desktop.controllers.components;

import atlantafx.base.theme.Styles;
import com.pelletsfactory.stock_manager.desktop.services.NavigationEvent;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ThemePreferencesService;
import com.pelletsfactory.stock_manager.desktop.services.ViewId;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SidebarController {

    private final NavigationService navigationService;
    private final ThemePreferencesService themePreferencesService;

    @FXML private ToggleButton btnDashboard, btnFuncionarios, btnOrders, btnProduction,
            btnStock, btnClients, btnSuppliers, btnPurchaseOrders,
            btnRawMaterials, btnPelletTypes, btnFormulas, btnBatches, btnSettings;
    @FXML private Button btnSair;
    @FXML private ImageView sidebarLogoImage;

    private final ToggleGroup navigationGroup = new ToggleGroup();
    private final Map<ViewId, ToggleButton> navByViewId = new EnumMap<>(ViewId.class);
    private List<ToggleButton> navButtons;

    private static final Background TRANSPARENT_BG =
            new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-insets: 0; -fx-border-insets: 0; -fx-effect: null;";
    private static final String ACTIVE_STYLE =
            "-fx-background-insets: 0; -fx-border-insets: 0;";

    public SidebarController(NavigationService navigationService, ThemePreferencesService themePreferencesService) {
        this.navigationService = navigationService;
        this.themePreferencesService = themePreferencesService;
    }

    @FXML
    public void initialize() {
        loadThemeLogo();
        themePreferencesService.addThemeChangeListener(() -> Platform.runLater(this::loadThemeLogo));

        navButtons = List.of(
                btnDashboard, btnFuncionarios, btnOrders, btnProduction,
                btnStock, btnClients, btnSuppliers, btnPurchaseOrders,
                btnRawMaterials, btnPelletTypes, btnFormulas, btnBatches,
                btnSettings
        );

        navButtons.forEach(this::prepareSidebarButton);
        btnSair.setFocusTraversable(false);
        applyAtlantaFlatStyle(btnSair);
        clearInactiveVisuals(btnSair);

        navigationGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            ToggleButton selected = (newToggle instanceof ToggleButton tb) ? tb : null;
            refreshSidebarVisualState(selected);
        });

        navByViewId.put(ViewId.DASHBOARD, btnDashboard);
        navByViewId.put(ViewId.FUNCIONARIOS, btnFuncionarios);
        navByViewId.put(ViewId.ORDERS, btnOrders);
        navByViewId.put(ViewId.PRODUCTION, btnProduction);
        navByViewId.put(ViewId.STOCK, btnStock);
        navByViewId.put(ViewId.CLIENTS, btnClients);
        navByViewId.put(ViewId.SUPPLIERS, btnSuppliers);
        navByViewId.put(ViewId.PURCHASE_ORDERS, btnPurchaseOrders);
        navByViewId.put(ViewId.RAW_MATERIALS, btnRawMaterials);
        navByViewId.put(ViewId.PELLET_TYPES, btnPelletTypes);
        navByViewId.put(ViewId.FORMULAS, btnFormulas);
        navByViewId.put(ViewId.BATCHES, btnBatches);
        navByViewId.put(ViewId.SETTINGS, btnSettings);

        setActiveView(ViewId.DASHBOARD);
    }

    private void loadThemeLogo() {
        if (sidebarLogoImage == null) {
            return;
        }

        URL logoUrl = getClass().getResource(themePreferencesService.getThemeLogoResource());
        if (logoUrl == null) {
            return;
        }

        sidebarLogoImage.setImage(new Image(logoUrl.toExternalForm(), true));
    }

    private void prepareSidebarButton(ToggleButton button) {
        button.setToggleGroup(navigationGroup);
        button.setFocusTraversable(false);
        applyAtlantaFlatStyle(button);
        clearInactiveVisuals(button);
    }

    private void applyAtlantaFlatStyle(ButtonBase button) {
        if (!button.getStyleClass().contains(Styles.FLAT)) {
            button.getStyleClass().add(Styles.FLAT);
        }
    }

    // --- MÉTODOS DE NAVEGAÇÃO PRINCIPAL ---

    @FXML
    private void handleDashboard() {
        switchView(btnDashboard);
        navigationService.navigateTo("/dashboard");
    }

    @FXML
    private void handleFuncionarios() {
        switchView(btnFuncionarios);
        navigationService.navigateTo("/funcionarios");
    }

    @FXML
    private void handleOrders() {
        switchView(btnOrders);
        navigationService.navigateTo("/orders");
    }

    @FXML
    private void handleProduction() {
        switchView(btnProduction);
        navigationService.navigateTo("/production");
    }

    @FXML
    private void handleStock() {
        switchView(btnStock);
        navigationService.navigateTo("/stock");
    }

    @FXML
    private void handleClients() {
        switchView(btnClients);
        navigationService.navigateTo("/clients");
    }

    @FXML
    private void handleSuppliers() {
        switchView(btnSuppliers);
        navigationService.navigateTo("/suppliers");
    }

    @FXML
    private void handlePurchaseOrders() {
        switchView(btnPurchaseOrders);
        navigationService.navigateTo("/purchase-orders");
    }

    @FXML
    private void handleRawMaterials() {
        switchView(btnRawMaterials);
        navigationService.navigateTo("/raw-materials");
    }

    @FXML
    private void handlePelletTypes() {
        switchView(btnPelletTypes);
        navigationService.navigateTo("/pellet-types");
    }

    @FXML
    private void handleFormulas() {
        switchView(btnFormulas);
        navigationService.navigateTo("/formulas");
    }

    @FXML
    private void handleBatches() {
        switchView(btnBatches);
        navigationService.navigateTo("/batches");
    }

    @FXML
    private void handleSettings() {
        switchView(btnSettings);
        navigationService.navigateTo("/settings");
    }

    @EventListener
    public void onNavigationEvent(NavigationEvent event) {
        Platform.runLater(() -> {
            if (event.viewId() != null) {
                setActiveView(event.viewId());
            }
        });
    }

    public void setSelectedButton(ToggleButton button) {
        if (button != null) {
            switchView(button);
        }
    }

    public ToggleGroup getNavigationGroup() {
        return navigationGroup;
    }

    private void setActiveView(ViewId viewId) {
        ToggleButton target = navByViewId.get(viewId);
        if (target != null) {
            switchView(target);
        }
    }

    public void switchView(ToggleButton targetBtn) {
        if (targetBtn == null) {
            return;
        }

        navigationGroup.selectToggle(targetBtn);
        targetBtn.setSelected(true);
        refreshSidebarVisualState(targetBtn);
    }

    private void refreshSidebarVisualState(ToggleButton selectedButton) {
        for (ToggleButton navButton : navButtons) {
            boolean active = navButton == selectedButton;
            if (active) {
                if (!navButton.getStyleClass().contains("accent")) {
                    navButton.getStyleClass().add("accent");
                }
                navButton.setStyle(ACTIVE_STYLE);
            } else {
                navButton.getStyleClass().remove("accent");
                clearInactiveVisuals(navButton);
            }
        }
    }

    private void clearInactiveVisuals(ButtonBase button) {
        if (button instanceof Node node) {
            node.setStyle(INACTIVE_STYLE);
        }
        if (button instanceof ToggleButton toggleButton) {
            toggleButton.setBackground(TRANSPARENT_BG);
            toggleButton.setBorder(Border.EMPTY);
        } else if (button instanceof Button normalButton) {
            normalButton.setBackground(TRANSPARENT_BG);
            normalButton.setBorder(Border.EMPTY);
        }
    }

    // Fallback for plain Button usage: manually mark active pseudo-class.
    public void applyManualPseudoClass(ButtonBase activeButton, List<? extends ButtonBase> allButtons) {
        for (ButtonBase candidate : allButtons) {
            candidate.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), candidate == activeButton);
        }
    }

    @FXML
    private void handleSair() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/login-view.fxml")
            );

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


