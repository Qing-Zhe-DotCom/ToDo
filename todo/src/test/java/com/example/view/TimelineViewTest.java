package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.application.IconKey;
import com.example.application.WheelModifier;
import com.example.model.Schedule;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

class TimelineViewTest {

    @BeforeAll
    static void initToolkit() {
        new JFXPanel();
    }

    @Test
    void resolveTimelineRangeHandlesMissingBoundariesAndMinutePrecision() {
        Schedule s1 = new Schedule();
        s1.setStartAt(LocalDateTime.of(2023, 10, 1, 10, 5));
        s1.setDueAt(LocalDateTime.of(2023, 10, 5, 18, 0));

        assertEquals(LocalDateTime.of(2023, 10, 1, 10, 5), TimelineView.resolveTimelineStartAt(s1));
        assertEquals(LocalDateTime.of(2023, 10, 5, 18, 0), TimelineView.resolveTimelineEndAt(s1));
        assertEquals(LocalDate.of(2023, 10, 1), TimelineView.resolveTimelineStartDate(s1));
        assertEquals(LocalDate.of(2023, 10, 5), TimelineView.resolveTimelineEndDate(s1));

        Schedule dueOnly = new Schedule();
        dueOnly.setDueAt(LocalDateTime.of(2023, 10, 5, 12, 34));
        assertEquals(LocalDateTime.of(2023, 10, 5, 0, 0), TimelineView.resolveTimelineStartAt(dueOnly));
        assertEquals(LocalDateTime.of(2023, 10, 5, 12, 34), TimelineView.resolveTimelineEndAt(dueOnly));

        Schedule startOnly = new Schedule();
        startOnly.setStartAt(LocalDateTime.of(2023, 10, 1, 9, 0));
        assertEquals(LocalDateTime.of(2023, 10, 1, 9, 0), TimelineView.resolveTimelineStartAt(startOnly));
        assertEquals(LocalDateTime.of(2023, 10, 1, 23, 59), TimelineView.resolveTimelineEndAt(startOnly));
    }

    @Test
    void formatRangeDateUsesIsoLikeDisplayFormat() {
        assertEquals("", TimelineView.formatRangeDate(null));
        assertEquals("2026-04-04", TimelineView.formatRangeDate(LocalDate.of(2026, 4, 4)));
    }

    @Test
    void parseRangeDateSupportsEmptyAndFormattedValue() {
        assertNull(TimelineView.parseRangeDate(""));
        assertNull(TimelineView.parseRangeDate("   "));
        assertEquals(LocalDate.of(2026, 4, 4), TimelineView.parseRangeDate("2026-04-04"));
    }

    @Test
    void ctrlWheelDecisionPrefersZoomOverHorizontalScroll() {
        TimelineView.ScrollDecision zoomIn = TimelineView.resolveScrollDecision(
            true,
            0,
            120,
            0.5,
            420,
            2_400,
            0.5
        );
        assertEquals(TimelineView.ScrollAction.ZOOM_IN, zoomIn.action());

        TimelineView.ScrollDecision zoomOut = TimelineView.resolveScrollDecision(
            true,
            0,
            -120,
            0.5,
            420,
            2_400,
            0.5
        );
        assertEquals(TimelineView.ScrollAction.ZOOM_OUT, zoomOut.action());
    }

    @Test
    void plainWheelDecisionScrollsHorizontallyAndWideViewportStaysStable() {
        TimelineView.ScrollDecision scroll = TimelineView.resolveScrollDecision(
            false,
            0,
            120,
            0.5,
            400,
            2_000,
            0.5
        );
        assertEquals(TimelineView.ScrollAction.HORIZONTAL_SCROLL, scroll.action());
        assertTrue(scroll.targetHvalue() < 0.5);

        TimelineView.ScrollDecision wideViewport = TimelineView.resolveScrollDecision(
            false,
            0,
            120,
            0.5,
            400,
            320,
            0.5
        );
        assertEquals(TimelineView.ScrollAction.HORIZONTAL_SCROLL, wideViewport.action());
        assertEquals(0.0, wideViewport.targetHvalue());
    }

    @Test
    void zoomAnchorPreservesViewportCenterAcrossScaleChanges() {
        long totalMinutes = 21L * 24 * 60;
        long anchorMinuteOffset = 8L * 24 * 60 + 135;
        double viewportWidth = 640;
        double pixelsPerMinuteBefore = 0.6;
        double totalWidthBefore = TimelineView.computeTimelineWidth(totalMinutes, pixelsPerMinuteBefore);

        double hvalueBefore = TimelineView.computeAnchoredHvalue(
            anchorMinuteOffset,
            totalWidthBefore,
            pixelsPerMinuteBefore,
            viewportWidth,
            0.5
        );
        assertEquals(
            anchorMinuteOffset,
            TimelineView.resolveViewportCenterMinuteOffset(
                viewportWidth,
                totalWidthBefore,
                hvalueBefore,
                pixelsPerMinuteBefore,
                totalMinutes
            )
        );

        double accidentallyScrolledHvalue = TimelineView.computeHorizontalScrollHvalue(
            hvalueBefore,
            120,
            viewportWidth,
            totalWidthBefore,
            pixelsPerMinuteBefore
        );
        assertNotEquals(
            anchorMinuteOffset,
            TimelineView.resolveViewportCenterMinuteOffset(
                viewportWidth,
                totalWidthBefore,
                accidentallyScrolledHvalue,
                pixelsPerMinuteBefore,
                totalMinutes
            )
        );

        double pixelsPerMinuteAfter = pixelsPerMinuteBefore * 1.12;
        double totalWidthAfter = TimelineView.computeTimelineWidth(totalMinutes, pixelsPerMinuteAfter);
        double hvalueAfter = TimelineView.computeAnchoredHvalue(
            anchorMinuteOffset,
            totalWidthAfter,
            pixelsPerMinuteAfter,
            viewportWidth,
            0.5
        );
        assertEquals(
            anchorMinuteOffset,
            TimelineView.resolveViewportCenterMinuteOffset(
                viewportWidth,
                totalWidthAfter,
                hvalueAfter,
                pixelsPerMinuteAfter,
                totalMinutes
            )
        );
    }

    @Test
    void horizontalWheelScrollMovesAcrossTimelineAndClampsAtEdges() {
        double viewportWidth = 400;
        double contentWidth = 2_000;
        double pixelsPerMinute = 0.5;

        assertTrue(
            TimelineView.computeHorizontalScrollHvalue(0.5, 120, viewportWidth, contentWidth, pixelsPerMinute) < 0.5
        );
        assertTrue(
            TimelineView.computeHorizontalScrollHvalue(0.5, -120, viewportWidth, contentWidth, pixelsPerMinute) > 0.5
        );
        assertEquals(
            0.0,
            TimelineView.computeHorizontalScrollHvalue(0.01, 120, viewportWidth, contentWidth, pixelsPerMinute)
        );
        assertEquals(
            1.0,
            TimelineView.computeHorizontalScrollHvalue(0.99, -120, viewportWidth, contentWidth, pixelsPerMinute)
        );
    }

    @Test
    void anchoredHvalueClampsWhenViewportIsWideOrAnchorIsOutOfRange() {
        long totalMinutes = 120;
        double pixelsPerMinute = 1.0;
        double totalWidth = TimelineView.computeTimelineWidth(totalMinutes, pixelsPerMinute);

        assertEquals(0.0, TimelineView.computeAnchoredHvalue(30, totalWidth, pixelsPerMinute, totalWidth + 50, 0.5));
        assertEquals(0.0, TimelineView.computeAnchoredHvalue(0, totalWidth, pixelsPerMinute, 80, 0.5));
        assertEquals(1.0, TimelineView.computeAnchoredHvalue(totalMinutes, totalWidth, pixelsPerMinute, 80, 0.5));
        assertEquals(1.0, TimelineView.computeAnchoredHvalue(totalMinutes + 500, totalWidth, pixelsPerMinute, 80, 0.5));
    }

    @Test
    void ctrlWheelScrollEventIsConsumedAndKeepsViewportCenterStable() throws Exception {
        Schedule schedule = new Schedule();
        schedule.setName("Quarterly plan");
        schedule.setPriority(Schedule.PRIORITY_MEDIUM);
        schedule.setStartAt(LocalDateTime.of(2026, 4, 1, 9, 0));
        schedule.setDueAt(LocalDateTime.of(2026, 4, 5, 18, 0));

        long totalMinutes = ChronoUnit.MINUTES.between(
            TimelineView.resolveTimelineStartDate(schedule).minusDays(3).atStartOfDay(),
            TimelineView.resolveTimelineEndDate(schedule).plusDays(8).atStartOfDay()
        );

        TestTimelineController controller = new TestTimelineController(List.of(schedule));
        AtomicReference<TimelineView> viewRef = new AtomicReference<>();
        AtomicReference<StackPane> hostRef = new AtomicReference<>();

        runOnFxThreadAndWait(() -> {
            TimelineView view = new TimelineView(controller, false);
            StackPane host = new StackPane(view.getView());
            host.setPrefSize(1_120, 720);
            new Scene(host, 1_120, 720);
            host.applyCss();
            host.layout();

            viewRef.set(view);
            hostRef.set(host);
        });

        runOnFxThreadAndWait(() -> viewRef.get().refresh());
        waitForFxEvents(3);

        runOnFxThreadAndWait(() -> {
            StackPane host = hostRef.get();
            TimelineView view = viewRef.get();
            host.applyCss();
            host.layout();

            ScrollPane scrollPane = findNode(host, ScrollPane.class, "timeline-scroll");
            Region timelinePane = findNode(host, Region.class, "timeline-pane");
            assertNotNull(scrollPane);
            assertNotNull(timelinePane);

            scrollPane.setHvalue(0.5);
            host.applyCss();
            host.layout();

            double viewportWidthBefore = scrollPane.getViewportBounds().getWidth();
            assertTrue(viewportWidthBefore > 0, "Expected rendered viewport width");

            double pixelsPerMinuteBefore = TimelineView.computePixelsPerMinute(view.getZoomFactor());
            double contentWidthBefore = resolveContentWidth(timelinePane);
            long centerMinuteBefore = TimelineView.resolveViewportCenterMinuteOffset(
                viewportWidthBefore,
                contentWidthBefore,
                scrollPane.getHvalue(),
                pixelsPerMinuteBefore,
                totalMinutes
            );

            AtomicReference<Boolean> handlerReached = new AtomicReference<>(false);
            scrollPane.addEventHandler(ScrollEvent.SCROLL, ignored -> handlerReached.set(true));
            ScrollEvent event = createWheelEvent(scrollPane, true, 120);
            Event.fireEvent(scrollPane, event);
            host.applyCss();
            host.layout();

            double pixelsPerMinuteAfter = TimelineView.computePixelsPerMinute(view.getZoomFactor());
            double contentWidthAfter = resolveContentWidth(timelinePane);
            long centerMinuteAfter = TimelineView.resolveViewportCenterMinuteOffset(
                scrollPane.getViewportBounds().getWidth(),
                contentWidthAfter,
                scrollPane.getHvalue(),
                pixelsPerMinuteAfter,
                totalMinutes
            );

            assertTrue(!handlerReached.get(), "Expected Ctrl + wheel event to be consumed before later handlers");
            assertTrue(view.getZoomFactor() > 1.0, "Expected zoom factor to increase after Ctrl + wheel");
            assertTrue(
                Math.abs(centerMinuteAfter - centerMinuteBefore) <= 1,
                "Expected viewport center to stay stable, before=" + centerMinuteBefore + ", after=" + centerMinuteAfter
            );
        });
    }

    private static double resolveContentWidth(Region timelinePane) {
        return Math.max(
            0,
            Math.max(
                timelinePane.getWidth(),
                Math.max(timelinePane.getLayoutBounds().getWidth(), timelinePane.getPrefWidth())
            )
        );
    }

    private static ScrollEvent createWheelEvent(Node target, boolean controlDown, double deltaY) {
        return new ScrollEvent(
            ScrollEvent.SCROLL,
            120,
            120,
            120,
            120,
            false,
            controlDown,
            false,
            false,
            false,
            false,
            0,
            deltaY,
            0,
            deltaY,
            ScrollEvent.HorizontalTextScrollUnits.NONE,
            0,
            ScrollEvent.VerticalTextScrollUnits.NONE,
            0,
            0,
            new PickResult(target, 120, 120)
        );
    }

    private static void waitForFxEvents(int rounds) throws Exception {
        for (int i = 0; i < rounds; i++) {
            runOnFxThreadAndWait(() -> {
            });
        }
    }

    private static void runOnFxThreadAndWait(FxRunnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for JavaFX thread");
        Throwable throwable = failure.get();
        if (throwable == null) {
            return;
        }
        if (throwable instanceof Exception exception) {
            throw exception;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        throw new RuntimeException(throwable);
    }

    private static <T extends Node> T findNode(Node root, Class<T> type, String styleClass) {
        if (type.isInstance(root) && root.getStyleClass().contains(styleClass)) {
            return type.cast(root);
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findNode(child, type, styleClass);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface FxRunnable {
        void run() throws Exception;
    }

    private static final class TestTimelineController implements TimelineView.TimelineController {
        private final List<Schedule> schedules;

        private TestTimelineController(List<Schedule> schedules) {
            this.schedules = schedules;
        }

        @Override
        public WheelModifier getTimelineZoomWheelModifier() {
            return WheelModifier.CTRL;
        }

        @Override
        public Node createSvgIcon(IconKey iconKey, String title, double size) {
            Region icon = new Region();
            icon.setMinSize(size, size);
            icon.setPrefSize(size, size);
            return icon;
        }

        @Override
        public Label createHeaderClockLabel() {
            return new Label("clock");
        }

        @Override
        public String text(String key, Object... args) {
            return key;
        }

        @Override
        public List<Schedule> applyPendingCompletionMutations(List<Schedule> schedules) {
            return schedules;
        }

        @Override
        public List<Schedule> loadAllSchedules() {
            return schedules;
        }

        @Override
        public String getCurrentScheduleCardStyle() {
            return ScheduleCardStyleSupport.getDefaultStyleId();
        }
    }
}
