package com.example.data;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.example.model.Schedule;

public interface ScheduleRepository {
    int addSchedule(Schedule schedule) throws SQLException;

    boolean updateSchedule(Schedule schedule) throws SQLException;

    boolean deleteSchedule(int id) throws SQLException;

    Schedule getScheduleById(int id) throws SQLException;

    List<Schedule> getAllSchedules() throws SQLException;

    List<Schedule> searchSchedules(String keyword) throws SQLException;

    boolean updateScheduleStatus(int id, boolean completed) throws SQLException;

    Map<LocalDate, Integer> getDailyCompletionStats(LocalDate startDate, LocalDate endDate) throws SQLException;
}
