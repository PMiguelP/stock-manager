package com.pelletsfactory.stock_manager;

import com.pelletsfactory.stock_manager.desktop.services.ThemePreferencesService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.StageStyle;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Classe principal da aplicação
 * Inicializa Spring Boot + JavaFX
 */
@SpringBootApplication
public class StockManagerApplication extends Application {

    private ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        Application.launch(StockManagerApplication.class, args);
    }

    @Override
    public void init() {
        // Inicializa o contexto Spring
        this.springContext = new SpringApplicationBuilder(StockManagerApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    @Override
    public void start(Stage stage) throws Exception {

        ThemePreferencesService themePreferencesService = springContext.getBean(ThemePreferencesService.class);
        I18nService i18nService = springContext.getBean(I18nService.class);
        themePreferencesService.applyCurrentTheme();

        stage.initStyle(StageStyle.UNDECORATED);

        //Carrega
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        i18nService.applyTo(root);
        enableWindowDrag(root, stage);

        Scene scene = new Scene(root, 1920, 1080);
        themePreferencesService.applyToScene(scene);
        stage.setTitle("Stock Manager - Login");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setFullScreen(false);
        stage.setMaximized(false);
        stage.show();
        Platform.runLater(() -> fillToVisibleBounds(stage));
    }

    private void fillToVisibleBounds(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    private void enableWindowDrag(Parent root, Stage stage) {
        final double[] dragOffset = new double[2];

        root.setOnMousePressed(event -> {
            dragOffset[0] = event.getSceneX();
            dragOffset[1] = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - dragOffset[0]);
            stage.setY(event.getScreenY() - dragOffset[1]);
        });
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }
}
