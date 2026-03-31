package com.example.databaseutil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.example.model.Schedule;

public class ScheduleDAO {
    //增加日程
    public int addSchedule(Schedule schedule) throws SQLException {
        String sql = "INSERT INTO schedules (name, description, due_date, completed, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schedule.getName());
            pstmt.setString(2, schedule.getDescription());
            pstmt.setString(3, schedule.getDueDate());
            pstmt.setBoolean(4, schedule.isCompleted());
            pstmt.setString(5, schedule.getCreatedAt());
            pstmt.setString(6, schedule.getUpdatedAt());
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error adding schedule", e);
        }
    }
    //删除日程
    public int deleteSchedule(int id) throws SQLException {
        String sql = "DELETE FROM schedules WHERE id = ?";
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting schedule", e);
        }
    }
    //查询所有日程
    //查询所有日程
    public List<Schedule> getAllSchedules() throws SQLException {
        String sql = "SELECT * FROM schedules ORDER BY due_date ASC";
        List<Schedule> schedules = new ArrayList<>();
        
        try (Connection conn = Connectdatabase.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Schedule schedule = new Schedule();
                schedule.setId(rs.getInt("id"));
                schedule.setName(rs.getString("name"));
                schedule.setDescription(rs.getString("description"));
                schedule.setDueDate(rs.getString("due_date"));
                schedule.setCompleted(rs.getBoolean("completed"));
                schedule.setCreatedAt(rs.getString("created_at"));
                schedule.setUpdatedAt(rs.getString("updated_at"));
                
                schedules.add(schedule);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying schedules", e);
        }
        
        return schedules;
    }
    //查询指定日程详情
    public Schedule getScheduleById(int id) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE id = ?";
        
        try (Connection conn = Connectdatabase.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);  // 🔑 关键：绑定参数
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Schedule schedule = new Schedule();
                    schedule.setId(rs.getInt("id"));
                    schedule.setName(rs.getString("name"));
                    schedule.setDescription(rs.getString("description"));
                    schedule.setDueDate(rs.getString("due_date"));
                    schedule.setCompleted(rs.getBoolean("completed"));
                    schedule.setCreatedAt(rs.getString("created_at"));
                    schedule.setUpdatedAt(rs.getString("updated_at"));
                    return schedule;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying schedule with id: " + id, e);
        }
        
        return null; // 如果没有找到对应id的日程，返回null
    }
    
    //按状态查询日程（已完成/未完成）
    public List<Schedule> getSchedulesByStatus(boolean completed) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE completed = ? ORDER BY due_date ASC";
        List<Schedule> schedules = new ArrayList<>();
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, completed);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Schedule schedule = new Schedule();
                    schedule.setId(rs.getInt("id"));
                    schedule.setName(rs.getString("name"));
                    schedule.setDescription(rs.getString("description"));
                    schedule.setDueDate(rs.getString("due_date"));
                    schedule.setCompleted(rs.getBoolean("completed"));
                    schedule.setCreatedAt(rs.getString("created_at"));
                    schedule.setUpdatedAt(rs.getString("updated_at"));
                    
                    schedules.add(schedule);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying schedules by status: " + completed, e);
        }
        
        return schedules;
    }
    
    //按日期范围查询日程
    public List<Schedule> getSchedulesByDateRange(String startDate, String endDate) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE due_date BETWEEN ? AND ? ORDER BY due_date ASC";
        List<Schedule> schedules = new ArrayList<>();
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Schedule schedule = new Schedule();
                    schedule.setId(rs.getInt("id"));
                    schedule.setName(rs.getString("name"));
                    schedule.setDescription(rs.getString("description"));
                    schedule.setDueDate(rs.getString("due_date"));
                    schedule.setCompleted(rs.getBoolean("completed"));
                    schedule.setCreatedAt(rs.getString("created_at"));
                    schedule.setUpdatedAt(rs.getString("updated_at"));
                    
                    schedules.add(schedule);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying schedules by date range: " + startDate + " to " + endDate, e);
        }
        
        return schedules;
    }
    
    //模糊搜索日程（按名称或描述）
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
                    Schedule schedule = new Schedule();
                    schedule.setId(rs.getInt("id"));
                    schedule.setName(rs.getString("name"));
                    schedule.setDescription(rs.getString("description"));
                    schedule.setDueDate(rs.getString("due_date"));
                    schedule.setCompleted(rs.getBoolean("completed"));
                    schedule.setCreatedAt(rs.getString("created_at"));
                    schedule.setUpdatedAt(rs.getString("updated_at"));
                    
                    schedules.add(schedule);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching schedules with keyword: " + keyword, e);
        }
        
        return schedules;
    }
    
    //更新日程信息（完整更新）
    public boolean updateSchedule(Schedule schedule) throws SQLException {
        String sql = "UPDATE schedules SET name = ?, description = ?, due_date = ?, completed = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, schedule.getName());
            pstmt.setString(2, schedule.getDescription());
            pstmt.setString(3, schedule.getDueDate());
            pstmt.setBoolean(4, schedule.isCompleted());
            pstmt.setString(5, schedule.getUpdatedAt());
            pstmt.setInt(6, schedule.getId());
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating schedule with id: " + schedule.getId(), e);
        }
    }
    
    //更新日程完成状态
    public boolean updateScheduleStatus(int id, boolean completed) throws SQLException {
        String sql = "UPDATE schedules SET completed = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, completed);
            pstmt.setInt(2, id);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating schedule status with id: " + id, e);
        }
    }
    
    //统计功能：获取日程总数
    public int getTotalSchedulesCount() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM schedules";
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error counting schedules", e);
        }
    }
    
    //统计功能：获取已完成日程数量
    public int getCompletedSchedulesCount() throws SQLException {
        String sql = "SELECT COUNT(*) as completed_count FROM schedules WHERE completed = true";
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("completed_count");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error counting completed schedules", e);
        }
    }
    
    //分页查询：支持大量数据的分页显示
    public List<Schedule> getSchedulesWithPagination(int page, int pageSize) throws SQLException {
        String sql = "SELECT * FROM schedules ORDER BY due_date ASC LIMIT ? OFFSET ?";
        List<Schedule> schedules = new ArrayList<>();
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int offset = (page - 1) * pageSize;
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Schedule schedule = new Schedule();
                    schedule.setId(rs.getInt("id"));
                    schedule.setName(rs.getString("name"));
                    schedule.setDescription(rs.getString("description"));
                    schedule.setDueDate(rs.getString("due_date"));
                    schedule.setCompleted(rs.getBoolean("completed"));
                    schedule.setCreatedAt(rs.getString("created_at"));
                    schedule.setUpdatedAt(rs.getString("updated_at"));
                    
                    schedules.add(schedule);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying schedules with pagination", e);
        }
        
        return schedules;
    }
    
    //获取即将到期的日程（未来7天内）
    public List<Schedule> getUpcomingSchedules() throws SQLException {
        String sql = "SELECT * FROM schedules WHERE due_date BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY) AND completed = false ORDER BY due_date ASC";
        List<Schedule> schedules = new ArrayList<>();
        
        try (Connection conn = Connectdatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Schedule schedule = new Schedule();
                schedule.setId(rs.getInt("id"));
                schedule.setName(rs.getString("name"));
                schedule.setDescription(rs.getString("description"));
                schedule.setDueDate(rs.getString("due_date"));
                schedule.setCompleted(rs.getBoolean("completed"));
                schedule.setCreatedAt(rs.getString("created_at"));
                schedule.setUpdatedAt(rs.getString("updated_at"));
                
                schedules.add(schedule);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying upcoming schedules", e);
        }
        
        return schedules;
    }

}