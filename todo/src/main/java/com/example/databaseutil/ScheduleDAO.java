package com.example.databaseutil;

import com.example.model.Schedule;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleDAO {
    private static final String ORDER_BY_DEADLINE = " ORDER BY COALESCE(due_at, due_date, start_at, start_date) ASC";

    private Schedule mapResultSetToSchedule(ResultSet rs) throws SQLException {
        Schedule schedule = new Schedule();
        schedule.setId(rs.getInt("id"));
        schedule.setName(rs.getString("name"));
        schedule.setDescription(rs.getString("description"));

        Timestamp startAt = tryGetTimestamp(rs, "start_at");
        if (startAt != null) {
            schedule.setStartAt(startAt.toLocalDateTime());
        } else {
            Date startDate = tryGetDate(rs, "start_date");
            if (startDate != null) {
                schedule.setStartDate(startDate.toLocalDate());
            }
        }

        Timestamp dueAt = tryGetTimestamp(rs, "due_at");
        if (dueAt != null) {
            schedule.setDueAt(dueAt.toLocalDateTime());
        } else {
            Date dueDate = tryGetDate(rs, "due_date");
            if (dueDate != null) {
                schedule.setDueDate(dueDate.toLocalDate());
            }
        }

        schedule.setCompleted(rs.getBoolean("completed"));
        schedule.setPriority(tryGetString(rs, "priority"));
        schedule.setCategory(tryGetString(rs, "category"));
        schedule.setTags(tryGetString(rs, "tags"));

        Timestamp reminderTime = tryGetTimestamp(rs, "reminder_time");
        if (reminderTime != null) {
            schedule.setReminderTime(reminderTime.toLocalDateTime());
        }

        schedule.setColor(tryGetString(rs, "color"));

        Timestamp createdAt = tryGetTimestamp(rs, "created_at");
        if (createdAt != null) {
            schedule.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = tryGetTimestamp(rs, "updated_at");
        if (updatedAt != null) {
            schedule.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return schedule;
    }

    public int addSchedule(Schedule schedule) throws SQLException {
        try {
            String sql = "INSERT INTO schedules (name, description, start_date, due_date, start_at, due_at, completed, " +
                "priority, category, tags, reminder_time, color, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            LocalDateTime now = LocalDateTime.now();
            try (Connection conn = Connectdatabase.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setString(1, schedule.getName());
                pstmt.setString(2, schedule.getDescription());
                pstmt.setDate(3, toSqlDate(schedule.getStartDate()));
                pstmt.setDate(4, toSqlDate(schedule.getDueDate()));
                pstmt.setTimestamp(5, toSqlTimestamp(schedule.getStartAt()));
                pstmt.setTimestamp(6, toSqlTimestamp(schedule.getDueAt()));
                pstmt.setBoolean(7, schedule.isCompleted());
                pstmt.setString(8, schedule.getPriority());
                pstmt.setString(9, schedule.getCategory());
                pstmt.setString(10, schedule.getTags());
                pstmt.setTimestamp(11, toSqlTimestamp(schedule.getReminderTime()));
                pstmt.setString(12, schedule.getColor());
                pstmt.setTimestamp(13, Timestamp.valueOf(schedule.getCreatedAt() != null ? schedule.getCreatedAt() : now));
                pstmt.setTimestamp(14, Timestamp.valueOf(now));

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            schedule.setId(generatedKeys.getInt(1));
                        }
                    }
                }

                return affectedRows;
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
                return addScheduleOldSchema(schedule);
            }
            throw e;
        }
    }

    private int addScheduleOldSchema(Schedule schedule) throws SQLException {
        String sql = "INSERT INTO schedules (name, description, due_date, completed, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = LocalDateTime.now();
            pstmt.setString(1, schedule.getName());
            pstmt.setString(2, schedule.getDescription());
            pstmt.setString(3, schedule.getDueDate() != null ? schedule.getDueDate().toString() : null);
            pstmt.setBoolean(4, schedule.isCompleted());
            pstmt.setString(5, now.toString());
            pstmt.setString(6, now.toString());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        schedule.setId(generatedKeys.getInt(1));
                    }
                }
            }

            return affectedRows;
        }
    }

    public boolean updateSchedule(Schedule schedule) throws SQLException {
        try {
            String sql = "UPDATE schedules SET name = ?, description = ?, start_date = ?, due_date = ?, start_at = ?, due_at = ?, " +
                "completed = ?, priority = ?, category = ?, tags = ?, reminder_time = ?, color = ?, updated_at = ? WHERE id = ?";

            try (Connection conn = Connectdatabase.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, schedule.getName());
                pstmt.setString(2, schedule.getDescription());
                pstmt.setDate(3, toSqlDate(schedule.getStartDate()));
                pstmt.setDate(4, toSqlDate(schedule.getDueDate()));
                pstmt.setTimestamp(5, toSqlTimestamp(schedule.getStartAt()));
                pstmt.setTimestamp(6, toSqlTimestamp(schedule.getDueAt()));
                pstmt.setBoolean(7, schedule.isCompleted());
                pstmt.setString(8, schedule.getPriority());
                pstmt.setString(9, schedule.getCategory());
                pstmt.setString(10, schedule.getTags());
                pstmt.setTimestamp(11, toSqlTimestamp(schedule.getReminderTime()));
                pstmt.setString(12, schedule.getColor());
                pstmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setInt(14, schedule.getId());

                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
                return updateScheduleOldSchema(schedule);
            }
            throw e;
        }
    }

    private boolean updateScheduleOldSchema(Schedule schedule) throws SQLException {
        String sql = "UPDATE schedules SET name = ?, description = ?, due_date = ?, completed = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, schedule.getName());
            pstmt.setString(2, schedule.getDescription());
            pstmt.setString(3, schedule.getDueDate() != null ? schedule.getDueDate().toString() : null);
            pstmt.setBoolean(4, schedule.isCompleted());
            pstmt.setString(5, LocalDateTime.now().toString());
            pstmt.setInt(6, schedule.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteSchedule(int id) throws SQLException {
        String sql = "DELETE FROM schedules WHERE id = ?";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public Schedule getScheduleById(int id) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE id = ?";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSchedule(rs);
                }
            }
        }
        return null;
    }

    public List<Schedule> getAllSchedules() throws SQLException {
        return querySchedules("SELECT * FROM schedules" + ORDER_BY_DEADLINE);
    }

    public List<Schedule> getSchedulesByStatus(boolean completed) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE completed = ?" + ORDER_BY_DEADLINE;
        List<Schedule> schedules = new ArrayList<>();

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, completed);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    public List<Schedule> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE DATE(COALESCE(due_at, due_date)) BETWEEN ? AND ?" + ORDER_BY_DEADLINE;
        List<Schedule> schedules = new ArrayList<>();

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    public List<Schedule> getUpcomingSchedules(int days) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE DATE(COALESCE(due_at, due_date)) BETWEEN ? AND ? AND completed = false" +
            ORDER_BY_DEADLINE;
        List<Schedule> schedules = new ArrayList<>();

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(LocalDate.now()));
            pstmt.setDate(2, Date.valueOf(LocalDate.now().plusDays(days)));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    public List<Schedule> getOverdueSchedules() throws SQLException {
        String sql = "SELECT * FROM schedules WHERE COALESCE(due_at, due_date) < ? AND completed = false" + ORDER_BY_DEADLINE;
        List<Schedule> schedules = new ArrayList<>();

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    public List<Schedule> searchSchedules(String keyword) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE " +
            "name LIKE ? OR description LIKE ? OR category LIKE ? OR tags LIKE ?" +
            ORDER_BY_DEADLINE;
        List<Schedule> schedules = new ArrayList<>();

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    public List<Schedule> getSchedulesByCategory(String category) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE category = ?" + ORDER_BY_DEADLINE;
        List<Schedule> schedules = new ArrayList<>();

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    public List<Schedule> getSchedulesByPriority(String priority) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE priority = ?" + ORDER_BY_DEADLINE;
        List<Schedule> schedules = new ArrayList<>();

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, priority);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    public boolean updateScheduleStatus(int id, boolean completed) throws SQLException {
        String sql = "UPDATE schedules SET completed = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, completed);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(3, id);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            String sqlOld = "UPDATE schedules SET completed = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection conn = Connectdatabase.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlOld)) {

                pstmt.setBoolean(1, completed);
                pstmt.setInt(2, id);

                return pstmt.executeUpdate() > 0;
            }
        }
    }

    public int getTotalSchedulesCount() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM schedules";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        return 0;
    }

    public int getCompletedSchedulesCount() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM schedules WHERE completed = true";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    public int getCompletedCountByDate(LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM schedules WHERE completed = true AND DATE(updated_at) = ?";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(date));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    public Map<LocalDate, Integer> getDailyCompletionStats(LocalDate startDate, LocalDate endDate) throws SQLException {
        Map<LocalDate, Integer> stats = new HashMap<>();
        String sql = "SELECT DATE(updated_at) as completion_date, COUNT(*) as count " +
            "FROM schedules WHERE completed = true AND DATE(updated_at) BETWEEN ? AND ? " +
            "GROUP BY DATE(updated_at) ORDER BY completion_date";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("completion_date").toLocalDate();
                    int count = rs.getInt("count");
                    stats.put(date, count);
                }
            }
        }

        return stats;
    }

    public List<String> getAllCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM schedules WHERE category IS NOT NULL ORDER BY category";

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        }

        return categories;
    }

    public List<Schedule> getSchedulesWithPagination(int page, int pageSize) throws SQLException {
        String sql = "SELECT * FROM schedules" + ORDER_BY_DEADLINE + " LIMIT ? OFFSET ?";
        List<Schedule> schedules = new ArrayList<>();

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, (page - 1) * pageSize);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    private List<Schedule> querySchedules(String sql) throws SQLException {
        List<Schedule> schedules = new ArrayList<>();

        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                schedules.add(mapResultSetToSchedule(rs));
            }
        }

        return schedules;
    }

    private String tryGetString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private Date tryGetDate(ResultSet rs, String column) {
        try {
            return rs.getDate(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private Timestamp tryGetTimestamp(ResultSet rs, String column) {
        try {
            return rs.getTimestamp(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private Timestamp toSqlTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
