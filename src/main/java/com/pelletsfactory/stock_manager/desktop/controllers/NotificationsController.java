package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class NotificationsController {

    @FXML private Label lblNotificationsSummary;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private Button btnMarkAllRead;
    @FXML private VBox notificationsList;
    @FXML private ScrollPane notificationsScroll;

    private final ObservableList<NotificationItem> allNotifications = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cmbType.setItems(FXCollections.observableArrayList("All Types", "order", "stock", "system", "employee"));
        cmbStatus.setItems(FXCollections.observableArrayList("All Status", "Unread", "Read"));
        cmbType.setValue("All Types");
        cmbStatus.setValue("All Status");

        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> render());
        cmbType.valueProperty().addListener((obs, oldValue, newValue) -> render());
        cmbStatus.valueProperty().addListener((obs, oldValue, newValue) -> render());
        btnMarkAllRead.setOnAction(event -> {
            allNotifications.forEach(item -> item.unread = false);
            render();
        });

        loadDemoData();
        render();
    }

    private void loadDemoData() {
        allNotifications.setAll(
                new NotificationItem("New Order Received", "Order #ORD-006 from Global Energy Ltd - 120 tons", "order", "5 min ago", "2026-04-10", "mdi2c-cube-outline", "#3b82f6", true),
                new NotificationItem("Low Stock Alert", "Current stock level (2,850 tons) is below minimum threshold", "stock", "1 hour ago", "2026-04-10", "mdi2a-alert-outline", "#f97316", true),
                new NotificationItem("Production Batch Completed", "BATCH-2026-006 completed - 245 tons produced", "system", "2 hours ago", "2026-04-10", "mdi2c-clock-outline", "#94a3b8", false),
                new NotificationItem("New Employee Added", "Isabel Silva joined as Production Operator", "employee", "3 hours ago", "2026-04-10", "mdi2a-account-plus-outline", "#22c55e", false),
                new NotificationItem("Purchase Order Approved", "PO-2026-088 was approved by procurement", "order", "6 hours ago", "2026-04-10", "mdi2c-check-circle-outline", "#3b82f6", false),
                new NotificationItem("Stock Transfer Completed", "Transfer RM-14 to Production Area finished", "stock", "8 hours ago", "2026-04-10", "mdi2t-truck-check-outline", "#f97316", false),
                new NotificationItem("Batch QC Pending", "BATCH-2026-010 is waiting quality confirmation", "system", "10 hours ago", "2026-04-09", "mdi2f-flask-outline", "#94a3b8", false),
                new NotificationItem("Client Account Updated", "Global Energy Ltd contact details were updated", "employee", "12 hours ago", "2026-04-09", "mdi2a-account-edit-outline", "#22c55e", false),
                new NotificationItem("Order Shipment Scheduled", "Order ORD-004 shipping planned for tomorrow", "order", "1 day ago", "2026-04-09", "mdi2s-shipping-pallet", "#3b82f6", false),
                new NotificationItem("Safety Checklist Completed", "Daily production safety checklist submitted", "system", "1 day ago", "2026-04-09", "mdi2c-clipboard-check-outline", "#94a3b8", false)
        );
    }

    private void render() {
        List<NotificationItem> filtered = filterNotifications();
        notificationsList.getChildren().clear();

        if (filtered.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setPadding(new Insets(40));
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; -fx-border-color: -color-border-muted; -fx-border-radius: 12;");
            Label title = new Label("Sem notificacoes para estes filtros");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");
            Label desc = new Label("Tenta ajustar a pesquisa ou os filtros de tipo e estado.");
            desc.getStyleClass().add("text-muted");
            emptyState.getChildren().addAll(title, desc);
            notificationsList.getChildren().add(emptyState);
        } else {
            filtered.forEach(item -> notificationsList.getChildren().add(createNotificationCard(item)));
        }

        long unread = allNotifications.stream().filter(n -> n.unread).count();
        lblNotificationsSummary.setText(unread + " unread notifications • " + allNotifications.size() + " total");
        btnMarkAllRead.setDisable(unread == 0);
        notificationsScroll.setVvalue(0);
    }

    private List<NotificationItem> filterNotifications() {
        String search = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String type = cmbType.getValue() == null ? "All Types" : cmbType.getValue();
        String status = cmbStatus.getValue() == null ? "All Status" : cmbStatus.getValue();

        return allNotifications.stream()
                .filter(item -> search.isEmpty()
                        || item.title.toLowerCase(Locale.ROOT).contains(search)
                        || item.description.toLowerCase(Locale.ROOT).contains(search))
                .filter(item -> "All Types".equals(type) || item.type.equalsIgnoreCase(type))
                .filter(item -> {
                    if ("Unread".equals(status)) return item.unread;
                    if ("Read".equals(status)) return !item.unread;
                    return true;
                })
                .collect(Collectors.toList());
    }

    private HBox createNotificationCard(NotificationItem item) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 10; -fx-border-color: -color-accent-emphasis; -fx-border-radius: 10;");

        CheckBox checkBox = new CheckBox();
        checkBox.setFocusTraversable(false);
        checkBox.setPadding(new Insets(3, 0, 0, 0));

        StackPane iconWrap = new StackPane();
        iconWrap.setMinSize(48, 48);
        iconWrap.setPrefSize(48, 48);
        iconWrap.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 999; -fx-border-color: -color-border-muted; -fx-border-radius: 999;");

        FontIcon icon = new FontIcon(item.iconLiteral + ":22");
        icon.setIconColor(Color.web(item.iconColor));
        iconWrap.getChildren().add(icon);

        VBox content = new VBox(10);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(item.title);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");
        titleLine.getChildren().add(title);

        if (item.unread) {
            StackPane dot = new StackPane();
            dot.setMinSize(7, 7);
            dot.setPrefSize(7, 7);
            dot.setStyle("-fx-background-color: -color-accent-emphasis; -fx-background-radius: 999;");
            titleLine.getChildren().add(dot);
        }

        Label desc = new Label(item.description);
        desc.getStyleClass().add("text-muted");
        desc.setStyle("-fx-font-size: 15px;");

        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(item.type);
        typeBadge.setStyle(String.format(
                "-fx-padding: 4 12 4 12; -fx-background-radius: 999; -fx-border-radius: 999; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: %s; -fx-border-color: %s;",
                item.iconColor,
                item.iconColor
        ));
        meta.getChildren().add(typeBadge);

        if (item.unread) {
            Hyperlink markRead = new Hyperlink("Mark as read");
            markRead.setFocusTraversable(false);
            markRead.setStyle("-fx-text-fill: -color-accent-emphasis;");
            markRead.setOnAction(event -> {
                item.unread = false;
                render();
            });
            meta.getChildren().add(markRead);
        }

        content.getChildren().addAll(titleLine, desc, meta);

        VBox right = new VBox(2);
        right.setAlignment(Pos.TOP_RIGHT);
        Label timeAgo = new Label(item.timeAgo);
        timeAgo.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-fg-muted;");
        Label date = new Label(item.date);
        date.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-fg-muted;");
        right.getChildren().addAll(timeAgo, date);

        card.getChildren().addAll(checkBox, iconWrap, content, right);
        return card;
    }

    private static final class NotificationItem {
        private final String title;
        private final String description;
        private final String type;
        private final String timeAgo;
        private final String date;
        private final String iconLiteral;
        private final String iconColor;
        private boolean unread;

        private NotificationItem(String title, String description, String type, String timeAgo, String date,
                                 String iconLiteral, String iconColor, boolean unread) {
            this.title = title;
            this.description = description;
            this.type = type;
            this.timeAgo = timeAgo;
            this.date = date;
            this.iconLiteral = iconLiteral;
            this.iconColor = iconColor;
            this.unread = unread;
        }
    }
}

