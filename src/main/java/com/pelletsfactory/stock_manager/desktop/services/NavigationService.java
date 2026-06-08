package com.pelletsfactory.stock_manager.desktop.services;

import atlantafx.base.controls.ModalPane;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NavigationService {

    private static final Logger log = LoggerFactory.getLogger(NavigationService.class);

    private final ConfigurableApplicationContext springContext;
    private final ApplicationEventPublisher eventPublisher;
    private final I18nService i18nService;
    private BorderPane contentArea;
    private ModalPane modalPane;

    private final Map<String, Parent> viewCache = new HashMap<>();
    private final Map<String, RouteInfo> routes = new HashMap<>();
    private final Map<String, Object> controllerByFxmlPath = new HashMap<>();
    private final Map<String, Object> viewStateByRoute = new HashMap<>();
    private String currentRoute;
    private Object currentController;

    public NavigationService(ConfigurableApplicationContext springContext,
                             ApplicationEventPublisher eventPublisher,
                             I18nService i18nService,
                             LanguagePreferencesService languagePreferencesService) {
        this.springContext = springContext;
        this.eventPublisher = eventPublisher;
        this.i18nService = i18nService;
        initializeRoutes();
        languagePreferencesService.addLanguageChangeListener(lang -> {
            i18nService.reload();
            reloadCurrentRoute();
        });
    }

    private void initializeRoutes() {
        addRoute("/dashboard", "/fxml/views/dashboard-view.fxml", "dashboard.title",
                "dashboard.subtitle", ViewId.DASHBOARD,
                "nav.home", "dashboard.title");

        addRoute("/funcionarios", "/fxml/views/funcionario-view.fxml", "employees.title",
                "employees.subtitle", ViewId.FUNCIONARIOS,
                "nav.home", "nav.employees");

        addRoute("/settings", "/fxml/views/settings-view.fxml", "settings.title",
                "settings.subtitle", ViewId.SETTINGS,
                "nav.home", "settings.title");

        addRoute("/orders", "/fxml/views/orders-view.fxml", "orders.title",
                "orders.subtitle", ViewId.ORDERS,
                "nav.home", "nav.orders");

        addRoute("/production", "/fxml/views/production-view.fxml", "production.title",
                "production.subtitle", ViewId.PRODUCTION,
                "nav.home", "nav.production");

        addRoute("/allocations", "/fxml/views/allocations-view.fxml", "allocations.title",
                "allocations.subtitle", ViewId.ALLOCATIONS,
                "nav.home", "nav.allocations");

        addRoute("/stock", "/fxml/views/stock-view.fxml", "stock.title",
                "stock.subtitle", ViewId.STOCK,
                "nav.home", "stock.title");

        addRoute("/clients", "/fxml/views/clients-view.fxml", "clients.title",
                "clients.subtitle", ViewId.CLIENTS,
                "nav.home", "nav.clients");

        addRoute("/suppliers", "/fxml/views/suppliers-view.fxml", "suppliers.title",
                "suppliers.subtitle", ViewId.SUPPLIERS,
                "nav.home", "nav.suppliers");

        addRoute("/purchase-orders", "/fxml/views/purchase-orders-view.fxml", "purchaseOrders.title",
                "purchaseOrders.subtitle", ViewId.PURCHASE_ORDERS,
                "nav.home", "purchaseOrders.title");

        addRoute("/raw-materials", "/fxml/views/raw-materials-view.fxml", "rawMaterials.title",
                "rawMaterials.subtitle", ViewId.RAW_MATERIALS,
                "nav.home", "rawMaterials.title");

        addRoute("/pellet-types", "/fxml/views/pellet-types-view.fxml", "pelletTypes.title",
                "pelletTypes.subtitle", ViewId.PELLET_TYPES,
                "nav.home", "pelletTypes.title");

        addRoute("/formulas", "/fxml/views/formulas-view.fxml", "formulas.title",
                "formulas.subtitle", ViewId.FORMULAS,
                "nav.home", "nav.formulas");

        addRoute("/batches", "/fxml/views/batches-view.fxml", "batches.title",
                "batches.subtitle", ViewId.BATCHES,
                "nav.home", "nav.batches");

        addRoute("/notifications", "/fxml/views/notifications-view.fxml", "notifications.title",
                "notifications.subtitle", ViewId.NOTIFICATIONS,
                "nav.home", "notifications.title");

        addRoute("/support", "/fxml/views/support-view.fxml", "support.title",
                "support.subtitle", ViewId.SUPPORT,
                "nav.home", "support.title");

        addRoute("/financial", "/fxml/views/financial-view.fxml", "financial.title",
                "financial.subtitle", ViewId.FINANCIAL,
                "nav.home", "nav.financial");
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
            log.warn("ModalPane não foi definido no NavigationService");
        }
    }

    public void hideModal() {
        if (modalPane != null) {
            modalPane.hide();
        }
    }

    public void clearCache() {
        viewCache.clear();
        controllerByFxmlPath.clear();
        viewStateByRoute.clear();
    }

    public void navigateTo(String route) {
        if (contentArea == null) {
            throw new IllegalStateException("Content area nao foi definida!");
        }

        RouteInfo routeInfo = routes.get(route);
        if (routeInfo == null) {
            log.warn("Rota não encontrada: {}", route);
            return;
        }

        try {
            currentRoute = route;
            Parent view = loadView(routeInfo.fxmlPath);
            currentController = controllerByFxmlPath.get(routeInfo.fxmlPath);
            contentArea.setCenter(view);
            if (currentController instanceof ViewReloadable reloadable) {
                reloadable.onViewShown();
            }

            eventPublisher.publishEvent(new NavigationEvent(
                    i18nService.translate(routeInfo.titulo),
                    i18nService.translate(routeInfo.subtitulo),
                    routeInfo.breadcrumbs.stream().map(i18nService::translate).toList(),
                    routeInfo.viewId
            ));

        } catch (IOException e) {
            log.error("Erro ao carregar view {}", routeInfo.fxmlPath, e);
        }
    }

    private Parent loadView(String fxmlPath) throws IOException {
        if (viewCache.containsKey(fxmlPath)) {
            return viewCache.get(fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(springContext::getBean);
        Parent view = loader.load();
        i18nService.applyTo(view);

        controllerByFxmlPath.put(fxmlPath, loader.getController());
        viewCache.put(fxmlPath, view);
        return view;
    }

    public Node loadExternalView(String fxmlPath) {
        String normalizedPath = fxmlPath.startsWith("/") ? fxmlPath : "/fxml/views/" + fxmlPath;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(normalizedPath));
            loader.setControllerFactory(springContext::getBean);
            Node view = loader.load();
            i18nService.applyTo(view);
            return view;
        } catch (IOException e) {
            log.error("Erro ao carregar FXML externo {}", normalizedPath, e);
            return null;
        }
    }

    private void reloadCurrentRoute() {
        if (currentRoute == null || contentArea == null) {
            return;
        }
        captureCurrentViewState();
        clearCache();
        navigateTo(currentRoute);
        restoreCurrentViewState();
    }

    private void captureCurrentViewState() {
        if (currentController instanceof ViewStateful<?> stateful) {
            viewStateByRoute.put(currentRoute, stateful.captureViewState());
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreCurrentViewState() {
        if (currentController instanceof ViewStateful<?> stateful) {
            Object state = viewStateByRoute.get(currentRoute);
            if (state != null) {
                ((ViewStateful<Object>) stateful).restoreViewState(state);
            }
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
