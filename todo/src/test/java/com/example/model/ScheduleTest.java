package com.example.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ScheduleTest {

    @Test
    void effectiveDateRangeFallsBackAndNormalizes() {
        Schedule dueOnly = new Schedule();
        dueOnly.setDueDate(LocalDate.of(2026, 4, 5));

        assertEquals(LocalDate.of(2026, 4, 5), dueOnly.getEffectiveStartDate());
        assertEquals(LocalDate.of(2026, 4, 5), dueOnly.getEffectiveEndDate());
        assertEquals(1, dueOnly.getEffectiveDurationDays());

        Schedule reversed = new Schedule();
        reversed.setStartDate(LocalDate.of(2026, 4, 8));
        reversed.setDueDate(LocalDate.of(2026, 4, 4));

        assertEquals(LocalDate.of(2026, 4, 4), reversed.getEffectiveStartDate());
        assertEquals(LocalDate.of(2026, 4, 8), reversed.getEffectiveEndDate());
        assertEquals(5, reversed.getEffectiveDurationDays());
    }

    @Test
    void includesDateUsesNormalizedEffectiveRange() {
        Schedule schedule = new Schedule();
        schedule.setStartDate(LocalDate.of(2026, 4, 6));
        schedule.setDueDate(LocalDate.of(2026, 4, 4));

        assertTrue(schedule.includesDate(LocalDate.of(2026, 4, 4)));
        assertTrue(schedule.includesDate(LocalDate.of(2026, 4, 5)));
        assertTrue(schedule.includesDate(LocalDate.of(2026, 4, 6)));
        assertFalse(schedule.includesDate(LocalDate.of(2026, 4, 3)));
        assertFalse(schedule.includesDate(LocalDate.of(2026, 4, 7)));
    }

    @Test
    void includesDateReturnsFalseWhenNoDatesExist() {
        Schedule schedule = new Schedule();

        assertNull(schedule.getEffectiveStartDate());
        assertNull(schedule.getEffectiveEndDate());
        assertEquals(0, schedule.getEffectiveDurationDays());
        assertFalse(schedule.includesDate(LocalDate.of(2026, 4, 4)));
    }

    @Test
    void shortSchedulesAreUpcomingOnlyWhenDueToday() {
        Schedule dueToday = new Schedule();
        dueToday.setDueDate(LocalDate.now());

        Schedule dueTomorrow = new Schedule();
        dueTomorrow.setDueDate(LocalDate.now().plusDays(1));

        assertTrue(dueToday.isUpcoming());
        assertFalse(dueTomorrow.isUpcoming());
    }

    @Test
    void mediumSchedulesUseThreeDayThreshold() {
        Schedule withinThreshold = new Schedule();
        withinThreshold.setStartDate(LocalDate.now().minusDays(4));
        withinThreshold.setDueDate(LocalDate.now().plusDays(3));

        Schedule outsideThreshold = new Schedule();
        outsideThreshold.setStartDate(LocalDate.now().minusDays(4));
        outsideThreshold.setDueDate(LocalDate.now().plusDays(4));

        assertTrue(withinThreshold.isUpcoming());
        assertFalse(outsideThreshold.isUpcoming());
    }

    @Test
    void longSchedulesUseSevenDayThreshold() {
        Schedule withinThreshold = new Schedule();
        withinThreshold.setStartDate(LocalDate.now().minusDays(40));
        withinThreshold.setDueDate(LocalDate.now().plusDays(7));

        Schedule outsideThreshold = new Schedule();
        outsideThreshold.setStartDate(LocalDate.now().minusDays(40));
        outsideThreshold.setDueDate(LocalDate.now().plusDays(8));

        assertTrue(withinThreshold.isUpcoming());
        assertFalse(outsideThreshold.isUpcoming());
    }

    @Test
    void completedOrMissingDeadlineSchedulesAreNeverUpcoming() {
        Schedule completed = new Schedule();
        completed.setDueDate(LocalDate.now());
        completed.setCompleted(true);

        Schedule missingDeadline = new Schedule();
        missingDeadline.setStartDate(LocalDate.now());

        assertFalse(completed.isUpcoming());
        assertFalse(missingDeadline.isUpcoming());
    }
}
