package com.pelletsfactory.stock_manager.desktop.controllers.components;

import atlantafx.base.theme.Styles;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.LanguagePreferencesService;
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
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SidebarController {

    private static final Logger log = LoggerFactory.getLogger(SidebarController.class);

    private final NavigationService navigationService;
    private final ThemePreferencesService themePreferencesService;
    private final I18nService i18nService;
    private final ConfigurableApplicationContext springContext;

    @FXML private ToggleButton btnDashboard, btnFuncionarios, btnOrders, btnProduction, btnAllocations,
            btnStock, btnClients, btnSuppliers, btnPurchaseOrders,
            btnRawMaterials, btnPelletTypes, btnFormulas, btnBatches, btnSettings;
    @FXML private Button btnSair;
    @FXML private ImageView sidebarLogoImage;
    @FXML private Label lblMainSection;
    @FXML private Label lblProcurementSection;
    @FXML private Label lblCatalogSection;

    private final ToggleGroup navigationGroup = new ToggleGroup();
    private final Map<ViewId, ToggleButton> navByViewId = new EnumMap<>(ViewId.class);
    private List<ToggleButton> navButtons;

    private static final Background TRANSPARENT_BG =
            new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-insets: 0; -fx-border-insets: 0; -fx-effect: null;";
    private static final String ACTIVE_STYLE =
            "-fx-background-insets: 0; -fx-border-insets: 0;";

    public SidebarController(NavigationService navigationService,
                             ThemePreferencesService themePreferencesService,
                             I18nService i18nService,
                             LanguagePreferencesService languagePreferencesService,
                             ConfigurableApplicationContext springContext) {
        this.navigationService = navigationService;
        this.themePreferencesService = themePreferencesService;
        this.i18nService = i18nService;
        this.springContext = springContext;
        languagePreferencesService.addLanguageChangeListener(lang ->
                Platform.runLater(this::applyTranslations));
    }

    @FXML
    public void initialize() {
        loadThemeLogo();
        themePreferencesService.addThemeChangeListener(() -> Platform.runLater(this::loadThemeLogo));
        applyTranslations();

        navButtons = List.of(
                btnDashboard, btnFuncionarios, btnOrders, btnProduction, btnAllocations,
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
        navByViewId.put(ViewId.ALLOCATIONS, btnAllocations);
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

    private void applyTranslations() {
        if (btnDashboard == null) {
            return;
        }

        lblMainSection.setText(i18nService.translate("sidebar.main"));
        lblProcurementSection.setText(i18nService.translate("sidebar.procurement"));
        lblCatalogSection.setText(i18nService.translate("sidebar.catalog"));

        btnDashboard.setText(i18nService.translate("dashboard.title"));
        btnFuncionarios.setText(i18nService.translate("nav.employees"));
        btnOrders.setText(i18nService.translate("nav.orders"));
        btnProduction.setText(i18nService.translate("nav.production"));
        btnAllocations.setText(i18nService.translate("nav.allocations"));
        btnStock.setText(i18nService.translate("stock.title"));
        btnClients.setText(i18nService.translate("nav.clients"));
        btnSuppliers.setText(i18nService.translate("nav.suppliers"));
        btnPurchaseOrders.setText(i18nService.translate("purchaseOrders.title"));
        btnRawMaterials.setText(i18nService.translate("rawMaterials.title"));
        btnPelletTypes.setText(i18nService.translate("pelletTypes.title"));
        btnFormulas.setText(i18nService.translate("nav.formulas"));
        btnBatches.setText(i18nService.translate("nav.batches"));
        btnSettings.setText(i18nService.translate("settings.title"));
        btnSair.setText(i18nService.translate("nav.logout"));
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
    private void handleAllocations() {
        switchView(btnAllocations);
        navigationService.navigateTo("/allocations");
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
                if (event.viewId() == ViewId.NOTIFICATIONS) {
                    navigationGroup.selectToggle(null);
                    refreshSidebarVisualState(null);
                    return;
                }
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
            loader.setControllerFactory(springContext::getBean);

            javafx.stage.Stage stage = (javafx.stage.Stage) btnSair.getScene().getWindow();
            javafx.scene.Parent root = loader.load();
            i18nService.applyTo(root);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 450, 600);
            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (Exception e) {
            log.error("Erro ao fazer logout", e);
        }
    }
}
