package com.pelletsfactory.stock_manager.desktop.services;

import atlantafx.base.controls.ModalPane;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
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
        addRoute("/dashboard", "/fxml/views/dashboard-view.fxml", "Dashboard",
                "Visao geral do sistema", ViewId.DASHBOARD,
                "Home", "Dashboard");

        addRoute("/funcionarios", "/fxml/views/funcionario-view.fxml", "Employees",
                "Team management", ViewId.FUNCIONARIOS,
                "Home", "Administration", "Employees");

        addRoute("/settings", "/fxml/views/settings-view.fxml", "Settings",
                "Application preferences", ViewId.SETTINGS,
                "Home", "Administration", "Settings");

        addRoute("/orders", "/fxml/views/orders-view.fxml", "Orders",
                "Customer orders and fulfillment", ViewId.ORDERS,
                "Home", "Operations", "Orders");

        addRoute("/production", "/fxml/views/production-view.fxml", "Production",
                "Production planning and batches", ViewId.PRODUCTION,
                "Home", "Operations", "Production");

        addRoute("/stock", "/fxml/views/stock-view.fxml", "Stock",
                "Inventory levels and turnover", ViewId.STOCK,
                "Home", "Inventory", "Stock");

        addRoute("/clients", "/fxml/views/clients-view.fxml", "Clients",
                "Customer portfolio", ViewId.CLIENTS,
                "Home", "Sales", "Clients");

        addRoute("/suppliers", "/fxml/views/suppliers-view.fxml", "Suppliers",
                "Supplier directory", ViewId.SUPPLIERS,
                "Home", "Procurement", "Suppliers");

        addRoute("/purchase-orders", "/fxml/views/purchase-orders-view.fxml", "Purchase Orders",
                "Inbound order tracking", ViewId.PURCHASE_ORDERS,
                "Home", "Procurement", "Purchase Orders");

        addRoute("/raw-materials", "/fxml/views/raw-materials-view.fxml", "Raw Materials",
                "Material stock and reorder points", ViewId.RAW_MATERIALS,
                "Home", "Inventory", "Raw Materials");

        addRoute("/pellet-types", "/fxml/views/pellet-types-view.fxml", "Pellet Types",
                "Product variants and specs", ViewId.PELLET_TYPES,
                "Home", "Catalog", "Pellet Types");

        addRoute("/formulas", "/fxml/views/formulas-view.fxml", "Formulas",
                "Production recipes", ViewId.FORMULAS,
                "Home", "Catalog", "Formulas");

        addRoute("/batches", "/fxml/views/batches-view.fxml", "Batches",
                "Batch traceability", ViewId.BATCHES,
                "Home", "Catalog", "Batches");

        addRoute("/notifications", "/fxml/views/notifications-view.fxml", "Notifications",
                "Notifications center", ViewId.NOTIFICATIONS,
                "Home", "Notifications");
    }

    private void addRoute(String route,
                          String fxmlPath,
                          String titulo,
                          String subtitulo,
                          ViewId viewId,
                          String... breadcrumbs) {
        routes.put(route, new RouteInfo(
                fxmlPath,
                titulo,
                subtitulo,
                List.of(breadcrumbs),
                viewId
        ));
    }

    public void setContentArea(BorderPane contentArea) {
        this.contentArea = contentArea;
    }

    public void setModalPane(ModalPane modalPane) {
        this.modalPane = modalPane;
        this.modalPane.setAlignment(Pos.CENTER_RIGHT);
        this.modalPane.usePredefinedTransitionFactories(Side.RIGHT);
    }

    public void showModal(Node content) {
        if (modalPane != null) {
            modalPane.show(content);
        } else {
            System.err.println("Erro: ModalPane nao foi injetado no NavigationService!");
        }
    }

    public void hideModal() {
        if (modalPane != null) {
            modalPane.hide();
        }
    }

    public void navigateTo(String route) {
        if (contentArea == null) {
            throw new IllegalStateException("Content area nao foi definida!");
        }

        RouteInfo routeInfo = routes.get(route);
        if (routeInfo == null) {
            System.err.println("Rota nao encontrada: " + route);
            return;
        }

        try {
            Parent view = loadView(routeInfo.fxmlPath);
            contentArea.setCenter(view);

            eventPublisher.publishEvent(new NavigationEvent(
                    routeInfo.titulo,
                    routeInfo.subtitulo,
                    routeInfo.breadcrumbs,
                    routeInfo.viewId
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

    public Node loadExternalView(String fxmlPath) {
        String normalizedPath = fxmlPath.startsWith("/") ? fxmlPath : "/fxml/views/" + fxmlPath;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(normalizedPath));
            loader.setControllerFactory(springContext::getBean);
            return loader.load();
        } catch (IOException e) {
            System.err.println("Erro ao carregar FXML externo: " + normalizedPath);
            return null;
        }
    }

    private record RouteInfo(
            String fxmlPath,
            String titulo,
            String subtitulo,
            List<String> breadcrumbs,
            ViewId viewId
    ) {}
}