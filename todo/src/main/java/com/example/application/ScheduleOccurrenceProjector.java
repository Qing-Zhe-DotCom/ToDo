package com.example.application;

import com.example.model.RecurrenceRule;
import com.example.model.Reminder;
import com.example.model.Schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ScheduleOccurrenceProjector {
    private static final int MAX_PROJECTION_DAYS = 3650;

    private ScheduleOccurrenceProjector() {
    }

    public static List<Schedule> projectForRange(
        List<Schedule> schedules,
        LocalDate rangeStart,
        LocalDate rangeEnd,
        boolean keepNonRecurringOutsideRange
    ) {
        List<Schedule> projected = new ArrayList<>();
        if (schedules == null || schedules.isEmpty()) {
            return projected;
        }

        for (Schedule schedule : schedules) {
            if (schedule == null || schedule.isDeleted()) {
                continue;
            }
            if (!schedule.hasRecurrence()) {
                if (keepNonRecurringOutsideRange || intersects(schedule, rangeStart, rangeEnd)) {
                    projected.add(projectSingle(schedule, null));
                }
                continue;
            }
            projected.addAll(projectRecurring(schedule, rangeStart, rangeEnd));
        }
        return projected;
    }

    private static List<Schedule> projectRecurring(Schedule schedule, LocalDate rangeStart, LocalDate rangeEnd) {
        List<Schedule> projected = new ArrayList<>();
        RecurrenceRule rule = schedule.getRecurrenceRule();
        LocalDateTime anchorDateTime = resolveAnchorDateTime(schedule);
        if (rule == null || !rule.isActive() || anchorDateTime == null || rangeStart == null || rangeEnd == null) {
            projected.add(projectSingle(schedule, null));
            return projected;
        }

        LocalDate anchorDate = anchorDateTime.toLocalDate();
        LocalDate cursor = anchorDate;
        int emittedOccurrences = 0;
        int scannedDays = 0;
        Integer maxOccurrences = rule.getOccurrenceCount();

        while (!cursor.isAfter(rangeEnd) && scannedDays <= MAX_PROJECTION_DAYS) {
            if (matchesOccurrence(rule, anchorDate, cursor)) {
                LocalDateTime projectedAnchor = anchorDateTime.plusDays(ChronoUnit.DAYS.between(anchorDate, cursor));
                if (rule.getUntilAtUtc() != null && projectedAnchor.isAfter(rule.getUntilAtUtc())) {
                    break;
                }

                emittedOccurrences++;
                if (maxOccurrences != null && emittedOccurrences > maxOccurrences) {
                    break;
                }

                if (!cursor.isBefore(rangeStart)) {
                    projected.add(projectSingle(schedule, cursor));
                }
            }

            cursor = cursor.plusDays(1);
            scannedDays++;
        }
        return projected;
    }

    private static boolean matchesOccurrence(RecurrenceRule rule, LocalDate anchorDate, LocalDate candidateDate) {
        if (candidateDate.isBefore(anchorDate)) {
            return false;
        }

        int interval = Math.max(1, rule.getInterval());
        return switch (rule.getFrequency()) {
            case RecurrenceRule.FREQ_WEEKLY -> matchesWeekly(rule, anchorDate, candidateDate, interval);
            case RecurrenceRule.FREQ_MONTHLY -> matchesMonthly(rule, anchorDate, candidateDate, interval);
            case RecurrenceRule.FREQ_YEARLY -> matchesYearly(rule, anchorDate, candidateDate, interval);
            default -> matchesDaily(anchorDate, candidateDate, interval);
        };
    }

    private static boolean matchesDaily(LocalDate anchorDate, LocalDate candidateDate, int interval) {
        long days = ChronoUnit.DAYS.between(anchorDate, candidateDate);
        return days >= 0 && days % interval == 0;
    }

    private static boolean matchesWeekly(RecurrenceRule rule, LocalDate anchorDate, LocalDate candidateDate, int interval) {
        LocalDate anchorWeekStart = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate candidateWeekStart = candidateDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long weeks = ChronoUnit.WEEKS.between(anchorWeekStart, candidateWeekStart);
        if (weeks < 0 || weeks % interval != 0) {
            return false;
        }

        Set<DayOfWeek> byDays = new LinkedHashSet<>(rule.getByDays());
        if (byDays.isEmpty()) {
            byDays.add(anchorDate.getDayOfWeek());
        }
        return byDays.contains(candidateDate.getDayOfWeek());
    }

    private static boolean matchesMonthly(RecurrenceRule rule, LocalDate anchorDate, LocalDate candidateDate, int interval) {
        LocalDate anchorMonth = anchorDate.withDayOfMonth(1);
        LocalDate candidateMonth = candidateDate.withDayOfMonth(1);
        long months = ChronoUnit.MONTHS.between(anchorMonth, candidateMonth);
        if (months < 0 || months % interval != 0) {
            return false;
        }

        int targetDay = rule.getByMonthDay() != null ? rule.getByMonthDay() : anchorDate.getDayOfMonth();
        return targetDay > 0
            && targetDay <= candidateDate.lengthOfMonth()
            && candidateDate.getDayOfMonth() == targetDay;
    }

    private static boolean matchesYearly(RecurrenceRule rule, LocalDate anchorDate, LocalDate candidateDate, int interval) {
        LocalDate anchorYear = anchorDate.withDayOfYear(1);
        LocalDate candidateYear = candidateDate.withDayOfYear(1);
        long years = ChronoUnit.YEARS.between(anchorYear, candidateYear);
        if (years < 0 || years % interval != 0) {
            return false;
        }

        int targetDay = rule.getByMonthDay() != null ? rule.getByMonthDay() : anchorDate.getDayOfMonth();
        return candidateDate.getMonth() == anchorDate.getMonth()
            && targetDay > 0
            && targetDay <= candidateDate.lengthOfMonth()
            && candidateDate.getDayOfMonth() == targetDay;
    }

    private static Schedule projectSingle(Schedule schedule, LocalDate occurrenceDate) {
        Schedule projected = new Schedule(schedule);
        LocalDateTime anchorDateTime = resolveAnchorDateTime(schedule);
        LocalDate anchorDate = anchorDateTime != null ? anchorDateTime.toLocalDate() : null;
        long shiftDays = occurrenceDate == null || anchorDate == null
            ? 0
            : ChronoUnit.DAYS.between(anchorDate, occurrenceDate);

        if (shiftDays != 0) {
            if (schedule.getStartAt() != null) {
                projected.setStartAt(schedule.getStartAt().plusDays(shiftDays));
            }
            if (schedule.getEndAt() != null) {
                projected.setEndAt(schedule.getEndAt().plusDays(shiftDays));
            }
            if (schedule.getDueAt() != null) {
                projected.setDueAt(schedule.getDueAt().plusDays(shiftDays));
            }

            List<Reminder> shiftedReminders = new ArrayList<>();
            for (Reminder reminder : schedule.getReminders()) {
                Reminder shifted = reminder.copy();
                if (shifted.getRemindAtUtc() != null) {
                    shifted.setRemindAtUtc(shifted.getRemindAtUtc().plusDays(shiftDays));
                }
                shiftedReminders.add(shifted);
            }
            projected.setReminders(shiftedReminders);
        }

        String viewKey = occurrenceDate == null
            ? projected.getId()
            : projected.getId() + "@" + occurrenceDate;
        projected.setViewKey(viewKey);
        return projected;
    }

    private static boolean intersects(Schedule schedule, LocalDate rangeStart, LocalDate rangeEnd) {
        if (rangeStart == null || rangeEnd == null) {
            return true;
        }
        LocalDate start = schedule.getEffectiveStartDate();
        LocalDate end = schedule.getEffectiveEndDate();
        if (start == null || end == null) {
            return false;
        }
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        return !end.isBefore(rangeStart) && !start.isAfter(rangeEnd);
    }

    private static LocalDateTime resolveAnchorDateTime(Schedule schedule) {
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
}
