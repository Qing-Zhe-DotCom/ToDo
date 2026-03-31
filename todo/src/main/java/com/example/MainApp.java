package com.example;

import java.sql.SQLException;

import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;

public class MainApp {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        // Schedule schedule = new Schedule("Task 1", "Description 1", "2023-12-31", false);
        // ScheduleDAO scheduleDAO = new ScheduleDAO();
        // try {
        //     int rowsAffected = scheduleDAO.addSchedule(schedule);
        //     System.out.println("Rows affected: " + rowsAffected);
        // } catch (SQLException e) {
        //     System.err.println("Error adding schedule: " + e.getMessage());
        // }
        //删除日程
        // ScheduleDAO scheduleDAO = new ScheduleDAO();
        // try {
        //     int rowsAffected = scheduleDAO.deleteSchedule(1);
        //     System.out.println("Rows affected: " + rowsAffected);
        // } catch (SQLException e) {
        //     System.err.println("Error deleting schedule: " + e.getMessage());
        // }
       //写一个查询所有日程的方法
      //写一个查询所有日程的方法
    //     ScheduleDAO scheduleDAO = new ScheduleDAO();
    //     try {
    //         List<Schedule> schedules = scheduleDAO.getAllSchedules();
            
    //         System.out.println("\n=== 日程列表 (共" + schedules.size() + "项) ===");
    //         if (schedules.isEmpty()) {
    //             System.out.println("暂无日程安排");
    //         } else {
    //             for (int i = 0; i < schedules.size(); i++) {
    //                 Schedule schedule = schedules.get(i);
    //                 String status = schedule.isCompleted() ? "✅ 已完成" : "⏳ 待完成";
    //                 System.out.printf("%d. [%s] %s - 截止: %s%n", 
    //                         i + 1, status, schedule.getName(), schedule.getDueDate());
    //                 if (schedule.getDescription() != null && !schedule.getDescription().isEmpty()) {
    //                     System.out.println("   描述: " + schedule.getDescription());
    //                 }
    //                 System.out.println();
    //             }
    //         }
    //     } catch (SQLException e) {
    //         System.err.println("查询日程失败: " + e.getMessage());
    //     }
    // }    
    // 测试查询特定ID的日程
        System.out.println("\n=== 测试查询特定ID日程 ===");
        try {
            ScheduleDAO scheduleDAO = new ScheduleDAO();
            Schedule specificSchedule = scheduleDAO.getScheduleById(2); // 查询ID为1的日程
            
            if (specificSchedule != null) {
                System.out.println("找到日程:");
                System.out.println("ID: " + specificSchedule.getId());
                System.out.println("名称: " + specificSchedule.getName());
                System.out.println("描述: " + specificSchedule.getDescription());
                System.out.println("截止日期: " + specificSchedule.getDueDate());
                System.out.println("完成状态: " + (specificSchedule.isCompleted() ? "已完成" : "未完成"));
                System.out.println("创建时间: " + specificSchedule.getCreatedAt());
                System.out.println("更新时间: " + specificSchedule.getUpdatedAt());
            } else {
                System.out.println("未找到ID为1的日程");
            }
        } catch (SQLException e) {
            System.err.println("查询特定日程失败: " + e.getMessage());
        }
    }



}
