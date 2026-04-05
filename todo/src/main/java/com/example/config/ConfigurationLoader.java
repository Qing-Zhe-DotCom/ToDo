package com.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class ConfigurationLoader {
    private static final String DEFAULTS_RESOURCE = "/application-defaults.properties";
    private static final String EXTERNAL_CONFIG_ENV = "TODO_CONFIG_FILE";

    private ConfigurationLoader() {
    }

    public static AppProperties loadAppProperties() {
        Properties properties = loadMergedProperties();
        return new AppProperties(
            resolve(properties, "todo.app.default-theme", "TODO_APP_DEFAULT_THEME", "light"),
            resolve(properties, "todo.app.default-schedule-card-style", "TODO_APP_DEFAULT_SCHEDULE_CARD_STYLE", "Classic")
        );
    }

    public static DatabaseProperties loadDatabaseProperties() {
        Properties properties = loadMergedProperties();
        return new DatabaseProperties(
            resolve(properties, "todo.db.driver", "TODO_DB_DRIVER", "com.mysql.cj.jdbc.Driver"),
            resolve(properties, "todo.db.url", "TODO_DB_URL", "jdbc:mysql://localhost:3306/todo_db"),
            resolve(properties, "todo.db.user", "TODO_DB_USER", "root"),
            resolve(properties, "todo.db.password", "TODO_DB_PASSWORD", "")
        );
    }

    private static Properties loadMergedProperties() {
        Properties properties = new Properties();
        loadClasspathDefaults(properties);
        loadExternalOverrides(properties);
        return properties;
    }

    private static void loadClasspathDefaults(Properties properties) {
        try (InputStream inputStream = ConfigurationLoader.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ignored) {
        }
    }

    private static void loadExternalOverrides(Properties properties) {
        Path explicitConfig = resolveExternalConfigPath();
        if (explicitConfig != null) {
            loadFileInto(properties, explicitConfig);
            return;
        }

        Path[] fallbackCandidates = {
            Path.of("application.properties"),
            Path.of("config", "application.properties")
        };
        for (Path candidate : fallbackCandidates) {
            if (Files.isRegularFile(candidate)) {
                loadFileInto(properties, candidate);
                return;
            }
        }
    }

    private static Path resolveExternalConfigPath() {
        String configuredPath = System.getenv(EXTERNAL_CONFIG_ENV);
        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }
        return Path.of(configuredPath.trim());
    }

    private static void loadFileInto(Properties properties, Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        } catch (IOException ignored) {
        }
    }

    private static String resolve(Properties properties, String propertyKey, String envKey, String fallback) {
        Map<String, String> env = System.getenv();
        String envValue = env.get(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return fallback;
    }
}
