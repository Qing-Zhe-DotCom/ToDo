package com.example.data;

import com.example.model.RecurrenceRule;
import com.example.model.Reminder;
import com.example.model.ScheduleItem;
import com.example.model.Tag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SqlScheduleItemRepository implements ScheduleItemRepository {
    private static final String ENTITY_TYPE_SCHEDULE_ITEM = "schedule_item";
    private static final String OPERATION_UPSERT = "upsert";
    private static final String OPERATION_DELETE = "delete";

    private final ConnectionFactory connectionFactory;
    private final SchemaManager schemaManager;
    private final SqlDialect dialect;

    public SqlScheduleItemRepository(
        ConnectionFactory connectionFactory,
        SchemaManager schemaManager,
        SqlDialect dialect
    ) {
        this.connectionFactory = connectionFactory;
        this.schemaManager = schemaManager;
        this.dialect = dialect;
    }

    @Override
    public String addScheduleItem(ScheduleItem item) throws SQLException {
        Objects.requireNonNull(item, "item");
        ScheduleItem prepared = prepareForInsert(item);

        try (Connection connection = openConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                insertScheduleItem(connection, prepared);
                replaceTags(connection, prepared);
                replaceReminders(connection, prepared);
                replaceRecurrenceRule(connection, prepared);
                insertOutbox(connection, prepared, OPERATION_UPSERT, buildSchedulePayloadJson(prepared));
                connection.commit();
                return prepared.getId();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    @Override
    public boolean updateScheduleItem(ScheduleItem item) throws SQLException {
        Objects.requireNonNull(item, "item");
        ScheduleItem prepared = prepareForUpdate(item);

        try (Connection connection = openConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int updated = updateScheduleItemRow(connection, prepared);
                if (updated <= 0) {
                    connection.rollback();
                    return false;
                }
                replaceTags(connection, prepared);
                replaceReminders(connection, prepared);
                replaceRecurrenceRule(connection, prepared);
                insertOutbox(connection, prepared, OPERATION_UPSERT, buildSchedulePayloadJson(prepared));
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    @Override
    public boolean softDeleteScheduleItem(String scheduleItemId, String deviceId) throws SQLException {
        if (scheduleItemId == null || scheduleItemId.isBlank()) {
            return false;
        }

        try (Connection connection = openConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ScheduleItem current = loadScheduleItem(connection, scheduleItemId);
                if (current == null || current.isDeleted()) {
                    connection.rollback();
                    return false;
                }

                current.markDeleted(deviceId);
                current.setUpdatedAtUtc(nowUtc());

                try (PreparedStatement statement = connection.prepareStatement(
                    """
                    UPDATE schedule_item
                    SET deleted_at_utc = ?, updated_at_utc = ?, version = ?, sync_status = ?, device_id = ?
                    WHERE id = ? AND deleted_at_utc IS NULL
                    """
                )) {
                    statement.setString(1, toDateTimeString(current.getDeletedAtUtc()));
                    statement.setString(2, toDateTimeString(current.getUpdatedAtUtc()));
                    statement.setInt(3, current.getVersion());
                    statement.setString(4, current.getSyncStatus());
                    statement.setString(5, current.getDeviceId());
                    statement.setString(6, current.getId());
                    if (statement.executeUpdate() <= 0) {
                        connection.rollback();
                        return false;
                    }
                }

                insertOutbox(connection, current, OPERATION_DELETE, buildDeletePayloadJson(current));
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    @Override
    public boolean restoreScheduleItem(String scheduleItemId, String deviceId) throws SQLException {
        if (scheduleItemId == null || scheduleItemId.isBlank()) {
            return false;
        }

        try (Connection connection = openConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ScheduleItem current = loadScheduleItem(connection, scheduleItemId);
                if (current == null || !current.isDeleted()) {
                    connection.rollback();
                    return false;
                }

                current.restoreFromTrash(deviceId);
                current.setUpdatedAtUtc(nowUtc());

                try (PreparedStatement statement = connection.prepareStatement(
                    """
                    UPDATE schedule_item
                    SET deleted_at_utc = NULL, updated_at_utc = ?, version = ?, sync_status = ?, device_id = ?
                    WHERE id = ? AND deleted_at_utc IS NOT NULL
                    """
                )) {
                    statement.setString(1, toDateTimeString(current.getUpdatedAtUtc()));
                    statement.setInt(2, current.getVersion());
                    statement.setString(3, current.getSyncStatus());
                    statement.setString(4, current.getDeviceId());
                    statement.setString(5, current.getId());
                    if (statement.executeUpdate() <= 0) {
                        connection.rollback();
                        return false;
                    }
                }

                insertOutbox(connection, current, OPERATION_UPSERT, buildSchedulePayloadJson(current));
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    @Override
    public boolean permanentlyDeleteScheduleItem(String scheduleItemId) throws SQLException {
        if (scheduleItemId == null || scheduleItemId.isBlank()) {
            return false;
        }

        try (Connection connection = openConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ScheduleItem current = loadScheduleItem(connection, scheduleItemId);
                if (current == null || current.getLastSyncedAtUtc() != null) {
                    connection.rollback();
                    return false;
                }

                try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM schedule_item WHERE id = ?"
                )) {
                    statement.setString(1, scheduleItemId);
                    if (statement.executeUpdate() <= 0) {
                        connection.rollback();
                        return false;
                    }
                }

                cleanupOrphanTags(connection);
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    @Override
    public ScheduleItem getScheduleItemById(String scheduleItemId) throws SQLException {
        if (scheduleItemId == null || scheduleItemId.isBlank()) {
            return null;
        }
        try (Connection connection = openConnection()) {
            return loadScheduleItem(connection, scheduleItemId);
        }
    }

    @Override
    public List<ScheduleItem> getActiveScheduleItems() throws SQLException {
        try (Connection connection = openConnection()) {
            return queryScheduleItems(
                connection,
                "SELECT * FROM schedule_item WHERE deleted_at_utc IS NULL" + orderBy("schedule_item"),
                statement -> {
                }
            );
        }
    }

    @Override
    public List<ScheduleItem> getDeletedScheduleItems() throws SQLException {
        try (Connection connection = openConnection()) {
            return queryScheduleItems(
                connection,
                "SELECT * FROM schedule_item WHERE deleted_at_utc IS NOT NULL" + orderBy("schedule_item"),
                statement -> {
                }
            );
        }
    }

    @Override
    public List<ScheduleItem> searchActiveScheduleItems(String keyword) throws SQLException {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String searchPattern = "%" + normalizedKeyword + "%";

        try (Connection connection = openConnection()) {
            return queryScheduleItems(
                connection,
                """
                SELECT DISTINCT si.*
                FROM schedule_item si
                LEFT JOIN schedule_item_tag sit ON sit.schedule_item_id = si.id
                LEFT JOIN tag t ON t.id = sit.tag_id
                WHERE si.deleted_at_utc IS NULL
                  AND (
                      LOWER(COALESCE(si.title, '')) LIKE ?
                      OR LOWER(COALESCE(si.description, '')) LIKE ?
                      OR LOWER(COALESCE(si.notes, '')) LIKE ?
                      OR LOWER(COALESCE(si.category, '')) LIKE ?
                      OR LOWER(COALESCE(t.name, '')) LIKE ?
                  )
                """ + orderBy("si"),
                statement -> {
                    statement.setString(1, searchPattern);
                    statement.setString(2, searchPattern);
                    statement.setString(3, searchPattern);
                    statement.setString(4, searchPattern);
                    statement.setString(5, searchPattern);
                }
            );
        }
    }

    @Override
    public List<String> suggestActiveScheduleTitles(String keyword, int limit) throws SQLException {
        if (limit <= 0) {
            return List.of();
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }
        String searchPattern = "%" + normalizedKeyword + "%";

        List<String> suggestions = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 """
                 SELECT DISTINCT si.title AS value
                 FROM schedule_item si
                 WHERE si.deleted_at_utc IS NULL
                   AND TRIM(COALESCE(si.title, '')) <> ''
                   AND LOWER(COALESCE(si.title, '')) LIKE ?
                 ORDER BY LOWER(si.title), si.title
                 LIMIT ?
                 """
             )) {
            statement.setString(1, searchPattern);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String value = resultSet.getString("value");
                    if (value != null && !value.isBlank()) {
                        suggestions.add(value);
                    }
                }
            }
        }
        return suggestions;
    }

    @Override
    public List<String> suggestActiveTagNames(String keyword, int limit) throws SQLException {
        if (limit <= 0) {
            return List.of();
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }
        String searchPattern = "%" + normalizedKeyword + "%";

        List<String> suggestions = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 """
                 SELECT DISTINCT t.name AS value
                 FROM tag t
                 JOIN schedule_item_tag sit ON sit.tag_id = t.id
                 JOIN schedule_item si ON si.id = sit.schedule_item_id
                 WHERE si.deleted_at_utc IS NULL
                   AND TRIM(COALESCE(t.name, '')) <> ''
                   AND LOWER(COALESCE(t.name, '')) LIKE ?
                 ORDER BY LOWER(t.name), t.name
                 LIMIT ?
                 """
             )) {
            statement.setString(1, searchPattern);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String value = resultSet.getString("value");
                    if (value != null && !value.isBlank()) {
                        suggestions.add(value);
                    }
                }
            }
        }
        return suggestions;
    }

    @Override
    public List<String> suggestActiveCategories(String keyword, int limit) throws SQLException {
        if (limit <= 0) {
            return List.of();
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }
        String searchPattern = "%" + normalizedKeyword + "%";

        List<String> suggestions = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 """
                 SELECT DISTINCT si.category AS value
                 FROM schedule_item si
                 WHERE si.deleted_at_utc IS NULL
                   AND TRIM(COALESCE(si.category, '')) <> ''
                   AND LOWER(COALESCE(si.category, '')) LIKE ?
                 ORDER BY LOWER(si.category), si.category
                 LIMIT ?
                 """
             )) {
            statement.setString(1, searchPattern);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String value = resultSet.getString("value");
                    if (value != null && !value.isBlank()) {
                        suggestions.add(value);
                    }
                }
            }
        }
        return suggestions;
    }

    @Override
    public boolean updateScheduleItemCompletion(String scheduleItemId, boolean completed, String deviceId) throws SQLException {
        if (scheduleItemId == null || scheduleItemId.isBlank()) {
            return false;
        }

        try (Connection connection = openConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ScheduleItem current = loadScheduleItem(connection, scheduleItemId);
                if (current == null || current.isDeleted()) {
                    connection.rollback();
                    return false;
                }

                current.setCompleted(completed);
                current.touchForWrite(deviceId);
                current.setUpdatedAtUtc(nowUtc());

                try (PreparedStatement statement = connection.prepareStatement(
                    """
                    UPDATE schedule_item
                    SET status = ?, completed_at_utc = ?, updated_at_utc = ?, version = ?, sync_status = ?, device_id = ?
                    WHERE id = ? AND deleted_at_utc IS NULL
                    """
                )) {
                    statement.setString(1, current.getStatus());
                    statement.setString(2, toDateTimeString(current.getCompletedAtUtc()));
                    statement.setString(3, toDateTimeString(current.getUpdatedAtUtc()));
                    statement.setInt(4, current.getVersion());
                    statement.setString(5, current.getSyncStatus());
                    statement.setString(6, current.getDeviceId());
                    statement.setString(7, current.getId());
                    if (statement.executeUpdate() <= 0) {
                        connection.rollback();
                        return false;
                    }
                }

                insertOutbox(connection, current, OPERATION_UPSERT, buildSchedulePayloadJson(current));
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    @Override
    public Map<LocalDate, Integer> getDailyCompletionStats(LocalDate startDate, LocalDate endDate) throws SQLException {
        Map<LocalDate, Integer> stats = new LinkedHashMap<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 """
                 SELECT SUBSTR(completed_at_utc, 1, 10) AS completion_date, COUNT(*) AS count
                 FROM schedule_item
                 WHERE deleted_at_utc IS NULL
                   AND status = ?
                   AND completed_at_utc IS NOT NULL
                   AND SUBSTR(completed_at_utc, 1, 10) BETWEEN ? AND ?
                 GROUP BY SUBSTR(completed_at_utc, 1, 10)
                 ORDER BY completion_date
                 """
             )) {
            statement.setString(1, ScheduleItem.STATUS_COMPLETED);
            statement.setString(2, startDate.toString());
            statement.setString(3, endDate.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String completionDate = resultSet.getString("completion_date");
                    if (completionDate != null && !completionDate.isBlank()) {
                        stats.put(LocalDate.parse(completionDate), resultSet.getInt("count"));
                    }
                }
            }
        }
        return stats;
    }

    @Override
    public void ensureDeviceRegistered(String deviceId, String deviceName, String appVersion) throws SQLException {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }

        try (Connection connection = openConnection()) {
            LocalDateTime now = nowUtc();
            boolean exists;
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM device_registry WHERE device_id = ?"
            )) {
                statement.setString(1, deviceId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    exists = resultSet.next();
                }
            }

            if (exists) {
                try (PreparedStatement statement = connection.prepareStatement(
                    """
                    UPDATE device_registry
                    SET device_name = ?, platform = ?, app_version = ?, last_seen_at_utc = ?
                    WHERE device_id = ?
                    """
                )) {
                    statement.setString(1, coalesceText(deviceName, deviceId));
                    statement.setString(2, detectPlatform());
                    statement.setString(3, appVersion);
                    statement.setString(4, toDateTimeString(now));
                    statement.setString(5, deviceId);
                    statement.executeUpdate();
                }
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO device_registry(
                    device_id, device_name, platform, app_version, created_at_utc, last_seen_at_utc
                ) VALUES (?, ?, ?, ?, ?, ?)
                """
            )) {
                statement.setString(1, deviceId);
                statement.setString(2, coalesceText(deviceName, deviceId));
                statement.setString(3, detectPlatform());
                statement.setString(4, appVersion);
                statement.setString(5, toDateTimeString(now));
                statement.setString(6, toDateTimeString(now));
                statement.executeUpdate();
            }
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = connectionFactory.getConnection();
        try {
            schemaManager.ensureSchema(connection);
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            throw exception;
        }
    }

    private ScheduleItem prepareForInsert(ScheduleItem item) {
        ScheduleItem prepared = item.copy();
        LocalDateTime now = nowUtc();
        if (prepared.getId() == null || prepared.getId().isBlank()) {
            prepared.setId(UUID.randomUUID().toString());
        }
        prepared.setViewKey(prepared.getId());
        if (prepared.getTimezone() == null || prepared.getTimezone().isBlank()) {
            prepared.setTimezone(ZoneId.systemDefault().getId());
        }
        prepared.setCreatedAtUtc(prepared.getCreatedAtUtc() != null ? prepared.getCreatedAtUtc() : now);
        prepared.setUpdatedAtUtc(now);
        prepared.setVersion(Math.max(1, prepared.getVersion()));
        prepared.setSyncStatus(normalizeSyncStatus(prepared.getSyncStatus(), false));
        prepared.setStatus(normalizeStatus(prepared));
        prepared.setPriority(ScheduleItem.normalizePriority(prepared.getPriority()));
        prepared.setCategory(ScheduleItem.normalizeCategory(prepared.getCategory()));
        prepared.setTagNames(prepared.getTagNames());
        prepared.setReminders(prepared.getReminders());
        prepared.setRecurrenceRule(prepared.getRecurrenceRule());
        return prepared;
    }

    private ScheduleItem prepareForUpdate(ScheduleItem item) {
        ScheduleItem prepared = item.copy();
        prepared.setUpdatedAtUtc(prepared.getUpdatedAtUtc() != null ? prepared.getUpdatedAtUtc() : nowUtc());
        prepared.setStatus(normalizeStatus(prepared));
        prepared.setPriority(ScheduleItem.normalizePriority(prepared.getPriority()));
        prepared.setCategory(ScheduleItem.normalizeCategory(prepared.getCategory()));
        prepared.setSyncStatus(normalizeSyncStatus(prepared.getSyncStatus(), prepared.getLastSyncedAtUtc() != null));
        prepared.setTagNames(prepared.getTagNames());
        prepared.setReminders(prepared.getReminders());
        prepared.setRecurrenceRule(prepared.getRecurrenceRule());
        prepared.setViewKey(prepared.getId());
        return prepared;
    }

    private void insertScheduleItem(Connection connection, ScheduleItem item) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            """
            INSERT INTO schedule_item(
                id, title, description, notes, status, priority, category,
                start_at_utc, end_at_utc, due_at_utc, completed_at_utc,
                is_all_day, time_precision, timezone, color,
                created_at_utc, updated_at_utc, deleted_at_utc,
                version, sync_status, last_synced_at_utc, device_id, metadata_json, is_suspended
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
        )) {
            bindScheduleItem(statement, item);
            statement.executeUpdate();
        }
    }

    private int updateScheduleItemRow(Connection connection, ScheduleItem item) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            """
            UPDATE schedule_item
            SET title = ?, description = ?, notes = ?, status = ?, priority = ?, category = ?,
                start_at_utc = ?, end_at_utc = ?, due_at_utc = ?, completed_at_utc = ?,
                is_all_day = ?, time_precision = ?, timezone = ?, color = ?,
                created_at_utc = ?, updated_at_utc = ?, deleted_at_utc = ?,
                version = ?, sync_status = ?, last_synced_at_utc = ?, device_id = ?, metadata_json = ?,
                is_suspended = ?
            WHERE id = ?
            """
        )) {
            bindScheduleItemWithoutId(statement, item, 1);
            statement.setString(24, item.getId());
            return statement.executeUpdate();
        }
    }

    private void bindScheduleItem(PreparedStatement statement, ScheduleItem item) throws SQLException {
        statement.setString(1, item.getId());
        bindScheduleItemWithoutId(statement, item, 2);
    }

    private void bindScheduleItemWithoutId(PreparedStatement statement, ScheduleItem item, int startIndex) throws SQLException {
        int index = startIndex;
        statement.setString(index++, item.getTitle());
        statement.setString(index++, emptyToNull(item.getDescription()));
        statement.setString(index++, emptyToNull(item.getNotes()));
        statement.setString(index++, item.getStatus());
        statement.setString(index++, item.getPriority());
        statement.setString(index++, item.getCategory());
        statement.setString(index++, toDateTimeString(item.getStartAtUtc()));
        statement.setString(index++, toDateTimeString(item.getEndAtUtc()));
        statement.setString(index++, toDateTimeString(item.getDueAtUtc()));
        statement.setString(index++, toDateTimeString(item.getCompletedAtUtc()));
        statement.setInt(index++, item.isAllDay() ? 1 : 0);
        statement.setString(index++, item.getTimePrecision());
        statement.setString(index++, item.getTimezone());
        statement.setString(index++, emptyToNull(item.getColor()));
        statement.setString(index++, toDateTimeString(item.getCreatedAtUtc()));
        statement.setString(index++, toDateTimeString(item.getUpdatedAtUtc()));
        statement.setString(index++, toDateTimeString(item.getDeletedAtUtc()));
        statement.setInt(index++, item.getVersion());
        statement.setString(index++, item.getSyncStatus());
        statement.setString(index++, toDateTimeString(item.getLastSyncedAtUtc()));
        statement.setString(index++, emptyToNull(item.getDeviceId()));
        statement.setString(index++, item.getMetadataJson());
        statement.setInt(index, item.isSuspended() ? 1 : 0);
    }

    private void replaceTags(Connection connection, ScheduleItem item) throws SQLException {
        try (PreparedStatement deleteJoins = connection.prepareStatement(
            "DELETE FROM schedule_item_tag WHERE schedule_item_id = ?"
        )) {
            deleteJoins.setString(1, item.getId());
            deleteJoins.executeUpdate();
        }

        LinkedHashSet<String> uniqueNames = new LinkedHashSet<>(item.getTagNames());
        for (String tagName : uniqueNames) {
            String normalizedName = tagName == null ? "" : tagName.strip();
            if (normalizedName.isEmpty()) {
                continue;
            }
            Tag existing = findTagByName(connection, normalizedName);
            Tag tag = existing != null ? existing : new Tag(normalizedName);
            if (existing == null) {
                insertTag(connection, tag);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO schedule_item_tag(schedule_item_id, tag_id) VALUES (?, ?)"
            )) {
                statement.setString(1, item.getId());
                statement.setString(2, tag.getId());
                statement.executeUpdate();
            }
        }

        cleanupOrphanTags(connection);
    }

    private void insertTag(Connection connection, Tag tag) throws SQLException {
        LocalDateTime now = nowUtc();
        if (tag.getCreatedAtUtc() == null) {
            tag.setCreatedAtUtc(now);
        }
        tag.setUpdatedAtUtc(now);
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO tag(id, name, color, created_at_utc, updated_at_utc) VALUES (?, ?, ?, ?, ?)"
        )) {
            statement.setString(1, tag.getId());
            statement.setString(2, tag.getName());
            statement.setString(3, emptyToNull(tag.getColor()));
            statement.setString(4, toDateTimeString(tag.getCreatedAtUtc()));
            statement.setString(5, toDateTimeString(tag.getUpdatedAtUtc()));
            statement.executeUpdate();
        }
    }

    private Tag findTagByName(Connection connection, String tagName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT * FROM tag WHERE LOWER(name) = LOWER(?)"
        )) {
            statement.setString(1, tagName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                Tag tag = new Tag();
                tag.setId(resultSet.getString("id"));
                tag.setName(resultSet.getString("name"));
                tag.setColor(resultSet.getString("color"));
                tag.setCreatedAtUtc(parseDateTime(resultSet.getString("created_at_utc")));
                tag.setUpdatedAtUtc(parseDateTime(resultSet.getString("updated_at_utc")));
                return tag;
            }
        }
    }

    private void cleanupOrphanTags(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            """
            DELETE FROM tag
            WHERE id NOT IN (
                SELECT DISTINCT tag_id FROM schedule_item_tag
            )
            """
        )) {
            statement.executeUpdate();
        }
    }

    private void replaceReminders(Connection connection, ScheduleItem item) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
            "DELETE FROM reminder WHERE schedule_item_id = ?"
        )) {
            delete.setString(1, item.getId());
            delete.executeUpdate();
        }

        for (Reminder reminder : item.getReminders()) {
            Reminder copy = reminder.copy();
            if (copy.getId() == null || copy.getId().isBlank()) {
                copy.setId(UUID.randomUUID().toString());
            }
            copy.setScheduleItemId(item.getId());
            LocalDateTime now = nowUtc();
            copy.setCreatedAtUtc(copy.getCreatedAtUtc() != null ? copy.getCreatedAtUtc() : now);
            copy.setUpdatedAtUtc(now);
            try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO reminder(
                    id, schedule_item_id, remind_at_utc, offset_minutes, channel, status, created_at_utc, updated_at_utc
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """
            )) {
                statement.setString(1, copy.getId());
                statement.setString(2, copy.getScheduleItemId());
                statement.setString(3, toDateTimeString(copy.getRemindAtUtc()));
                if (copy.getOffsetMinutes() == null) {
                    statement.setObject(4, null);
                } else {
                    statement.setInt(4, copy.getOffsetMinutes());
                }
                statement.setString(5, copy.getChannel());
                statement.setString(6, copy.getStatus());
                statement.setString(7, toDateTimeString(copy.getCreatedAtUtc()));
                statement.setString(8, toDateTimeString(copy.getUpdatedAtUtc()));
                statement.executeUpdate();
            }
        }
    }

    private void replaceRecurrenceRule(Connection connection, ScheduleItem item) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
            "DELETE FROM recurrence_rule WHERE schedule_item_id = ?"
        )) {
            delete.setString(1, item.getId());
            delete.executeUpdate();
        }

        RecurrenceRule rule = item.getRecurrenceRule();
        if (rule == null) {
            return;
        }

        RecurrenceRule copy = rule.copy();
        if (copy.getId() == null || copy.getId().isBlank()) {
            copy.setId(UUID.randomUUID().toString());
        }
        copy.setScheduleItemId(item.getId());
        if (copy.getTimezone() == null || copy.getTimezone().isBlank()) {
            copy.setTimezone(item.getTimezone());
        }
        LocalDateTime now = nowUtc();
        copy.setCreatedAtUtc(copy.getCreatedAtUtc() != null ? copy.getCreatedAtUtc() : now);
        copy.setUpdatedAtUtc(now);

        try (PreparedStatement statement = connection.prepareStatement(
            """
            INSERT INTO recurrence_rule(
                id, schedule_item_id, freq, interval, by_day_csv, by_month_day,
                until_at_utc, occurrence_count, timezone, is_active, created_at_utc, updated_at_utc
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
        )) {
            statement.setString(1, copy.getId());
            statement.setString(2, copy.getScheduleItemId());
            statement.setString(3, copy.getFrequency());
            statement.setInt(4, copy.getInterval());
            statement.setString(5, emptyToNull(copy.getByDayCsv()));
            if (copy.getByMonthDay() == null) {
                statement.setObject(6, null);
            } else {
                statement.setInt(6, copy.getByMonthDay());
            }
            statement.setString(7, toDateTimeString(copy.getUntilAtUtc()));
            if (copy.getOccurrenceCount() == null) {
                statement.setObject(8, null);
            } else {
                statement.setInt(8, copy.getOccurrenceCount());
            }
            statement.setString(9, emptyToNull(copy.getTimezone()));
            statement.setInt(10, copy.isActive() ? 1 : 0);
            statement.setString(11, toDateTimeString(copy.getCreatedAtUtc()));
            statement.setString(12, toDateTimeString(copy.getUpdatedAtUtc()));
            statement.executeUpdate();
        }
    }

    private void insertOutbox(Connection connection, ScheduleItem item, String operation, String payloadJson) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            """
            INSERT INTO sync_outbox(
                id, entity_type, entity_id, operation, payload_json,
                local_version, device_id, created_at_utc, retry_count, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
        )) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, ENTITY_TYPE_SCHEDULE_ITEM);
            statement.setString(3, item.getId());
            statement.setString(4, operation);
            statement.setString(5, payloadJson);
            statement.setInt(6, item.getVersion());
            statement.setString(7, emptyToNull(item.getDeviceId()));
            statement.setString(8, toDateTimeString(nowUtc()));
            statement.setInt(9, 0);
            statement.setString(10, "pending");
            statement.executeUpdate();
        }
    }

    private List<ScheduleItem> queryScheduleItems(Connection connection, String sql, StatementBinder binder) throws SQLException {
        List<ScheduleItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(loadScheduleItemAggregate(connection, resultSet));
                }
            }
        }
        return items;
    }

    private ScheduleItem loadScheduleItem(Connection connection, String scheduleItemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT * FROM schedule_item WHERE id = ?"
        )) {
            statement.setString(1, scheduleItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return loadScheduleItemAggregate(connection, resultSet);
            }
        }
    }

    private ScheduleItem loadScheduleItemAggregate(Connection connection, ResultSet resultSet) throws SQLException {
        ScheduleItem item = mapScheduleItem(resultSet);
        item.setTagObjects(loadTags(connection, item.getId()));
        item.setReminders(loadReminders(connection, item.getId()));
        item.setRecurrenceRule(loadRecurrenceRule(connection, item.getId()));
        item.setViewKey(item.getId());
        return item;
    }

    private ScheduleItem mapScheduleItem(ResultSet resultSet) throws SQLException {
        ScheduleItem item = new ScheduleItem();
        item.setId(resultSet.getString("id"));
        item.setTitle(resultSet.getString("title"));
        item.setDescription(resultSet.getString("description"));
        item.setNotes(resultSet.getString("notes"));
        item.setStatus(resultSet.getString("status"));
        item.setPriority(resultSet.getString("priority"));
        item.setCategory(resultSet.getString("category"));
        item.setStartAtUtc(parseDateTime(resultSet.getString("start_at_utc")));
        item.setEndAtUtc(parseDateTime(resultSet.getString("end_at_utc")));
        item.setDueAtUtc(parseDateTime(resultSet.getString("due_at_utc")));
        item.setCompletedAtUtc(parseDateTime(resultSet.getString("completed_at_utc")));
        item.setAllDay(resultSet.getInt("is_all_day") == 1);
        item.setTimePrecision(resultSet.getString("time_precision"));
        item.setTimezone(resultSet.getString("timezone"));
        item.setColor(resultSet.getString("color"));
        item.setCreatedAtUtc(parseDateTime(resultSet.getString("created_at_utc")));
        item.setUpdatedAtUtc(parseDateTime(resultSet.getString("updated_at_utc")));
        item.setDeletedAtUtc(parseDateTime(resultSet.getString("deleted_at_utc")));
        item.setVersion(resultSet.getInt("version"));
        item.setSyncStatus(resultSet.getString("sync_status"));
        item.setLastSyncedAtUtc(parseDateTime(resultSet.getString("last_synced_at_utc")));
        item.setDeviceId(resultSet.getString("device_id"));
        item.setMetadataJson(resultSet.getString("metadata_json"));
        item.setSuspended(resultSet.getInt("is_suspended") == 1);
        return item;
    }

    private List<Tag> loadTags(Connection connection, String scheduleItemId) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
            """
            SELECT t.*
            FROM tag t
            INNER JOIN schedule_item_tag sit ON sit.tag_id = t.id
            WHERE sit.schedule_item_id = ?
            ORDER BY LOWER(t.name), t.created_at_utc, t.id
            """
        )) {
            statement.setString(1, scheduleItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Tag tag = new Tag();
                    tag.setId(resultSet.getString("id"));
                    tag.setName(resultSet.getString("name"));
                    tag.setColor(resultSet.getString("color"));
                    tag.setCreatedAtUtc(parseDateTime(resultSet.getString("created_at_utc")));
                    tag.setUpdatedAtUtc(parseDateTime(resultSet.getString("updated_at_utc")));
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private List<Reminder> loadReminders(Connection connection, String scheduleItemId) throws SQLException {
        List<Reminder> reminders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
            """
            SELECT *
            FROM reminder
            WHERE schedule_item_id = ?
            ORDER BY remind_at_utc, created_at_utc, id
            """
        )) {
            statement.setString(1, scheduleItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Reminder reminder = new Reminder();
                    reminder.setId(resultSet.getString("id"));
                    reminder.setScheduleItemId(resultSet.getString("schedule_item_id"));
                    reminder.setRemindAtUtc(parseDateTime(resultSet.getString("remind_at_utc")));
                    int offset = resultSet.getInt("offset_minutes");
                    reminder.setOffsetMinutes(resultSet.wasNull() ? null : offset);
                    reminder.setChannel(resultSet.getString("channel"));
                    reminder.setStatus(resultSet.getString("status"));
                    reminder.setCreatedAtUtc(parseDateTime(resultSet.getString("created_at_utc")));
                    reminder.setUpdatedAtUtc(parseDateTime(resultSet.getString("updated_at_utc")));
                    reminders.add(reminder);
                }
            }
        }
        return reminders;
    }

    private RecurrenceRule loadRecurrenceRule(Connection connection, String scheduleItemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT * FROM recurrence_rule WHERE schedule_item_id = ?"
        )) {
            statement.setString(1, scheduleItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                RecurrenceRule rule = new RecurrenceRule();
                rule.setId(resultSet.getString("id"));
                rule.setScheduleItemId(resultSet.getString("schedule_item_id"));
                rule.setFrequency(resultSet.getString("freq"));
                rule.setInterval(resultSet.getInt("interval"));
                rule.setByDayCsv(resultSet.getString("by_day_csv"));
                int byMonthDay = resultSet.getInt("by_month_day");
                rule.setByMonthDay(resultSet.wasNull() ? null : byMonthDay);
                rule.setUntilAtUtc(parseDateTime(resultSet.getString("until_at_utc")));
                int occurrenceCount = resultSet.getInt("occurrence_count");
                rule.setOccurrenceCount(resultSet.wasNull() ? null : occurrenceCount);
                rule.setTimezone(resultSet.getString("timezone"));
                rule.setActive(resultSet.getInt("is_active") == 1);
                rule.setCreatedAtUtc(parseDateTime(resultSet.getString("created_at_utc")));
                rule.setUpdatedAtUtc(parseDateTime(resultSet.getString("updated_at_utc")));
                return rule;
            }
        }
    }

    private String buildSchedulePayloadJson(ScheduleItem item) {
        List<String> reminderPayloads = new ArrayList<>();
        for (Reminder reminder : item.getReminders()) {
            reminderPayloads.add(
                "{\"id\":\"" + jsonEscape(reminder.getId()) + "\"," +
                    "\"remindAtUtc\":" + jsonString(reminder.getRemindAtUtc()) + "," +
                    "\"channel\":\"" + jsonEscape(reminder.getChannel()) + "\"," +
                    "\"status\":\"" + jsonEscape(reminder.getStatus()) + "\"}"
            );
        }

        RecurrenceRule rule = item.getRecurrenceRule();
        String recurrenceJson = rule == null
            ? "null"
            : "{" +
                "\"id\":\"" + jsonEscape(rule.getId()) + "\"," +
                "\"freq\":\"" + jsonEscape(rule.getFrequency()) + "\"," +
                "\"interval\":" + rule.getInterval() + "," +
                "\"byDayCsv\":" + jsonString(emptyToNull(rule.getByDayCsv())) + "," +
                "\"byMonthDay\":" + (rule.getByMonthDay() == null ? "null" : rule.getByMonthDay()) + "," +
                "\"untilAtUtc\":" + jsonString(rule.getUntilAtUtc()) + "," +
                "\"occurrenceCount\":" + (rule.getOccurrenceCount() == null ? "null" : rule.getOccurrenceCount()) + "," +
                "\"timezone\":" + jsonString(rule.getTimezone()) + "," +
                "\"isActive\":" + rule.isActive() +
                "}";

        return "{" +
            "\"id\":\"" + jsonEscape(item.getId()) + "\"," +
            "\"title\":\"" + jsonEscape(item.getTitle()) + "\"," +
            "\"description\":" + jsonString(item.getDescription()) + "," +
            "\"notes\":" + jsonString(item.getNotes()) + "," +
            "\"status\":\"" + jsonEscape(item.getStatus()) + "\"," +
            "\"priority\":\"" + jsonEscape(item.getPriority()) + "\"," +
            "\"category\":\"" + jsonEscape(item.getCategory()) + "\"," +
            "\"startAtUtc\":" + jsonString(item.getStartAtUtc()) + "," +
            "\"endAtUtc\":" + jsonString(item.getEndAtUtc()) + "," +
            "\"dueAtUtc\":" + jsonString(item.getDueAtUtc()) + "," +
            "\"completedAtUtc\":" + jsonString(item.getCompletedAtUtc()) + "," +
            "\"isAllDay\":" + item.isAllDay() + "," +
            "\"timePrecision\":\"" + jsonEscape(item.getTimePrecision()) + "\"," +
            "\"timezone\":\"" + jsonEscape(item.getTimezone()) + "\"," +
            "\"color\":" + jsonString(item.getColor()) + "," +
            "\"createdAtUtc\":" + jsonString(item.getCreatedAtUtc()) + "," +
            "\"updatedAtUtc\":" + jsonString(item.getUpdatedAtUtc()) + "," +
            "\"deletedAtUtc\":" + jsonString(item.getDeletedAtUtc()) + "," +
            "\"version\":" + item.getVersion() + "," +
            "\"syncStatus\":\"" + jsonEscape(item.getSyncStatus()) + "\"," +
            "\"lastSyncedAtUtc\":" + jsonString(item.getLastSyncedAtUtc()) + "," +
            "\"deviceId\":" + jsonString(item.getDeviceId()) + "," +
            "\"tags\":[" + joinQuoted(item.getTagNames()) + "]," +
            "\"reminders\":[" + String.join(",", reminderPayloads) + "]," +
            "\"recurrence\":" + recurrenceJson +
            "}";
    }

    private String buildDeletePayloadJson(ScheduleItem item) {
        return "{" +
            "\"id\":\"" + jsonEscape(item.getId()) + "\"," +
            "\"deletedAtUtc\":" + jsonString(item.getDeletedAtUtc()) + "," +
            "\"status\":\"" + jsonEscape(item.getSyncStatus()) + "\"" +
            "}";
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.trim().replace(' ', 'T'));
    }

    private String toDateTimeString(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private String normalizeStatus(ScheduleItem item) {
        return item.isCompleted() ? ScheduleItem.STATUS_COMPLETED : item.getStatus();
    }

    private String normalizeSyncStatus(String syncStatus, boolean hasSyncedState) {
        if (syncStatus == null || syncStatus.isBlank()) {
            return hasSyncedState ? ScheduleItem.SYNC_STATUS_PENDING_UPLOAD : ScheduleItem.SYNC_STATUS_LOCAL_ONLY;
        }
        return syncStatus;
    }

    private String detectPlatform() {
        return System.getProperty("os.name", "unknown") + " / " + dialect.name().toLowerCase(Locale.ROOT);
    }

    private String coalesceText(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String orderBy(String qualifier) {
        String prefix = qualifier == null || qualifier.isBlank() ? "" : qualifier + ".";
        return " ORDER BY COALESCE(" +
            prefix + "due_at_utc, " +
            prefix + "end_at_utc, " +
            prefix + "start_at_utc, " +
            prefix + "created_at_utc), " +
            prefix + "updated_at_utc DESC, " +
            prefix + "id ASC";
    }

    private String jsonString(LocalDateTime value) {
        return jsonString(toDateTimeString(value));
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + jsonEscape(value) + "\"";
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    private String joinQuoted(List<String> values) {
        List<String> quoted = new ArrayList<>();
        for (String value : values) {
            quoted.add("\"" + jsonEscape(value) + "\"");
        }
        return String.join(",", quoted);
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
