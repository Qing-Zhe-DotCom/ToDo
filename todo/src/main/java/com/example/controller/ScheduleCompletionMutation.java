package com.example.controller;

import java.time.LocalDateTime;

import com.example.model.ScheduleItem;

public final class ScheduleCompletionMutation {
    private final String scheduleId;
    private final boolean previousCompleted;
    private final boolean targetCompleted;
    private final LocalDateTime previousUpdatedAt;
    private final LocalDateTime optimisticUpdatedAt;

    public ScheduleCompletionMutation(ScheduleItem schedule, boolean targetCompleted) {
        this(
            schedule != null ? schedule.getId() : null,
            schedule != null && schedule.isCompleted(),
            targetCompleted,
            schedule != null ? schedule.getUpdatedAt() : null,
            LocalDateTime.now()
        );
    }

    public ScheduleCompletionMutation(
        String scheduleId,
        boolean previousCompleted,
        boolean targetCompleted,
        LocalDateTime previousUpdatedAt,
        LocalDateTime optimisticUpdatedAt
    ) {
        this.scheduleId = scheduleId;
        this.previousCompleted = previousCompleted;
        this.targetCompleted = targetCompleted;
        this.previousUpdatedAt = previousUpdatedAt;
        this.optimisticUpdatedAt = optimisticUpdatedAt != null ? optimisticUpdatedAt : LocalDateTime.now();
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public boolean isPreviousCompleted() {
        return previousCompleted;
    }

    public boolean isTargetCompleted() {
        return targetCompleted;
    }

    public LocalDateTime getPreviousUpdatedAt() {
        return previousUpdatedAt;
    }

    public LocalDateTime getOptimisticUpdatedAt() {
        return optimisticUpdatedAt;
    }

    public int pendingCountDelta() {
        if (previousCompleted == targetCompleted) {
            return 0;
        }
        return targetCompleted ? -1 : 1;
    }

    public void applyTo(ScheduleItem schedule) {
        if (!matches(schedule)) {
            return;
        }
        schedule.setCompleted(targetCompleted);
        schedule.setUpdatedAt(optimisticUpdatedAt);
    }

    public void revertOn(ScheduleItem schedule) {
        if (!matches(schedule)) {
            return;
        }
        schedule.setCompleted(previousCompleted);
        schedule.setUpdatedAt(previousUpdatedAt);
    }

    public boolean matches(ScheduleItem schedule) {
        return schedule != null && scheduleId != null && scheduleId.equals(schedule.getId());
    }
}
