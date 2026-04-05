package com.example.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.config.AppDataPaths;
import com.example.config.DatabaseProperties;
import com.example.model.Schedule;

class SqliteScheduleRepositoryTest {

    @TempDir
    Path tempDir;

    private SqliteConnectionFactory connectionFactory;
    private SqliteScheduleRepository repository;

    @BeforeEach
    void setUp() {
        AppDataPaths appDataPaths = new AppDataPaths(tempDir.resolve("data").toString(), "schedule.sqlite");
        connectionFactory = new SqliteConnectionFactory(sqliteProperties(), appDataPaths);
        repository = new SqliteScheduleRepository(connectionFactory, new SqliteMigrationRunner());
    }

    @Test
    void addAndLoadSchedulePersistsMinuteFields() throws SQLException {
        Schedule created = buildSchedule("Write plan", "Phase A core switch");

        repository.addSchedule(created);
        Schedule reloaded = repository.getScheduleById(created.getId());

        assertTrue(created.getId() > 0);
        assertNotNull(reloaded);
        assertEquals("Write plan", reloaded.getName());
        assertEquals("Phase A core switch", reloaded.getDescription());
        assertEquals(LocalDateTime.of(2026, 4, 5, 8, 30), reloaded.getStartAt());
        assertEquals(LocalDateTime.of(2026, 4, 7, 23, 59), reloaded.getDueAt());
        assertEquals(LocalDate.of(2026, 4, 5), reloaded.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 7), reloaded.getDueDate());
        assertEquals("工作", reloaded.getCategory());
        assertEquals("规划, SQLite", reloaded.getTags());
        assertEquals(LocalDateTime.of(2026, 4, 5, 9, 30), reloaded.getReminderTime());
    }

    @Test
    void searchStatusAndStatsIncludeCategoryAndTags() throws SQLException {
        Schedule schedule = buildSchedule("Migrate database", "Replace MySQL with SQLite");
        schedule.setCategory("基础设施");
        schedule.setTags("SQLite, 本地优先");
        repository.addSchedule(schedule);

        schedule.setDescription("Replace MySQL with SQLite in default runtime");
        schedule.setPriority("高");
        schedule.setCompleted(true);

        assertTrue(repository.updateSchedule(schedule));

        List<Schedule> tagResults = repository.searchSchedules("本地优先");
        List<Schedule> categoryResults = repository.searchSchedules("基础设施");
        assertEquals(1, tagResults.size());
        assertEquals(1, categoryResults.size());
        assertEquals(schedule.getId(), tagResults.get(0).getId());
        assertEquals(schedule.getId(), categoryResults.get(0).getId());

        assertTrue(repository.updateScheduleStatus(schedule.getId(), true));

        Map<LocalDate, Integer> stats = repository.getDailyCompletionStats(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1)
        );
        assertEquals(Integer.valueOf(1), stats.get(LocalDate.now()));
    }

    @Test
    void migrationBackfillsLegacyDateColumnsIntoMinuteColumns() throws Exception {
        try (Connection connection = connectionFactory.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS schedules (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "start_date TEXT, " +
                "due_date TEXT, " +
                "completed INTEGER NOT NULL DEFAULT 0, " +
                "priority TEXT DEFAULT '中', " +
                "category TEXT DEFAULT '默认', " +
                "tags TEXT, " +
                "reminder_time TEXT, " +
                "color TEXT DEFAULT '#2196F3', " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL, " +
                "deleted_at TEXT, " +
                "version INTEGER NOT NULL DEFAULT 1, " +
                "sync_status TEXT NOT NULL DEFAULT 'local_only', " +
                "last_synced_at TEXT, " +
                "device_id TEXT, " +
                "metadata_json TEXT" +
                ")");
            statement.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, script_name TEXT NOT NULL, applied_at TEXT NOT NULL)");
            statement.execute("INSERT INTO schema_version(version, script_name, applied_at) VALUES " +
                "(1, '/db/sqlite/V001__create_schedules.sql', '2026-04-05T08:00:00'), " +
                "(2, '/db/sqlite/V002__add_local_sync_columns.sql', '2026-04-05T08:00:01')");
            statement.execute("INSERT INTO schedules(name, description, start_date, due_date, completed, priority, category, tags, reminder_time, color, created_at, updated_at, deleted_at, version, sync_status, last_synced_at, device_id, metadata_json) VALUES (" +
                "'Legacy task', 'before minute precision', '2026-04-05', '2026-04-07', 0, '中', '默认', '迁移', null, '#2196F3', '2026-04-05T08:00:00', '2026-04-05T08:00:00', null, 1, 'local_only', null, null, null" +
                ")");
        }

        List<Schedule> schedules = repository.getAllSchedules();

        assertEquals(1, schedules.size());
        assertEquals(LocalDateTime.of(2026, 4, 5, 0, 0), schedules.get(0).getStartAt());
        assertEquals(LocalDateTime.of(2026, 4, 7, 23, 59), schedules.get(0).getDueAt());
        assertEquals("未分类", schedules.get(0).getCategory());
    }

    @Test
    void deleteRemovesScheduleFromSubsequentQueries() throws SQLException {
        Schedule schedule = buildSchedule("Temporary", "Delete me");
        repository.addSchedule(schedule);

        assertFalse(repository.getAllSchedules().isEmpty());
        assertTrue(repository.deleteSchedule(schedule.getId()));
        assertNull(repository.getScheduleById(schedule.getId()));
    }

    private DatabaseProperties sqliteProperties() {
        return new DatabaseProperties("sqlite", "org.sqlite.JDBC", "", "", "", null);
    }

    private Schedule buildSchedule(String name, String description) {
        Schedule schedule = new Schedule();
        schedule.setName(name);
        schedule.setDescription(description);
        schedule.setStartAt(LocalDateTime.of(2026, 4, 5, 8, 30));
        schedule.setDueAt(LocalDateTime.of(2026, 4, 7, 23, 59));
        schedule.setCompleted(false);
        schedule.setPriority("中");
        schedule.setCategory("工作");
        schedule.setTags("规划,SQLite");
        schedule.setReminderTime(LocalDateTime.of(2026, 4, 5, 9, 30));
        schedule.setColor("#42A5F5");
        schedule.setCreatedAt(LocalDateTime.of(2026, 4, 5, 8, 0));
        schedule.setUpdatedAt(LocalDateTime.of(2026, 4, 5, 8, 0));
        return schedule;
    }
}
