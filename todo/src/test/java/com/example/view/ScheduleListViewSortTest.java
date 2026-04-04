package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.model.Schedule;

class ScheduleListViewSortTest {

    @Test
    void completedSchedulesUseLastInFirstOutOrder() {
        Schedule pending = schedule(1, "pending", false, null, LocalDate.of(2026, 4, 10), 2);
        Schedule completedOlder = schedule(2, "completedOlder", true, LocalDateTime.of(2026, 4, 3, 10, 0), null, 2);
        Schedule completedNewest = schedule(3, "completedNewest", true, LocalDateTime.of(2026, 4, 3, 12, 0), null, 2);

        List<Schedule> schedules = new ArrayList<>(List.of(completedOlder, completedNewest, pending));
        schedules.sort(ScheduleListView.buildDisplayComparator());

        assertEquals(List.of(pending, completedNewest, completedOlder), schedules);
    }

    @Test
    void pendingSchedulesStayDateSortedAfterRemovingSortModes() {
        Schedule laterDue = schedule(11, "laterDue", false, LocalDateTime.of(2026, 4, 2, 10, 0), LocalDate.of(2026, 4, 12), 3);
        Schedule earlierDue = schedule(12, "earlierDue", false, LocalDateTime.of(2026, 4, 3, 9, 0), LocalDate.of(2026, 4, 8), 1);
        Schedule completed = schedule(13, "completed", true, LocalDateTime.of(2026, 4, 3, 13, 0), LocalDate.of(2026, 4, 9), 2);

        List<Schedule> schedules = new ArrayList<>(List.of(laterDue, completed, earlierDue));
        schedules.sort(ScheduleListView.buildDisplayComparator());

        assertEquals(List.of(earlierDue, laterDue, completed), schedules);
    }

    @Test
    void myDayFilterOnlyIncludesSchedulesCoveringToday() {
        LocalDate today = LocalDate.of(2026, 4, 4);

        Schedule spanningToday = datedSchedule(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 5));
        Schedule dueToday = datedSchedule(null, LocalDate.of(2026, 4, 4));
        Schedule startOnlyToday = datedSchedule(LocalDate.of(2026, 4, 4), null);
        Schedule outsideToday = datedSchedule(LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 8));
        Schedule noDates = datedSchedule(null, null);

        assertTrue(ScheduleListView.matchesFilter(spanningToday, ScheduleListView.FILTER_MY_DAY, today));
        assertTrue(ScheduleListView.matchesFilter(dueToday, ScheduleListView.FILTER_MY_DAY, today));
        assertTrue(ScheduleListView.matchesFilter(startOnlyToday, ScheduleListView.FILTER_MY_DAY, today));
        assertFalse(ScheduleListView.matchesFilter(outsideToday, ScheduleListView.FILTER_MY_DAY, today));
        assertFalse(ScheduleListView.matchesFilter(noDates, ScheduleListView.FILTER_MY_DAY, today));
    }

    @Test
    void allFilterDoesNotApplyLegacyDateWindow() {
        LocalDate today = LocalDate.of(2026, 4, 4);

        Schedule overduePending = datedSchedule(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 4, 1));
        overduePending.setCompleted(false);
        Schedule farFuture = datedSchedule(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 30));

        assertTrue(ScheduleListView.matchesFilter(overduePending, ScheduleListView.FILTER_ALL, today));
        assertTrue(ScheduleListView.matchesFilter(farFuture, ScheduleListView.FILTER_ALL, today));
    }

    @Test
    void overdueAndUpcomingFiltersReuseCurrentModelSemantics() {
        LocalDate today = LocalDate.of(2026, 4, 4);

        Schedule overdue = datedSchedule(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 3));
        overdue.setCompleted(false);
        Schedule upcoming = datedSchedule(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 4));
        upcoming.setCompleted(false);

        assertTrue(ScheduleListView.matchesFilter(overdue, ScheduleListView.FILTER_OVERDUE, today));
        assertTrue(ScheduleListView.matchesFilter(upcoming, ScheduleListView.FILTER_UPCOMING, today));
    }

    @Test
    void highPriorityFilterOnlyKeepsHighPrioritySchedules() {
        LocalDate today = LocalDate.of(2026, 4, 4);

        Schedule high = datedSchedule(LocalDate.of(2026, 4, 4), LocalDate.of(2026, 4, 4));
        high.setPriority("高");
        Schedule medium = datedSchedule(LocalDate.of(2026, 4, 4), LocalDate.of(2026, 4, 4));
        medium.setPriority("中");

        assertTrue(ScheduleListView.matchesFilter(high, ScheduleListView.FILTER_HIGH_PRIORITY, today));
        assertFalse(ScheduleListView.matchesFilter(medium, ScheduleListView.FILTER_HIGH_PRIORITY, today));
    }

    private Schedule schedule(
        int id,
        String name,
        boolean completed,
        LocalDateTime updatedAt,
        LocalDate dueDate,
        int priorityValue
    ) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setName(name);
        schedule.setCompleted(completed);
        schedule.setUpdatedAt(updatedAt);
        schedule.setCreatedAt(updatedAt != null ? updatedAt.minusHours(1) : LocalDateTime.of(2026, 4, 1, 8, 0));
        schedule.setDueDate(dueDate);
        schedule.setPriority(resolvePriority(priorityValue));
        return schedule;
    }

    private Schedule datedSchedule(LocalDate startDate, LocalDate dueDate) {
        Schedule schedule = new Schedule();
        schedule.setStartDate(startDate);
        schedule.setDueDate(dueDate);
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
