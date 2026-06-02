package com.pelletsfactory.stock_manager.desktop.services;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

@Service
public class I18nService {

    private static final Logger log = LoggerFactory.getLogger(I18nService.class);
    private static final String BUNDLE_BASE = "i18n.messages";
    private static final String I18N_TEXT_KEY = "i18n.text.key";
    private static final String I18N_PROMPT_KEY = "i18n.prompt.key";
    private static final String I18N_TOOLTIP_KEY = "i18n.tooltip.key";
    private static final String I18N_COLUMN_KEY = "i18n.column.key";
    private static final String I18N_AXIS_KEY = "i18n.axis.key";

    private final LanguagePreferencesService languagePreferencesService;
    private final Set<String> missingKeys = new HashSet<>();
    private ResourceBundle bundle;
    private ResourceBundle fallbackBundle;

    public I18nService(LanguagePreferencesService languagePreferencesService) {
        this.languagePreferencesService = languagePreferencesService;
        reloadBundle(languagePreferencesService.getLocale());
        languagePreferencesService.addLanguageChangeListener(lang -> reloadBundle(lang.toLocale()));
    }

    public String translate(String key) {
        if (key == null || key.isBlank()) {
            return key;
        }
        if (bundle != null && bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        if (fallbackBundle != null && fallbackBundle.containsKey(key)) {
            return fallbackBundle.getString(key);
        }
        if (missingKeys.add(key)) {
            log.warn("Missing translation key: {}", key);
        }
        return key;
    }

    public void reload() {
        reloadBundle(languagePreferencesService.getLocale());
    }

    public void applyTo(Node root) {
        if (root == null) {
            return;
        }
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            translateNode(node);
            if (node instanceof Parent parent) {
                parent.getChildrenUnmodifiable().forEach(stack::push);
            }
            if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
                stack.push(scrollPane.getContent());
            }
            if (node instanceof XYChart<?, ?> chart) {
                stack.push(chart.getXAxis());
                stack.push(chart.getYAxis());
            }
        }
    }

    private void translateNode(Node node) {
        if (node instanceof Labeled labeled) {
            labeled.setText(translate(originalValue(labeled.getProperties(), I18N_TEXT_KEY, labeled.getText())));
        }
        if (node instanceof TextInputControl input) {
            input.setPromptText(translate(originalValue(input.getProperties(), I18N_PROMPT_KEY, input.getPromptText())));
        }
        if (node instanceof ComboBox<?> combo) {
            combo.setPromptText(translate(originalValue(combo.getProperties(), I18N_PROMPT_KEY, combo.getPromptText())));
        }
        if (node instanceof TitledPane titledPane) {
            titledPane.setText(translate(originalValue(titledPane.getProperties(), I18N_TEXT_KEY, titledPane.getText())));
        }
        if (node instanceof TabPane tabPane) {
            for (Tab tab : tabPane.getTabs()) {
                tab.setText(translate(originalValue(tab.getProperties(), I18N_TEXT_KEY, tab.getText())));
            }
        }
        if (node instanceof TableView<?> table) {
            translateColumns(table.getColumns());
        }
        if (node instanceof Axis<?> axis) {
            axis.setLabel(translate(originalValue(axis.getProperties(), I18N_AXIS_KEY, axis.getLabel())));
        }
        if (node instanceof Control control && control.getTooltip() != null) {
            Tooltip tooltip = control.getTooltip();
            tooltip.setText(translate(originalValue(tooltip.getProperties(), I18N_TOOLTIP_KEY, tooltip.getText())));
        }
    }

    private void translateColumns(List<? extends TableColumn<?, ?>> columns) {
        for (TableColumn<?, ?> column : columns) {
            column.setText(translate(originalValue(column.getProperties(), I18N_COLUMN_KEY, column.getText())));
            if (!column.getColumns().isEmpty()) {
                translateColumns(column.getColumns());
            }
        }
    }

    private String originalValue(java.util.Map<Object, Object> properties, String propertyKey, String currentValue) {
        if (currentValue == null || currentValue.isBlank()) {
            return currentValue;
        }
        Object stored = properties.get(propertyKey);
        if (stored instanceof String key && !key.isBlank()) {
            return key;
        }
        properties.put(propertyKey, currentValue);
        return currentValue;
    }

    private void reloadBundle(Locale locale) {
        ClassLoader classLoader = getClass().getClassLoader();
        ResourceBundle.clearCache(classLoader);
        missingKeys.clear();
        fallbackBundle = ResourceBundle.getBundle(BUNDLE_BASE, Locale.ENGLISH, classLoader);
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale, classLoader);
        } catch (MissingResourceException e) {
            bundle = fallbackBundle;
        }
    }
}
