package com.example.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import com.example.model.Schedule;

public final class ScheduleCompletionCoordinator {

    public interface MutationApplier {
        void applyOptimistic(ScheduleCompletionMutation mutation);

        void confirm(ScheduleCompletionMutation mutation);

        void revert(ScheduleCompletionMutation mutation);
    }

    public interface CompletionStatusWriter {
        boolean updateCompletion(int scheduleId, boolean targetCompleted) throws SQLException;
    }

    public interface ErrorReporter {
        void reportPersistenceFailure(Throwable throwable);
    }

    public final class PendingCompletion {
        private final ScheduleCompletionMutation mutation;
        private boolean committed;
        private boolean finished;

        private PendingCompletion(ScheduleCompletionMutation mutation) {
            this.mutation = mutation;
        }

        public ScheduleCompletionMutation getMutation() {
            return mutation;
        }

        public synchronized boolean commit() {
            if (finished || committed) {
                return false;
            }
            committed = true;
            activeMutations.put(mutation.getScheduleId(), mutation);
            mutationApplier.applyOptimistic(mutation);
            persistAsync(this);
            return true;
        }

        public synchronized void cancel() {
            if (finished || committed) {
                return;
            }
            finished = true;
            inFlightScheduleIds.remove(mutation.getScheduleId());
        }

        private synchronized boolean finishIfCommitted() {
            if (finished || !committed) {
                return false;
            }
            finished = true;
            inFlightScheduleIds.remove(mutation.getScheduleId());
            activeMutations.remove(mutation.getScheduleId());
            return true;
        }
    }

    private final CompletionStatusWriter statusWriter;
    private final MutationApplier mutationApplier;
    private final ErrorReporter errorReporter;
    private final Executor backgroundExecutor;
    private final Executor uiExecutor;
    private final Set<Integer> inFlightScheduleIds = ConcurrentHashMap.newKeySet();
    private final Map<Integer, ScheduleCompletionMutation> activeMutations = new ConcurrentHashMap<>();

    public ScheduleCompletionCoordinator(
        CompletionStatusWriter statusWriter,
        MutationApplier mutationApplier,
        ErrorReporter errorReporter,
        Executor backgroundExecutor,
        Executor uiExecutor
    ) {
        this.statusWriter = statusWriter;
        this.mutationApplier = mutationApplier;
        this.errorReporter = errorReporter;
        this.backgroundExecutor = backgroundExecutor;
        this.uiExecutor = uiExecutor;
    }

    public PendingCompletion prepare(Schedule schedule, boolean targetCompleted) {
        if (schedule == null || schedule.getId() <= 0) {
            return null;
        }
        if (!inFlightScheduleIds.add(schedule.getId())) {
            return null;
        }
        return new PendingCompletion(new ScheduleCompletionMutation(schedule, targetCompleted));
    }

    public boolean submitImmediate(Schedule schedule, boolean targetCompleted) {
        PendingCompletion pendingCompletion = prepare(schedule, targetCompleted);
        return pendingCompletion != null && pendingCompletion.commit();
    }

    public List<ScheduleCompletionMutation> snapshotCommittedMutations() {
        return new ArrayList<>(activeMutations.values());
    }

    private void persistAsync(PendingCompletion pendingCompletion) {
        CompletableFuture
            .supplyAsync(() -> persistMutation(pendingCompletion.getMutation()), backgroundExecutor)
            .whenComplete((updated, throwable) -> uiExecutor.execute(() -> {
                if (!pendingCompletion.finishIfCommitted()) {
                    return;
                }
                if (throwable != null || !Boolean.TRUE.equals(updated)) {
                    mutationApplier.revert(pendingCompletion.getMutation());
                    if (errorReporter != null) {
                        errorReporter.reportPersistenceFailure(throwable);
                    }
                    return;
                }
                mutationApplier.confirm(pendingCompletion.getMutation());
            }));
    }

    private boolean persistMutation(ScheduleCompletionMutation mutation) {
        try {
            return statusWriter.updateCompletion(mutation.getScheduleId(), mutation.isTargetCompleted());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
