package com.example.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class AppDataPaths {
    private static final String APP_DIRECTORY_NAME = "ToDo";
    private static final String DEFAULT_SQLITE_FILE_NAME = "todo.sqlite";

    private final Path dataDirectory;
    private final Path sqliteDatabasePath;
    private final Path logsDirectory;
    private final Path backupsDirectory;

    public AppDataPaths(String dataDirectoryOverride, String sqlitePathOverride) {
        this.dataDirectory = resolveDataDirectory(dataDirectoryOverride);
        this.sqliteDatabasePath = resolveSqliteDatabasePath(sqlitePathOverride, dataDirectory);
        this.logsDirectory = dataDirectory.resolve("logs");
        this.backupsDirectory = dataDirectory.resolve("backups");
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Path getSqliteDatabasePath() {
        return sqliteDatabasePath;
    }

    public Path getLogsDirectory() {
        return logsDirectory;
    }

    public Path getBackupsDirectory() {
        return backupsDirectory;
    }

    public void ensureBaseDirectories() throws IOException {
        Files.createDirectories(dataDirectory);
        Files.createDirectories(logsDirectory);
        Files.createDirectories(backupsDirectory);
        Path databaseParent = sqliteDatabasePath.getParent();
        if (databaseParent != null) {
            Files.createDirectories(databaseParent);
        }
    }

    private Path resolveDataDirectory(String override) {
        if (override != null && !override.isBlank()) {
            return Path.of(override.trim()).toAbsolutePath().normalize();
        }

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String userHome = System.getProperty("user.home", ".");
        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, APP_DIRECTORY_NAME).toAbsolutePath().normalize();
            }
            return Path.of(userHome, "AppData", "Roaming", APP_DIRECTORY_NAME).toAbsolutePath().normalize();
        }
        if (osName.contains("mac")) {
            return Path.of(userHome, "Library", "Application Support", APP_DIRECTORY_NAME).toAbsolutePath().normalize();
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Path.of(xdgDataHome, APP_DIRECTORY_NAME).toAbsolutePath().normalize();
        }
        return Path.of(userHome, ".local", "share", APP_DIRECTORY_NAME).toAbsolutePath().normalize();
    }

    private Path resolveSqliteDatabasePath(String sqlitePathOverride, Path resolvedDataDirectory) {
        if (sqlitePathOverride == null || sqlitePathOverride.isBlank()) {
            return resolvedDataDirectory.resolve(DEFAULT_SQLITE_FILE_NAME).toAbsolutePath().normalize();
        }

        Path configuredPath = Path.of(sqlitePathOverride.trim());
        if (configuredPath.isAbsolute()) {
            return configuredPath.toAbsolutePath().normalize();
        }
        return resolvedDataDirectory.resolve(configuredPath).toAbsolutePath().normalize();
    }
}
