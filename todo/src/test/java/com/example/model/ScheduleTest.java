package com.example.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class ScheduleTest {

    @Test
    void legacyDateSettersProjectToMinutePrecisionBoundaries() {
        Schedule schedule = new Schedule();

        schedule.setStartDate(LocalDate.of(2026, 4, 5));
        schedule.setDueDate(LocalDate.of(2026, 4, 7));

        assertEquals(LocalDateTime.of(2026, 4, 5, 0, 0), schedule.getStartAt());
        assertEquals(LocalDateTime.of(2026, 4, 7, 23, 59), schedule.getDueAt());
        assertEquals(LocalDate.of(2026, 4, 5), schedule.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 7), schedule.getDueDate());
    }

    @Test
    void effectiveDateRangeFallsBackAndNormalizes() {
        Schedule dueOnly = new Schedule();
        dueOnly.setDueAt(LocalDateTime.of(2026, 4, 5, 23, 59));

        assertEquals(LocalDate.of(2026, 4, 5), dueOnly.getEffectiveStartDate());
        assertEquals(LocalDate.of(2026, 4, 5), dueOnly.getEffectiveEndDate());
        assertEquals(1, dueOnly.getEffectiveDurationDays());

        Schedule reversed = new Schedule();
        reversed.setStartAt(LocalDateTime.of(2026, 4, 8, 10, 0));
        reversed.setDueAt(LocalDateTime.of(2026, 4, 4, 9, 0));

        assertEquals(LocalDate.of(2026, 4, 4), reversed.getEffectiveStartDate());
        assertEquals(LocalDate.of(2026, 4, 8), reversed.getEffectiveEndDate());
        assertEquals(5, reversed.getEffectiveDurationDays());
    }

    @Test
    void includesDateUsesNormalizedEffectiveRange() {
        Schedule schedule = new Schedule();
        schedule.setStartAt(LocalDateTime.of(2026, 4, 6, 8, 30));
        schedule.setDueAt(LocalDateTime.of(2026, 4, 4, 19, 0));

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
    void overdueUsesMinutePrecision() {
        Schedule overdue = new Schedule();
        overdue.setDueAt(LocalDateTime.now().minusMinutes(1));

        Schedule upcoming = new Schedule();
        upcoming.setDueAt(LocalDateTime.now().plusMinutes(1));

        assertTrue(overdue.isOverdue());
        assertFalse(upcoming.isOverdue());
    }

    @Test
    void shortSchedulesAreUpcomingOnlyWhenDueToday() {
        Schedule dueToday = new Schedule();
        dueToday.setDueAt(LocalDate.now().atTime(23, 59));

        Schedule dueTomorrow = new Schedule();
        dueTomorrow.setDueAt(LocalDate.now().plusDays(1).atTime(23, 59));

        assertTrue(dueToday.isUpcoming());
        assertFalse(dueTomorrow.isUpcoming());
    }

    @Test
    void normalizesCategoryAndTagsForFutureSearch() {
        Schedule schedule = new Schedule();
        schedule.setCategory("   ");
        schedule.setTags("论文, 学习，复盘, 学习");

        assertEquals(Schedule.DEFAULT_CATEGORY, schedule.getCategory());
        assertEquals("论文, 学习, 复盘", schedule.getTags());
        assertEquals(List.of("论文", "学习", "复盘"), Schedule.splitTags(schedule.getTags()));
    }
    @Test
    void pinFlagPersistsInsideMetadataJsonWithoutDroppingOtherFields() {
        Schedule schedule = new Schedule();
        schedule.setMetadataJson("{\"foo\":1,\"pin\":false,\"bar\":\"x\"}");

        assertFalse(schedule.isPinned());

        schedule.setPinned(true);
        assertTrue(schedule.isPinned());
        assertTrue(schedule.getMetadataJson().contains("\"foo\":1"));
        assertTrue(schedule.getMetadataJson().contains("\"bar\":\"x\""));
        assertTrue(schedule.getMetadataJson().contains("\"pin\":true"));

        schedule.setPinned(false);
        assertFalse(schedule.isPinned());
        assertTrue(schedule.getMetadataJson().contains("\"foo\":1"));
        assertTrue(schedule.getMetadataJson().contains("\"bar\":\"x\""));
        assertFalse(schedule.getMetadataJson().contains("\"pin\""));
    }
}
