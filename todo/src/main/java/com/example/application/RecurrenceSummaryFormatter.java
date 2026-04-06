package com.example.application;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.example.model.RecurrenceRule;

public final class RecurrenceSummaryFormatter {
    private RecurrenceSummaryFormatter() {
    }

    public static String describe(RecurrenceRule rule, LocalizationService localizationService) {
        if (rule == null || !rule.isActive()) {
            return localizationService.text("recurrence.none");
        }

        StringBuilder builder = new StringBuilder();
        switch (rule.getFrequency()) {
            case RecurrenceRule.FREQ_WEEKLY -> {
                builder.append(rule.getInterval() == 1
                    ? localizationService.text("recurrence.weekly.every")
                    : localizationService.text("recurrence.weekly.interval", rule.getInterval()));
                if (!rule.getByDays().isEmpty()) {
                    String days = rule.getByDays().stream()
                        .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                        .map(localizationService::weekdayShort)
                        .collect(Collectors.joining(localizationService.text("list.separator")));
                    builder.append(" ").append(days);
                }
            }
            case RecurrenceRule.FREQ_MONTHLY -> {
                builder.append(rule.getInterval() == 1
                    ? localizationService.text("recurrence.monthly.every")
                    : localizationService.text("recurrence.monthly.interval", rule.getInterval()));
                if (rule.getByMonthDay() != null) {
                    builder.append(" ").append(localizationService.text("recurrence.monthly.day", rule.getByMonthDay()));
                }
            }
            case RecurrenceRule.FREQ_YEARLY -> builder.append(
                rule.getInterval() == 1
                    ? localizationService.text("recurrence.yearly.every")
                    : localizationService.text("recurrence.yearly.interval", rule.getInterval())
            );
            default -> builder.append(
                rule.getInterval() == 1
                    ? localizationService.text("recurrence.daily.every")
                    : localizationService.text("recurrence.daily.interval", rule.getInterval())
            );
        }

        if (rule.getUntilAtUtc() != null) {
            DateTimeFormatter formatter = localizationService.formatter("format.recurrence.untilDate");
            builder.append(localizationService.text("recurrence.until", formatter.format(rule.getUntilAtUtc().toLocalDate())));
        } else if (rule.getOccurrenceCount() != null && rule.getOccurrenceCount() > 0) {
            builder.append(localizationService.text("recurrence.count", rule.getOccurrenceCount()));
        }

        return builder.toString();
    }
}
