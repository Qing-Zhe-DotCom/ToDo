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

    @Test
    void layoutSignatureIgnoresTinySizeNoise() {
        String signatureA = HeatmapView.buildLayoutSignature(
            "month",
            LocalDate.of(2026, 4, 1),
            false,
            812.2,
            503.2
        );
        String signatureB = HeatmapView.buildLayoutSignature(
            "month",
            LocalDate.of(2026, 4, 1),
            false,
            812.4,
            503.4
        );

        assertEquals(signatureA, signatureB);
    }

    @Test
    void viewportMustBeRenderableBeforeHeatmapUsesIt() {
        assertFalse(HeatmapView.hasRenderableViewport(0, 420));
        assertFalse(HeatmapView.hasRenderableViewport(640, 0));
        assertTrue(HeatmapView.hasRenderableViewport(640, 420));
    }

    @Test
    void monthCellSizingFitsWithinViewportWidth() {
        double cellSize = HeatmapView.calculateCalendarCellSize(
            900,
            620,
            20,
            20,
            28,
            7,
            6,
            3,
            4,
            2,
            18,
            90
        );

        double footprintWidth = HeatmapView.calculateCalendarFootprintWidth(
            cellSize,
            20,
            7,
            3,
            4,
            2
        );

        assertTrue(footprintWidth <= 900);
    }

    @Test
    void sidebarWidthMatchesCollapsedAndExpandedStates() {
        assertEquals(40, HeatmapView.resolveSidebarWidth(true));
        assertEquals(280, HeatmapView.resolveSidebarWidth(false));
    }

    @Test
    void yearMonthGridUsesFourByThreeLayoutOrder() {
        assertEquals(0, HeatmapView.resolveYearMonthColumn(1));
        assertEquals(0, HeatmapView.resolveYearMonthRow(1));
        assertEquals(3, HeatmapView.resolveYearMonthColumn(4));
        assertEquals(0, HeatmapView.resolveYearMonthRow(4));
        assertEquals(0, HeatmapView.resolveYearMonthColumn(5));
        assertEquals(1, HeatmapView.resolveYearMonthRow(5));
        assertEquals(3, HeatmapView.resolveYearMonthColumn(12));
        assertEquals(2, HeatmapView.resolveYearMonthRow(12));
        assertEquals("1月", HeatmapView.buildYearMonthTitle(LocalDate.of(2026, 1, 1)));
        assertEquals("12月", HeatmapView.buildYearMonthTitle(LocalDate.of(2026, 12, 1)));
    }
}
