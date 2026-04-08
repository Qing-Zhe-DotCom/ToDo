package com.example.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsInstallerConfigFromAppDataPath() throws IOException {
        Path appDataRoot = tempDir.resolve("appdata");
        writeProperties(
            appDataRoot.resolve("ToDo").resolve("config").resolve("application.properties"),
            "todo.app.default-language=zh-TW\n"
        );

        Map<String, String> environment = new HashMap<>();
        environment.put("APPDATA", appDataRoot.toString());

        AppProperties appProperties = ConfigurationLoader.loadAppProperties(environment, tempDir.resolve("working"));
        assertEquals("zh-TW", appProperties.getDefaultLanguage());
    }

    @Test
    void explicitConfigPathOverridesAppDataConfig() throws IOException {
        Path appDataRoot = tempDir.resolve("appdata");
        writeProperties(
            appDataRoot.resolve("ToDo").resolve("config").resolve("application.properties"),
            "todo.app.default-language=zh-TW\n"
        );

        Path explicitConfig = tempDir.resolve("explicit.properties");
        writeProperties(explicitConfig, "todo.app.default-language=en\n");

        Map<String, String> environment = new HashMap<>();
        environment.put("APPDATA", appDataRoot.toString());
        environment.put("TODO_CONFIG_FILE", explicitConfig.toString());

        AppProperties appProperties = ConfigurationLoader.loadAppProperties(environment, tempDir.resolve("working"));
        assertEquals("en", appProperties.getDefaultLanguage());
    }

    @Test
    void fallsBackToWorkingDirectoryConfigWhenAppDataConfigMissing() throws IOException {
        Path appDataRoot = tempDir.resolve("appdata");
        Path workingDirectory = tempDir.resolve("working");
        writeProperties(workingDirectory.resolve("application.properties"), "todo.app.default-language=zh-CN\n");

        Map<String, String> environment = new HashMap<>();
        environment.put("APPDATA", appDataRoot.toString());

        AppProperties appProperties = ConfigurationLoader.loadAppProperties(environment, workingDirectory);
        assertEquals("zh-CN", appProperties.getDefaultLanguage());
    }

    @Test
    void fallsBackToConfigSubdirectoryWhenRootPropertiesMissing() throws IOException {
        Path appDataRoot = tempDir.resolve("appdata");
        Path workingDirectory = tempDir.resolve("working");
        writeProperties(
            workingDirectory.resolve("config").resolve("application.properties"),
            "todo.app.default-language=en\n"
        );

        Map<String, String> environment = new HashMap<>();
        environment.put("APPDATA", appDataRoot.toString());

        AppProperties appProperties = ConfigurationLoader.loadAppProperties(environment, workingDirectory);
        assertEquals("en", appProperties.getDefaultLanguage());
    }

    @Test
    void keepsDefaultLanguageNullWhenNoOverrideExists() {
        AppProperties appProperties = ConfigurationLoader.loadAppProperties(Map.of(), tempDir.resolve("working"));
        assertNull(appProperties.getDefaultLanguage());
    }

    private static void writeProperties(Path path, String content) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content);
    }
}
