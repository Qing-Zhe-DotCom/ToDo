package com.example.application;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.example.data.ScheduleRepository;
import com.example.model.Schedule;

public final class ScheduleService {
    private final ScheduleRepository repository;

    public ScheduleService(ScheduleRepository repository) {
        this.repository = repository;
    }

    public int addSchedule(Schedule schedule) throws SQLException {
        return repository.addSchedule(schedule);
    }

    public boolean updateSchedule(Schedule schedule) throws SQLException {
        return repository.updateSchedule(schedule);
    }

    public boolean deleteSchedule(int id) throws SQLException {
        return repository.deleteSchedule(id);
    }

    public Schedule getScheduleById(int id) throws SQLException {
        return repository.getScheduleById(id);
    }

    public List<Schedule> getAllSchedules() throws SQLException {
        return repository.getAllSchedules();
    }

    public List<Schedule> searchSchedules(String keyword) throws SQLException {
        return repository.searchSchedules(keyword);
    }

    public boolean updateScheduleStatus(int id, boolean completed) throws SQLException {
        return repository.updateScheduleStatus(id, completed);
    }

    public Map<LocalDate, Integer> getDailyCompletionStats(LocalDate startDate, LocalDate endDate) throws SQLException {
        return repository.getDailyCompletionStats(startDate, endDate);
    }

    public int getPendingCount() throws SQLException {
        int pendingCount = 0;
        for (Schedule schedule : repository.getAllSchedules()) {
            if (!schedule.isCompleted()) {
                pendingCount++;
            }
        }
        return pendingCount;
    }
}
