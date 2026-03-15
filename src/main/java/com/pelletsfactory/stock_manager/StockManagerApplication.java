package com.pelletsfactory.stock_manager;

import atlantafx.base.theme.PrimerDark;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.boot.builder.SpringApplicationBuilder;
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
        this.springContext = new SpringApplicationBuilder(StockManagerApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Aplica o tema AtlantaFX (Primer Dark por padrão)
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        //Carrega
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();

        Scene scene = new Scene(root, 450, 600);
        stage.setTitle("Stock Manager - Login");
        stage.setScene(scene);
        stage.setResizable(false); // Login não redimensionável
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }
}
