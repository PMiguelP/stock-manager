package com.pelletsfactory.stock_manager.desktop.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

public final class UiFactory {

    private UiFactory() {
    }

    public static VBox drawerRoot(double width) {
        VBox root = new VBox(0);
        root.setMinWidth(width);
        root.setPrefWidth(width);
        root.setMaxWidth(width);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");
        return root;
    }

    public static HBox drawerHeader(String title, Runnable onClose) {
        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");

        Label label = new Label(title);
        label.getStyleClass().add("title-3");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = iconButton("mdi2c-close", 22);
        close.getStyleClass().add("flat");
        close.setOnAction(e -> onClose.run());

        header.getChildren().addAll(label, spacer, close);
        return header;
    }

    public static ScrollPane transparentScroll(VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return scrollPane;
    }

    public static HBox drawerFooter() {
        HBox footer = new HBox();
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        return footer;
    }

    public static Button iconButton(String iconLiteral, int iconSize) {
        Button button = new Button();
        button.getStyleClass().add("button-icon");

        FontIcon icon = new FontIcon();
        icon.setIconLiteral(iconLiteral);
        icon.setIconSize(iconSize);
        button.setGraphic(icon);
        return button;
    }

    public static HBox statusBadge(String label, String iconLiteral, String color) {
        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(4, 10, 4, 10));
        badge.setStyle(String.format(
                "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;" +
                        "-fx-background-color: %s20; -fx-border-color: %s;",
                color.replace("#", ""), color));

        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconColor(Color.web(color));

        Label text = new Label(label);
        text.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");

        badge.getChildren().addAll(icon, text);
        return badge;
    }
}
