package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.application.ProductivityConfig;
import com.example.model.Schedule;

class ScheduleListViewSortTest {

    @Test
    void completedSchedulesUseLastInFirstOutOrder() {
        Schedule pending = schedule(1, "pending", false, null, LocalDateTime.of(2026, 4, 10, 23, 59), 2);
        Schedule completedOlder = schedule(2, "completedOlder", true, LocalDateTime.of(2026, 4, 3, 10, 0), null, 2);
        Schedule completedNewest = schedule(3, "completedNewest", true, LocalDateTime.of(2026, 4, 3, 12, 0), null, 2);

        List<Schedule> schedules = new ArrayList<>(List.of(completedOlder, completedNewest, pending));
        schedules.sort(ScheduleListView.buildDisplayComparator(ProductivityConfig.SortMode.PRIORITY));

        assertEquals(List.of(pending, completedNewest, completedOlder), schedules);
    }

    @Test
    void pendingSchedulesFollowRemainingTimeSortedOrder() {
        Schedule laterDue = schedule(11, "laterDue", false, LocalDateTime.of(2026, 4, 2, 10, 0), LocalDateTime.of(2026, 4, 12, 23, 59), 3);
        Schedule earlierDue = schedule(12, "earlierDue", false, LocalDateTime.of(2026, 4, 3, 9, 0), LocalDateTime.of(2026, 4, 8, 23, 59), 1);
        Schedule completed = schedule(13, "completed", true, LocalDateTime.of(2026, 4, 3, 13, 0), LocalDateTime.of(2026, 4, 9, 23, 59), 2);

        List<Schedule> schedules = new ArrayList<>(List.of(laterDue, completed, earlierDue));
        schedules.sort(ScheduleListView.buildDisplayComparator(ProductivityConfig.SortMode.REMAINING_TIME));

        assertEquals(List.of(earlierDue, laterDue, completed), schedules);
    }

    @Test
    void pinnedPendingSchedulesAreRankedBeforeUnpinnedThenByDueDate() {
        Schedule pinnedLaterDue = schedule(21, "pinnedLaterDue", false, LocalDateTime.of(2026, 4, 2, 10, 0), LocalDateTime.of(2026, 4, 15, 23, 59), 2);
        pinnedLaterDue.setPinned(true);
        Schedule unpinnedEarlierDue = schedule(22, "unpinnedEarlierDue", false, LocalDateTime.of(2026, 4, 2, 10, 0), LocalDateTime.of(2026, 4, 8, 23, 59), 2);
        Schedule unpinnedLaterDue = schedule(23, "unpinnedLaterDue", false, LocalDateTime.of(2026, 4, 2, 10, 0), LocalDateTime.of(2026, 4, 12, 23, 59), 2);

        List<Schedule> schedules = new ArrayList<>(List.of(unpinnedLaterDue, unpinnedEarlierDue, pinnedLaterDue));
        schedules.sort(ScheduleListView.buildDisplayComparator(ProductivityConfig.SortMode.PRIORITY));

        assertEquals(List.of(pinnedLaterDue, unpinnedEarlierDue, unpinnedLaterDue), schedules);
    }

    @Test
    void prioritySortKeepsHigherPriorityAhead() {
        Schedule lowPriority = schedule(31, "lowPriority", false, LocalDateTime.of(2026, 4, 5, 9, 0), LocalDateTime.of(2026, 4, 18, 20, 0), 1);
        Schedule mediumPriority = schedule(32, "mediumPriority", false, LocalDateTime.of(2026, 4, 5, 10, 0), LocalDateTime.of(2026, 4, 19, 20, 0), 2);
        Schedule highPriority = schedule(33, "highPriority", false, LocalDateTime.of(2026, 4, 5, 11, 0), LocalDateTime.of(2026, 4, 20, 20, 0), 3);

        List<Schedule> schedules = new ArrayList<>(List.of(mediumPriority, lowPriority, highPriority));
        schedules.sort(ScheduleListView.buildDisplayComparator(ProductivityConfig.SortMode.PRIORITY));

        assertEquals(List.of(highPriority, mediumPriority, lowPriority), schedules);
    }

    @Test
    void createdTimeSortShowsNewestFirst() {
        Schedule older = schedule(41, "older", false, LocalDateTime.of(2026, 4, 1, 9, 0), LocalDateTime.of(2026, 4, 20, 20, 0), 2);
        Schedule newer = schedule(42, "newer", false, LocalDateTime.of(2026, 4, 3, 9, 0), LocalDateTime.of(2026, 4, 22, 20, 0), 2);
        older.setCreatedAt(LocalDateTime.of(2026, 4, 1, 8, 0));
        newer.setCreatedAt(LocalDateTime.of(2026, 4, 3, 8, 0));

        List<Schedule> schedules = new ArrayList<>(List.of(older, newer));
        schedules.sort(ScheduleListView.buildDisplayComparator(ProductivityConfig.SortMode.CREATED_TIME));

        assertEquals(List.of(newer, older), schedules);
    }

    @Test
    void myDayFilterOnlyIncludesSchedulesCoveringToday() {
        LocalDate today = LocalDate.of(2026, 4, 4);

        Schedule spanningToday = datedSchedule(LocalDateTime.of(2026, 4, 3, 8, 0), LocalDateTime.of(2026, 4, 5, 18, 0));
        Schedule dueToday = datedSchedule(null, LocalDateTime.of(2026, 4, 4, 23, 59));
        Schedule startOnlyToday = datedSchedule(LocalDateTime.of(2026, 4, 4, 8, 0), null);
        Schedule outsideToday = datedSchedule(LocalDateTime.of(2026, 4, 6, 8, 0), LocalDateTime.of(2026, 4, 8, 18, 0));
        Schedule noDates = datedSchedule(null, null);

        assertTrue(ScheduleListView.matchesFilter(spanningToday, ScheduleListView.FILTER_MY_DAY, today));
        assertTrue(ScheduleListView.matchesFilter(dueToday, ScheduleListView.FILTER_MY_DAY, today));
        assertTrue(ScheduleListView.matchesFilter(startOnlyToday, ScheduleListView.FILTER_MY_DAY, today));
        assertFalse(ScheduleListView.matchesFilter(outsideToday, ScheduleListView.FILTER_MY_DAY, today));
        assertFalse(ScheduleListView.matchesFilter(noDates, ScheduleListView.FILTER_MY_DAY, today));
    }

    @Test
    void listDateTextUsesMinutePrecision() {
        Schedule schedule = datedSchedule(LocalDateTime.of(2026, 4, 4, 9, 0), LocalDateTime.of(2026, 4, 4, 23, 59));

        assertEquals("04-04 09:00 - 23:59", ScheduleListView.buildScheduleDateText(schedule));
    }

    @Test
    void formatGroupCountWrapsCountWithParentheses() {
        assertEquals("(0)", ScheduleListView.formatGroupCount(0));
        assertEquals("(7)", ScheduleListView.formatGroupCount(7));
        assertEquals("(0)", ScheduleListView.formatGroupCount(-3));
    }

    private Schedule schedule(
        int id,
        String name,
        boolean completed,
        LocalDateTime updatedAt,
        LocalDateTime dueAt,
        int priorityValue
    ) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setName(name);
        schedule.setCompleted(completed);
        schedule.setUpdatedAt(updatedAt);
        schedule.setCreatedAt(updatedAt != null ? updatedAt.minusHours(1) : LocalDateTime.of(2026, 4, 1, 8, 0));
        schedule.setDueAt(dueAt);
        schedule.setPriority(resolvePriority(priorityValue));
        return schedule;
    }

    private Schedule datedSchedule(LocalDateTime startAt, LocalDateTime dueAt) {
        Schedule schedule = new Schedule();
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
