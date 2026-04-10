package com.example.data;

import com.example.model.ScheduleItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ScheduleItemRepository {
    String addScheduleItem(ScheduleItem item) throws SQLException;

    boolean updateScheduleItem(ScheduleItem item) throws SQLException;

    boolean softDeleteScheduleItem(String scheduleItemId, String deviceId) throws SQLException;

    boolean restoreScheduleItem(String scheduleItemId, String deviceId) throws SQLException;

    boolean permanentlyDeleteScheduleItem(String scheduleItemId) throws SQLException;

    ScheduleItem getScheduleItemById(String scheduleItemId) throws SQLException;

    List<ScheduleItem> getActiveScheduleItems() throws SQLException;

    List<ScheduleItem> getDeletedScheduleItems() throws SQLException;

    List<ScheduleItem> searchActiveScheduleItems(String keyword) throws SQLException;

    List<String> suggestActiveScheduleTitles(String keyword, int limit) throws SQLException;

    List<String> suggestActiveTagNames(String keyword, int limit) throws SQLException;

    List<String> suggestActiveCategories(String keyword, int limit) throws SQLException;

    boolean updateScheduleItemCompletion(String scheduleItemId, boolean completed, String deviceId) throws SQLException;

    Map<LocalDate, Integer> getDailyCompletionStats(LocalDate startDate, LocalDate endDate) throws SQLException;

    void ensureDeviceRegistered(String deviceId, String deviceName, String appVersion) throws SQLException;
}
