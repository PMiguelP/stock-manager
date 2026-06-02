package com.pelletsfactory.stock_manager.desktop.services;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nResourcesTest {

    private static final Pattern TRANSLATABLE_ATTRIBUTE =
            Pattern.compile("(?:text|promptText|label)=\"([^\"]+)\"");
    private static final Pattern TRANSLATION_KEY =
            Pattern.compile("[a-z][A-Za-z0-9]*(?:\\.[A-Za-z0-9]+)+");
    private static final Pattern PLACEHOLDER =
            Pattern.compile("[-—0.]+");

    @Test
    void everyFxmlTextUsesAnExistingTranslationKey() throws IOException {
        Properties english = loadProperties("i18n/messages_en.properties");
        Properties portuguese = loadProperties("i18n/messages_pt.properties");

        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/fxml"))) {
            List<Path> fxmlFiles = files.filter(path -> path.toString().endsWith(".fxml")).toList();
            for (Path file : fxmlFiles) {
                assertTranslatedAttributes(file, english, portuguese);
            }
        }
    }

    private void assertTranslatedAttributes(Path file, Properties english, Properties portuguese) throws IOException {
        Matcher matcher = TRANSLATABLE_ATTRIBUTE.matcher(Files.readString(file));
        while (matcher.find()) {
            String value = matcher.group(1);
            if (PLACEHOLDER.matcher(value).matches()) {
                continue;
            }

            assertTrue(TRANSLATION_KEY.matcher(value).matches(),
                    () -> file + " contains hardcoded FXML text: " + value);
            assertTrue(english.containsKey(value),
                    () -> file + " is missing English translation: " + value);
            assertTrue(portuguese.containsKey(value),
                    () -> file + " is missing Portuguese translation: " + value);
        }
    }

    private Properties loadProperties(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertTrue(input != null, () -> "Missing resource: " + resource);
            properties.load(input);
        }
        return properties;
    }
}
