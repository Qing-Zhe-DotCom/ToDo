package com.example.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

final class ManualTimeInputParser {
    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-M-d H:mm"),
        DateTimeFormatter.ofPattern("yyyy-M-d HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd H:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    };
    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/M/d")
    };

    private static final TimePattern[] TIME_PATTERNS = new TimePattern[] {
        new TimePattern(DateTimeFormatter.ofPattern("H:mm"), false),
        new TimePattern(DateTimeFormatter.ofPattern("HH:mm"), false),
        new TimePattern(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH), true),
        new TimePattern(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH), true)
    };

    private ManualTimeInputParser() {
    }

    static Optional<LocalDateTime> parse(String input, LocalDateTime fallback) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        LocalDateTime base = fallback != null ? fallback : LocalDateTime.now();
        Optional<LocalDateTime> dateTime = tryParseDateTime(normalized);
        if (dateTime.isPresent()) {
            return dateTime;
        }
        Optional<LocalDate> dateOnly = tryParseDate(normalized);
        if (dateOnly.isPresent()) {
            return Optional.of(LocalDateTime.of(dateOnly.get(), base.toLocalTime()));
        }
        Optional<LocalTime> timeOnly = tryParseTime(normalized);
        if (timeOnly.isPresent()) {
            return Optional.of(LocalDateTime.of(base.toLocalDate(), timeOnly.get()));
        }
        return Optional.empty();
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.trim()
            .replace('：', ':')
            .replace('／', '/')
            .replace('　', ' ')
            .replace('\u00A0', ' ');
        return normalized.replaceAll("\\s+", " ");
    }

    private static Optional<LocalDateTime> tryParseDateTime(String text) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return Optional.of(LocalDateTime.parse(text, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }

    private static Optional<LocalDate> tryParseDate(String text) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(text, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }

    private static Optional<LocalTime> tryParseTime(String text) {
        for (TimePattern pattern : TIME_PATTERNS) {
            try {
                String candidate = pattern.uppercase() ? text.toUpperCase(Locale.ENGLISH) : text;
                return Optional.of(LocalTime.parse(candidate, pattern.formatter()));
            } catch (DateTimeParseException ignored) {
            }
        }
        return tryParseDigits(text);
    }

    private static Optional<LocalTime> tryParseDigits(String text) {
        if (!text.matches("\\d{1,4}")) {
            return Optional.empty();
        }
        int length = text.length();
        if (length <= 2) {
            int hour = Integer.parseInt(text);
            if (hour >= 0 && hour < 24) {
                return Optional.of(LocalTime.of(hour, 0));
            }
            return Optional.empty();
        }
        int minute = Integer.parseInt(text.substring(length - 2));
        int hour = Integer.parseInt(text.substring(0, length - 2));
        if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60) {
            return Optional.of(LocalTime.of(hour, minute));
        }
        return Optional.empty();
    }

    private record TimePattern(DateTimeFormatter formatter, boolean uppercase) {
    }
}
