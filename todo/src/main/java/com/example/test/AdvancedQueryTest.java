package com.example.test;

import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;
import java.sql.SQLException;
import java.util.List;

public class AdvancedQueryTest {
    public static void main(String[] args) {
        ScheduleDAO dao = new ScheduleDAO();
        
        System.out.println("=== 高级查询功能测试 ===\n");
        
        try {
            // 测试1：按状态查询（未完成的日程）
            System.out.println("1. 查询未完成的日程：");
            List<Schedule> pendingSchedules = dao.getSchedulesByStatus(false);
            printScheduleList(pendingSchedules);
            
            // 测试2：按状态查询（已完成的日程）
            System.out.println("\n2. 查询已完成的日程：");
            List<Schedule> completedSchedules = dao.getSchedulesByStatus(true);
            printScheduleList(completedSchedules);
            
            // 测试3：按日期范围查询
            System.out.println("\n3. 查询2024-12-01到2024-12-31的日程：");
            List<Schedule> dateRangeSchedules = dao.getSchedulesByDateRange("2024-12-01", "2024-12-31");
            printScheduleList(dateRangeSchedules);
            
            // 测试4：模糊搜索（按关键词）
            System.out.println("\n4. 搜索包含'学习'关键词的日程：");
            List<Schedule> searchResults = dao.searchSchedules("学习");
            printScheduleList(searchResults);
            
            // 测试5：模糊搜索（按另一个关键词）
            System.out.println("\n5. 搜索包含'会议'关键词的日程：");
            List<Schedule> meetingResults = dao.searchSchedules("会议");
            printScheduleList(meetingResults);
            
            System.out.println("\n=== 所有高级查询测试完成 ===");
            
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