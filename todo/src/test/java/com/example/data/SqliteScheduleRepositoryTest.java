package com.example.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.config.AppDataPaths;
import com.example.config.DatabaseProperties;
import com.example.model.RecurrenceRule;
import com.example.model.Reminder;
import com.example.model.Schedule;

class SqliteScheduleRepositoryTest {

    @TempDir
    Path tempDir;

    private SqliteConnectionFactory connectionFactory;
    private SqlScheduleItemRepository repository;

    @BeforeEach
    void setUp() {
        AppDataPaths appDataPaths = new AppDataPaths(tempDir.resolve("data").toString(), "schedule.sqlite");
        connectionFactory = new SqliteConnectionFactory(sqliteProperties(), appDataPaths);
        repository = new SqlScheduleItemRepository(
            connectionFactory,
            new SqliteStageBSchemaManager(),
            SqlDialect.SQLITE
        );
    }

    @Test
    void addAndLoadSchedulePersistsStageBAggregateFields() throws SQLException {
        Schedule created = buildSchedule("Write plan", "Phase B full switch");

        repository.addScheduleItem(created);
        Schedule reloaded = new Schedule(repository.getScheduleItemById(created.getId()));

        assertNotNull(reloaded);
        assertFalse(reloaded.getId().isBlank());
        assertEquals("Write plan", reloaded.getName());
        assertEquals("Phase B full switch", reloaded.getDescription());
        assertEquals(LocalDateTime.of(2026, 4, 5, 8, 30), reloaded.getStartAt());
        assertEquals(LocalDateTime.of(2026, 4, 7, 23, 59), reloaded.getDueAt());
        assertEquals(List.of("SQLite", "规划", "阶段B"), reloaded.getTagNames());
        assertEquals(2, reloaded.getReminders().size());
        assertNotNull(reloaded.getRecurrenceRule());
        assertEquals(RecurrenceRule.FREQ_WEEKLY, reloaded.getRecurrenceRule().getFrequency());
    }

    @Test
    void searchStatusOutboxAndStatsFollowNewRepositoryContract() throws SQLException {
        Schedule schedule = buildSchedule("Migrate database", "Replace single table");
        schedule.setCategory("基础设施");
        schedule.setTags("SQLite, 本地优先");
        repository.addScheduleItem(schedule);

        schedule.setDescription("Replace single table with stage B aggregate");
        schedule.touchForWrite("device-a");
        assertTrue(repository.updateScheduleItem(schedule));

        List<?> tagResults = repository.searchActiveScheduleItems("本地优先");
        List<?> categoryResults = repository.searchActiveScheduleItems("基础设施");
        assertEquals(1, tagResults.size());
        assertEquals(1, categoryResults.size());

        assertTrue(repository.updateScheduleItemCompletion(schedule.getId(), true, "device-a"));

        Map<LocalDate, Integer> stats = repository.getDailyCompletionStats(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1)
        );
        assertEquals(Integer.valueOf(1), stats.get(LocalDate.now()));
        assertEquals(3, countRows("sync_outbox"));
    }

    @Test
    void softDeleteRestoreAndPermanentDeleteRespectSyncBoundary() throws SQLException {
        Schedule schedule = buildSchedule("Temporary", "Delete me");
        repository.addScheduleItem(schedule);

        assertEquals(1, repository.getActiveScheduleItems().size());
        assertTrue(repository.softDeleteScheduleItem(schedule.getId(), "device-a"));
        assertTrue(repository.getActiveScheduleItems().isEmpty());
        assertEquals(1, repository.getDeletedScheduleItems().size());

        assertTrue(repository.restoreScheduleItem(schedule.getId(), "device-a"));
        assertEquals(1, repository.getActiveScheduleItems().size());

        assertTrue(repository.softDeleteScheduleItem(schedule.getId(), "device-a"));
        assertTrue(repository.permanentlyDeleteScheduleItem(schedule.getId()));
        assertNull(repository.getScheduleItemById(schedule.getId()));
    }

    @Test
    void registeredDeviceAppearsInRegistry() throws SQLException {
        repository.ensureDeviceRegistered("device-x", "test-device", "0.1.0");
        assertEquals(1, countRows("device_registry"));
    }

    private int countRows(String tableName) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private DatabaseProperties sqliteProperties() {
        return new DatabaseProperties("sqlite", "org.sqlite.JDBC", "", "", "", null);
    }

    private Schedule buildSchedule(String name, String description) {
        Schedule schedule = new Schedule();
        schedule.setName(name);
        schedule.setDescription(description);
        schedule.setNotes(description);
        schedule.setStartAt(LocalDateTime.of(2026, 4, 5, 8, 30));
        schedule.setDueAt(LocalDateTime.of(2026, 4, 7, 23, 59));
        schedule.setCompleted(false);
        schedule.setPriority("中");
        schedule.setCategory("工作");
        schedule.setTags("规划, SQLite, 阶段B");
        schedule.addReminder(new Reminder(LocalDateTime.of(2026, 4, 5, 9, 30)));
        schedule.addReminder(new Reminder(LocalDateTime.of(2026, 4, 6, 9, 30)));
        schedule.setColor("#42A5F5");
        schedule.setCreatedAt(LocalDateTime.of(2026, 4, 5, 8, 0));
        schedule.setUpdatedAt(LocalDateTime.of(2026, 4, 5, 8, 0));
        schedule.setDeviceId("device-a");

        RecurrenceRule rule = new RecurrenceRule();
        rule.setFrequency(RecurrenceRule.FREQ_WEEKLY);
        rule.setInterval(1);
        rule.setByDays(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        rule.setTimezone("Asia/Shanghai");
        schedule.setRecurrenceRule(rule);
        return schedule;
    }
}
