package com.pelletsfactory.stock_manager.desktop.services;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;
import java.util.function.Consumer;

@Service
public class LanguagePreferencesService {

    public enum AppLanguage {
        EN("en"),
        PT("pt");

        private final String code;

        AppLanguage(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public Locale toLocale() {
            return Locale.forLanguageTag(code);
        }

        public static AppLanguage fromCode(String code) {
            if (code == null) {
                return PT;
            }
            return "en".equalsIgnoreCase(code) ? EN : PT;
        }
    }

    private static final String PREF_KEY = "app.language";

    private final Preferences preferences = Preferences.userNodeForPackage(LanguagePreferencesService.class);
    private final List<Consumer<AppLanguage>> listeners = new CopyOnWriteArrayList<>();

    private AppLanguage language = AppLanguage.fromCode(preferences.get(PREF_KEY, "pt"));

    public AppLanguage getLanguage() {
        return language;
    }

    public Locale getLocale() {
        return language.toLocale();
    }

    public void setLanguage(AppLanguage newLanguage) {
        AppLanguage normalized = Objects.requireNonNullElse(newLanguage, AppLanguage.PT);
        if (language == normalized) {
            return;
        }
        language = normalized;
        preferences.put(PREF_KEY, normalized.code());
        listeners.forEach(listener -> listener.accept(normalized));
    }

    public void addLanguageChangeListener(Consumer<AppLanguage> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
}
