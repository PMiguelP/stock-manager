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

@SpringBootApplication
public class StockManagerApplication extends Application {

    private ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        Application.launch(StockManagerApplication.class, args);
    }

    @Override
    public void init() {
        this.springContext = new SpringApplicationBuilder(StockManagerApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        // 1. APLICAR O TEMA ATLANTAFX AQUI
        // Podes usar PrimerDark(), PrimerLight(), NordDark(), ou CupertinoDark()
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/funcionario-view.fxml"));

        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Pellets Factory - Gestão de Funcionários");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }
}
