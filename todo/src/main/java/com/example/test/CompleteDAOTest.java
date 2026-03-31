package com.example.test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;

public class CompleteDAOTest {
    public static void main(String[] args) {
        ScheduleDAO dao = new ScheduleDAO();
        
        System.out.println("=== 完整DAO功能测试 ===\n");
        
        try {
            // 测试1：统计功能
            System.out.println("1. 统计功能测试：");
            int totalCount = dao.getTotalSchedulesCount();
            int completedCount = dao.getCompletedSchedulesCount();
            double completionRate = totalCount > 0 ? (double) completedCount / totalCount * 100 : 0;
            
            System.out.printf("   总日程数: %d\n", totalCount);
            System.out.printf("   已完成数: %d\n", completedCount);
            System.out.printf("   完成率: %.1f%%\n\n", completionRate);
            
            // 测试2：即将到期日程
            System.out.println("2. 即将到期日程（未来7天内）：");
            List<Schedule> upcomingSchedules = dao.getUpcomingSchedules();
            printScheduleList(upcomingSchedules);
            
            // 测试3：分页查询
            System.out.println("\n3. 分页查询测试（第1页，每页3条）：");
            List<Schedule> page1 = dao.getSchedulesWithPagination(1, 3);
            printScheduleList(page1);
            
            // 测试4：更新功能（如果存在日程）
            if (totalCount > 0) {
                System.out.println("\n4. 更新功能测试：");
                // 获取第一个日程
                Schedule firstSchedule = dao.getScheduleById(1);
                if (firstSchedule != null) {
                    System.out.println("   更新前: " + firstSchedule.getName() + " (完成: " + firstSchedule.isCompleted() + ")");
                    
                    // 更新完成状态
                    boolean updated = dao.updateScheduleStatus(1, !firstSchedule.isCompleted());
                    if (updated) {
                        Schedule updatedSchedule = dao.getScheduleById(1);
                        System.out.println("   更新后: " + updatedSchedule.getName() + " (完成: " + updatedSchedule.isCompleted() + ")");
                        System.out.println("   状态更新成功！");
                        
                        // 恢复原状态
                        dao.updateScheduleStatus(1, firstSchedule.isCompleted());
                    }
                }
            }
            
            // 测试5：添加新日程（测试完整CRUD）
            System.out.println("\n5. CRUD完整测试：");
            
            // 创建新日程
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String currentTime = LocalDateTime.now().format(formatter);
            
            Schedule newSchedule = new Schedule("测试日程", "这是一个测试用的日程", "2024-12-25", false);
            newSchedule.setCreatedAt(currentTime);
            newSchedule.setUpdatedAt(currentTime);
            
            // 添加日程
            int rowsAdded = dao.addSchedule(newSchedule);
            if (rowsAdded > 0) {
                System.out.println("   添加日程成功！");
                
                // 查询最新添加的日程（假设ID是最大的）
                List<Schedule> allSchedules = dao.getAllSchedules();
                if (!allSchedules.isEmpty()) {
                    Schedule latestSchedule = allSchedules.get(allSchedules.size() - 1);
                    System.out.println("   最新日程: " + latestSchedule.getName() + " (ID: " + latestSchedule.getId() + ")");
                    
                    // 更新日程
                    latestSchedule.setName("修改后的测试日程");
                    latestSchedule.setDescription("这是修改后的描述");
                    latestSchedule.setUpdatedAt(LocalDateTime.now().format(formatter));
                    
                    boolean updateResult = dao.updateSchedule(latestSchedule);
                    if (updateResult) {
                        System.out.println("   更新日程成功！");
                        
                        // 验证更新
                        Schedule updated = dao.getScheduleById(latestSchedule.getId());
                        System.out.println("   更新后名称: " + updated.getName());
                        
                        // 删除测试数据
                        int deletedRows = dao.deleteSchedule(latestSchedule.getId());
                        if (deletedRows > 0) {
                            System.out.println("   删除测试数据成功！");
                        }
                    }
                }
            }
            
            System.out.println("\n=== 所有功能测试完成 ===");
            
        } catch (SQLException e) {
            System.err.println("数据库操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printScheduleList(List<Schedule> schedules) {
        if (schedules.isEmpty()) {
            System.out.println("   暂无相关日程");
            return;
        }
        
        for (int i = 0; i < schedules.size(); i++) {
            Schedule schedule = schedules.get(i);
            String status = schedule.isCompleted() ? "✅ 已完成" : "⏳ 待完成";
            System.out.printf("   %d. [%s] %s - 截止: %s%n", 
                    i + 1, status, schedule.getName(), schedule.getDueDate());
            if (schedule.getDescription() != null && !schedule.getDescription().isEmpty()) {
                System.out.println("      描述: " + schedule.getDescription());
            }
        }
        System.out.println("   共找到 " + schedules.size() + " 项日程");
    }
}