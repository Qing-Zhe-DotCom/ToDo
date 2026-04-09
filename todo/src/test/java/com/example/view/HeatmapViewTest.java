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
    void cornerIndicatorCountsSchedulesForDate() {
        LocalDate target = LocalDate.of(2026, 4, 1);
        Schedule firstCompleted = new Schedule();
        firstCompleted.setCompleted(true);
        Schedule secondCompleted = new Schedule();
        secondCompleted.setCompleted(true);
        Map<LocalDate, List<Schedule>> schedules = Map.of(
            target,
            List.of(firstCompleted, secondCompleted)
        );

        assertEquals(2, HeatmapView.countSchedulesForDate(schedules, target));
        assertEquals(0, HeatmapView.countSchedulesForDate(schedules, target.plusDays(1)));
    }

    @Test
    void countSchedulesForDateOnlyIncludesCompletedTasks() {
        LocalDate target = LocalDate.of(2026, 4, 5);
        Schedule completed = new Schedule();
        completed.setCompleted(true);
        Schedule pending = new Schedule();
        pending.setCompleted(false);

        Map<LocalDate, List<Schedule>> schedules = Map.of(target, List.of(completed, pending));

        assertEquals(1, HeatmapView.countSchedulesForDate(schedules, target));
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
    void monthCellSizingFitsWithinViewportHeight() {
        double cellSize = HeatmapView.calculateCalendarCellSize(
            900,
            620,
            20,
            20,
            28,
            7,
            HeatmapView.resolveMonthGridRows(),
            3,
            4,
            2,
            18,
            90
        );

        double footprintHeight = HeatmapView.calculateCalendarFootprintHeight(
            cellSize,
            20,
            28,
            HeatmapView.resolveMonthGridRows(),
            3,
            4,
            2
        );

        assertTrue(footprintHeight <= 620);
    }

    @Test
    void monthGridUsesFixedSixRowsForStableLayout() {
        assertEquals(6, HeatmapView.resolveMonthGridRows());
        assertEquals(22, HeatmapView.calculateCellFootprintSize(18, 4));
    }

    @Test
    void sidebarWidthMatchesCollapsedAndExpandedStates() {
        assertEquals(40, HeatmapView.resolveSidebarWidth(true));
        assertEquals(280, HeatmapView.resolveSidebarWidth(false));
    }

    @Test
    void sidebarToggleHostWidthMatchesCollapsedSidebar() {
        assertEquals(HeatmapView.resolveSidebarWidth(true), HeatmapView.resolveSidebarToggleHostWidth());
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
    @Test
    void heatmapLevelRespectsCustomThresholds() {
        int[] thresholds = {1, 3, 6};
        assertEquals(0, HeatmapView.resolveHeatmapLevel(0, thresholds));
        assertEquals(1, HeatmapView.resolveHeatmapLevel(1, thresholds));
        assertEquals(2, HeatmapView.resolveHeatmapLevel(3, thresholds));
        assertEquals(3, HeatmapView.resolveHeatmapLevel(6, thresholds));
        assertEquals(4, HeatmapView.resolveHeatmapLevel(7, thresholds));
    }

    @Test
    void legendLabelsFollowConfiguredThresholds() {
        assertEquals(
            List.of("0", "1-1", "2-3", "4-6", "7+"),
            HeatmapView.buildLegendLabels(new int[] {1, 3, 6})
        );
    }

    @Test
    void wheelNavigationDirectionUsesDominantAxisAndThreshold() {
        assertEquals(0, HeatmapView.resolveWheelNavigationDirection(40, 55));
        assertEquals(-1, HeatmapView.resolveWheelNavigationDirection(56, 55));
        assertEquals(1, HeatmapView.resolveWheelNavigationDirection(-56, 55));

        assertEquals(22, HeatmapView.resolveNavigationWheelDelta(22, 4));
        assertEquals(-30, HeatmapView.resolveNavigationWheelDelta(8, -30));
    }

    @Test
    void clampDateKeepsTodayWithinVisibleRange() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        LocalDate today = LocalDate.of(2024, 6, 15);

        assertEquals(today, HeatmapView.clampDateWithinRange(today, start, end));
        assertEquals(start, HeatmapView.clampDateWithinRange(today.minusDays(400), start, end));
        assertEquals(end, HeatmapView.clampDateWithinRange(today.plusDays(400), start, end));
    }
}
