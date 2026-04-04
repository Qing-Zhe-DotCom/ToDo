package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.model.Schedule;

class HeatmapViewTest {

    @Test
    void determineYearMonthColumnsRespondsToAvailableWidth() {
        assertEquals(4, HeatmapView.determineYearMonthColumns(1320));
        assertEquals(3, HeatmapView.determineYearMonthColumns(980));
        assertEquals(2, HeatmapView.determineYearMonthColumns(720));
        assertEquals(1, HeatmapView.determineYearMonthColumns(520));
    }

    @Test
    void layoutSizingHelpersPreserveCompactPanelAndCellBounds() {
        assertEquals(332.0, HeatmapView.determineDayPanelPreferredExtent(true), 0.001);
        assertEquals(132.0, HeatmapView.determineDayPanelPreferredExtent(false), 0.001);
        assertEquals(28.0, HeatmapView.clampGridCellSize("month", true, 12.0), 0.001);
        assertEquals(22.0, HeatmapView.clampGridCellSize("month", false, 12.0), 0.001);
        assertEquals(118.0, HeatmapView.clampGridCellSize("week", true, 200.0), 0.001);
    }

    @Test
    void buildSchedulesByDateIncludesEachCoveredDay() {
        Schedule spanningSchedule = new Schedule();
        spanningSchedule.setName("阶段任务");
        spanningSchedule.setStartDate(LocalDate.of(2026, 4, 1));
        spanningSchedule.setDueDate(LocalDate.of(2026, 4, 3));

        Schedule singleDaySchedule = new Schedule();
        singleDaySchedule.setName("单日任务");
        singleDaySchedule.setDueDate(LocalDate.of(2026, 4, 2));

        Map<LocalDate, List<Schedule>> grouped = HeatmapView.buildSchedulesByDate(
            List.of(spanningSchedule, singleDaySchedule),
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 5)
        );

        assertEquals(List.of(spanningSchedule), grouped.get(LocalDate.of(2026, 4, 1)));
        assertEquals(List.of(spanningSchedule, singleDaySchedule), grouped.get(LocalDate.of(2026, 4, 2)));
        assertEquals(List.of(spanningSchedule), grouped.get(LocalDate.of(2026, 4, 3)));
        assertFalse(grouped.containsKey(LocalDate.of(2026, 4, 4)));
    }

    @Test
    void scheduleOccursOnDateSupportsReversedAndSingleDates() {
        Schedule reversedRangeSchedule = new Schedule();
        reversedRangeSchedule.setStartDate(LocalDate.of(2026, 4, 5));
        reversedRangeSchedule.setDueDate(LocalDate.of(2026, 4, 3));

        Schedule singleDateSchedule = new Schedule();
        singleDateSchedule.setDueDate(LocalDate.of(2026, 4, 8));

        assertTrue(HeatmapView.scheduleOccursOnDate(reversedRangeSchedule, LocalDate.of(2026, 4, 4)));
        assertFalse(HeatmapView.scheduleOccursOnDate(reversedRangeSchedule, LocalDate.of(2026, 4, 6)));
        assertTrue(HeatmapView.scheduleOccursOnDate(singleDateSchedule, LocalDate.of(2026, 4, 8)));
    }

    @Test
    void dailyCompletionStatsUseUpdatedAtForCompletedSchedules() {
        Schedule completedToday = new Schedule();
        completedToday.setCompleted(true);
        completedToday.setUpdatedAt(LocalDateTime.of(2026, 4, 3, 9, 30));

        Schedule completedTomorrow = new Schedule();
        completedTomorrow.setCompleted(true);
        completedTomorrow.setUpdatedAt(LocalDateTime.of(2026, 4, 4, 11, 15));

        Schedule pending = new Schedule();
        pending.setCompleted(false);
        pending.setUpdatedAt(LocalDateTime.of(2026, 4, 3, 14, 0));

        Map<LocalDate, Integer> stats = HeatmapView.buildDailyCompletionStats(
            List.of(completedToday, completedTomorrow, pending),
            LocalDate.of(2026, 4, 3),
            LocalDate.of(2026, 4, 4)
        );

        assertEquals(1, stats.get(LocalDate.of(2026, 4, 3)));
        assertEquals(1, stats.get(LocalDate.of(2026, 4, 4)));
        assertEquals(2, stats.values().stream().mapToInt(Integer::intValue).sum());
    }
}
