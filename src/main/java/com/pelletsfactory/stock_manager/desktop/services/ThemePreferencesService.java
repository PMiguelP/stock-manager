package com.pelletsfactory.stock_manager.desktop.services;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ThemePreferencesService {

    public enum ThemeMode {
        LIGHT,
        DARK,
        AUTO
    }

    private static final String BASE_STYLE_KEY = "theme.baseStyle";

    private ThemeMode themeMode = ThemeMode.AUTO;
    private String accentHex = "#4C7AF2";
    private final List<Runnable> themeChangeListeners = new CopyOnWriteArrayList<>();

    public ThemeMode getThemeMode() {
        return themeMode;
    }

    public String getAccentHex() {
        return accentHex;
    }

    public boolean isDarkThemeEffective() {
        return switch (themeMode) {
            case DARK -> true;
            case LIGHT -> false;
            case AUTO -> isSystemDarkPreferred();
        };
    }

    public String getThemeLogoResource() {
        return isDarkThemeEffective() ? "/static/logodarkbg.png" : "/static/logowhitebg.png";
    }

    public void addThemeChangeListener(Runnable listener) {
        if (listener != null) {
            themeChangeListeners.add(listener);
        }
    }

    public void setThemeMode(ThemeMode mode) {
        themeMode = Objects.requireNonNullElse(mode, ThemeMode.AUTO);
        applyCurrentTheme();
    }

    public void setAccentHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return;
        }
        accentHex = hex;
        applyAccentToAllOpenScenes();
    }

    public void applyCurrentTheme() {
        Theme theme = resolveTheme();
        Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
        applyAccentToAllOpenScenes();
        notifyThemeChangeListeners();
    }

    public void applyToScene(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        applyAccentToRoot(scene.getRoot());
    }

    private Theme resolveTheme() {
        return switch (themeMode) {
            case LIGHT -> new PrimerLight();
            case DARK -> new PrimerDark();
            case AUTO -> isSystemDarkPreferred() ? new PrimerDark() : new PrimerLight();
        };
    }

    private void notifyThemeChangeListeners() {
        for (Runnable listener : themeChangeListeners) {
            try {
                listener.run();
            } catch (RuntimeException ignored) {
                // Keep UI theme updates resilient even if one listener fails.
            }
        }
    }

    private boolean isSystemDarkPreferred() {
        String macAppearance = System.getProperty("apple.awt.application.appearance", "");
        if (macAppearance.toLowerCase().contains("dark")) {
            return true;
        }

        String gtkTheme = System.getenv("GTK_THEME");
        if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
            return true;
        }

        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(18, 0)) || now.isBefore(LocalTime.of(7, 0));
    }

    private void applyAccentToAllOpenScenes() {
        for (Window window : Window.getWindows()) {
            if (window.getScene() != null && window.getScene().getRoot() != null) {
                applyAccentToRoot(window.getScene().getRoot());
            }
        }
    }

    private void applyAccentToRoot(Node root) {
        String baseStyle = (String) root.getProperties().computeIfAbsent(BASE_STYLE_KEY,
                key -> root.getStyle() == null ? "" : root.getStyle());

        String managedStyle = String.format(
                "-color-accent: %s; -color-accent-emphasis: %s; -color-accent-fg: white;",
                accentHex,
                accentHex
        );

        root.setStyle(baseStyle + (baseStyle.isBlank() ? "" : ";") + managedStyle);
    }
}

