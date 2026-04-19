package com.example.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScheduleItem {
    public static final String PRIORITY_HIGH = "高";
    public static final String PRIORITY_MEDIUM = "中";
    public static final String PRIORITY_LOW = "低";
    public static final String DEFAULT_PRIORITY = PRIORITY_MEDIUM;
    public static final String DEFAULT_CATEGORY = "未分类";
    public static final String LEGACY_DEFAULT_CATEGORY = "默认";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    public static final String TIME_PRECISION_DAY = "day";
    public static final String TIME_PRECISION_MINUTE = "minute";

    public static final String SYNC_STATUS_LOCAL_ONLY = "local_only";
    public static final String SYNC_STATUS_PENDING_UPLOAD = "pending_upload";
    public static final String SYNC_STATUS_SYNCED = "synced";
    public static final String SYNC_STATUS_PENDING_DELETE = "pending_delete";

    private static final LocalTime START_OF_DAY_TIME = LocalTime.MIDNIGHT;
    private static final LocalTime END_OF_DAY_TIME = LocalTime.of(23, 59);

    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty viewKey = new SimpleStringProperty(id.get());
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty notes = new SimpleStringProperty("");
    private final ObjectProperty<LocalDateTime> startAtUtc = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> endAtUtc = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> dueAtUtc = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> completedAtUtc = new SimpleObjectProperty<>();
    private final BooleanProperty allDay = new SimpleBooleanProperty(false);
    private final BooleanProperty suspended = new SimpleBooleanProperty(false);
    private final StringProperty timePrecision = new SimpleStringProperty(TIME_PRECISION_MINUTE);
    private final StringProperty timezone = new SimpleStringProperty(ZoneId.systemDefault().getId());
    private final StringProperty status = new SimpleStringProperty(STATUS_ACTIVE);
    private final StringProperty priority = new SimpleStringProperty(DEFAULT_PRIORITY);
    private final StringProperty category = new SimpleStringProperty(DEFAULT_CATEGORY);
    private final StringProperty color = new SimpleStringProperty("#2196F3");
    private final ObjectProperty<LocalDateTime> createdAtUtc = new SimpleObjectProperty<>(Tag.nowUtc());
    private final ObjectProperty<LocalDateTime> updatedAtUtc = new SimpleObjectProperty<>(Tag.nowUtc());
    private final ObjectProperty<LocalDateTime> deletedAtUtc = new SimpleObjectProperty<>();
    private final IntegerProperty version = new SimpleIntegerProperty(1);
    private final StringProperty syncStatus = new SimpleStringProperty(SYNC_STATUS_LOCAL_ONLY);
    private final ObjectProperty<LocalDateTime> lastSyncedAtUtc = new SimpleObjectProperty<>();
    private final StringProperty deviceId = new SimpleStringProperty();
    private final StringProperty metadataJson = new SimpleStringProperty();

    private final ObjectProperty<LocalDate> startDateProjection = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> dueDateProjection = new SimpleObjectProperty<>();
    private final StringProperty tagsText = new SimpleStringProperty("");
    private final ObjectProperty<LocalDateTime> reminderTime = new SimpleObjectProperty<>();
    private final List<Tag> tagObjects = new ArrayList<>();
    private final List<Reminder> reminders = new ArrayList<>();
    private final ObjectProperty<RecurrenceRule> recurrenceRule = new SimpleObjectProperty<>();

    private boolean syncingDateProjection;

    public ScheduleItem() {
        configureProjectionSync();
    }

    public ScheduleItem(String title) {
        this();
        setTitle(title);
    }

    public ScheduleItem(ScheduleItem source) {
        this();
        if (source == null) {
            return;
        }
        setId(source.getId());
        setViewKey(source.getViewKey());
        setTitle(source.getTitle());
        setDescription(source.getDescription());
        setNotes(source.getNotes());
        setStartAtUtc(source.getStartAtUtc());
        setEndAtUtc(source.getEndAtUtc());
        setDueAtUtc(source.getDueAtUtc());
        setCompletedAtUtc(source.getCompletedAtUtc());
        setAllDay(source.isAllDay());
        setSuspended(source.isSuspended());
        setTimePrecision(source.getTimePrecision());
        setTimezone(source.getTimezone());
        setStatus(source.getStatus());
        setPriority(source.getPriority());
        setCategory(source.getCategory());
        setColor(source.getColor());
        setCreatedAtUtc(source.getCreatedAtUtc());
        setUpdatedAtUtc(source.getUpdatedAtUtc());
        setDeletedAtUtc(source.getDeletedAtUtc());
        setVersion(source.getVersion());
        setSyncStatus(source.getSyncStatus());
        setLastSyncedAtUtc(source.getLastSyncedAtUtc());
        setDeviceId(source.getDeviceId());
        setMetadataJson(source.getMetadataJson());
        setTagObjects(source.getTagObjects());
        setReminders(source.getReminders());
        setRecurrenceRule(source.getRecurrenceRule());
    }

    public ScheduleItem copy() {
        return new ScheduleItem(this);
    }

    public String getId() {
        return id.get();
    }

    public void setId(String id) {
        String previousId = this.id.get();
        String previousViewKey = this.viewKey.get();
        this.id.set(normalizeId(id));
        if (previousViewKey == null || previousViewKey.isBlank() || Objects.equals(previousViewKey, previousId)) {
            setViewKey(this.id.get());
        }
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getViewKey() {
        return viewKey.get();
    }

    public void setViewKey(String viewKey) {
        String normalized = Tag.normalizeNullableText(viewKey);
        this.viewKey.set(normalized != null ? normalized : getId());
    }

    public StringProperty viewKeyProperty() {
        return viewKey;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(Tag.normalizeText(title));
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getName() {
        return getTitle();
    }

    public void setName(String name) {
        setTitle(name);
    }

    public StringProperty nameProperty() {
        return title;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description == null ? "" : description);
        if (getNotes().isBlank()) {
            this.notes.set(this.description.get());
        }
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public String getNotes() {
        return notes.get();
    }

    public void setNotes(String notes) {
        this.notes.set(notes == null ? "" : notes);
        if (getDescription().isBlank()) {
            this.description.set(this.notes.get());
        }
    }

    public StringProperty notesProperty() {
        return notes;
    }

    public LocalDateTime getStartAtUtc() {
        return startAtUtc.get();
    }

    public void setStartAtUtc(LocalDateTime startAtUtc) {
        this.startAtUtc.set(truncateToMinutes(startAtUtc));
    }

    public ObjectProperty<LocalDateTime> startAtUtcProperty() {
        return startAtUtc;
    }

    public LocalDateTime getStartAt() {
        return getStartAtUtc();
    }

    public void setStartAt(LocalDateTime startAt) {
        setStartAtUtc(startAt);
    }

    public LocalDateTime getEndAtUtc() {
        return endAtUtc.get();
    }

    public void setEndAtUtc(LocalDateTime endAtUtc) {
        this.endAtUtc.set(truncateToMinutes(endAtUtc));
    }

    public ObjectProperty<LocalDateTime> endAtUtcProperty() {
        return endAtUtc;
    }

    public LocalDateTime getEndAt() {
        return getEndAtUtc();
    }

    public void setEndAt(LocalDateTime endAt) {
        setEndAtUtc(endAt);
    }

    public LocalDateTime getDueAtUtc() {
        return dueAtUtc.get();
    }

    public void setDueAtUtc(LocalDateTime dueAtUtc) {
        this.dueAtUtc.set(truncateToMinutes(dueAtUtc));
    }

    public ObjectProperty<LocalDateTime> dueAtUtcProperty() {
        return dueAtUtc;
    }

    public LocalDateTime getDueAt() {
        return getDueAtUtc();
    }

    public void setDueAt(LocalDateTime dueAt) {
        setDueAtUtc(dueAt);
    }

    public LocalDateTime getCompletedAtUtc() {
        return completedAtUtc.get();
    }

    public void setCompletedAtUtc(LocalDateTime completedAtUtc) {
        this.completedAtUtc.set(Tag.truncateSeconds(completedAtUtc));
    }

    public ObjectProperty<LocalDateTime> completedAtUtcProperty() {
        return completedAtUtc;
    }

    public LocalDateTime getCompletedAt() {
        return getCompletedAtUtc();
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        setCompletedAtUtc(completedAt);
    }

    public boolean isAllDay() {
        return allDay.get();
    }

    public void setAllDay(boolean allDay) {
        this.allDay.set(allDay);
        if (allDay) {
            if (getStartAt() != null) {
                setStartAt(getStartAt().toLocalDate().atTime(START_OF_DAY_TIME));
            }
            if (getEndAt() != null) {
                setEndAt(getEndAt().toLocalDate().atTime(END_OF_DAY_TIME));
            }
            if (getDueAt() != null) {
                setDueAt(getDueAt().toLocalDate().atTime(END_OF_DAY_TIME));
            }
            setTimePrecision(TIME_PRECISION_DAY);
        }
    }

    public BooleanProperty allDayProperty() {
        return allDay;
    }

    public boolean isSuspended() {
        return suspended.get();
    }

    public void setSuspended(boolean suspended) {
        this.suspended.set(suspended);
    }

    public BooleanProperty suspendedProperty() {
        return suspended;
    }

    public String getTimePrecision() {
        return timePrecision.get();
    }

    public void setTimePrecision(String timePrecision) {
        String normalized = Tag.normalizeNullableText(timePrecision);
        if (!TIME_PRECISION_DAY.equals(normalized)) {
            this.timePrecision.set(TIME_PRECISION_MINUTE);
            return;
        }
        this.timePrecision.set(TIME_PRECISION_DAY);
    }

    public StringProperty timePrecisionProperty() {
        return timePrecision;
    }

    public String getTimezone() {
        return timezone.get();
    }

    public void setTimezone(String timezone) {
        String normalized = Tag.normalizeNullableText(timezone);
        this.timezone.set(normalized != null ? normalized : ZoneId.systemDefault().getId());
    }

    public StringProperty timezoneProperty() {
        return timezone;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        String normalized = Tag.normalizeNullableText(status);
        if (STATUS_COMPLETED.equals(normalized)) {
            this.status.set(STATUS_COMPLETED);
            if (getCompletedAtUtc() == null) {
                setCompletedAtUtc(Tag.nowUtc());
            }
            return;
        }
        if (STATUS_CANCELLED.equals(normalized)) {
            this.status.set(STATUS_CANCELLED);
            return;
        }
        this.status.set(STATUS_ACTIVE);
        if (getCompletedAtUtc() != null && !STATUS_COMPLETED.equals(normalized)) {
            setCompletedAtUtc(null);
        }
    }

    public StringProperty statusProperty() {
        return status;
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(getStatus());
    }

    public void setCompleted(boolean completed) {
        if (completed) {
            setStatus(STATUS_COMPLETED);
            if (getCompletedAtUtc() == null) {
                setCompletedAtUtc(Tag.nowUtc());
            }
            return;
        }
        if (isCompleted()) {
            status.set(STATUS_ACTIVE);
            setCompletedAtUtc(null);
        }
    }

    public String getPriority() {
        return priority.get();
    }

    public void setPriority(String priority) {
        this.priority.set(normalizePriority(priority));
    }

    public StringProperty priorityProperty() {
        return priority;
    }

    public String getCategory() {
        return category.get();
    }

    public void setCategory(String category) {
        this.category.set(normalizeCategory(category));
    }

    public StringProperty categoryProperty() {
        return category;
    }

    public String getColor() {
        return color.get();
    }

    public void setColor(String color) {
        this.color.set(color == null || color.isBlank() ? "#2196F3" : color);
    }

    public StringProperty colorProperty() {
        return color;
    }

    public LocalDateTime getCreatedAtUtc() {
        return createdAtUtc.get();
    }

    public void setCreatedAtUtc(LocalDateTime createdAtUtc) {
        this.createdAtUtc.set(Tag.truncateSeconds(createdAtUtc));
    }

    public ObjectProperty<LocalDateTime> createdAtUtcProperty() {
        return createdAtUtc;
    }

    public LocalDateTime getCreatedAt() {
        return getCreatedAtUtc();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        setCreatedAtUtc(createdAt);
    }

    public LocalDateTime getUpdatedAtUtc() {
        return updatedAtUtc.get();
    }

    public void setUpdatedAtUtc(LocalDateTime updatedAtUtc) {
        this.updatedAtUtc.set(Tag.truncateSeconds(updatedAtUtc));
    }

    public ObjectProperty<LocalDateTime> updatedAtUtcProperty() {
        return updatedAtUtc;
    }

    public LocalDateTime getUpdatedAt() {
        return getUpdatedAtUtc();
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        setUpdatedAtUtc(updatedAt);
    }

    public LocalDateTime getDeletedAtUtc() {
        return deletedAtUtc.get();
    }

    public void setDeletedAtUtc(LocalDateTime deletedAtUtc) {
        this.deletedAtUtc.set(Tag.truncateSeconds(deletedAtUtc));
    }

    public ObjectProperty<LocalDateTime> deletedAtUtcProperty() {
        return deletedAtUtc;
    }

    public LocalDateTime getDeletedAt() {
        return getDeletedAtUtc();
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        setDeletedAtUtc(deletedAt);
    }

    public boolean isDeleted() {
        return getDeletedAtUtc() != null;
    }

    public int getVersion() {
        return version.get();
    }

    public void setVersion(int version) {
        this.version.set(Math.max(1, version));
    }

    public IntegerProperty versionProperty() {
        return version;
    }

    public String getSyncStatus() {
        return syncStatus.get();
    }

    public void setSyncStatus(String syncStatus) {
        String normalized = Tag.normalizeNullableText(syncStatus);
        this.syncStatus.set(normalized != null ? normalized : SYNC_STATUS_LOCAL_ONLY);
    }

    public StringProperty syncStatusProperty() {
        return syncStatus;
    }

    public LocalDateTime getLastSyncedAtUtc() {
        return lastSyncedAtUtc.get();
    }

    public void setLastSyncedAtUtc(LocalDateTime lastSyncedAtUtc) {
        this.lastSyncedAtUtc.set(Tag.truncateSeconds(lastSyncedAtUtc));
    }

    public ObjectProperty<LocalDateTime> lastSyncedAtUtcProperty() {
        return lastSyncedAtUtc;
    }

    public String getDeviceId() {
        return deviceId.get();
    }

    public void setDeviceId(String deviceId) {
        this.deviceId.set(Tag.normalizeNullableText(deviceId));
    }

    public StringProperty deviceIdProperty() {
        return deviceId;
    }

    public String getMetadataJson() {
        return metadataJson.get();
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson.set(Tag.normalizeNullableText(metadataJson));
    }

    public StringProperty metadataJsonProperty() {
        return metadataJson;
    }

    public LocalDate getStartDate() {
        return startDateProjection.get();
    }

    public void setStartDate(LocalDate startDate) {
        setStartAt(startDate == null ? null : startDate.atTime(START_OF_DAY_TIME));
        if (startDate != null && isAllDay()) {
            setTimePrecision(TIME_PRECISION_DAY);
        }
    }

    public ObjectProperty<LocalDate> startDateProperty() {
        return startDateProjection;
    }

    public LocalDate getDueDate() {
        return dueDateProjection.get();
    }

    public void setDueDate(LocalDate dueDate) {
        setDueAt(dueDate == null ? null : dueDate.atTime(END_OF_DAY_TIME));
        if (dueDate != null && isAllDay()) {
            setTimePrecision(TIME_PRECISION_DAY);
        }
    }

    public ObjectProperty<LocalDate> dueDateProperty() {
        return dueDateProjection;
    }

    public String getTags() {
        return tagsText.get();
    }

    public void setTags(String tags) {
        setTagNames(splitTags(tags));
    }

    public StringProperty tagsProperty() {
        return tagsText;
    }

    public List<String> getTagNames() {
        return tagObjects.stream()
            .map(Tag::getName)
            .filter(name -> !name.isBlank())
            .collect(Collectors.toList());
    }

    public void setTagNames(List<String> tagNames) {
        tagObjects.clear();
        if (tagNames != null) {
            LinkedHashSet<String> uniqueTags = new LinkedHashSet<>();
            for (String tagName : tagNames) {
                String normalized = Tag.normalizeText(tagName);
                if (!normalized.isEmpty()) {
                    uniqueTags.add(normalized);
                }
            }
            for (String tagName : uniqueTags) {
                tagObjects.add(new Tag(tagName));
            }
        }
        syncTagsText();
    }

    public List<Tag> getTagObjects() {
        return tagObjects.stream().map(Tag::copy).collect(Collectors.toList());
    }

    public void setTagObjects(List<Tag> tags) {
        tagObjects.clear();
        if (tags != null) {
            for (Tag tag : tags) {
                if (tag != null) {
                    tagObjects.add(tag.copy());
                }
            }
        }
        syncTagsText();
    }

    public LocalDateTime getReminderTime() {
        Reminder firstReminder = getPrimaryReminder();
        return firstReminder != null ? firstReminder.getRemindAtUtc() : reminderTime.get();
    }

    public void setReminderTime(LocalDateTime reminderTime) {
        this.reminderTime.set(truncateToMinutes(reminderTime));
        reminders.clear();
        if (reminderTime != null) {
            reminders.add(new Reminder(this.reminderTime.get()));
        }
    }

    public ObjectProperty<LocalDateTime> reminderTimeProperty() {
        return reminderTime;
    }

    public List<Reminder> getReminders() {
        return reminders.stream()
            .map(Reminder::copy)
            .sorted((left, right) -> {
                LocalDateTime leftValue = left.getRemindAtUtc();
                LocalDateTime rightValue = right.getRemindAtUtc();
                if (leftValue == null && rightValue == null) {
                    return 0;
                }
                if (leftValue == null) {
                    return 1;
                }
                if (rightValue == null) {
                    return -1;
                }
                return leftValue.compareTo(rightValue);
            })
            .collect(Collectors.toList());
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders.clear();
        if (reminders != null) {
            for (Reminder reminder : reminders) {
                if (reminder == null) {
                    continue;
                }
                Reminder copy = reminder.copy();
                copy.setScheduleItemId(getId());
                this.reminders.add(copy);
            }
        }
        syncPrimaryReminder();
    }

    public void addReminder(Reminder reminder) {
        if (reminder == null) {
            return;
        }
        Reminder copy = reminder.copy();
        copy.setScheduleItemId(getId());
        reminders.add(copy);
        syncPrimaryReminder();
    }

    public void removeReminder(String reminderId) {
        reminders.removeIf(reminder -> Objects.equals(reminder.getId(), reminderId));
        syncPrimaryReminder();
    }

    public RecurrenceRule getRecurrenceRule() {
        RecurrenceRule rule = recurrenceRule.get();
        return rule != null ? rule.copy() : null;
    }

    public void setRecurrenceRule(RecurrenceRule recurrenceRule) {
        if (recurrenceRule == null) {
            this.recurrenceRule.set(null);
            return;
        }
        RecurrenceRule copy = recurrenceRule.copy();
        copy.setScheduleItemId(getId());
        if (copy.getTimezone() == null) {
            copy.setTimezone(getTimezone());
        }
        this.recurrenceRule.set(copy);
    }

    public ObjectProperty<RecurrenceRule> recurrenceRuleProperty() {
        return recurrenceRule;
    }

    public boolean hasRecurrence() {
        RecurrenceRule rule = recurrenceRule.get();
        return rule != null && rule.isActive();
    }

    public void touchForWrite(String deviceId) {
        setUpdatedAtUtc(Tag.nowUtc());
        setDeviceId(deviceId);
        setVersion(getVersion() + 1);
        if (SYNC_STATUS_SYNCED.equals(getSyncStatus())) {
            setSyncStatus(SYNC_STATUS_PENDING_UPLOAD);
        }
    }

    public void markDeleted(String deviceId) {
        setDeletedAtUtc(Tag.nowUtc());
        touchForWrite(deviceId);
        setSyncStatus(SYNC_STATUS_PENDING_DELETE);
    }

    public void restoreFromTrash(String deviceId) {
        setDeletedAtUtc(null);
        if (STATUS_CANCELLED.equals(getStatus())) {
            setStatus(STATUS_ACTIVE);
        }
        touchForWrite(deviceId);
    }

    public LocalDateTime getEffectiveStartAt() {
        LocalDateTime primaryStart = firstNonNull(getStartAt(), getEndAt(), getDueAt());
        LocalDateTime primaryEnd = firstNonNull(getEndAt(), getDueAt(), getStartAt());
        if (primaryStart != null && primaryEnd != null && primaryStart.isAfter(primaryEnd)) {
            return primaryEnd;
        }
        return primaryStart;
    }

    public LocalDateTime getEffectiveEndAt() {
        LocalDateTime primaryStart = firstNonNull(getStartAt(), getEndAt(), getDueAt());
        LocalDateTime primaryEnd = firstNonNull(getEndAt(), getDueAt(), getStartAt());
        if (primaryStart != null && primaryEnd != null && primaryStart.isAfter(primaryEnd)) {
            return primaryStart;
        }
        return primaryEnd;
    }

    public LocalDate getEffectiveStartDate() {
        LocalDateTime effectiveStartAt = getEffectiveStartAt();
        return effectiveStartAt != null ? effectiveStartAt.toLocalDate() : null;
    }

    public LocalDate getEffectiveEndDate() {
        LocalDateTime effectiveEndAt = getEffectiveEndAt();
        return effectiveEndAt != null ? effectiveEndAt.toLocalDate() : null;
    }

    public long getEffectiveDurationDays() {
        LocalDate effectiveStartDate = getEffectiveStartDate();
        LocalDate effectiveEndDate = getEffectiveEndDate();
        if (effectiveStartDate == null || effectiveEndDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(effectiveStartDate, effectiveEndDate) + 1;
    }

    public boolean includesDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        LocalDate effectiveStartDate = getEffectiveStartDate();
        LocalDate effectiveEndDate = getEffectiveEndDate();
        if (effectiveStartDate == null || effectiveEndDate == null) {
            return false;
        }
        return !date.isBefore(effectiveStartDate) && !date.isAfter(effectiveEndDate);
    }

    public boolean isOverdue() {
        LocalDateTime deadline = getDueAt() != null ? getDueAt() : getEffectiveEndAt();
        return !isCompleted() && deadline != null && deadline.isBefore(LocalDateTime.now());
    }

    public boolean isUpcoming() {
        if (isCompleted()) {
            return false;
        }
        LocalDateTime deadline = getDueAt() != null ? getDueAt() : getEffectiveEndAt();
        if (deadline == null || deadline.isBefore(LocalDateTime.now())) {
            return false;
        }

        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), deadline.toLocalDate());
        if (daysUntilDeadline < 0) {
            return false;
        }

        long durationDays = getEffectiveDurationDays();
        if (durationDays <= 0) {
            return false;
        }
        if (durationDays < 7) {
            return daysUntilDeadline == 0;
        }
        if (durationDays <= 35) {
            return daysUntilDeadline <= 3;
        }
        return daysUntilDeadline <= 7;
    }

    public int getPriorityValue() {
        String currentPriority = getPriority();
        if (PRIORITY_HIGH.equals(currentPriority)) {
            return 3;
        }
        if (PRIORITY_MEDIUM.equals(currentPriority)) {
            return 2;
        }
        if (PRIORITY_LOW.equals(currentPriority)) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public static String normalizePriority(String priority) {
        String normalized = Tag.normalizeText(priority);
        if (normalized.isEmpty()) {
            return DEFAULT_PRIORITY;
        }
        return normalized;
    }

    public static String normalizeCategory(String category) {
        String normalized = Tag.normalizeText(category);
        if (normalized.isEmpty() || LEGACY_DEFAULT_CATEGORY.equals(normalized)) {
            return DEFAULT_CATEGORY;
        }
        return normalized;
    }

    public static boolean isDefaultCategory(String category) {
        return DEFAULT_CATEGORY.equals(normalizeCategory(category));
    }

    public static List<String> splitTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> uniqueTags = new LinkedHashSet<>();
        for (String piece : rawTags.split("[,，]")) {
            String normalized = Tag.normalizeText(piece);
            if (!normalized.isEmpty()) {
                uniqueTags.add(normalized);
            }
        }
        return new ArrayList<>(uniqueTags);
    }

    public static String normalizeTags(String rawTags) {
        List<String> normalizedTags = splitTags(rawTags);
        return normalizedTags.isEmpty() ? "" : String.join(", ", normalizedTags);
    }

    protected static LocalDateTime truncateToMinutes(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.MINUTES);
    }

    protected static String normalizeId(String id) {
        String normalized = Tag.normalizeNullableText(id);
        return normalized != null ? normalized : UUID.randomUUID().toString();
    }

    private void configureProjectionSync() {
        startAtUtc.addListener((obs, oldValue, newValue) -> syncDateProjections());
        endAtUtc.addListener((obs, oldValue, newValue) -> syncDateProjections());
        dueAtUtc.addListener((obs, oldValue, newValue) -> syncDateProjections());
        startDateProjection.addListener((obs, oldValue, newValue) -> {
            if (!syncingDateProjection) {
                setStartDate(newValue);
            }
        });
        dueDateProjection.addListener((obs, oldValue, newValue) -> {
            if (!syncingDateProjection) {
                setDueDate(newValue);
            }
        });
        syncDateProjections();
    }

    private void syncDateProjections() {
        syncingDateProjection = true;
        try {
            startDateProjection.set(getStartAt() != null ? getStartAt().toLocalDate() : null);
            LocalDateTime dueLike = getDueAt() != null ? getDueAt() : getEndAt();
            dueDateProjection.set(dueLike != null ? dueLike.toLocalDate() : null);
        } finally {
            syncingDateProjection = false;
        }
    }

    private void syncTagsText() {
        tagsText.set(tagObjects.stream()
            .map(Tag::getName)
            .filter(name -> !name.isBlank())
            .collect(Collectors.joining(", ")));
    }

    private void syncPrimaryReminder() {
        Reminder firstReminder = getPrimaryReminder();
        reminderTime.set(firstReminder != null ? firstReminder.getRemindAtUtc() : null);
    }

    private Reminder getPrimaryReminder() {
        return reminders.stream()
            .filter(Objects::nonNull)
            .filter(reminder -> reminder.getRemindAtUtc() != null)
            .sorted((left, right) -> left.getRemindAtUtc().compareTo(right.getRemindAtUtc()))
            .findFirst()
            .orElse(null);
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
