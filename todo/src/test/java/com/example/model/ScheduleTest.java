package com.example.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.model.ScheduleItem;

class ScheduleTest {

    @Test
    void legacyDateSettersProjectToMinutePrecisionBoundaries() {
        ScheduleItem schedule = new ScheduleItem();

        schedule.setStartDate(LocalDate.of(2026, 4, 5));
        schedule.setDueDate(LocalDate.of(2026, 4, 7));

        assertEquals(LocalDateTime.of(2026, 4, 5, 0, 0), schedule.getStartAt());
        assertEquals(LocalDateTime.of(2026, 4, 7, 23, 59), schedule.getDueAt());
        assertEquals(LocalDate.of(2026, 4, 5), schedule.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 7), schedule.getDueDate());
    }

    @Test
    void effectiveDateRangeFallsBackAndNormalizes() {
        ScheduleItem dueOnly = new ScheduleItem();
        dueOnly.setDueAt(LocalDateTime.of(2026, 4, 5, 23, 59));

        assertEquals(LocalDate.of(2026, 4, 5), dueOnly.getEffectiveStartDate());
        assertEquals(LocalDate.of(2026, 4, 5), dueOnly.getEffectiveEndDate());
        assertEquals(1, dueOnly.getEffectiveDurationDays());

        ScheduleItem reversed = new ScheduleItem();
        reversed.setStartAt(LocalDateTime.of(2026, 4, 8, 10, 0));
        reversed.setDueAt(LocalDateTime.of(2026, 4, 4, 9, 0));

        assertEquals(LocalDate.of(2026, 4, 4), reversed.getEffectiveStartDate());
        assertEquals(LocalDate.of(2026, 4, 8), reversed.getEffectiveEndDate());
        assertEquals(5, reversed.getEffectiveDurationDays());
    }

    @Test
    void includesDateUsesNormalizedEffectiveRange() {
        ScheduleItem schedule = new ScheduleItem();
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
        ScheduleItem schedule = new ScheduleItem();

        assertNull(schedule.getEffectiveStartDate());
        assertNull(schedule.getEffectiveEndDate());
        assertEquals(0, schedule.getEffectiveDurationDays());
        assertFalse(schedule.includesDate(LocalDate.of(2026, 4, 4)));
    }

    @Test
    void overdueUsesMinutePrecision() {
        ScheduleItem overdue = new ScheduleItem();
        overdue.setDueAt(LocalDateTime.now().minusMinutes(1));

        ScheduleItem upcoming = new ScheduleItem();
        upcoming.setDueAt(LocalDateTime.now().plusMinutes(1));

        assertTrue(overdue.isOverdue());
        assertFalse(upcoming.isOverdue());
    }

    @Test
    void shortSchedulesAreUpcomingOnlyWhenDueToday() {
        ScheduleItem dueToday = new ScheduleItem();
        dueToday.setDueAt(LocalDate.now().atTime(23, 59));

        ScheduleItem dueTomorrow = new ScheduleItem();
        dueTomorrow.setDueAt(LocalDate.now().plusDays(1).atTime(23, 59));

        assertTrue(dueToday.isUpcoming());
        assertFalse(dueTomorrow.isUpcoming());
    }

    @Test
    void normalizesCategoryAndTagsForFutureSearch() {
        ScheduleItem schedule = new ScheduleItem();
        schedule.setCategory("   ");
        schedule.setTags("论文, 学习，复盘, 学习");

        assertEquals(ScheduleItem.DEFAULT_CATEGORY, schedule.getCategory());
        assertEquals("论文, 学习, 复盘", schedule.getTags());
        assertEquals(List.of("论文", "学习", "复盘"), ScheduleItem.splitTags(schedule.getTags()));
    }
}
