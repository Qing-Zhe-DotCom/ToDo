package com.example.application;

import com.example.model.Schedule;
import com.example.model.ScheduleItem;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class ReminderNotificationService {
    private static final long DEFAULT_DEBOUNCE_MILLIS = 1500;

    private final ScheduleItemService scheduleItemService;
    private final LocalizationService localizationService;
    private final Consumer<String> warningReporter;
    private final ScheduledExecutorService executor;
    private final ReminderToastPlanner planner;
    private final WindowsToastScheduler windowsToastScheduler;

    private final Object lock = new Object();
    private ScheduledFuture<?> pendingResync;
    private boolean warnedOnce;

    public ReminderNotificationService(
        ScheduleItemService scheduleItemService,
        LocalizationService localizationService,
        Consumer<String> warningReporter
    ) {
        this.scheduleItemService = Objects.requireNonNull(scheduleItemService, "scheduleItemService");
        this.localizationService = Objects.requireNonNull(localizationService, "localizationService");
        this.warningReporter = warningReporter != null ? warningReporter : ignored -> {
        };
        this.planner = new ReminderToastPlanner(localizationService);
        this.windowsToastScheduler = new WindowsToastScheduler();
        this.executor = Executors.newSingleThreadScheduledExecutor(new ReminderThreadFactory());
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public void requestResync() {
        synchronized (lock) {
            if (pendingResync != null) {
                pendingResync.cancel(false);
            }
            pendingResync = executor.schedule(this::resyncNow, DEFAULT_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    private void resyncNow() {
        try {
            List<Schedule> schedules = loadSchedules();
            List<PlannedToast> planned = planner.plan(schedules, Instant.now(), ReminderToastPlanner.DEFAULT_WINDOW_DAYS, ReminderToastPlanner.DEFAULT_MAX_TOASTS);
            windowsToastScheduler.syncScheduledToasts(planned);
        } catch (Exception exception) {
            System.err.println("[todo] Reminder toast sync failed: " + exception.getMessage());
            if (!warnedOnce) {
                warnedOnce = true;
                warningReporter.accept(exception.getMessage());
            }
        }
    }

    private List<Schedule> loadSchedules() throws SQLException {
        List<ScheduleItem> items = scheduleItemService.getActiveScheduleItems();
        List<Schedule> schedules = new ArrayList<>();
        if (items == null) {
            return schedules;
        }
        for (ScheduleItem item : items) {
            if (item == null) {
                continue;
            }
            schedules.add(item instanceof Schedule schedule ? new Schedule(schedule) : new Schedule(item));
        }
        return schedules;
    }

    private static final class ReminderThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "todo-reminder-sync-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}

