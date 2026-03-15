package com.pelletsfactory.stock_manager.desktop.services;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//Servico que vai serponsavel pelo routing da app desktop
@Service
public class NavigationService {

    private final ConfigurableApplicationContext springContext;
    private BorderPane contentArea;
    private final Map<String, Parent> viewCache = new HashMap<>();
    private final Map<String, String> routes = new HashMap<>();

    public NavigationService(ConfigurableApplicationContext springContext) {
        this.springContext = springContext;
        initializeRoutes();
    }

    private void initializeRoutes() {
        routes.put("/dashboard", "/fxml/views/dashboard-view.fxml");
        routes.put("/funcionarios", "/fxml/views/funcionario-view.fxml");
    }

    public void setContentArea(BorderPane contentArea) {
        this.contentArea = contentArea;
    }


    public void navigateTo(String route) {
        if (contentArea == null) {
            throw new IllegalStateException("Content area não foi definida!");
        }

        String fxmlPath = routes.get(route);
        if (fxmlPath == null) {
            System.err.println("Rota não encontrada: " + route);
            return;
        }

        try {
            Parent view = loadView(fxmlPath);
            contentArea.setCenter(view);
        } catch (IOException e) {
            System.err.println("Erro ao carregar view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private Parent loadView(String fxmlPath) throws IOException {
        if (viewCache.containsKey(fxmlPath)) {
            return viewCache.get(fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(springContext::getBean);
        Parent view = loader.load();

        viewCache.put(fxmlPath, view);

        return view;
    }

    public void clearCache() {
        viewCache.clear();
    }


    public void clearCache(String route) {
        String fxmlPath = routes.get(route);
        if (fxmlPath != null) {
            viewCache.remove(fxmlPath);
        }
    }
}
