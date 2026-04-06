package com.example.model;

import java.util.ArrayList;
import java.util.List;

public final class ScheduleItemAggregate {
    private final ScheduleItem item;
    private final List<Tag> tags;
    private final List<Reminder> reminders;
    private final RecurrenceRule recurrenceRule;

    public ScheduleItemAggregate(ScheduleItem item) {
        this(
            item,
            item != null ? item.getTagObjects() : List.of(),
            item != null ? item.getReminders() : List.of(),
            item != null ? item.getRecurrenceRule() : null
        );
    }

    public ScheduleItemAggregate(
        ScheduleItem item,
        List<Tag> tags,
        List<Reminder> reminders,
        RecurrenceRule recurrenceRule
    ) {
        this.item = item != null ? item.copy() : new ScheduleItem();
        this.tags = copyTags(tags);
        this.reminders = copyReminders(reminders);
        this.recurrenceRule = recurrenceRule != null ? recurrenceRule.copy() : null;
    }

    public ScheduleItem getItem() {
        ScheduleItem copy = item.copy();
        copy.setTagObjects(tags);
        copy.setReminders(reminders);
        copy.setRecurrenceRule(recurrenceRule);
        return copy;
    }

    public List<Tag> getTags() {
        return copyTags(tags);
    }

    public List<Reminder> getReminders() {
        return copyReminders(reminders);
    }

    public RecurrenceRule getRecurrenceRule() {
        return recurrenceRule != null ? recurrenceRule.copy() : null;
    }

    private static List<Tag> copyTags(List<Tag> source) {
        List<Tag> copies = new ArrayList<>();
        if (source == null) {
            return copies;
        }
        for (Tag tag : source) {
            if (tag != null) {
                copies.add(tag.copy());
            }
        }
        return copies;
    }

    private static List<Reminder> copyReminders(List<Reminder> source) {
        List<Reminder> copies = new ArrayList<>();
        if (source == null) {
            return copies;
        }
        for (Reminder reminder : source) {
            if (reminder != null) {
                copies.add(reminder.copy());
            }
        }
        return copies;
    }
}
