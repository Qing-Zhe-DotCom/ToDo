package com.example.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppDataPathsTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesRelativeSqlitePathInsideConfiguredDataDirectory() throws IOException {
        Path configuredDataDir = tempDir.resolve("app-data");
        AppDataPaths appDataPaths = new AppDataPaths(configuredDataDir.toString(), "db/main.sqlite");

        assertEquals(configuredDataDir.toAbsolutePath().normalize(), appDataPaths.getDataDirectory());
        assertEquals(
            configuredDataDir.resolve("db/main.sqlite").toAbsolutePath().normalize(),
            appDataPaths.getSqliteDatabasePath()
        );

        appDataPaths.ensureBaseDirectories();

        assertTrue(Files.isDirectory(appDataPaths.getDataDirectory()));
        assertTrue(Files.isDirectory(appDataPaths.getLogsDirectory()));
        assertTrue(Files.isDirectory(appDataPaths.getBackupsDirectory()));
        assertTrue(Files.isDirectory(appDataPaths.getSqliteDatabasePath().getParent()));
    }

    @Test
    void usesDefaultSqliteFileNameWhenNoOverrideIsConfigured() {
        Path configuredDataDir = tempDir.resolve("local-store");
        AppDataPaths appDataPaths = new AppDataPaths(configuredDataDir.toString(), null);

        assertEquals(
            configuredDataDir.resolve("todo.sqlite").toAbsolutePath().normalize(),
            appDataPaths.getSqliteDatabasePath()
        );
    }
}
