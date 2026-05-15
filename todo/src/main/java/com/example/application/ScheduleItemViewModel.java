package com.example.application;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.example.model.RecurrenceRule;
import com.example.model.Reminder;
import com.example.model.ScheduleItem;
import com.example.model.Tag;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ScheduleItemViewModel {

    private final StringProperty id;
    private final StringProperty title;
    private final StringProperty description;
    private final StringProperty notes;
    private final StringProperty priority;
    private final StringProperty category;
    private final StringProperty color;
    private final BooleanProperty completed;
    private final BooleanProperty allDay;
    private final BooleanProperty suspended;
    private final BooleanProperty hasRecurrence;
    private final ObjectProperty<LocalDateTime> startAt;
    private final ObjectProperty<LocalDateTime> dueAt;
    private final ObjectProperty<LocalDateTime> endAt;
    private final ObjectProperty<LocalDateTime> createdAt;
    private final ObjectProperty<LocalDateTime> updatedAt;
    private final ObjectProperty<LocalDateTime> deletedAt;
    private final IntegerProperty version;
    private final StringProperty syncStatus;
    private final StringProperty deviceId;
    private final StringProperty tagsText;
    private final ObjectProperty<LocalDateTime> reminderTime;
    private final ObjectProperty<RecurrenceRule> recurrenceRule;
    private final ObservableList<Tag> tags;
    private final ObservableList<Reminder> reminders;

    private final StringProperty formattedDate;
    private final StringProperty formattedTime;
    private final StringProperty recurrenceSummary;

    private final ScheduleItem item;

    public ScheduleItemViewModel(ScheduleItem item) {
        this.item = item != null ? item.copy() : new ScheduleItem();

        this.id = new SimpleStringProperty(this.item.getId());
        this.title = new SimpleStringProperty(this.item.getTitle());
        this.description = new SimpleStringProperty(this.item.getDescription());
        this.notes = new SimpleStringProperty(this.item.getNotes());
        this.priority = new SimpleStringProperty(this.item.getPriority());
        this.category = new SimpleStringProperty(this.item.getCategory());
        this.color = new SimpleStringProperty(this.item.getColor());
        this.completed = new SimpleBooleanProperty(this.item.isCompleted());
        this.allDay = new SimpleBooleanProperty(this.item.isAllDay());
        this.suspended = new SimpleBooleanProperty(this.item.isSuspended());
        this.hasRecurrence = new SimpleBooleanProperty(this.item.hasRecurrence());
        this.startAt = new SimpleObjectProperty<>(this.item.getStartAt());
        this.dueAt = new SimpleObjectProperty<>(this.item.getDueAt());
        this.endAt = new SimpleObjectProperty<>(this.item.getEndAt());
        this.createdAt = new SimpleObjectProperty<>(this.item.getCreatedAt());
        this.updatedAt = new SimpleObjectProperty<>(this.item.getUpdatedAt());
        this.deletedAt = new SimpleObjectProperty<>(this.item.getDeletedAt());
        this.version = new SimpleIntegerProperty(this.item.getVersion());
        this.syncStatus = new SimpleStringProperty(this.item.getSyncStatus());
        this.deviceId = new SimpleStringProperty(this.item.getDeviceId());
        this.tagsText = new SimpleStringProperty(this.item.getTags());
        this.reminderTime = new SimpleObjectProperty<>(this.item.getReminderTime());
        this.recurrenceRule = new SimpleObjectProperty<>(this.item.getRecurrenceRule());
        this.tags = FXCollections.observableArrayList(this.item.getTagObjects());
        this.reminders = FXCollections.observableArrayList(this.item.getReminders());

        this.formattedDate = new SimpleStringProperty("");
        this.formattedTime = new SimpleStringProperty("");
        this.recurrenceSummary = new SimpleStringProperty("");

        syncBindings();
    }

    public static ScheduleItemViewModel from(ScheduleItem item) {
        return new ScheduleItemViewModel(item);
    }

    public ScheduleItem toScheduleItem() {
        ScheduleItem result = item.copy();
        result.setTitle(title.get());
        result.setDescription(description.get());
        result.setNotes(notes.get());
        result.setPriority(priority.get());
        result.setCategory(category.get());
        result.setColor(color.get());
        result.setCompleted(completed.get());
        result.setAllDay(allDay.get());
        result.setSuspended(suspended.get());
        result.setStartAt(startAt.get());
        result.setDueAt(dueAt.get());
        result.setEndAt(endAt.get());
        result.setReminderTime(reminderTime.get());
        result.setRecurrenceRule(recurrenceRule.get());
        result.setTagObjects(new ArrayList<>(tags));
        result.setReminders(new ArrayList<>(reminders));
        return result;
    }

    public ScheduleItem getSourceItem() {
        return item.copy();
    }

    // Property getters
    public StringProperty idProperty() { return id; }
    public StringProperty titleProperty() { return title; }
    public StringProperty descriptionProperty() { return description; }
    public StringProperty notesProperty() { return notes; }
    public StringProperty priorityProperty() { return priority; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty colorProperty() { return color; }
    public BooleanProperty completedProperty() { return completed; }
    public BooleanProperty allDayProperty() { return allDay; }
    public BooleanProperty suspendedProperty() { return suspended; }
    public BooleanProperty hasRecurrenceProperty() { return hasRecurrence; }
    public ObjectProperty<LocalDateTime> startAtProperty() { return startAt; }
    public ObjectProperty<LocalDateTime> dueAtProperty() { return dueAt; }
    public ObjectProperty<LocalDateTime> endAtProperty() { return endAt; }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
    public ObjectProperty<LocalDateTime> updatedAtProperty() { return updatedAt; }
    public ObjectProperty<LocalDateTime> deletedAtProperty() { return deletedAt; }
    public IntegerProperty versionProperty() { return version; }
    public StringProperty syncStatusProperty() { return syncStatus; }
    public StringProperty deviceIdProperty() { return deviceId; }
    public StringProperty tagsTextProperty() { return tagsText; }
    public ObjectProperty<LocalDateTime> reminderTimeProperty() { return reminderTime; }
    public ObjectProperty<RecurrenceRule> recurrenceRuleProperty() { return recurrenceRule; }
    public ObservableList<Tag> tagsList() { return tags; }
    public ObservableList<Reminder> remindersList() { return reminders; }
    public StringProperty formattedDateProperty() { return formattedDate; }
    public StringProperty formattedTimeProperty() { return formattedTime; }
    public StringProperty recurrenceSummaryProperty() { return recurrenceSummary; }

    // Value getters
    public String getId() { return id.get(); }
    public String getTitle() { return title.get(); }
    public String getDescription() { return description.get(); }
    public String getNotes() { return notes.get(); }
    public String getPriority() { return priority.get(); }
    public String getCategory() { return category.get(); }
    public boolean isCompleted() { return completed.get(); }
    public boolean isAllDay() { return allDay.get(); }
    public boolean isSuspended() { return suspended.get(); }
    public boolean hasRecurrence() { return hasRecurrence.get(); }
    public LocalDateTime getStartAt() { return startAt.get(); }
    public LocalDateTime getDueAt() { return dueAt.get(); }
    public LocalDateTime getEndAt() { return endAt.get(); }
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public LocalDateTime getUpdatedAt() { return updatedAt.get(); }
    public LocalDateTime getDeletedAt() { return deletedAt.get(); }
    public int getVersion() { return version.get(); }
    public String getSyncStatus() { return syncStatus.get(); }
    public String getDeviceId() { return deviceId.get(); }
    public LocalDateTime getReminderTime() { return reminderTime.get(); }
    public RecurrenceRule getRecurrenceRule() { return recurrenceRule.get(); }

    public void updateFormattedFields(String dateText, String timeText, String recurrenceText) {
        formattedDate.set(dateText != null ? dateText : "");
        formattedTime.set(timeText != null ? timeText : "");
        recurrenceSummary.set(recurrenceText != null ? recurrenceText : "");
    }

    private void syncBindings() {
        title.addListener((obs, oldVal, newVal) -> item.setTitle(newVal));
        description.addListener((obs, oldVal, newVal) -> item.setDescription(newVal));
        notes.addListener((obs, oldVal, newVal) -> item.setNotes(newVal));
        completed.addListener((obs, oldVal, newVal) -> item.setCompleted(newVal));
    }
}
