package com.example.application;

public record PlannedToast(
    String id,
    long dueEpochMillis,
    String title,
    String body,
    String scheduleId,
    String viewKey,
    String reminderId
) {
}

