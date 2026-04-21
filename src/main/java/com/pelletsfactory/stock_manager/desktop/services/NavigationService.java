package com.pelletsfactory.stock_manager.desktop.services;

import atlantafx.base.controls.ModalPane; // Importante adicionar este import
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node; // Importante adicionar este import
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NavigationService {

    private final ConfigurableApplicationContext springContext;
    private final ApplicationEventPublisher eventPublisher;
    private BorderPane contentArea;

    // --- ADICIONA ESTA LINHA ---
    private ModalPane modalPane;

    private final Map<String, Parent> viewCache = new HashMap<>();
    private final Map<String, RouteInfo> routes = new HashMap<>();

    public NavigationService(ConfigurableApplicationContext springContext,
                             ApplicationEventPublisher eventPublisher) {
        this.springContext = springContext;
        this.eventPublisher = eventPublisher;
        initializeRoutes();
    }

    private void initializeRoutes() {
        routes.put("/dashboard", new RouteInfo(
                "/fxml/views/dashboard-view.fxml",
                "Dashboard",
                "Visão geral do sistema",
                List.of("Home", "Dashboard")
        ));

        routes.put("/funcionarios", new RouteInfo(
                "/fxml/views/funcionario-view.fxml",
                "Gestão de Funcionários",
                "Administração da equipa Pellets Factory",
                List.of("Home", "Administração", "Funcionários")
        ));
        routes.put("/settings", new RouteInfo(
                "/fxml/views/settings-view.fxml",
                "Settings Page",
                "Settings",
                List.of("Home", "Administração", "Funcionários")
        ));
    }

    public void setContentArea(BorderPane contentArea) {
        this.contentArea = contentArea;
    }

    // --- ADICIONA ESTES TRÊS MÉTODOS ABAIXO ---

    public void setModalPane(ModalPane modalPane) {
        this.modalPane = modalPane;
        this.modalPane.setAlignment(Pos.CENTER_RIGHT);
        this.modalPane.usePredefinedTransitionFactories(Side.RIGHT);
    }

    public void showModal(Node content) {
        if (modalPane != null) {
            modalPane.show(content);
        } else {
            System.err.println("Erro: ModalPane não foi injetado no NavigationService!");
        }
    }

    public void hideModal() {
        if (modalPane != null) {
            modalPane.hide();
        }
    }

    // ------------------------------------------

    public void navigateTo(String route) {
        if (contentArea == null) {
            throw new IllegalStateException("Content area não foi definida!");
        }

        RouteInfo routeInfo = routes.get(route);
        if (routeInfo == null) {
            System.err.println("Rota não encontrada: " + route);
            return;
        }

        try {
            Parent view = loadView(routeInfo.fxmlPath);
            contentArea.setCenter(view);

            eventPublisher.publishEvent(new NavigationEvent(
                    routeInfo.titulo,
                    routeInfo.subtitulo,
                    routeInfo.breadcrumbs
            ));

        } catch (IOException e) {
            System.err.println("Erro ao carregar view: " + routeInfo.fxmlPath);
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

    private record RouteInfo(
            String fxmlPath,
            String titulo,
            String subtitulo,
            List<String> breadcrumbs
    ) {}

    public Node loadExternalView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);
            return loader.load();
        } catch (IOException e) {
            System.err.println("Erro ao carregar FXML externo: " + fxmlPath);
            return null;
        }
    }
}