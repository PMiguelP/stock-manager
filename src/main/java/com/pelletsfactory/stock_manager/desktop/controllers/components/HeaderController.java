package com.pelletsfactory.stock_manager.desktop.controllers.components;

import atlantafx.base.controls.Breadcrumbs;
import atlantafx.base.controls.Breadcrumbs.BreadCrumbItem;
import atlantafx.base.theme.Styles;
import com.pelletsfactory.stock_manager.desktop.services.NavigationEvent;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HeaderController {

    @FXML private Breadcrumbs<String> breadcrumbs;

    @Autowired
    private NavigationService navigationService;

    // Record para estruturar os dados da notificação
    private record NotificationItem(
            String title,
            String desc,
            String time,
            String icon,
            String color,
            boolean unread
    ) {}

    @FXML
    public void initialize() {
        configurarBreadcrumbs(List.of("Home"));
    }

    @FXML
    private void handleOpenSettings() {
        navigationService.navigateTo("/settings");
    }

    @FXML
    private void handleOpenNotifications() {
        try {
            VBox notificationsDrawer = new VBox(0);
            notificationsDrawer.setMinWidth(550);
            notificationsDrawer.setPrefWidth(550);
            notificationsDrawer.setMaxWidth(550);
            notificationsDrawer.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(notificationsDrawer, Priority.ALWAYS);
            notificationsDrawer.setStyle("-fx-background-color: -color-bg-default; -fx-border-width: 0 0 0 1; -fx-border-color: -color-border-muted;");

            // 1. HEADER
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(30, 30, 10, 30));

            Label title = new Label("Notifications");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnClose = new Button();
            // CORREÇÃO CRÍTICA: mdi2c-close em vez de mdi2x-close
            btnClose.setGraphic(new FontIcon("mdi2c-close"));
            btnClose.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 18px;");
            btnClose.setOnAction(e -> navigationService.hideModal());

            header.getChildren().addAll(title, spacer, btnClose);
            notificationsDrawer.getChildren().add(header);

            // 2. SUB-HEADER
            HBox subHeader = new HBox();
            subHeader.setAlignment(Pos.CENTER_LEFT);
            subHeader.setPadding(new Insets(0, 30, 20, 30));

            Label subTitle = new Label("2 unread notifications");
            subTitle.setStyle("-fx-text-fill: -color-fg-muted;");

            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            Hyperlink markRead = new Hyperlink("Mark all as read");
            markRead.setStyle("-fx-text-fill: -color-accent-fg; -fx-underline: false; -fx-font-weight: bold;");

            subHeader.getChildren().addAll(subTitle, spacer2, markRead);
            notificationsDrawer.getChildren().add(subHeader);

            // 3. LISTA DE CARDS (SCROLLABLE)
            VBox listContainer = new VBox(15);
            listContainer.setPadding(new Insets(0, 30, 30, 30));

            ScrollPane scrollPane = new ScrollPane(listContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            // Dados das notificações (Prefixos mdi2 validados)
            List<NotificationItem> items = List.of(
                    new NotificationItem("New Order Received", "Order #ORD-006 from Global Energy Ltd", "5 min ago", "mdi2p-package-variant", "#3498db", true),
                    new NotificationItem("Low Stock Alert", "Current stock level is below threshold", "1 hour ago", "mdi2a-alert-circle", "#e67e22", true),
                    new NotificationItem("Production Batch Completed", "BATCH-2026-006 completed", "2 hours ago", "mdi2c-check-circle-outline", "#95a5a6", false)
            );

            for (NotificationItem item : items) {
                HBox card = new HBox(15);
                card.setPadding(new Insets(18));
                card.setAlignment(Pos.TOP_LEFT);
                card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; -fx-border-color: -color-border-muted; -fx-border-radius: 12;");

                // Ícone com fundo circular suave
                StackPane iconBox = new StackPane();
                iconBox.setMinWidth(48); iconBox.setMinHeight(48);
                iconBox.setStyle("-fx-background-color: " + item.color + "15; -fx-background-radius: 10;");

                FontIcon icon = new FontIcon(item.icon);
                icon.setIconSize(22);
                icon.setStyle("-fx-icon-color: " + item.color + ";");
                iconBox.getChildren().add(icon);

                // Textos
                VBox texts = new VBox(4);
                HBox.setHgrow(texts, Priority.ALWAYS);

                HBox titleLine = new HBox();
                titleLine.setAlignment(Pos.CENTER_LEFT);
                Label lblTitle = new Label(item.title);
                lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                titleLine.getChildren().add(lblTitle);

                if (item.unread) {
                    Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
                    Circle dot = new Circle(4, Color.web("#3498db"));
                    titleLine.getChildren().addAll(s, dot);
                }

                Label lblDesc = new Label(item.desc);
                lblDesc.setWrapText(true);
                lblDesc.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");

                Label lblTime = new Label(item.time);
                lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");

                texts.getChildren().addAll(titleLine, lblDesc, lblTime);
                card.getChildren().addAll(iconBox, texts);

                // Hover
                card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: -color-base-3; -fx-background-radius: 12; -fx-border-color: -color-accent-emphasis; -fx-border-radius: 12; -fx-cursor: hand;"));
                card.setOnMouseExited(e -> card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; -fx-border-color: -color-border-muted; -fx-border-radius: 12;"));

                listContainer.getChildren().add(card);
            }

            notificationsDrawer.getChildren().add(scrollPane);

            // 4. FOOTER
            HBox footer = new HBox();
            footer.setAlignment(Pos.CENTER);
            footer.setPadding(new Insets(20));
            Hyperlink viewAll = new Hyperlink("View All Notifications");
            viewAll.setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold;");
            footer.getChildren().add(viewAll);
            notificationsDrawer.getChildren().add(footer);

            navigationService.showModal(notificationsDrawer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onNavigationEvent(NavigationEvent event) {
        Platform.runLater(() -> {
            if (breadcrumbs != null) configurarBreadcrumbs(event.breadcrumbs());
        });
    }

    private void configurarBreadcrumbs(List<String> items) {
        BreadCrumbItem<String> root = Breadcrumbs.buildTreeModel(items.toArray(String[]::new));
        breadcrumbs.setCrumbFactory(crumb -> {
            var btn = new Button(crumb.getValue());
            btn.getStyleClass().add(Styles.FLAT);
            btn.setFocusTraversable(false);
            return btn;
        });
        breadcrumbs.setDividerFactory(item -> (item == null) ? new FontIcon(MaterialDesignH.HOME) : (!item.isLast() ? new FontIcon(MaterialDesignC.CHEVRON_RIGHT) : null));

        BreadCrumbItem<String> lastItem = root;
        while (lastItem.getChildren() != null && !lastItem.getChildren().isEmpty()) {
            lastItem = (BreadCrumbItem<String>) lastItem.getChildren().get(0);
        }
        breadcrumbs.setSelectedCrumb(lastItem);
    }
}