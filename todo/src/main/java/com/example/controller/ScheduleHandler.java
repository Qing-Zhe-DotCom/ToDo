package com.example.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.example.application.ScheduleItemService;
import com.example.model.ScheduleItem;

public final class ScheduleHandler {

    private final ScheduleItemService scheduleItemService;
    private final ScheduleCompletionCoordinator completionCoordinator;
    private final Runnable onDataChanged;
    private final Runnable onReminderResync;

    public ScheduleHandler(
        ScheduleItemService scheduleItemService,
        ScheduleCompletionCoordinator completionCoordinator,
        Runnable onDataChanged,
        Runnable onReminderResync
    ) {
        this.scheduleItemService = scheduleItemService;
        this.completionCoordinator = completionCoordinator;
        this.onDataChanged = onDataChanged;
        this.onReminderResync = onReminderResync;
    }

    public ScheduleCompletionCoordinator.PendingCompletion prepareCompletion(
        ScheduleItem item,
        boolean targetCompleted
    ) {
        return completionCoordinator.prepare(item, targetCompleted);
    }

    public boolean updateCompletion(ScheduleItem item, boolean targetCompleted) {
        if (item == null) {
            return false;
        }
        return completionCoordinator.submitImmediate(item, targetCompleted);
    }

    public String createSchedule(ScheduleItem item) throws SQLException {
        String createdId = scheduleItemService.addScheduleItem(item);
        onReminderResync.run();
        return createdId;
    }

    public ScheduleItem quickCreateSchedule(String rawTitle) throws SQLException {
        String title = rawTitle == null ? "" : rawTitle.strip();
        if (title.isEmpty()) {
            return null;
        }

        ScheduleItem item = new ScheduleItem();
        item.setTitle(title);
        item.setDescription("");
        item.setStartAt(item.getCreatedAt().truncatedTo(ChronoUnit.MINUTES));
        item.setDueAt(LocalDate.now().atTime(23, 59));
        item.setCompleted(false);
        item.setPriority(ScheduleItem.DEFAULT_PRIORITY);
        item.setCategory(ScheduleItem.DEFAULT_CATEGORY);
        item.setTags("");
        item.setReminderTime(null);
        createSchedule(item);
        onDataChanged.run();
        return item;
    }

    public boolean saveSchedule(ScheduleItem item) throws SQLException {
        boolean updated = scheduleItemService.updateScheduleItem(item);
        if (updated) {
            onReminderResync.run();
        }
        return updated;
    }

    public boolean removeSchedule(String scheduleId) throws SQLException {
        boolean removed = scheduleItemService.softDeleteScheduleItem(scheduleId);
        if (removed) {
            onReminderResync.run();
        }
        return removed;
    }

    public ScheduleItem findScheduleById(String scheduleId) throws SQLException {
        return scheduleItemService.getScheduleItemById(scheduleId);
    }

    public List<ScheduleItem> loadAllSchedules() throws SQLException {
        return scheduleItemService.getActiveScheduleItems();
    }

    public List<ScheduleItem> searchSchedules(String keyword) throws SQLException {
        return scheduleItemService.searchActiveScheduleItems(keyword);
    }

    public List<ScheduleItem> loadDeletedSchedules() throws SQLException {
        return scheduleItemService.getDeletedScheduleItems();
    }

    public boolean restoreDeletedSchedule(String scheduleId) throws SQLException {
        boolean restored = scheduleItemService.restoreScheduleItem(scheduleId);
        if (restored) {
            onReminderResync.run();
        }
        return restored;
    }

    public boolean permanentlyDeleteSchedule(String scheduleId) throws SQLException {
        boolean deleted = scheduleItemService.permanentlyDeleteScheduleItem(scheduleId);
        if (deleted) {
            onReminderResync.run();
        }
        return deleted;
    }
}
