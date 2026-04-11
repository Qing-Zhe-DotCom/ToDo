package com.example.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.config.UserPreferencesStore;
import com.example.model.RecurrenceRule;
import com.example.model.Reminder;
import com.example.model.Schedule;

class ReminderToastPlannerTest {

    @Test
    void plansSingleToastForNonRecurringSchedule() {
        LocalizationService localizationService = serviceFor(AppLanguage.ENGLISH);
        ReminderToastPlanner planner = new ReminderToastPlanner(localizationService);
        ZoneId zone = ZoneId.of("Asia/Shanghai");

        Schedule schedule = new Schedule();
        schedule.setId("s1");
        schedule.setTitle("My Task");
        schedule.setTimezone(zone.getId());

        Reminder reminder = new Reminder(LocalDateTime.of(2026, 4, 11, 12, 1));
        reminder.setId("r1");
        schedule.setReminders(List.of(reminder));

        Instant now = ZonedDateTime.of(2026, 4, 11, 12, 0, 0, 0, zone).toInstant();
        List<PlannedToast> toasts = planner.plan(List.of(schedule), now, 1, 2000);

        assertEquals(1, toasts.size());
        PlannedToast toast = toasts.getFirst();
        assertEquals(ReminderToastPlanner.stableToastId("s1:r1"), toast.id());
        assertEquals("My Task", toast.title());
        assertTrue(toast.body().contains("Reminder time:"));
    }

    @Test
    void recurringSchedulesGenerateToastsPerOccurrenceWithinWindow() {
        LocalizationService localizationService = serviceFor(AppLanguage.ENGLISH);
        ReminderToastPlanner planner = new ReminderToastPlanner(localizationService);
        ZoneId zone = ZoneId.of("Asia/Shanghai");

        Schedule schedule = new Schedule();
        schedule.setId("s2");
        schedule.setTitle("Daily");
        schedule.setTimezone(zone.getId());
        schedule.setStartAt(LocalDateTime.of(2026, 4, 11, 9, 0));

        RecurrenceRule rule = new RecurrenceRule();
        rule.setActive(true);
        rule.setFrequency(RecurrenceRule.FREQ_DAILY);
        rule.setInterval(1);
        schedule.setRecurrenceRule(rule);

        Reminder reminder = new Reminder(LocalDateTime.of(2026, 4, 11, 8, 0));
        reminder.setId("r1");
        schedule.setReminders(List.of(reminder));

        Instant now = ZonedDateTime.of(2026, 4, 11, 7, 0, 0, 0, zone).toInstant();
        List<PlannedToast> toasts = planner.plan(List.of(schedule), now, 3, 2000);

        assertEquals(3, toasts.size());
        assertTrue(toasts.get(0).dueEpochMillis() < toasts.get(1).dueEpochMillis());
        assertTrue(toasts.get(1).dueEpochMillis() < toasts.get(2).dueEpochMillis());
    }

    @Test
    void offsetRemindersExtendProjectionRangeSoWindowIsNotMissed() {
        LocalizationService localizationService = serviceFor(AppLanguage.ENGLISH);
        ReminderToastPlanner planner = new ReminderToastPlanner(localizationService);
        ZoneId zone = ZoneId.of("Asia/Shanghai");

        Schedule schedule = new Schedule();
        schedule.setId("s3");
        schedule.setTitle("Offset");
        schedule.setTimezone(zone.getId());
        schedule.setStartAt(LocalDateTime.of(2026, 4, 1, 9, 0));

        RecurrenceRule rule = new RecurrenceRule();
        rule.setActive(true);
        rule.setFrequency(RecurrenceRule.FREQ_DAILY);
        rule.setInterval(1);
        schedule.setRecurrenceRule(rule);

        Reminder reminder = new Reminder(LocalDateTime.of(2026, 4, 3, 8, 0)); // +2 days from anchor
        reminder.setId("r1");
        schedule.setReminders(List.of(reminder));

        Instant now = ZonedDateTime.of(2026, 4, 11, 7, 0, 0, 0, zone).toInstant();
        List<PlannedToast> toasts = planner.plan(List.of(schedule), now, 2, 2000);

        long expected = LocalDateTime.of(2026, 4, 11, 8, 0).atZone(zone).toInstant().toEpochMilli();
        assertTrue(toasts.stream().anyMatch(toast -> toast.dueEpochMillis() == expected));
    }

    private LocalizationService serviceFor(AppLanguage language) {
        return new LocalizationService(new MapPreferencesStore(Map.of("todo.language", language.getId())));
    }

    private static final class MapPreferencesStore implements UserPreferencesStore {
        private final Map<String, String> values = new HashMap<>();

        private MapPreferencesStore(Map<String, String> seed) {
            if (seed != null) {
                values.putAll(seed);
            }
        }

        @Override
        public String get(String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }

        @Override
        public void put(String key, String value) {
            values.put(key, value);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }
    }
}

