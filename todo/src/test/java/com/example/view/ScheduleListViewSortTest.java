package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.model.ScheduleItem;

class ScheduleListViewSortTest {

    @Test
    void completedSchedulesUseLastInFirstOutOrder() {
        ScheduleItem pending = schedule(1, "pending", false, null, LocalDateTime.of(2026, 4, 10, 23, 59), 2);
        ScheduleItem completedOlder = schedule(2, "completedOlder", true, LocalDateTime.of(2026, 4, 3, 10, 0), null, 2);
        ScheduleItem completedNewest = schedule(3, "completedNewest", true, LocalDateTime.of(2026, 4, 3, 12, 0), null, 2);

        List<ScheduleItem> schedules = new ArrayList<>(List.of(completedOlder, completedNewest, pending));
        schedules.sort(ScheduleListView.buildDisplayComparator());

        assertEquals(List.of(pending, completedNewest, completedOlder), schedules);
    }

    @Test
    void pendingSchedulesStayDueAtSorted() {
        ScheduleItem laterDue = schedule(11, "laterDue", false, LocalDateTime.of(2026, 4, 2, 10, 0), LocalDateTime.of(2026, 4, 12, 23, 59), 3);
        ScheduleItem earlierDue = schedule(12, "earlierDue", false, LocalDateTime.of(2026, 4, 3, 9, 0), LocalDateTime.of(2026, 4, 8, 23, 59), 1);
        ScheduleItem completed = schedule(13, "completed", true, LocalDateTime.of(2026, 4, 3, 13, 0), LocalDateTime.of(2026, 4, 9, 23, 59), 2);

        List<ScheduleItem> schedules = new ArrayList<>(List.of(laterDue, completed, earlierDue));
        schedules.sort(ScheduleListView.buildDisplayComparator());

        assertEquals(List.of(earlierDue, laterDue, completed), schedules);
    }

    @Test
    void myDayFilterOnlyIncludesSchedulesCoveringToday() {
        LocalDate today = LocalDate.of(2026, 4, 4);

        ScheduleItem spanningToday = datedSchedule(LocalDateTime.of(2026, 4, 3, 8, 0), LocalDateTime.of(2026, 4, 5, 18, 0));
        ScheduleItem dueToday = datedSchedule(null, LocalDateTime.of(2026, 4, 4, 23, 59));
        ScheduleItem startOnlyToday = datedSchedule(LocalDateTime.of(2026, 4, 4, 8, 0), null);
        ScheduleItem outsideToday = datedSchedule(LocalDateTime.of(2026, 4, 6, 8, 0), LocalDateTime.of(2026, 4, 8, 18, 0));
        ScheduleItem noDates = datedSchedule(null, null);

        assertTrue(ScheduleListView.matchesFilter(spanningToday, ScheduleListView.FILTER_MY_DAY, today));
        assertTrue(ScheduleListView.matchesFilter(dueToday, ScheduleListView.FILTER_MY_DAY, today));
        assertTrue(ScheduleListView.matchesFilter(startOnlyToday, ScheduleListView.FILTER_MY_DAY, today));
        assertFalse(ScheduleListView.matchesFilter(outsideToday, ScheduleListView.FILTER_MY_DAY, today));
        assertFalse(ScheduleListView.matchesFilter(noDates, ScheduleListView.FILTER_MY_DAY, today));
    }

    @Test
    void listDateTextUsesMinutePrecision() {
        ScheduleItem schedule = datedSchedule(LocalDateTime.of(2026, 4, 4, 9, 0), LocalDateTime.of(2026, 4, 4, 23, 59));

        assertEquals("04-04 09:00 - 23:59", ScheduleListView.buildScheduleDateText(schedule));
    }

    @Test
    void quickAddSuccessTextTrimsTitleAndHandlesNull() {
        assertEquals(
            "Created schedule: Sprint review",
            ScheduleListView.buildQuickAddSuccessText("Created schedule: {0}", "  Sprint review  ")
        );
        assertEquals(
            "Created schedule: ",
            ScheduleListView.buildQuickAddSuccessText("Created schedule: {0}", null)
        );
    }

    @Test
    void dueRelativeTextUsesExpectedThresholds() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 11, 12, 0);

        assertEquals("ends in >1 mo", ScheduleListView.buildDueRelativeText(now, now.plusDays(45), null));
        assertEquals("ended >1 mo ago", ScheduleListView.buildDueRelativeText(now, now.minusDays(45), null));

        assertEquals("ends in 3d", ScheduleListView.buildDueRelativeText(now, now.plusDays(3).plusHours(5), null));
        assertEquals("ended 3d ago", ScheduleListView.buildDueRelativeText(now, now.minusDays(3).minusHours(5), null));

        assertEquals("ends in 13h", ScheduleListView.buildDueRelativeText(now, now.plusHours(13), null));
        assertEquals("ended 13h ago", ScheduleListView.buildDueRelativeText(now, now.minusHours(13), null));

        assertEquals("ends in 5h 20m", ScheduleListView.buildDueRelativeText(now, now.plusHours(5).plusMinutes(20), null));
        assertEquals("ended 5h 20m ago", ScheduleListView.buildDueRelativeText(now, now.minusHours(5).minusMinutes(20), null));

        assertEquals("", ScheduleListView.buildDueRelativeText(now, null, null));
    }

    @Test
    void startRelativeTextOnlyUsesFutureAndMatchesThresholds() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 11, 12, 0);

        assertEquals("", ScheduleListView.buildStartRelativeText(now, now.minusMinutes(1), null));
        assertEquals("", ScheduleListView.buildStartRelativeText(now, now, null));

        assertEquals("starts in >1 mo", ScheduleListView.buildStartRelativeText(now, now.plusDays(45), null));
        assertEquals("starts in 3d", ScheduleListView.buildStartRelativeText(now, now.plusDays(3).plusHours(5), null));
        assertEquals("starts in 13h", ScheduleListView.buildStartRelativeText(now, now.plusHours(13), null));
        assertEquals("starts in 5h 20m", ScheduleListView.buildStartRelativeText(now, now.plusHours(5).plusMinutes(20), null));

        assertEquals("", ScheduleListView.buildStartRelativeText(now, null, null));
    }

    private ScheduleItem schedule(
        int id,
        String name,
        boolean completed,
        LocalDateTime updatedAt,
        LocalDateTime dueAt,
        int priorityValue
    ) {
        ScheduleItem schedule = new ScheduleItem();
        schedule.setId(String.valueOf(id));
        schedule.setName(name);
        schedule.setCompleted(completed);
        schedule.setUpdatedAt(updatedAt);
        schedule.setCreatedAt(updatedAt != null ? updatedAt.minusHours(1) : LocalDateTime.of(2026, 4, 1, 8, 0));
        schedule.setDueAt(dueAt);
        schedule.setPriority(resolvePriority(priorityValue));
        return schedule;
    }

    private ScheduleItem datedSchedule(LocalDateTime startAt, LocalDateTime dueAt) {
        ScheduleItem schedule = new ScheduleItem();
        schedule.setStartAt(startAt);
        schedule.setDueAt(dueAt);
        schedule.setPriority("中");
        return schedule;
    }

    private String resolvePriority(int priorityValue) {
        if (priorityValue >= 3) {
            return "高";
        }
        if (priorityValue == 1) {
            return "低";
        }
        return "中";
    }
}
