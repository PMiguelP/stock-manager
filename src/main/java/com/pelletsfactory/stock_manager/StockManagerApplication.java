package com.pelletsfactory.stock_manager;
import javafx.scene.layout.VBox;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
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
        //FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        //loader.setControllerFactory(springContext::getBean);
        //Parent root = loader.load();
        Label label = new Label("Teste");

        VBox vbox = new VBox(label);
        Scene scene = new Scene(vbox, 1920, 1080);
        stage.setTitle("PEllET LOUCURA");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }
}
