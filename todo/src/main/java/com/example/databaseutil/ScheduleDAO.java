package com.example.databaseutil;

import com.example.model.Schedule;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleDAO {

    private Schedule mapResultSetToSchedule(ResultSet rs) throws SQLException {
        Schedule schedule = new Schedule();
        schedule.setId(rs.getInt("id"));
        schedule.setName(rs.getString("name"));
        schedule.setDescription(rs.getString("description"));
        
        // 兼容旧表结构 - 使用try-catch处理可能不存在的列
        try {
            Date startDate = rs.getDate("start_date");
            if (startDate != null) {
                schedule.setStartDate(startDate.toLocalDate());
            }
        } catch (SQLException e) {
            // 列不存在，忽略
        }
        
        try {
            Date dueDate = rs.getDate("due_date");
            if (dueDate != null) {
                schedule.setDueDate(dueDate.toLocalDate());
            }
        } catch (SQLException e) {
            // 尝试旧列名
            try {
                Date dueDate = rs.getDate("due_date");
                if (dueDate != null) {
                    schedule.setDueDate(dueDate.toLocalDate());
                }
            } catch (SQLException e2) {
                // 忽略
            }
        }
        
        schedule.setCompleted(rs.getBoolean("completed"));
        
        // 兼容新字段
        try {
            String priority = rs.getString("priority");
            if (priority != null) {
                schedule.setPriority(priority);
            }
        } catch (SQLException e) {
            schedule.setPriority("中");
        }
        
        try {
            String category = rs.getString("category");
            if (category != null) {
                schedule.setCategory(category);
            }
        } catch (SQLException e) {
            schedule.setCategory("默认");
        }
        
        try {
            schedule.setTags(rs.getString("tags"));
        } catch (SQLException e) {
            // 忽略
        }
        
        try {
            Timestamp reminderTime = rs.getTimestamp("reminder_time");
            if (reminderTime != null) {
                schedule.setReminderTime(reminderTime.toLocalDateTime());
            }
        } catch (SQLException e) {
            // 忽略
        }
        
        try {
            String color = rs.getString("color");
            if (color != null) {
                schedule.setColor(color);
            }
        } catch (SQLException e) {
            schedule.setColor("#2196F3");
        }
        
        try {
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                schedule.setCreatedAt(createdAt.toLocalDateTime());
            }
        } catch (SQLException e) {
            // 忽略
        }
        
        try {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                schedule.setUpdatedAt(updatedAt.toLocalDateTime());
            }
        } catch (SQLException e) {
            // 忽略
        }
        
        return schedule;
    }

    public int addSchedule(Schedule schedule) throws SQLException {
        // 尝试使用新表结构，如果失败则使用旧表结构
        try {
            String sql = "INSERT INTO schedules (name, description, start_date, due_date, completed, " +
                         "priority, category, tags, reminder_time, color, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = Connectdatabase.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                pstmt.setString(1, schedule.getName());
                pstmt.setString(2, schedule.getDescription());
                pstmt.setDate(3, schedule.getStartDate() != null ? Date.valueOf(schedule.getStartDate()) : null);
                pstmt.setDate(4, schedule.getDueDate() != null ? Date.valueOf(schedule.getDueDate()) : null);
                pstmt.setBoolean(5, schedule.isCompleted());
                pstmt.setString(6, schedule.getPriority());
                pstmt.setString(7, schedule.getCategory());
                pstmt.setString(8, schedule.getTags());
                pstmt.setTimestamp(9, schedule.getReminderTime() != null ? Timestamp.valueOf(schedule.getReminderTime()) : null);
                pstmt.setString(10, schedule.getColor());
                pstmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
                
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
            // 如果新表结构失败，尝试旧表结构
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
            
            pstmt.setString(1, schedule.getName());
            pstmt.setString(2, schedule.getDescription());
            pstmt.setString(3, schedule.getDueDate() != null ? schedule.getDueDate().toString() : null);
            pstmt.setBoolean(4, schedule.isCompleted());
            pstmt.setString(5, LocalDateTime.now().toString());
            pstmt.setString(6, LocalDateTime.now().toString());
            
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
            String sql = "UPDATE schedules SET name = ?, description = ?, start_date = ?, due_date = ?, " +
                         "completed = ?, priority = ?, category = ?, tags = ?, reminder_time = ?, " +
                         "color = ?, updated_at = ? WHERE id = ?";
            
            try (Connection conn = Connectdatabase.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, schedule.getName());
                pstmt.setString(2, schedule.getDescription());
                pstmt.setDate(3, schedule.getStartDate() != null ? Date.valueOf(schedule.getStartDate()) : null);
                pstmt.setDate(4, schedule.getDueDate() != null ? Date.valueOf(schedule.getDueDate()) : null);
                pstmt.setBoolean(5, schedule.isCompleted());
                pstmt.setString(6, schedule.getPriority());
                pstmt.setString(7, schedule.getCategory());
                pstmt.setString(8, schedule.getTags());
                pstmt.setTimestamp(9, schedule.getReminderTime() != null ? Timestamp.valueOf(schedule.getReminderTime()) : null);
                pstmt.setString(10, schedule.getColor());
                pstmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setInt(12, schedule.getId());
                
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
        String sql = "SELECT * FROM schedules ORDER BY due_date ASC";
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

    public List<Schedule> getSchedulesByStatus(boolean completed) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE completed = ? ORDER BY due_date ASC";
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
        String sql = "SELECT * FROM schedules WHERE due_date BETWEEN ? AND ? ORDER BY due_date ASC";
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
        String sql = "SELECT * FROM schedules WHERE due_date BETWEEN ? AND ? AND completed = false ORDER BY due_date ASC";
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
        String sql = "SELECT * FROM schedules WHERE due_date < ? AND completed = false ORDER BY due_date ASC";
        List<Schedule> schedules = new ArrayList<>();
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, Date.valueOf(LocalDate.now()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    public List<Schedule> searchSchedules(String keyword) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE name LIKE ? OR description LIKE ? ORDER BY due_date ASC";
        List<Schedule> schedules = new ArrayList<>();
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        }
        return schedules;
    }

    public List<Schedule> getSchedulesByCategory(String category) throws SQLException {
        try {
            String sql = "SELECT * FROM schedules WHERE category = ? ORDER BY due_date ASC";
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
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
                return new ArrayList<>();
            }
            throw e;
        }
    }

    public List<Schedule> getSchedulesByPriority(String priority) throws SQLException {
        try {
            String sql = "SELECT * FROM schedules WHERE priority = ? ORDER BY due_date ASC";
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
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
                return new ArrayList<>();
            }
            throw e;
        }
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
            // 兼容旧表结构
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
        try {
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
        } catch (SQLException e) {
            // 兼容旧表结构
        }
        return 0;
    }

    public Map<LocalDate, Integer> getDailyCompletionStats(LocalDate startDate, LocalDate endDate) throws SQLException {
        Map<LocalDate, Integer> stats = new HashMap<>();
        
        try {
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
        } catch (SQLException e) {
            // 兼容旧表结构，返回空统计
        }
        
        return stats;
    }

    public List<String> getAllCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        
        try {
            String sql = "SELECT DISTINCT category FROM schedules WHERE category IS NOT NULL ORDER BY category";
            
            try (Connection conn = Connectdatabase.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                while (rs.next()) {
                    categories.add(rs.getString("category"));
                }
            }
        } catch (SQLException e) {
            // 兼容旧表结构
        }
        
        return categories;
    }

    public List<Schedule> getSchedulesWithPagination(int page, int pageSize) throws SQLException {
        String sql = "SELECT * FROM schedules ORDER BY due_date ASC LIMIT ? OFFSET ?";
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
}
