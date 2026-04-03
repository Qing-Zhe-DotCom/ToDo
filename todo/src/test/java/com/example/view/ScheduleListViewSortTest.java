package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.model.Schedule;

class ScheduleListViewSortTest {

    @Test
    void completedSchedulesUseLastInFirstOutOrder() {
        Schedule pending = schedule(1, "pending", false, null, LocalDate.of(2026, 4, 10), 0);
        Schedule completedOlder = schedule(2, "completedOlder", true, LocalDateTime.of(2026, 4, 3, 10, 0), null, 0);
        Schedule completedNewest = schedule(3, "completedNewest", true, LocalDateTime.of(2026, 4, 3, 12, 0), null, 0);

        List<Schedule> schedules = new ArrayList<>(List.of(completedOlder, completedNewest, pending));
        schedules.sort(ScheduleListView.buildComparatorForSortIndex(0));

        assertEquals(List.of(pending, completedNewest, completedOlder), schedules);
    }

    @Test
    void completedStackOrderWinsOverPendingSortMode() {
        Schedule completedOlder = schedule(11, "older", true, LocalDateTime.of(2026, 4, 3, 9, 0), null, 0);
        Schedule completedNewest = schedule(12, "newer", true, LocalDateTime.of(2026, 4, 3, 13, 0), null, 0);
        Schedule pendingAlpha = schedule(13, "pendingAlpha", false, null, LocalDate.of(2026, 4, 9), 1);
        pendingAlpha.setCategory("A");
        Schedule pendingBeta = schedule(14, "pendingBeta", false, null, LocalDate.of(2026, 4, 11), 3);
        pendingBeta.setCategory("B");

        List<Schedule> schedules = new ArrayList<>(List.of(completedOlder, pendingBeta, completedNewest, pendingAlpha));
        schedules.sort(ScheduleListView.buildComparatorForSortIndex(2));

        assertEquals(List.of(pendingAlpha, pendingBeta, completedNewest, completedOlder), schedules);
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

    private String resolvePriority(int priorityValue) {
        if (priorityValue >= 3) {
            return "楂?";
        }
        if (priorityValue == 1) {
            return "浣?";
        }
        return "涓?";
    }
}
