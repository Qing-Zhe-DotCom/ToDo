package com.example.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
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

    private SqliteScheduleRepository repository;

    @BeforeEach
    void setUp() {
        AppDataPaths appDataPaths = new AppDataPaths(tempDir.resolve("data").toString(), "schedule.sqlite");
        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(sqliteProperties(), appDataPaths);
        repository = new SqliteScheduleRepository(connectionFactory, new SqliteMigrationRunner());
    }

    @Test
    void addAndLoadSchedulePersistsCoreFields() throws SQLException {
        Schedule created = buildSchedule("Write plan", "Phase A core switch");

        repository.addSchedule(created);
        Schedule reloaded = repository.getScheduleById(created.getId());

        assertTrue(created.getId() > 0);
        assertNotNull(reloaded);
        assertEquals("Write plan", reloaded.getName());
        assertEquals("Phase A core switch", reloaded.getDescription());
        assertEquals(LocalDate.of(2026, 4, 5), reloaded.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 7), reloaded.getDueDate());
        assertEquals("工作", reloaded.getCategory());
        assertEquals("规划,SQLite", reloaded.getTags());
        assertEquals(LocalDateTime.of(2026, 4, 5, 9, 30), reloaded.getReminderTime());
    }

    @Test
    void updateSearchStatusAndStatsWorkAgainstSqlite() throws SQLException {
        Schedule schedule = buildSchedule("Migrate database", "Replace MySQL with SQLite");
        repository.addSchedule(schedule);

        schedule.setDescription("Replace MySQL with SQLite in default runtime");
        schedule.setPriority("高");
        schedule.setCompleted(true);

        assertTrue(repository.updateSchedule(schedule));

        List<Schedule> searchResults = repository.searchSchedules("sqlite");
        assertEquals(1, searchResults.size());
        assertEquals(schedule.getId(), searchResults.get(0).getId());

        assertTrue(repository.updateScheduleStatus(schedule.getId(), true));

        Map<LocalDate, Integer> stats = repository.getDailyCompletionStats(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1)
        );
        assertEquals(Integer.valueOf(1), stats.get(LocalDate.now()));
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
        schedule.setStartDate(LocalDate.of(2026, 4, 5));
        schedule.setDueDate(LocalDate.of(2026, 4, 7));
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
