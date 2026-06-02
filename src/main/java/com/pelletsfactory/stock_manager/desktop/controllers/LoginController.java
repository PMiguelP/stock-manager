package com.pelletsfactory.stock_manager.desktop.controllers;

import atlantafx.base.theme.Styles;
import com.pelletsfactory.stock_manager.common.services.AuthService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.ThemePreferencesService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;

/**
 * Controller para a tela de login
 */
@Component
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private static final String WRAPPER_BASE_STYLE = "-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-default; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;";
    private static final String WRAPPER_FOCUS_STYLE = "-fx-background-color: -color-bg-subtle; -fx-border-color: -color-accent-emphasis; -fx-border-width: 1.2; -fx-background-radius: 10; -fx-border-radius: 10;";

    private final ConfigurableApplicationContext springContext;
    private final AuthService authService;
    private final ThemePreferencesService themePreferencesService;
    private final I18nService i18nService;

    @FXML private HBox rootPane;
    @FXML private StackPane formSide;
    @FXML private VBox formBox;
    @FXML private VBox logoBox;
    @FXML private Label lblWelcome;
    @FXML private Label lblSubtitle;
    @FXML private HBox employeeFieldBox;
    @FXML private HBox pinFieldBox;
    @FXML private FontIcon employeeIcon;
    @FXML private FontIcon pinIcon;
    @FXML private TextField txtEmployeeNumber;
    @FXML private PasswordField txtPin;
    @FXML private Hyperlink lnkForgotPin;
    @FXML private Label lblErro;
    @FXML private Button btnLogin;
    @FXML private StackPane visualSide;
    @FXML private ImageView heroImage;
    @FXML private Region heroOverlay;
    @FXML private Label lblVisualTitle;
    @FXML private Label lblVisualSubtitle;

    public LoginController(
            ConfigurableApplicationContext springContext,
            AuthService authService,
            ThemePreferencesService themePreferencesService,
            I18nService i18nService
    ) {
        this.springContext = springContext;
        this.authService = authService;
        this.themePreferencesService = themePreferencesService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        styleLayout();
        buildBrandLogo();
        setupValidation();
        setupFocusTransitions();
        setupSingleClickAccent();
        loadHeroImage();

        lblErro.setVisible(false);
        lblErro.setManaged(false);
    }


    private void styleLayout() {
        rootPane.setStyle("-fx-background-color: -color-bg-default;");

        formSide.setStyle("-fx-background-color: -color-bg-default;");
        formBox.setStyle("-fx-background-color: -color-bg-default;");

        lblWelcome.getStyleClass().add(Styles.TITLE_1);
        lblSubtitle.getStyleClass().add(Styles.TEXT_MUTED);

        employeeFieldBox.setStyle(WRAPPER_BASE_STYLE);
        pinFieldBox.setStyle(WRAPPER_BASE_STYLE);

        employeeIcon.setIconCode(MaterialDesignA.ACCOUNT_GROUP);
        employeeIcon.setIconSize(18);
        pinIcon.setIconCode(MaterialDesignL.LOCK_OUTLINE);
        pinIcon.setIconSize(17);

        txtEmployeeNumber.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 12 0 12 0;");
        txtPin.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 12 0 12 0;");

        employeeIcon.setIconColor(Color.web("#7f8a9b"));
        pinIcon.setIconColor(Color.web("#7f8a9b"));

        lnkForgotPin.getStyleClass().add(Styles.ACCENT);
        lnkForgotPin.setFocusTraversable(false);

        lblErro.getStyleClass().add(Styles.DANGER);
        lblErro.setStyle("-fx-font-size: 12px;");

        lblVisualTitle.getStyleClass().add(Styles.TITLE_2);
        lblVisualTitle.setStyle("-fx-text-fill: white;");
        lblVisualSubtitle.getStyleClass().add(Styles.TEXT_MUTED);
        lblVisualSubtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.82);");

        heroOverlay.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(8,27,58,0.10), rgba(8,27,58,0.42));");
        heroOverlay.setMouseTransparent(true);

        btnLogin.setFocusTraversable(false);
    }

    private void buildBrandLogo() {
        logoBox.getChildren().clear();

        URL logoUrl = getClass().getResource(themePreferencesService.getThemeLogoResource());
        if (logoUrl != null) {
            ImageView logoImage = new ImageView(new Image(logoUrl.toExternalForm(), true));
            logoImage.setFitWidth(220);
            logoImage.setPreserveRatio(true);
            logoImage.setSmooth(true);
            logoImage.setFocusTraversable(false);
            logoBox.getChildren().add(logoImage);
            return;
        }

        FontIcon logoIcon = new FontIcon("mdi2f-factory:30");
        logoIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
        logoIcon.setFocusTraversable(false);

        Label stockLabel = new Label("STOCK");
        stockLabel.getStyleClass().add(Styles.TITLE_2);
        stockLabel.getStyleClass().add(Styles.TEXT_BOLD);

        Label managerLabel = new Label("MANAGER");
        managerLabel.getStyleClass().add(Styles.TITLE_2);
        managerLabel.getStyleClass().add(Styles.TEXT_BOLD);
        managerLabel.setStyle("-fx-text-fill: -color-accent-emphasis;");

        HBox wordmark = new HBox(6, stockLabel, managerLabel);

        Label tagline = new Label("Pellets Factory");
        tagline.getStyleClass().add(Styles.TEXT_MUTED);

        HBox brandRow = new HBox(10, logoIcon, wordmark);
        brandRow.setStyle("-fx-alignment: CENTER_LEFT;");

        logoBox.getChildren().addAll(brandRow, tagline);
    }

    private void setupValidation() {
        txtEmployeeNumber.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,8}") ? change : null
        ));

        txtPin.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,4}") ? change : null
        ));

        txtPin.setOnAction(event -> handleLogin());
    }

    private void setupFocusTransitions() {
        installFieldTransition(employeeFieldBox, employeeIcon, txtEmployeeNumber);
        installFieldTransition(pinFieldBox, pinIcon, txtPin);

        txtEmployeeNumber.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                lblErro.setVisible(false);
                lblErro.setManaged(false);
            }
        });
    }

    private void installFieldTransition(HBox wrapper, FontIcon icon, TextField field) {
        field.focusedProperty().addListener((obs, oldVal, focused) -> {
            wrapper.setStyle(focused ? WRAPPER_FOCUS_STYLE : WRAPPER_BASE_STYLE);

            FadeTransition transition = new FadeTransition(Duration.millis(120), icon);
            transition.setFromValue(focused ? 0.65 : 1.0);
            transition.setToValue(focused ? 1.0 : 0.75);
            transition.play();

            icon.setIconColor(focused
                    ? Color.web("#216fe5")
                    : Color.web("#7f8a9b"));
        });
    }

    private void setupSingleClickAccent() {
        if (!btnLogin.getStyleClass().contains(Styles.ACCENT)) {
            btnLogin.getStyleClass().add(Styles.ACCENT);
        }

        btnLogin.setStyle("-fx-background-radius: 10; -fx-padding: 12 16 12 16;");

        btnLogin.armedProperty().addListener((obs, oldVal, armed) -> updateLoginVisualState(armed));
        btnLogin.focusedProperty().addListener((obs, oldVal, focused) -> updateLoginVisualState(focused || btnLogin.isArmed()));
        btnLogin.setOnMousePressed(event -> updateLoginVisualState(true));
        btnLogin.setOnMouseReleased(event -> updateLoginVisualState(false));
    }

    private void updateLoginVisualState(boolean active) {
        if (active) {
            btnLogin.setStyle("-fx-background-radius: 10; -fx-padding: 12 16 12 16; -fx-effect: dropshadow(gaussian, rgba(33,111,229,0.30), 12, 0.18, 0, 2);");
        } else {
            btnLogin.setStyle("-fx-background-radius: 10; -fx-padding: 12 16 12 16; -fx-effect: null;");
        }
    }

    private void loadHeroImage() {
        heroImage.fitWidthProperty().bind(visualSide.widthProperty());
        heroImage.fitHeightProperty().bind(visualSide.heightProperty());

        URL localResource = getClass().getResource("/images/login-factory.jpg");
        if (localResource != null) {
            heroImage.setImage(new Image(localResource.toExternalForm(), true));
            return;
        }

        Image remote = new Image(
                "https://images.unsplash.com/photo-1581092918484-8313f36f6f9b?auto=format&fit=crop&w=1600&q=80",
                true
        );
        heroImage.setImage(remote);

        remote.errorProperty().addListener((obs, oldVal, error) -> {
            if (Boolean.TRUE.equals(error)) {
                heroImage.setVisible(false);
                visualSide.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f2945, #1a4b7a);");
            }
        });
    }

    @FXML
    private void handleLogin() {
        String employeeNumberRaw = txtEmployeeNumber.getText() != null ? txtEmployeeNumber.getText().trim() : "";
        String pin = txtPin.getText() != null ? txtPin.getText().trim() : "";

        if (employeeNumberRaw.isBlank() || pin.isBlank()) {
            showError("Preencha todos os campos!");
            return;
        }

        if (!employeeNumberRaw.matches("\\d+")) {
            showError("O número do funcionário deve conter apenas dígitos.");
            return;
        }

        if (!pin.matches("\\d{4}")) {
            showError("O PIN deve conter exatamente 4 dígitos.");
            return;
        }

        final int employeeNumber;
        try {
            employeeNumber = Integer.parseInt(employeeNumberRaw);
        } catch (NumberFormatException exception) {
            showError("O número do funcionário não é válido.");
            return;
        }

        try {
            authService.login(employeeNumber, pin);
            abrirTelaPrincipal();
        } catch (RuntimeException ex) {
            showError(ex.getMessage() != null ? ex.getMessage() : "Credenciais inválidas.");
        }
    }

    @FXML
    private void handleForgotPin() {
        showError("Entre em contato com o administrador para redefinir seu PIN.");
    }

    private void abrirTelaPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();
            i18nService.applyTo(root);
            Scene scene = new Scene(root, 1400, 900);
            themePreferencesService.applyToScene(scene);
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setTitle("Pellets Factory - Stock Manager");
            stage.setScene(scene);
            stage.setFullScreen(false);
            stage.setMaximized(false);
            Platform.runLater(() -> applyVisibleBounds(stage));

        } catch (Exception e) {
            log.error("Erro ao carregar tela principal", e);
            showError("Erro ao abrir a tela principal.");
        }
    }

    private void showError(String message) {
        lblErro.setText(message);
        lblErro.setManaged(true);
        lblErro.setVisible(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(event -> {
            lblErro.setVisible(false);
            lblErro.setManaged(false);
        });
        pause.play();
    }

    private void applyVisibleBounds(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }
}
