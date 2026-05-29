package com.pelletsfactory.stock_manager.desktop.services;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ToastService {

    private static final Logger log = LoggerFactory.getLogger(ToastService.class);
    private static final Duration FADE_IN_DURATION = Duration.millis(180);
    private static final Duration VISIBLE_DURATION = Duration.seconds(4);
    private static final Duration FADE_OUT_DURATION = Duration.millis(220);

    private VBox toastContainer;

    public void setToastContainer(VBox toastContainer) {
        this.toastContainer = toastContainer;
        this.toastContainer.setMouseTransparent(true);
    }

    public void showSuccess(String title, String message) {
        showToast(ToastType.SUCCESS, title, message);
    }

    public void showError(String title, String message) {
        showToast(ToastType.ERROR, title, message);
    }

    private void showToast(ToastType type, String title, String message) {
        Platform.runLater(() -> {
            if (toastContainer == null) {
                log.info("{}: {} - {}", type.name(), title, message);
                return;
            }

            HBox toast = buildToast(type, title, message);
            toast.setOpacity(0);
            toastContainer.getChildren().add(0, toast);

            FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, toast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            PauseTransition visible = new PauseTransition(VISIBLE_DURATION);

            FadeTransition fadeOut = new FadeTransition(FADE_OUT_DURATION, toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(event -> toastContainer.getChildren().remove(toast));

            SequentialTransition animation = new SequentialTransition(fadeIn, visible, fadeOut);
            animation.play();
        });
    }

    private HBox buildToast(ToastType type, String title, String message) {
        HBox root = new HBox(12);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(14, 16, 14, 16));
        root.setMaxWidth(420);
        root.setStyle(String.format(
                "-fx-background-color: #161a22; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-width: 1; -fx-border-color: %s;",
                type.borderColor
        ));

        FontIcon icon = new FontIcon(type.iconLiteral + ":16");
        icon.setIconColor(javafx.scene.paint.Color.web(type.iconColor));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #f5f7fb;");
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #a4acba;");

        VBox textBox = new VBox(4, titleLabel, messageLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button();
        close.setGraphic(new FontIcon("mdi2c-close:14"));
        close.getStyleClass().addAll("button-icon", "flat");
        close.setOnAction(event -> toastContainer.getChildren().remove(root));
        close.setMouseTransparent(false);

        root.getChildren().addAll(icon, textBox, spacer, close);
        return root;
    }

    private enum ToastType {
        SUCCESS("mdi2c-check-circle-outline", "#34d399", "#225f4a"),
        ERROR("mdi2a-alert-circle-outline", "#f87171", "#7f1d1d");

        private final String iconLiteral;
        private final String iconColor;
        private final String borderColor;

        ToastType(String iconLiteral, String iconColor, String borderColor) {
            this.iconLiteral = iconLiteral;
            this.iconColor = iconColor;
            this.borderColor = borderColor;
        }
    }
}
