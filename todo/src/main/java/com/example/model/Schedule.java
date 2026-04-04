package com.example.model;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

public class Schedule {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> dueDate = new SimpleObjectProperty<>();
    private final BooleanProperty completed = new SimpleBooleanProperty();
    private final StringProperty priority = new SimpleStringProperty("中");
    private final StringProperty category = new SimpleStringProperty("默认");
    private final StringProperty tags = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> reminderTime = new SimpleObjectProperty<>();
    private final StringProperty color = new SimpleStringProperty("#2196F3");
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>();
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Schedule() {
        this.createdAt.set(LocalDateTime.now());
        this.updatedAt.set(LocalDateTime.now());
    }

    public Schedule(String name, String description, LocalDate startDate, LocalDate dueDate, 
                    boolean completed, String priority, String category) {
        this();
        this.name.set(name);
        this.description.set(description);
        this.startDate.set(startDate);
        this.dueDate.set(dueDate);
        this.completed.set(completed);
        this.priority.set(priority);
        this.category.set(category);
    }

    // ID属性
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    // 名称属性
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    // 描述属性
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }

    // 开始日期属性
    public LocalDate getStartDate() { return startDate.get(); }
    public void setStartDate(LocalDate startDate) { this.startDate.set(startDate); }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }

    // 截止日期属性
    public LocalDate getDueDate() { return dueDate.get(); }
    public void setDueDate(LocalDate dueDate) { this.dueDate.set(dueDate); }
    public ObjectProperty<LocalDate> dueDateProperty() { return dueDate; }

    // 完成状态属性
    public boolean isCompleted() { return completed.get(); }
    public void setCompleted(boolean completed) { this.completed.set(completed); }
    public BooleanProperty completedProperty() { return completed; }

    // 优先级属性
    public String getPriority() { return priority.get(); }
    public void setPriority(String priority) { this.priority.set(priority); }
    public StringProperty priorityProperty() { return priority; }

    // 分类属性
    public String getCategory() { return category.get(); }
    public void setCategory(String category) { this.category.set(category); }
    public StringProperty categoryProperty() { return category; }

    // 标签属性
    public String getTags() { return tags.get(); }
    public void setTags(String tags) { this.tags.set(tags); }
    public StringProperty tagsProperty() { return tags; }

    // 提醒时间属性
    public LocalDateTime getReminderTime() { return reminderTime.get(); }
    public void setReminderTime(LocalDateTime reminderTime) { this.reminderTime.set(reminderTime); }
    public ObjectProperty<LocalDateTime> reminderTimeProperty() { return reminderTime; }

    // 颜色属性
    public String getColor() { return color.get(); }
    public void setColor(String color) { this.color.set(color); }
    public StringProperty colorProperty() { return color; }

    // 创建时间属性
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }

    // 更新时间属性
    public LocalDateTime getUpdatedAt() { return updatedAt.get(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt.set(updatedAt); }
    public ObjectProperty<LocalDateTime> updatedAtProperty() { return updatedAt; }

    // 兼容旧版数据库格式的getter/setter
    public String getDueDateString() {
        return dueDate.get() != null ? dueDate.get().format(DATE_FORMATTER) : "";
    }
    
    public void setDueDateString(String dateStr) {
        if (dateStr != null && !dateStr.isEmpty()) {
            this.dueDate.set(LocalDate.parse(dateStr, DATE_FORMATTER));
        }
    }

    public String getStartDateString() {
        return startDate.get() != null ? startDate.get().format(DATE_FORMATTER) : "";
    }
    
    public void setStartDateString(String dateStr) {
        if (dateStr != null && !dateStr.isEmpty()) {
            this.startDate.set(LocalDate.parse(dateStr, DATE_FORMATTER));
        }
    }

    public String getCreatedAtString() {
        return createdAt.get() != null ? createdAt.get().format(DATETIME_FORMATTER) : "";
    }
    
    public void setCreatedAtString(String dateTimeStr) {
        if (dateTimeStr != null && !dateTimeStr.isEmpty()) {
            this.createdAt.set(LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER));
        }
    }

    public String getUpdatedAtString() {
        return updatedAt.get() != null ? updatedAt.get().format(DATETIME_FORMATTER) : "";
    }
    
    public void setUpdatedAtString(String dateTimeStr) {
        if (dateTimeStr != null && !dateTimeStr.isEmpty()) {
            this.updatedAt.set(LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER));
        }
    }

    public String getReminderTimeString() {
        return reminderTime.get() != null ? reminderTime.get().format(DATETIME_FORMATTER) : null;
    }
    
    public void setReminderTimeString(String dateTimeStr) {
        if (dateTimeStr != null && !dateTimeStr.isEmpty()) {
            this.reminderTime.set(LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER));
        }
    }

    // 检查是否过期
    public boolean isOverdue() {
        return !completed.get() && dueDate.get() != null && dueDate.get().isBefore(LocalDate.now());
    }

    public LocalDate getEffectiveStartDate() {
        LocalDate[] normalizedRange = getNormalizedEffectiveDateRange();
        return normalizedRange != null ? normalizedRange[0] : null;
    }

    public LocalDate getEffectiveEndDate() {
        LocalDate[] normalizedRange = getNormalizedEffectiveDateRange();
        return normalizedRange != null ? normalizedRange[1] : null;
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

    // 检查是否即将到期（按时长分级）
    public boolean isUpcoming() {
        if (completed.get()) {
            return false;
        }

        LocalDate effectiveDeadline = getEffectiveDeadlineDate();
        if (effectiveDeadline == null) {
            return false;
        }

        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), effectiveDeadline);
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

    // 获取优先级数值（用于排序）
    public int getPriorityValue() {
        String p = priority.get();
        if ("高".equals(p)) return 3;
        if ("中".equals(p)) return 2;
        if ("低".equals(p)) return 1;
        return 0;
    }

    @Override
    public String toString() {
        return name.get();
    }

    private LocalDate getEffectiveDeadlineDate() {
        if (dueDate.get() == null) {
            return null;
        }
        return getEffectiveEndDate();
    }

    private LocalDate[] getNormalizedEffectiveDateRange() {
        LocalDate effectiveStartDate = startDate.get() != null ? startDate.get() : dueDate.get();
        LocalDate effectiveEndDate = dueDate.get() != null ? dueDate.get() : startDate.get();
        if (effectiveStartDate == null || effectiveEndDate == null) {
            return null;
        }
        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            LocalDate temp = effectiveStartDate;
            effectiveStartDate = effectiveEndDate;
            effectiveEndDate = temp;
        }
        return new LocalDate[] { effectiveStartDate, effectiveEndDate };
    }
}
