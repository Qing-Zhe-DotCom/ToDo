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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class Schedule {
    public static final String PRIORITY_HIGH = "\u9ad8";
    public static final String PRIORITY_MEDIUM = "\u4e2d";
    public static final String PRIORITY_LOW = "\u4f4e";
    public static final String DEFAULT_PRIORITY = PRIORITY_MEDIUM;
    public static final String DEFAULT_CATEGORY = "\u672a\u5206\u7c7b";
    public static final String LEGACY_DEFAULT_CATEGORY = "\u9ed8\u8ba4";

    private static final LocalTime START_OF_DAY_TIME = LocalTime.MIDNIGHT;
    private static final LocalTime END_OF_DAY_TIME = LocalTime.of(23, 59);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty("");
    private final ObjectProperty<LocalDateTime> startAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> dueAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> startDateProjection = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> dueDateProjection = new SimpleObjectProperty<>();
    private final BooleanProperty completed = new SimpleBooleanProperty();
    private final StringProperty priority = new SimpleStringProperty(DEFAULT_PRIORITY);
    private final StringProperty category = new SimpleStringProperty(DEFAULT_CATEGORY);
    private final StringProperty tags = new SimpleStringProperty("");
    private final ObjectProperty<LocalDateTime> reminderTime = new SimpleObjectProperty<>();
    private final StringProperty color = new SimpleStringProperty("#2196F3");
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>();

    private boolean syncingDateProjection;

    public Schedule() {
        configureDateProjectionSync();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        this.createdAt.set(now);
        this.updatedAt.set(now);
    }

    public Schedule(
        String name,
        String description,
        LocalDate startDate,
        LocalDate dueDate,
        boolean completed,
        String priority,
        String category
    ) {
        this();
        setName(name);
        setDescription(description);
        setStartDate(startDate);
        setDueDate(dueDate);
        setCompleted(completed);
        setPriority(priority);
        setCategory(category);
    }

    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name == null ? "" : name.strip());
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description == null ? "" : description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public LocalDateTime getStartAt() {
        return startAt.get();
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt.set(truncateToMinutes(startAt));
    }

    public ObjectProperty<LocalDateTime> startAtProperty() {
        return startAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt.get();
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt.set(truncateToMinutes(dueAt));
    }

    public ObjectProperty<LocalDateTime> dueAtProperty() {
        return dueAt;
    }

    public LocalDate getStartDate() {
        return startDateProjection.get();
    }

    public void setStartDate(LocalDate startDate) {
        setStartAt(startDate == null ? null : startDate.atTime(START_OF_DAY_TIME));
    }

    public ObjectProperty<LocalDate> startDateProperty() {
        return startDateProjection;
    }

    public LocalDate getDueDate() {
        return dueDateProjection.get();
    }

    public void setDueDate(LocalDate dueDate) {
        setDueAt(dueDate == null ? null : dueDate.atTime(END_OF_DAY_TIME));
    }

    public ObjectProperty<LocalDate> dueDateProperty() {
        return dueDateProjection;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public void setCompleted(boolean completed) {
        this.completed.set(completed);
    }

    public BooleanProperty completedProperty() {
        return completed;
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

    public String getTags() {
        return tags.get();
    }

    public void setTags(String tags) {
        this.tags.set(normalizeTags(tags));
    }

    public StringProperty tagsProperty() {
        return tags;
    }

    public LocalDateTime getReminderTime() {
        return reminderTime.get();
    }

    public void setReminderTime(LocalDateTime reminderTime) {
        this.reminderTime.set(truncateToMinutes(reminderTime));
    }

    public ObjectProperty<LocalDateTime> reminderTimeProperty() {
        return reminderTime;
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

    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt == null ? null : createdAt.truncatedTo(ChronoUnit.SECONDS));
    }

    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt.get();
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt.set(updatedAt == null ? null : updatedAt.truncatedTo(ChronoUnit.SECONDS));
    }

    public ObjectProperty<LocalDateTime> updatedAtProperty() {
        return updatedAt;
    }

    public String getDueDateString() {
        return getDueDate() != null ? getDueDate().format(DATE_FORMATTER) : "";
    }

    public void setDueDateString(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            setDueDate(null);
            return;
        }
        setDueDate(LocalDate.parse(dateStr, DATE_FORMATTER));
    }

    public String getStartDateString() {
        return getStartDate() != null ? getStartDate().format(DATE_FORMATTER) : "";
    }

    public void setStartDateString(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            setStartDate(null);
            return;
        }
        setStartDate(LocalDate.parse(dateStr, DATE_FORMATTER));
    }

    public String getStartAtString() {
        return getStartAt() != null ? getStartAt().format(DATETIME_FORMATTER) : "";
    }

    public void setStartAtString(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            setStartAt(null);
            return;
        }
        setStartAt(LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER));
    }

    public String getDueAtString() {
        return getDueAt() != null ? getDueAt().format(DATETIME_FORMATTER) : "";
    }

    public void setDueAtString(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            setDueAt(null);
            return;
        }
        setDueAt(LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER));
    }

    public String getCreatedAtString() {
        return getCreatedAt() != null ? getCreatedAt().format(DATETIME_FORMATTER) : "";
    }

    public void setCreatedAtString(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            setCreatedAt(null);
            return;
        }
        setCreatedAt(LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER));
    }

    public String getUpdatedAtString() {
        return getUpdatedAt() != null ? getUpdatedAt().format(DATETIME_FORMATTER) : "";
    }

    public void setUpdatedAtString(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            setUpdatedAt(null);
            return;
        }
        setUpdatedAt(LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER));
    }

    public String getReminderTimeString() {
        return getReminderTime() != null ? getReminderTime().format(DATETIME_FORMATTER) : null;
    }

    public void setReminderTimeString(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            setReminderTime(null);
            return;
        }
        setReminderTime(LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER));
    }

    public boolean isOverdue() {
        LocalDateTime deadline = getEffectiveEndAt();
        return !completed.get() && deadline != null && deadline.isBefore(LocalDateTime.now());
    }

    public LocalDateTime getEffectiveStartAt() {
        LocalDateTime[] normalizedRange = getNormalizedEffectiveDateTimeRange();
        return normalizedRange != null ? normalizedRange[0] : null;
    }

    public LocalDateTime getEffectiveEndAt() {
        LocalDateTime[] normalizedRange = getNormalizedEffectiveDateTimeRange();
        return normalizedRange != null ? normalizedRange[1] : null;
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

    public boolean isUpcoming() {
        if (completed.get()) {
            return false;
        }

        LocalDateTime effectiveDeadline = getEffectiveEndAt();
        if (effectiveDeadline == null || effectiveDeadline.isBefore(LocalDateTime.now())) {
            return false;
        }

        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), effectiveDeadline.toLocalDate());
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
        String currentPriority = priority.get();
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
        return name.get();
    }

    public static String normalizePriority(String priority) {
        String normalized = normalizeText(priority);
        if (normalized.isEmpty()) {
            return DEFAULT_PRIORITY;
        }
        return normalized;
    }

    public static String normalizeCategory(String category) {
        String normalized = normalizeText(category);
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
        for (String piece : rawTags.split("[,\uFF0C]")) {
            String normalized = normalizeText(piece);
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

    private void configureDateProjectionSync() {
        startAt.addListener((obs, oldValue, newValue) -> syncDateProjections());
        dueAt.addListener((obs, oldValue, newValue) -> syncDateProjections());
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
            startDateProjection.set(startAt.get() != null ? startAt.get().toLocalDate() : null);
            dueDateProjection.set(dueAt.get() != null ? dueAt.get().toLocalDate() : null);
        } finally {
            syncingDateProjection = false;
        }
    }

    private LocalDateTime[] getNormalizedEffectiveDateTimeRange() {
        LocalDateTime effectiveStartAt = startAt.get() != null ? startAt.get() : dueAt.get();
        LocalDateTime effectiveEndAt = dueAt.get() != null ? dueAt.get() : startAt.get();
        if (effectiveStartAt == null || effectiveEndAt == null) {
            return null;
        }
        if (effectiveStartAt.isAfter(effectiveEndAt)) {
            LocalDateTime temp = effectiveStartAt;
            effectiveStartAt = effectiveEndAt;
            effectiveEndAt = temp;
        }
        return new LocalDateTime[] { effectiveStartAt, effectiveEndAt };
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.strip();
    }

    private static LocalDateTime truncateToMinutes(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.MINUTES);
    }
}
