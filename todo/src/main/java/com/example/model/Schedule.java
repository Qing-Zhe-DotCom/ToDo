package com.example.model;

import java.time.LocalDate;

@Deprecated
public class Schedule extends ScheduleItem {
    public Schedule() {
        super();
    }

    @Deprecated
    public void setId(int legacyId) {
        super.setId(String.valueOf(legacyId));
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
        super();
        setName(name);
        setDescription(description);
        setNotes(description);
        setStartDate(startDate);
        setDueDate(dueDate);
        setCompleted(completed);
        setPriority(priority);
        setCategory(category);
    }

    public Schedule(ScheduleItem source) {
        super(source);
    }
}
