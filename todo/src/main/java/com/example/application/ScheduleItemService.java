package com.example.application;

import com.example.config.UserPreferencesStore;
import com.example.data.ScheduleItemRepository;
import com.example.model.ScheduleItem;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScheduleItemService {
    private static final String DEVICE_ID_KEY = "todo.device.id";

    private final ScheduleItemRepository repository;
    private final UserPreferencesStore preferencesStore;
    private final String appVersion;
    private final String deviceId;
    private final String deviceName;

    public ScheduleItemService(
        ScheduleItemRepository repository,
        UserPreferencesStore preferencesStore,
        String appVersion
    ) {
        this.repository = repository;
        this.preferencesStore = preferencesStore;
        this.appVersion = appVersion;
        this.deviceId = resolveDeviceId(preferencesStore);
        this.deviceName = resolveDeviceName();
    }

    public void initializeRuntime() throws SQLException {
        repository.ensureDeviceRegistered(deviceId, deviceName, appVersion);
    }

    public String addScheduleItem(ScheduleItem item) throws SQLException {
        ScheduleItem prepared = item.copy();
        prepared.setDeviceId(deviceId);
        if (prepared.getCreatedAtUtc() == null) {
            prepared.setCreatedAtUtc(now());
        }
        if (prepared.getUpdatedAtUtc() == null) {
            prepared.setUpdatedAtUtc(now());
        }
        if (prepared.getId() == null || prepared.getId().isBlank()) {
            prepared.setId(UUID.randomUUID().toString());
        }
        if (prepared.getViewKey() == null || prepared.getViewKey().isBlank()) {
            prepared.setViewKey(prepared.getId());
        }
        if (prepared.getVersion() <= 0) {
            prepared.setVersion(1);
        }
        if (prepared.getStatus() == null || prepared.getStatus().isBlank()) {
            prepared.setStatus(ScheduleItem.STATUS_ACTIVE);
        }
        if (prepared.getSyncStatus() == null || prepared.getSyncStatus().isBlank()) {
            prepared.setSyncStatus(ScheduleItem.SYNC_STATUS_LOCAL_ONLY);
        }
        String createdId = repository.addScheduleItem(prepared);
        item.setId(createdId);
        item.setViewKey(prepared.getViewKey());
        item.setDeviceId(prepared.getDeviceId());
        item.setCreatedAtUtc(prepared.getCreatedAtUtc());
        item.setUpdatedAtUtc(prepared.getUpdatedAtUtc());
        item.setVersion(prepared.getVersion());
        item.setSyncStatus(prepared.getSyncStatus());
        return createdId;
    }

    public boolean updateScheduleItem(ScheduleItem item) throws SQLException {
        ScheduleItem prepared = item.copy();
        prepared.touchForWrite(deviceId);
        return repository.updateScheduleItem(prepared);
    }

    public boolean softDeleteScheduleItem(String scheduleItemId) throws SQLException {
        return repository.softDeleteScheduleItem(scheduleItemId, deviceId);
    }

    public boolean restoreScheduleItem(String scheduleItemId) throws SQLException {
        return repository.restoreScheduleItem(scheduleItemId, deviceId);
    }

    public boolean permanentlyDeleteScheduleItem(String scheduleItemId) throws SQLException {
        return repository.permanentlyDeleteScheduleItem(scheduleItemId);
    }

    public ScheduleItem getScheduleItemById(String scheduleItemId) throws SQLException {
        return repository.getScheduleItemById(scheduleItemId);
    }

    public List<ScheduleItem> getActiveScheduleItems() throws SQLException {
        return repository.getActiveScheduleItems();
    }

    public List<ScheduleItem> getDeletedScheduleItems() throws SQLException {
        return repository.getDeletedScheduleItems();
    }

    public List<ScheduleItem> searchActiveScheduleItems(String keyword) throws SQLException {
        return repository.searchActiveScheduleItems(keyword);
    }

    public List<String> suggestActiveScheduleTitles(String keyword, int limit) throws SQLException {
        return repository.suggestActiveScheduleTitles(keyword, limit);
    }

    public List<String> suggestActiveTagNames(String keyword, int limit) throws SQLException {
        return repository.suggestActiveTagNames(keyword, limit);
    }

    public List<String> suggestActiveCategories(String keyword, int limit) throws SQLException {
        return repository.suggestActiveCategories(keyword, limit);
    }

    public boolean updateScheduleItemCompletion(String scheduleItemId, boolean completed) throws SQLException {
        return repository.updateScheduleItemCompletion(scheduleItemId, completed, deviceId);
    }

    public Map<LocalDate, Integer> getDailyCompletionStats(LocalDate startDate, LocalDate endDate) throws SQLException {
        return repository.getDailyCompletionStats(startDate, endDate);
    }

    public int getPendingCount() throws SQLException {
        int pendingCount = 0;
        for (ScheduleItem item : repository.getActiveScheduleItems()) {
            if (!item.isCompleted()) {
                pendingCount++;
            }
        }
        return pendingCount;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    private static String resolveDeviceId(UserPreferencesStore preferencesStore) {
        String existing = preferencesStore.get(DEVICE_ID_KEY, "");
        if (existing != null && !existing.isBlank()) {
            return existing.trim();
        }
        String generated = UUID.randomUUID().toString();
        preferencesStore.put(DEVICE_ID_KEY, generated);
        return generated;
    }

    private static String resolveDeviceName() {
        String username = System.getProperty("user.name", "user");
        String host = "device";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
        }
        return username + "@" + host;
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
