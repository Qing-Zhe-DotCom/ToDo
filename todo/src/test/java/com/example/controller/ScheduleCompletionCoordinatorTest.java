package com.example.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import com.example.model.Schedule;

class ScheduleCompletionCoordinatorTest {

    @Test
    void optimisticMutationConfirmsAfterSuccessfulPersistence() {
        RecordingApplier applier = new RecordingApplier();
        List<Throwable> errors = new ArrayList<>();
        ScheduleCompletionCoordinator coordinator = new ScheduleCompletionCoordinator(
            (scheduleId, targetCompleted) -> true,
            applier,
            errors::add,
            Runnable::run,
            Runnable::run
        );

        Schedule schedule = schedule(7, false);

        assertTrue(coordinator.submitImmediate(schedule, true));
        assertEquals(List.of("apply:7:true", "confirm:7:true"), applier.events);
        assertTrue(errors.isEmpty());
        assertTrue(coordinator.snapshotCommittedMutations().isEmpty());
    }

    @Test
    void optimisticMutationRevertsWhenPersistenceFails() {
        RecordingApplier applier = new RecordingApplier();
        List<Throwable> errors = new ArrayList<>();
        ScheduleCompletionCoordinator coordinator = new ScheduleCompletionCoordinator(
            (scheduleId, targetCompleted) -> {
                throw new SQLException("db down");
            },
            applier,
            errors::add,
            Runnable::run,
            Runnable::run
        );

        Schedule schedule = schedule(9, false);

        assertTrue(coordinator.submitImmediate(schedule, true));
        assertEquals(List.of("apply:9:true", "revert:9:true"), applier.events);
        assertEquals(1, errors.size());
        assertTrue(coordinator.snapshotCommittedMutations().isEmpty());
    }

    @Test
    void inFlightScheduleRejectsDuplicateMutationUntilSettled() {
        RecordingApplier applier = new RecordingApplier();
        ManualExecutor backgroundExecutor = new ManualExecutor();
        ScheduleCompletionCoordinator coordinator = new ScheduleCompletionCoordinator(
            (scheduleId, targetCompleted) -> true,
            applier,
            throwable -> { },
            backgroundExecutor,
            Runnable::run
        );

        Schedule schedule = schedule(11, false);
        ScheduleCompletionCoordinator.PendingCompletion pending = coordinator.prepare(schedule, true);

        assertNotNull(pending);
        assertTrue(pending.commit());
        assertEquals(1, coordinator.snapshotCommittedMutations().size());
        assertNull(coordinator.prepare(schedule, true));

        backgroundExecutor.runNext();

        assertEquals(List.of("apply:11:true", "confirm:11:true"), applier.events);
        assertTrue(coordinator.snapshotCommittedMutations().isEmpty());
        assertNotNull(coordinator.prepare(schedule, false));
    }

    private Schedule schedule(int id, boolean completed) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setCompleted(completed);
        schedule.setUpdatedAt(LocalDateTime.of(2026, 4, 3, 10, 0));
        return schedule;
    }

    private static final class RecordingApplier implements ScheduleCompletionCoordinator.MutationApplier {
        private final List<String> events = new ArrayList<>();

        @Override
        public void applyOptimistic(ScheduleCompletionMutation mutation) {
            events.add("apply:" + mutation.getScheduleId() + ":" + mutation.isTargetCompleted());
        }

        @Override
        public void confirm(ScheduleCompletionMutation mutation) {
            events.add("confirm:" + mutation.getScheduleId() + ":" + mutation.isTargetCompleted());
        }

        @Override
        public void revert(ScheduleCompletionMutation mutation) {
            events.add("revert:" + mutation.getScheduleId() + ":" + mutation.isTargetCompleted());
        }
    }

    private static final class ManualExecutor implements Executor {
        private final Deque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.addLast(command);
        }

        private void runNext() {
            Runnable task = tasks.pollFirst();
            assertNotNull(task);
            task.run();
        }
    }
}
