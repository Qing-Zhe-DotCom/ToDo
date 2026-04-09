package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.model.Reminder;

class ScheduleDialogReminderRulesTest {

    @Test
    void normalizeReminderDraftsFiltersNullAndSortsAscending() {
        Reminder late = new Reminder(LocalDateTime.of(2026, 4, 20, 10, 0));
        Reminder early = new Reminder(LocalDateTime.of(2026, 4, 18, 9, 30));
        Reminder invalid = new Reminder();
        invalid.setRemindAtUtc(null);

        List<Reminder> normalized = ScheduleDialog.normalizeReminderDrafts(List.of(late, invalid, early));

        assertEquals(2, normalized.size());
        assertEquals(LocalDateTime.of(2026, 4, 18, 9, 30), normalized.get(0).getRemindAtUtc());
        assertEquals(LocalDateTime.of(2026, 4, 20, 10, 0), normalized.get(1).getRemindAtUtc());
    }

    @Test
    void defaultReminderSeedPrefersDueDateThenStartDate() {
        LocalDate due = LocalDate.of(2026, 5, 2);
        LocalDate start = LocalDate.of(2026, 4, 30);

        assertEquals(
            LocalDateTime.of(2026, 5, 2, 23, 59),
            ScheduleDialog.resolveDefaultReminderSeed(due, start)
        );
        assertEquals(
            LocalDateTime.of(2026, 4, 30, 9, 0),
            ScheduleDialog.resolveDefaultReminderSeed(null, start)
        );
        assertTrue(ScheduleDialog.resolveDefaultReminderSeed(null, null).toLocalTime().equals(java.time.LocalTime.of(9, 0)));
    }
}
