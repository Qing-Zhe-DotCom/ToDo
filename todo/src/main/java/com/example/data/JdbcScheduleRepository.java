package com.example.data;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;

public final class JdbcScheduleRepository implements ScheduleRepository {
    private final ScheduleDAO delegate;

    public JdbcScheduleRepository(ScheduleDAO delegate) {
        this.delegate = delegate;
    }

    @Override
    public int addSchedule(Schedule schedule) throws SQLException {
        return delegate.addSchedule(schedule);
    }

    @Override
    public boolean updateSchedule(Schedule schedule) throws SQLException {
        return delegate.updateSchedule(schedule);
    }

    @Override
    public boolean deleteSchedule(int id) throws SQLException {
        return delegate.deleteSchedule(id);
    }

    @Override
    public Schedule getScheduleById(int id) throws SQLException {
        return delegate.getScheduleById(id);
    }

    @Override
    public List<Schedule> getAllSchedules() throws SQLException {
        return delegate.getAllSchedules();
    }

    @Override
    public List<Schedule> searchSchedules(String keyword) throws SQLException {
        return delegate.searchSchedules(keyword);
    }

    @Override
    public boolean updateScheduleStatus(int id, boolean completed) throws SQLException {
        return delegate.updateScheduleStatus(id, completed);
    }

    @Override
    public Map<LocalDate, Integer> getDailyCompletionStats(LocalDate startDate, LocalDate endDate) throws SQLException {
        return delegate.getDailyCompletionStats(startDate, endDate);
    }
}
