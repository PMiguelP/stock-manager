package com.pelletsfactory.stock_manager.desktop.services;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
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
        String osName = System.getProperty("os.name", "").toLowerCase();

        if (osName.contains("mac")) {
            String appearance = readCommandOutput("defaults", "read", "-g", "AppleInterfaceStyle");
            if (appearance != null) {
                return appearance.toLowerCase().contains("dark");
            }
        }

        if (osName.contains("win")) {
            String windowsAppsUseLightTheme = readCommandOutput(
                    "reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme"
            );
            if (windowsAppsUseLightTheme != null) {
                return windowsAppsUseLightTheme.contains("0x0");
            }
        }

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

    private String readCommandOutput(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!output.isEmpty()) {
                        output.append('\n');
                    }
                    output.append(line.trim());
                }
                return output.isEmpty() ? null : output.toString();
            }
        } catch (Exception ignored) {
            return null;
        }
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

        String managedStyle = buildAccentStyle();

        root.setStyle(baseStyle + (baseStyle.isBlank() ? "" : ";") + managedStyle);
    }

    private String buildAccentStyle() {
        int[] rgb = parseHexColor(accentHex);

        String subtle = isDarkThemeEffective()
                ? rgba(rgb, 0.16)
                : rgba(rgb, 0.18);

        return String.format(Locale.ROOT,
                "-color-accent-0: %s;" +
                        "-color-accent-1: %s;" +
                        "-color-accent-2: %s;" +
                        "-color-accent-3: %s;" +
                        "-color-accent-4: %s;" +
                        "-color-accent-5: %s;" +
                        "-color-accent-6: %s;" +
                        "-color-accent-7: %s;" +
                        "-color-accent-8: %s;" +
                        "-color-accent-9: %s;" +
                        "-color-accent: %s;" +
                        "-color-accent-fg: %s;" +
                        "-color-accent-emphasis: %s;" +
                        "-color-accent-muted: %s;" +
                        "-color-accent-subtle: %s;",
                mix(rgb, 255, 255, 255, 0.88),
                mix(rgb, 255, 255, 255, 0.72),
                mix(rgb, 255, 255, 255, 0.52),
                mix(rgb, 255, 255, 255, 0.32),
                mix(rgb, 255, 255, 255, 0.14),
                accentHex,
                mix(rgb, 0, 0, 0, 0.14),
                mix(rgb, 0, 0, 0, 0.28),
                mix(rgb, 0, 0, 0, 0.44),
                mix(rgb, 0, 0, 0, 0.60),
                accentHex,
                isDarkThemeEffective() ? mix(rgb, 255, 255, 255, 0.35) : mix(rgb, 0, 0, 0, 0.10),
                accentHex,
                rgba(rgb, 0.42),
                subtle
        );
    }

    private int[] parseHexColor(String hex) {
        String normalized = hex == null ? "" : hex.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.length() != 6) {
            return new int[]{76, 122, 242};
        }

        try {
            return new int[]{
                    Integer.parseInt(normalized.substring(0, 2), 16),
                    Integer.parseInt(normalized.substring(2, 4), 16),
                    Integer.parseInt(normalized.substring(4, 6), 16)
            };
        } catch (NumberFormatException ignored) {
            return new int[]{76, 122, 242};
        }
    }

    private String mix(int[] rgb, int targetRed, int targetGreen, int targetBlue, double amount) {
        int red = clamp((int) Math.round(rgb[0] + (targetRed - rgb[0]) * amount));
        int green = clamp((int) Math.round(rgb[1] + (targetGreen - rgb[1]) * amount));
        int blue = clamp((int) Math.round(rgb[2] + (targetBlue - rgb[2]) * amount));
        return String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue);
    }

    private String rgba(int[] rgb, double alpha) {
        return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.2f)",
                clamp(rgb[0]),
                clamp(rgb[1]),
                clamp(rgb[2]),
                Math.max(0.0, Math.min(1.0, alpha))
        );
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
