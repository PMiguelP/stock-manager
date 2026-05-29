package com.pelletsfactory.stock_manager.desktop.controllers;

import atlantafx.base.theme.Styles;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.services.FuncionarioService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.LanguagePreferencesService;
import com.pelletsfactory.stock_manager.desktop.services.ThemePreferencesService;
import com.pelletsfactory.stock_manager.desktop.services.ThemePreferencesService.ThemeMode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SettingsController {

    private static final String CARD_SELECTED_STYLE = "-fx-background-color: -color-bg-subtle; -fx-border-color: -color-accent-emphasis; -fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 14;";
    private static final String CARD_DEFAULT_STYLE = "-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-default; -fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 14;";
    private static final String PIN_WRAPPER_BASE = "-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;";
    private static final String PIN_WRAPPER_FOCUS = "-fx-background-color: -color-bg-default; -fx-border-color: -color-accent-emphasis; -fx-border-width: 1.2; -fx-background-radius: 10; -fx-border-radius: 10;";

    private final ThemePreferencesService themePreferencesService;
    private final FuncionarioService funcionarioService;
    private final LanguagePreferencesService languagePreferencesService;
    private final I18nService i18nService;

    @FXML private ScrollPane settingsScrollPane;
    @FXML private VBox settingsContent;

    @FXML private Button navAppearanceBtn;
    @FXML private Button navLanguageBtn;
    @FXML private Button navSecurityBtn;

    @FXML private VBox appearanceSection;
    @FXML private VBox languageSection;
    @FXML private VBox securitySection;

    @FXML private ToggleButton cardLight;
    @FXML private ToggleButton cardDark;
    @FXML private ToggleButton cardAuto;

    @FXML private Button accentBlue;
    @FXML private Button accentGreen;
    @FXML private Button accentOrange;
    @FXML private Button accentRed;
    @FXML private Button accentPurple;
    @FXML private Button accentPink;

    @FXML private ComboBox<String> cmbLanguage;

    @FXML private HBox currentPinBox;
    @FXML private HBox newPinBox;
    @FXML private StackPane currentPinIcon;
    @FXML private StackPane newPinIcon;
    @FXML private PasswordField txtCurrentPin;
    @FXML private PasswordField txtNewPin;
    @FXML private Label lblPinFeedback;

    private final ToggleGroup themeCardGroup = new ToggleGroup();
    private final Map<ToggleButton, ThemeMode> themeModeByCard = new LinkedHashMap<>();
    private final Map<Button, String> accentByChip = new LinkedHashMap<>();

    public SettingsController(ThemePreferencesService themePreferencesService,
                              FuncionarioService funcionarioService,
                              LanguagePreferencesService languagePreferencesService,
                              I18nService i18nService) {
        this.themePreferencesService = themePreferencesService;
        this.funcionarioService = funcionarioService;
        this.languagePreferencesService = languagePreferencesService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        setupSectionNavigation();
        setupThemeCards();
        setupAccentChips();
        setupLanguageRegion();
        setupSecurityPinFields();
        loadThemeState();
    }

    private void setupSectionNavigation() {
        List<Button> navButtons = List.of(navAppearanceBtn, navLanguageBtn, navSecurityBtn);
        navButtons.forEach(btn -> {
            btn.setFocusTraversable(false);
            if (!btn.getStyleClass().contains(Styles.FLAT)) {
                btn.getStyleClass().add(Styles.FLAT);
            }
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-insets: 0; -fx-border-insets: 0;");
        });
        activateSectionNav(navAppearanceBtn);
    }

    private void setupThemeCards() {
        bindThemeCard(cardLight, ThemeMode.LIGHT);
        bindThemeCard(cardDark, ThemeMode.DARK);
        bindThemeCard(cardAuto, ThemeMode.AUTO);

        themeCardGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (!(newToggle instanceof ToggleButton selected)) {
                return;
            }
            ThemeMode mode = themeModeByCard.get(selected);
            if (mode != null) {
                themePreferencesService.setThemeMode(mode);
                refreshThemeCardStyles();
            }
        });
    }

    private void bindThemeCard(ToggleButton card, ThemeMode mode) {
        card.setToggleGroup(themeCardGroup);
        card.setFocusTraversable(false);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(CARD_DEFAULT_STYLE);
        themeModeByCard.put(card, mode);
    }

    private void setupAccentChips() {
        accentByChip.put(accentBlue, "#4C7AF2");
        accentByChip.put(accentGreen, "#42B883");
        accentByChip.put(accentOrange, "#F2A43A");
        accentByChip.put(accentRed, "#E3554E");
        accentByChip.put(accentPurple, "#8A63E6");
        accentByChip.put(accentPink, "#D65A9E");

        accentByChip.forEach((chip, color) -> {
            chip.setFocusTraversable(false);
            chip.setOnAction(event -> {
                themePreferencesService.setAccentHex(color);
                refreshAccentChipStyles();
                refreshThemeCardStyles();
            });
        });
    }

    private void setupLanguageRegion() {
        refreshLanguageOptions();

        cmbLanguage.setOnAction(e -> {
            int index = cmbLanguage.getSelectionModel().getSelectedIndex();
            if (index == 1) {
                languagePreferencesService.setLanguage(LanguagePreferencesService.AppLanguage.EN);
            } else {
                languagePreferencesService.setLanguage(LanguagePreferencesService.AppLanguage.PT);
            }
        });
    }

    private void refreshLanguageOptions() {
        String pt = i18nService.translate("Portuguese");
        String en = i18nService.translate("English");
        cmbLanguage.getItems().setAll(pt, en);
        if (languagePreferencesService.getLanguage() == LanguagePreferencesService.AppLanguage.EN) {
            cmbLanguage.getSelectionModel().select(en);
        } else {
            cmbLanguage.getSelectionModel().select(pt);
        }
    }

    private void setupSecurityPinFields() {
        lblPinFeedback.setManaged(false);
        lblPinFeedback.setVisible(false);

        currentPinBox.setStyle(PIN_WRAPPER_BASE);
        newPinBox.setStyle(PIN_WRAPPER_BASE);

        txtCurrentPin.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 12 0 12 0;");
        txtNewPin.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 12 0 12 0;");

        setPinIcon(currentPinIcon, "mdi2l-lock-outline", "#7f8a9b");
        setPinIcon(newPinIcon, "mdi2k-key-outline", "#7f8a9b");

        txtCurrentPin.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,4}") ? change : null
        ));
        txtNewPin.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,4}") ? change : null
        ));

        installPinFieldTransition(currentPinBox, currentPinIcon, txtCurrentPin);
        installPinFieldTransition(newPinBox, newPinIcon, txtNewPin);

        txtNewPin.setOnAction(event -> handleUpdatePin());
    }

    private void installPinFieldTransition(HBox wrapper, StackPane icon, PasswordField field) {
        field.focusedProperty().addListener((obs, oldVal, focused) -> {
            wrapper.setStyle(focused ? PIN_WRAPPER_FOCUS : PIN_WRAPPER_BASE);
            setPinIcon(icon, icon == currentPinIcon ? "mdi2l-lock-outline" : "mdi2k-key-outline",
                    focused ? themePreferencesService.getAccentHex() : "#7f8a9b");
        });
    }

    private void setPinIcon(StackPane container, String iconLiteral, String color) {
        FontIcon icon = new FontIcon();
        icon.setIconLiteral(iconLiteral);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(color));
        container.getChildren().setAll(icon);
    }

    private void loadThemeState() {
        ThemeMode mode = themePreferencesService.getThemeMode();
        Toggle toggle = themeModeByCard.entrySet().stream()
                .filter(entry -> entry.getValue() == mode)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(cardAuto);

        themeCardGroup.selectToggle(toggle);
        refreshThemeCardStyles();
        refreshAccentChipStyles();
    }

    private void refreshThemeCardStyles() {
        Toggle selected = themeCardGroup.getSelectedToggle();
        themeModeByCard.keySet().forEach(card -> card.setStyle(card == selected ? CARD_SELECTED_STYLE : CARD_DEFAULT_STYLE));
    }

    private void refreshAccentChipStyles() {
        String currentAccent = themePreferencesService.getAccentHex();
        accentByChip.forEach((chip, color) -> {
            boolean selected = color.equalsIgnoreCase(currentAccent);
            String style = "-fx-background-color: " + color + ";"
                    + "-fx-min-width: 42; -fx-max-width: 42;"
                    + "-fx-min-height: 42; -fx-max-height: 42;"
                    + "-fx-background-radius: 21;"
                    + "-fx-border-radius: 21;"
                    + "-fx-border-width: 3;"
                    + "-fx-border-color: " + (selected ? "-color-accent-emphasis" : "-color-border-default") + ";";
            chip.setStyle(style);
        });
    }

    private void activateSectionNav(Button active) {
        for (Button nav : List.of(navAppearanceBtn, navLanguageBtn, navSecurityBtn)) {
            boolean selected = nav == active;
            nav.getStyleClass().remove(Styles.ACCENT);
            nav.setStyle(selected
                    ? "-fx-background-color: -color-accent-emphasis; -fx-text-fill: white; -fx-background-radius: 10; -fx-border-color: transparent;"
                    : "-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: -color-fg-default;");
        }
    }

    private void scrollToSection(Region section, Button navButton) {
        activateSectionNav(navButton);
        Platform.runLater(() -> {
            double contentHeight = settingsContent.getBoundsInLocal().getHeight();
            double viewportHeight = settingsScrollPane.getViewportBounds().getHeight();
            double targetY = section.getBoundsInParent().getMinY();
            double denominator = Math.max(1, contentHeight - viewportHeight);
            double v = Math.max(0, Math.min(1, targetY / denominator));
            settingsScrollPane.setVvalue(v);
        });
    }

    @FXML
    private void scrollToAppearance() {
        scrollToSection(appearanceSection, navAppearanceBtn);
    }

    @FXML
    private void scrollToLanguage() {
        scrollToSection(languageSection, navLanguageBtn);
    }

    @FXML
    private void scrollToSecurity() {
        scrollToSection(securitySection, navSecurityBtn);
    }

    @FXML
    private void handleUpdatePin() {
        String currentPin = txtCurrentPin.getText() == null ? "" : txtCurrentPin.getText().trim();
        String newPin = txtNewPin.getText() == null ? "" : txtNewPin.getText().trim();

        if (!currentPin.matches("\\d{4}")) {
            showPinFeedback("Current PIN must contain exactly 4 digits.", true);
            return;
        }
        if (!newPin.matches("\\d{4}")) {
            showPinFeedback("New PIN must contain exactly 4 digits.", true);
            return;
        }
        if (currentPin.equals(newPin)) {
            showPinFeedback("New PIN must be different from current PIN.", true);
            return;
        }

        Funcionario logged = SessaoFuncionario.getFuncionarioLogado();
        if (logged == null) {
            showPinFeedback("Session expired. Please login again.", true);
            return;
        }

        try {
            funcionarioService.alterarPin(logged.getId(), currentPin, newPin);
            txtCurrentPin.clear();
            txtNewPin.clear();
            showPinFeedback("PIN updated successfully.", false);
        } catch (RuntimeException ex) {
            showPinFeedback(ex.getMessage() != null ? ex.getMessage() : "Could not update PIN.", true);
        }
    }

    private void showPinFeedback(String message, boolean error) {
        lblPinFeedback.getStyleClass().removeAll(Styles.DANGER, Styles.SUCCESS, Styles.TEXT_MUTED);
        lblPinFeedback.getStyleClass().add(error ? Styles.DANGER : Styles.SUCCESS);
        lblPinFeedback.setText(message);
        lblPinFeedback.setManaged(true);
        lblPinFeedback.setVisible(true);
        VBox.setMargin(lblPinFeedback, new Insets(2, 0, 0, 0));
    }
}
