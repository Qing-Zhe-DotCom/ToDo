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
    private static final String APPDATA_ENV = "APPDATA";
    private static final String APPDATA_CONFIG_PATH_SEGMENT_1 = "ToDo";
    private static final String APPDATA_CONFIG_PATH_SEGMENT_2 = "config";
    private static final String APPDATA_CONFIG_FILE_NAME = "application.properties";

    private ConfigurationLoader() {
    }

    public static AppProperties loadAppProperties() {
        return loadAppProperties(System.getenv(), Path.of("").toAbsolutePath().normalize());
    }

    static AppProperties loadAppProperties(Map<String, String> environment, Path workingDirectory) {
        Properties properties = loadMergedProperties(environment, workingDirectory);
        return new AppProperties(
            resolve(properties, environment, "todo.app.version", "TODO_APP_VERSION", "dev"),
            resolve(properties, environment, "todo.app.default-theme-family", "TODO_APP_DEFAULT_THEME_FAMILY", "classic"),
            resolve(properties, environment, "todo.app.default-theme-appearance", "TODO_APP_DEFAULT_THEME_APPEARANCE", "light"),
            resolve(properties, environment, "todo.app.default-theme-classic-palette", "TODO_APP_DEFAULT_THEME_CLASSIC_PALETTE", "light"),
            resolveNullable(properties, environment, "todo.app.default-language", "TODO_APP_DEFAULT_LANGUAGE"),
            resolveNullable(properties, environment, "todo.app.data-dir", "TODO_APP_DATA_DIR")
        );
    }

    public static DatabaseProperties loadDatabaseProperties() {
        return loadDatabaseProperties(System.getenv(), Path.of("").toAbsolutePath().normalize());
    }

    static DatabaseProperties loadDatabaseProperties(Map<String, String> environment, Path workingDirectory) {
        Properties properties = loadMergedProperties(environment, workingDirectory);
        String mode = resolve(properties, environment, "todo.db.mode", "TODO_DB_MODE", "sqlite");
        String driverFallback = "sqlite".equalsIgnoreCase(mode)
            ? "org.sqlite.JDBC"
            : "com.mysql.cj.jdbc.Driver";
        String urlFallback = "sqlite".equalsIgnoreCase(mode)
            ? ""
            : "jdbc:mysql://localhost:3306/todo_db";
        String userFallback = "sqlite".equalsIgnoreCase(mode) ? "" : "root";
        return new DatabaseProperties(
            mode,
            resolve(properties, environment, "todo.db.driver", "TODO_DB_DRIVER", driverFallback),
            resolve(properties, environment, "todo.db.url", "TODO_DB_URL", urlFallback),
            resolve(properties, environment, "todo.db.user", "TODO_DB_USER", userFallback),
            resolve(properties, environment, "todo.db.password", "TODO_DB_PASSWORD", ""),
            resolveNullable(properties, environment, "todo.db.sqlite.path", "TODO_DB_SQLITE_PATH")
        );
    }

    private static Properties loadMergedProperties(Map<String, String> environment, Path workingDirectory) {
        Properties properties = new Properties();
        loadClasspathDefaults(properties);
        loadExternalOverrides(properties, environment, normalizeWorkingDirectory(workingDirectory));
        return properties;
    }

    private static Path normalizeWorkingDirectory(Path workingDirectory) {
        if (workingDirectory == null) {
            return Path.of("").toAbsolutePath().normalize();
        }
        return workingDirectory.toAbsolutePath().normalize();
    }

    private static void loadClasspathDefaults(Properties properties) {
        try (InputStream inputStream = ConfigurationLoader.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ignored) {
        }
    }

    private static void loadExternalOverrides(Properties properties, Map<String, String> environment, Path workingDirectory) {
        Path explicitConfig = resolveExternalConfigPath(environment, workingDirectory);
        if (explicitConfig != null) {
            loadFileInto(properties, explicitConfig);
            return;
        }

        Path appDataConfig = resolveAppDataConfigPath(environment);
        if (appDataConfig != null && Files.isRegularFile(appDataConfig)) {
            loadFileInto(properties, appDataConfig);
            return;
        }

        Path[] fallbackCandidates = {
            workingDirectory.resolve("application.properties"),
            workingDirectory.resolve("config").resolve("application.properties")
        };
        for (Path candidate : fallbackCandidates) {
            if (Files.isRegularFile(candidate)) {
                loadFileInto(properties, candidate);
                return;
            }
        }
    }

    private static Path resolveExternalConfigPath(Map<String, String> environment, Path workingDirectory) {
        String configuredPath = environment.get(EXTERNAL_CONFIG_ENV);
        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }
        Path candidate = Path.of(configuredPath.trim());
        if (candidate.isAbsolute()) {
            return candidate.toAbsolutePath().normalize();
        }
        return workingDirectory.resolve(candidate).toAbsolutePath().normalize();
    }

    private static Path resolveAppDataConfigPath(Map<String, String> environment) {
        String appData = environment.get(APPDATA_ENV);
        if (appData == null || appData.isBlank()) {
            return null;
        }
        return Path.of(
            appData.trim(),
            APPDATA_CONFIG_PATH_SEGMENT_1,
            APPDATA_CONFIG_PATH_SEGMENT_2,
            APPDATA_CONFIG_FILE_NAME
        ).toAbsolutePath().normalize();
    }

    private static void loadFileInto(Properties properties, Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        } catch (IOException ignored) {
        }
    }

    private static String resolve(
        Properties properties,
        Map<String, String> environment,
        String propertyKey,
        String envKey,
        String fallback
    ) {
        String envValue = environment.get(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return fallback;
    }

    private static String resolveNullable(
        Properties properties,
        Map<String, String> environment,
        String propertyKey,
        String envKey
    ) {
        String envValue = environment.get(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return null;
    }
}
