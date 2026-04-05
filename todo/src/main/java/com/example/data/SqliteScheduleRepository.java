package com.example.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.model.Schedule;

public final class SqliteScheduleRepository implements ScheduleRepository {
    private final ConnectionFactory connectionFactory;
    private final SqliteMigrationRunner migrationRunner;

    public SqliteScheduleRepository(ConnectionFactory connectionFactory, SqliteMigrationRunner migrationRunner) {
        this.connectionFactory = connectionFactory;
        this.migrationRunner = migrationRunner;
    }

    @Override
    public int addSchedule(Schedule schedule) throws SQLException {
        String sql = "INSERT INTO schedules (" +
            "name, description, start_date, due_date, completed, priority, category, tags, reminder_time, color, created_at, updated_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        if (schedule.getCreatedAt() == null) {
            schedule.setCreatedAt(now);
        }
        schedule.setUpdatedAt(now);

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindInsertSchedule(statement, schedule, now);
            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        schedule.setId(generatedKeys.getInt(1));
                    }
                }
            }
            return affectedRows;
        }
    }

    @Override
    public boolean updateSchedule(Schedule schedule) throws SQLException {
        String sql = "UPDATE schedules SET " +
            "name = ?, description = ?, start_date = ?, due_date = ?, completed = ?, priority = ?, category = ?, " +
            "tags = ?, reminder_time = ?, color = ?, updated_at = ?, " +
            "version = COALESCE(version, 1) + 1, " +
            "sync_status = CASE WHEN sync_status = 'synced' THEN 'pending_upload' ELSE sync_status END " +
            "WHERE id = ? AND deleted_at IS NULL";

        LocalDateTime updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        schedule.setUpdatedAt(updatedAt);

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindUpdateSchedule(statement, schedule, updatedAt);
            statement.setInt(12, schedule.getId());
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteSchedule(int id) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM schedules WHERE id = ?"
             )) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public Schedule getScheduleById(int id) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM schedules WHERE id = ? AND deleted_at IS NULL"
             )) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSchedule(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<Schedule> getAllSchedules() throws SQLException {
        return querySchedules(
            "SELECT * FROM schedules WHERE deleted_at IS NULL ORDER BY due_date ASC, id ASC",
            statement -> {
            }
        );
    }

    @Override
    public List<Schedule> searchSchedules(String keyword) throws SQLException {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return querySchedules(
            "SELECT * FROM schedules " +
                "WHERE deleted_at IS NULL AND (lower(name) LIKE lower(?) OR lower(description) LIKE lower(?)) " +
                "ORDER BY due_date ASC, id ASC",
            statement -> {
                String searchPattern = "%" + normalizedKeyword + "%";
                statement.setString(1, searchPattern);
                statement.setString(2, searchPattern);
            }
        );
    }

    @Override
    public boolean updateScheduleStatus(int id, boolean completed) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE schedules SET completed = ?, updated_at = ?, " +
                     "version = COALESCE(version, 1) + 1, " +
                     "sync_status = CASE WHEN sync_status = 'synced' THEN 'pending_upload' ELSE sync_status END " +
                     "WHERE id = ? AND deleted_at IS NULL"
             )) {
            statement.setBoolean(1, completed);
            statement.setString(2, toDateTimeString(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
            statement.setInt(3, id);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public Map<LocalDate, Integer> getDailyCompletionStats(LocalDate startDate, LocalDate endDate) throws SQLException {
        Map<LocalDate, Integer> stats = new LinkedHashMap<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT date(updated_at) AS completion_date, COUNT(*) AS count " +
                     "FROM schedules " +
                     "WHERE deleted_at IS NULL AND completed = 1 AND date(updated_at) BETWEEN ? AND ? " +
                     "GROUP BY date(updated_at) ORDER BY completion_date"
             )) {
            statement.setString(1, startDate.toString());
            statement.setString(2, endDate.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String completionDate = resultSet.getString("completion_date");
                    if (completionDate != null && !completionDate.isBlank()) {
                        stats.put(LocalDate.parse(completionDate), resultSet.getInt("count"));
                    }
                }
            }
        }
        return stats;
    }

    private Connection openConnection() throws SQLException {
        Connection connection = connectionFactory.getConnection();
        try {
            migrationRunner.ensureSchema(connection);
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            throw exception;
        }
    }

    private void bindInsertSchedule(PreparedStatement statement, Schedule schedule, LocalDateTime updatedAt) throws SQLException {
        statement.setString(1, schedule.getName());
        statement.setString(2, schedule.getDescription());
        statement.setString(3, toDateString(schedule.getStartDate()));
        statement.setString(4, toDateString(schedule.getDueDate()));
        statement.setBoolean(5, schedule.isCompleted());
        statement.setString(6, schedule.getPriority());
        statement.setString(7, schedule.getCategory());
        statement.setString(8, schedule.getTags());
        statement.setString(9, toDateTimeString(schedule.getReminderTime()));
        statement.setString(10, schedule.getColor());
        statement.setString(11, toDateTimeString(schedule.getCreatedAt()));
        statement.setString(12, toDateTimeString(updatedAt));
    }

    private void bindUpdateSchedule(PreparedStatement statement, Schedule schedule, LocalDateTime updatedAt) throws SQLException {
        statement.setString(1, schedule.getName());
        statement.setString(2, schedule.getDescription());
        statement.setString(3, toDateString(schedule.getStartDate()));
        statement.setString(4, toDateString(schedule.getDueDate()));
        statement.setBoolean(5, schedule.isCompleted());
        statement.setString(6, schedule.getPriority());
        statement.setString(7, schedule.getCategory());
        statement.setString(8, schedule.getTags());
        statement.setString(9, toDateTimeString(schedule.getReminderTime()));
        statement.setString(10, schedule.getColor());
        statement.setString(11, toDateTimeString(updatedAt));
    }

    private List<Schedule> querySchedules(String sql, StatementBinder binder) throws SQLException {
        List<Schedule> schedules = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapSchedule(resultSet));
                }
            }
        }
        return schedules;
    }

    private Schedule mapSchedule(ResultSet resultSet) throws SQLException {
        Schedule schedule = new Schedule();
        schedule.setId(resultSet.getInt("id"));
        schedule.setName(resultSet.getString("name"));
        schedule.setDescription(resultSet.getString("description"));
        schedule.setStartDate(parseDate(resultSet.getString("start_date")));
        schedule.setDueDate(parseDate(resultSet.getString("due_date")));
        schedule.setCompleted(resultSet.getBoolean("completed"));
        schedule.setPriority(defaultIfBlank(resultSet.getString("priority"), schedule.getPriority()));
        schedule.setCategory(defaultIfBlank(resultSet.getString("category"), schedule.getCategory()));
        schedule.setTags(resultSet.getString("tags"));
        schedule.setReminderTime(parseDateTime(resultSet.getString("reminder_time")));
        schedule.setColor(defaultIfBlank(resultSet.getString("color"), schedule.getColor()));
        schedule.setCreatedAt(parseDateTime(resultSet.getString("created_at")));
        schedule.setUpdatedAt(parseDateTime(resultSet.getString("updated_at")));
        return schedule;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value);
    }

    private String toDateString(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String toDateTimeString(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
