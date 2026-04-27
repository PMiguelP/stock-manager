package com.pelletsfactory.stock_manager.desktop.services;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import org.springframework.stereotype.Service;

import java.util.IdentityHashMap;
import java.util.Map;

@Service
public class FormValidationService {

    private static final String ERROR_STYLE = "-fx-border-color: #ef4444; -fx-border-width: 1.5;";
    private static final String DEFAULT_ERROR_LABEL_STYLE = "-fx-text-fill: #ef4444; -fx-font-size: 11px;";

    private final Map<Control, String> baseStyleByControl = new IdentityHashMap<>();

    public Label createErrorLabel() {
        Label error = new Label();
        error.setManaged(false);
        error.setVisible(false);
        error.setWrapText(true);
        error.setStyle(DEFAULT_ERROR_LABEL_STYLE);
        return error;
    }

    public boolean validateRequiredText(TextInputControl control, Label errorLabel, String message) {
        if (control.getText() == null || control.getText().trim().isEmpty()) {
            showError(control, errorLabel, message);
            return false;
        }

        clearError(control, errorLabel);
        return true;
    }

    public boolean validateRequiredCombo(ComboBox<?> control, Label errorLabel, String message) {
        if (control.getValue() == null) {
            showError(control, errorLabel, message);
            return false;
        }

        clearError(control, errorLabel);
        return true;
    }

    public boolean validateRegex(TextInputControl control, Label errorLabel, String regex, String message) {
        if (!control.getText().trim().matches(regex)) {
            showError(control, errorLabel, message);
            return false;
        }

        clearError(control, errorLabel);
        return true;
    }

    public void attachTextAutoClear(TextInputControl control, Label errorLabel) {
        control.textProperty().addListener((obs, oldValue, newValue) -> clearError(control, errorLabel));
    }

    public void attachComboAutoClear(ComboBox<?> control, Label errorLabel) {
        control.valueProperty().addListener((obs, oldValue, newValue) -> clearError(control, errorLabel));
    }

    public void clearError(Control control, Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setManaged(false);
            errorLabel.setVisible(false);
        }

        if (control != null) {
            control.setStyle(baseStyleByControl.getOrDefault(control, ""));
        }
    }

    private void showError(Control control, Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setManaged(true);
            errorLabel.setVisible(true);
        }

        if (control != null) {
            baseStyleByControl.putIfAbsent(control, control.getStyle() == null ? "" : control.getStyle());
            String baseStyle = baseStyleByControl.get(control);
            control.setStyle((baseStyle + " " + ERROR_STYLE).trim());
        }
    }
}

