package com.example.application;

import com.example.model.Reminder;
import com.example.model.Schedule;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReminderToastPlanner {
    public static final int DEFAULT_WINDOW_DAYS = 365;
    public static final int DEFAULT_MAX_TOASTS = 2000;

    static final int MAX_OFFSET_DAYS = 366;
    static final String TIME_FORMAT_KEY = "format.trash.dateTime";

    private final LocalizationService localizationService;

    public ReminderToastPlanner(LocalizationService localizationService) {
        this.localizationService = Objects.requireNonNull(localizationService, "localizationService");
    }

    public List<PlannedToast> plan(List<Schedule> schedules) {
        return plan(schedules, Instant.now(), DEFAULT_WINDOW_DAYS, DEFAULT_MAX_TOASTS);
    }

    List<PlannedToast> plan(List<Schedule> schedules, Instant now, int windowDays, int maxToasts) {
        if (schedules == null || schedules.isEmpty()) {
            return List.of();
        }
        Instant nowInstant = now != null ? now : Instant.now();

        Map<String, PlannedToast> unique = new HashMap<>();
        for (Schedule schedule : schedules) {
            planSchedule(schedule, nowInstant, windowDays, unique);
        }

        List<PlannedToast> planned = new ArrayList<>(unique.values());
        planned.sort(Comparator.comparingLong(PlannedToast::dueEpochMillis));
        if (maxToasts > 0 && planned.size() > maxToasts) {
            return planned.subList(0, maxToasts);
        }
        return planned;
    }

    private void planSchedule(
        Schedule schedule,
        Instant nowInstant,
        int windowDays,
        Map<String, PlannedToast> sink
    ) {
        if (schedule == null || schedule.isDeleted() || schedule.isCompleted()) {
            return;
        }

        List<Reminder> reminders = schedule.getReminders();
        if (reminders == null || reminders.isEmpty()) {
            return;
        }

        ZoneId zone = resolveZone(schedule.getTimezone());
        ZonedDateTime nowInZone = nowInstant.atZone(zone);
        Instant windowEnd = nowInZone.plusDays(Math.max(1, windowDays)).toInstant();

        if (!schedule.hasRecurrence()) {
            collectToastsForOccurrence(schedule, reminders, zone, nowInstant, windowEnd, sink);
            return;
        }

        LocalDateTime anchorDateTime = resolveAnchorDateTime(schedule);
        if (anchorDateTime == null) {
            collectToastsForOccurrence(schedule, reminders, zone, nowInstant, windowEnd, sink);
            return;
        }

        OffsetRange offsets = computeOffsetRange(anchorDateTime, reminders);
        LocalDate planStart = nowInZone.toLocalDate();
        LocalDate planEnd = nowInZone.plusDays(Math.max(1, windowDays)).toLocalDate();

        LocalDate occurrenceStart = planStart.minusDays(offsets.maxOffsetDays());
        LocalDate occurrenceEnd = planEnd.minusDays(offsets.minOffsetDays());
        if (occurrenceEnd.isBefore(occurrenceStart)) {
            LocalDate tmp = occurrenceStart;
            occurrenceStart = occurrenceEnd;
            occurrenceEnd = tmp;
        }

        List<Schedule> projected = ScheduleOccurrenceProjector.projectForRange(
            List.of(schedule),
            occurrenceStart,
            occurrenceEnd,
            false
        );
        for (Schedule occurrence : projected) {
            if (occurrence == null || occurrence.isDeleted() || occurrence.isCompleted()) {
                continue;
            }
            ZoneId occurrenceZone = resolveZone(occurrence.getTimezone());
            collectToastsForOccurrence(occurrence, occurrence.getReminders(), occurrenceZone, nowInstant, windowEnd, sink);
        }
    }

    private void collectToastsForOccurrence(
        Schedule schedule,
        List<Reminder> reminders,
        ZoneId zone,
        Instant nowInstant,
        Instant windowEnd,
        Map<String, PlannedToast> sink
    ) {
        if (schedule == null || reminders == null || reminders.isEmpty()) {
            return;
        }
        String scheduleId = schedule.getId();
        String viewKey = schedule.getViewKey();
        String title = schedule.getTitle();
        if (title == null || title.isBlank()) {
            title = WindowsToastIds.TODO_APP_NAME;
        }

        for (Reminder reminder : reminders) {
            if (reminder == null || reminder.getRemindAtUtc() == null) {
                continue;
            }
            LocalDateTime remindAt = reminder.getRemindAtUtc();
            long dueEpochMillis = remindAt.atZone(zone).toInstant().toEpochMilli();
            if (dueEpochMillis <= nowInstant.toEpochMilli()) {
                continue;
            }
            if (dueEpochMillis > windowEnd.toEpochMilli()) {
                continue;
            }

            String reminderId = reminder.getId();
            String idSource = (viewKey == null ? "" : viewKey) + ":" + (reminderId == null ? "" : reminderId);
            String toastId = stableToastId(idSource);
            String timeText = localizationService.format(TIME_FORMAT_KEY, remindAt);
            String body = localizationService.text("notification.reminder.body", timeText);

            sink.put(
                toastId,
                new PlannedToast(toastId, dueEpochMillis, title, body, scheduleId, viewKey, reminderId)
            );
        }
    }

    private OffsetRange computeOffsetRange(LocalDateTime anchor, List<Reminder> reminders) {
        if (anchor == null || reminders == null || reminders.isEmpty()) {
            return new OffsetRange(0, 0);
        }
        LocalDate anchorDate = anchor.toLocalDate();
        int minOffset = 0;
        int maxOffset = 0;
        boolean sawAny = false;

        for (Reminder reminder : reminders) {
            if (reminder == null || reminder.getRemindAtUtc() == null) {
                continue;
            }
            long raw = ChronoUnit.DAYS.between(anchorDate, reminder.getRemindAtUtc().toLocalDate());
            int clamped = (int) Math.max(-MAX_OFFSET_DAYS, Math.min(MAX_OFFSET_DAYS, raw));
            if (!sawAny) {
                minOffset = clamped;
                maxOffset = clamped;
                sawAny = true;
            } else {
                minOffset = Math.min(minOffset, clamped);
                maxOffset = Math.max(maxOffset, clamped);
            }
        }

        if (!sawAny) {
            return new OffsetRange(0, 0);
        }
        return new OffsetRange(minOffset, maxOffset);
    }

    private ZoneId resolveZone(String zoneId) {
        try {
            String normalized = zoneId != null ? zoneId.trim() : "";
            if (!normalized.isEmpty()) {
                return ZoneId.of(normalized);
            }
        } catch (Exception ignored) {
        }
        return ZoneId.systemDefault();
    }

    private LocalDateTime resolveAnchorDateTime(Schedule schedule) {
        if (schedule == null) {
            return null;
        }
        if (schedule.getStartAt() != null) {
            return schedule.getStartAt();
        }
        if (schedule.getDueAt() != null) {
            return schedule.getDueAt();
        }
        return schedule.getEndAt();
    }

    static String stableToastId(String input) {
        String normalized = input == null ? "" : input;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return toHex(hash, 16);
        } catch (NoSuchAlgorithmException e) {
            // Fallback: should never happen on Java 21 runtimes.
            return Integer.toHexString(normalized.hashCode());
        }
    }

    private static String toHex(byte[] bytes, int maxLen) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
            if (maxLen > 0 && sb.length() >= maxLen) {
                break;
            }
        }
        if (maxLen > 0 && sb.length() > maxLen) {
            return sb.substring(0, maxLen);
        }
        return sb.toString();
    }

    private record OffsetRange(int minOffsetDays, int maxOffsetDays) {
    }
}

