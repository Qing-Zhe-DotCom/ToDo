package com.example.view;

import java.util.List;
import java.util.function.Predicate;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

public class ScheduleStatusControl extends StackPane {

    public enum SizePreset {
        LIST("schedule-status-size-list", 24.0),
        TIMELINE("schedule-status-size-timeline", 18.0),
        HEATMAP("schedule-status-size-heatmap", 22.0),
        DETAIL("schedule-status-size-detail", 26.0);

        private final String styleClassName;
        private final double pixelSize;

        SizePreset(String styleClassName, double pixelSize) {
            this.styleClassName = styleClassName;
            this.pixelSize = pixelSize;
        }

        public String getStyleClassName() {
            return styleClassName;
        }

        public double getPixelSize() {
            return pixelSize;
        }
    }

    private static final Duration HOLD_THRESHOLD = Duration.millis(250);
    private static final Duration FORWARD_DURATION = Duration.millis(480);
    private static final Duration ROLLBACK_DURATION = Duration.millis(180);
    private static final Duration CHECK_POP_DURATION = Duration.millis(188);
    private static final Duration CHECK_SETTLE_DURATION = Duration.millis(248);

    private static final double VIEWBOX_SIZE = 100.0;
    private static final double CENTER = 50.0;
    private static final double RADIUS = 34.0;
    private static final double STROKE_WIDTH = 16.0;
    private static final double SHADOW_OFFSET_Y = 4.0;
    private static final double SEGMENT_SWEEP = 120.0;
    private final Predicate<Boolean> commitHandler;
    private final ScheduleStatusInteractionModel interactionModel;
    private final Arc staticSegmentA;
    private final Arc staticSegmentB;
    private final Arc staticSegmentC;
    private final Group checkGroup;
    private final Circle rippleCircle;
    private final Arc progressSegmentA;
    private final Arc progressSegmentB;
    private final Arc progressSegmentC;
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);

    private PauseTransition holdTransition;
    private Timeline forwardTimeline;
    private Timeline rollbackTimeline;
    private ParallelTransition completionTransition;
    private ParallelTransition reverseTransition;

    private boolean completed;
    private boolean holdThresholdReached;
    private boolean suppressCompletedClick;
    private boolean transitionBusy;

    public ScheduleStatusControl(
        boolean completed,
        SizePreset sizePreset,
        String roleStyleClass,
        Predicate<Boolean> commitHandler
    ) {
        this.completed = completed;
        this.commitHandler = commitHandler;
        this.interactionModel = new ScheduleStatusInteractionModel(completed);

        getStyleClass().add("schedule-status-control");
        getStyleClass().add(sizePreset.getStyleClassName());
        if (roleStyleClass != null && !roleStyleClass.isBlank()) {
            getStyleClass().add(roleStyleClass);
        }
        setAlignment(Pos.CENTER);
        setCursor(Cursor.HAND);
        setPickOnBounds(true);
        setMinSize(sizePreset.getPixelSize(), sizePreset.getPixelSize());
        setPrefSize(sizePreset.getPixelSize(), sizePreset.getPixelSize());
        setMaxSize(sizePreset.getPixelSize(), sizePreset.getPixelSize());

        Group graphicRoot = new Group();
        graphicRoot.setScaleX(sizePreset.getPixelSize() / VIEWBOX_SIZE);
        graphicRoot.setScaleY(sizePreset.getPixelSize() / VIEWBOX_SIZE);

        Circle ringShadow = new Circle(CENTER, CENTER + SHADOW_OFFSET_Y, RADIUS);
        ringShadow.getStyleClass().addAll("schedule-status-ring-base", "schedule-status-ring-shadow");
        ringShadow.setFill(null);

        staticSegmentA = createSegmentArc(90.0, SEGMENT_SWEEP, "schedule-status-ring-segment-a");
        staticSegmentB = createSegmentArc(-30.0, SEGMENT_SWEEP, "schedule-status-ring-segment-b");
        staticSegmentC = createSegmentArc(-150.0, SEGMENT_SWEEP, "schedule-status-ring-segment-c");

        progressSegmentA = createProgressArc(90.0, "schedule-status-ring-segment-a");
        progressSegmentB = createProgressArc(-30.0, "schedule-status-ring-segment-b");
        progressSegmentC = createProgressArc(-150.0, "schedule-status-ring-segment-c");

        rippleCircle = new Circle(CENTER, CENTER, RADIUS);
        rippleCircle.getStyleClass().add("schedule-status-ripple");
        rippleCircle.setFill(null);
        rippleCircle.setVisible(false);
        rippleCircle.setOpacity(0.0);

        checkGroup = buildCheckGroup();

        graphicRoot.getChildren().addAll(
            ringShadow,
            staticSegmentA,
            staticSegmentB,
            staticSegmentC,
            progressSegmentA,
            progressSegmentB,
            progressSegmentC,
            rippleCircle,
            checkGroup
        );

        getChildren().add(graphicRoot);

        progress.addListener((obs, oldValue, newValue) -> {
            updateProgressSegments(newValue.doubleValue());
            updateRingVisibility();
        });

        installInteractions();
        syncCompleted(completed);
    }

    public boolean isCompletedState() {
        return completed;
    }

    public void syncCompleted(boolean completed) {
        stopAllAnimations();
        this.completed = completed;
        holdThresholdReached = false;
        suppressCompletedClick = false;
        transitionBusy = false;
        progress.set(0.0);
        rippleCircle.setVisible(false);
        rippleCircle.setOpacity(0.0);
        rippleCircle.setScaleX(1.0);
        rippleCircle.setScaleY(1.0);
        checkGroup.setVisible(completed);
        checkGroup.setOpacity(completed ? 1.0 : 0.0);
        checkGroup.setScaleX(completed ? 1.0 : 0.82);
        checkGroup.setScaleY(completed ? 1.0 : 0.82);
        interactionModel.reset(completed);
        updateRingVisibility();
        updateStateClasses();
    }

    private void installInteractions() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || isDisabled()) {
                return;
            }
            if (!completed) {
                startForwardPreview();
            }
            event.consume();
        });

        addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || isDisabled()) {
                return;
            }
            if (!completed) {
                handlePendingRelease();
            }
            event.consume();
        });

        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || isDisabled()) {
                return;
            }
            if (completed) {
                if (suppressCompletedClick) {
                    suppressCompletedClick = false;
                } else {
                    startCompletedReverse();
                }
            }
            event.consume();
        });
    }

    private Arc createSegmentArc(double startAngle, double length, String segmentClass) {
        Arc arc = new Arc(CENTER, CENTER, RADIUS, RADIUS, startAngle, -length);
        arc.setType(ArcType.OPEN);
        arc.setFill(null);
        arc.setStrokeWidth(STROKE_WIDTH);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.getStyleClass().addAll("schedule-status-ring-segment", segmentClass);
        return arc;
    }

    private Arc createProgressArc(double startAngle, String segmentClass) {
        Arc arc = createSegmentArc(startAngle, 0.0, segmentClass);
        arc.getStyleClass().add("schedule-status-ring-progress");
        return arc;
    }

    private Group buildCheckGroup() {
        Group shadowCheck = buildCheckStrokeGroup(24.0, "schedule-status-check-shadow");
        shadowCheck.setTranslateY(SHADOW_OFFSET_Y);

        Group outlineCheck = buildCheckStrokeGroup(24.0, "schedule-status-check-outline");
        Group checkLine = buildCheckStrokeGroup(16.0, "schedule-status-check");

        return new Group(shadowCheck, outlineCheck, checkLine);
    }

    private Group buildCheckStrokeGroup(double strokeWidth, String styleClass) {
        Line lineOne = new Line(26.0, 52.0, 44.0, 68.0);
        lineOne.setStrokeWidth(strokeWidth);
        lineOne.setStrokeLineCap(StrokeLineCap.ROUND);
        lineOne.setStrokeLineJoin(StrokeLineJoin.ROUND);
        lineOne.getStyleClass().add(styleClass);

        Line lineTwo = new Line(44.0, 68.0, 76.0, 30.0);
        lineTwo.setStrokeWidth(strokeWidth);
        lineTwo.setStrokeLineCap(StrokeLineCap.ROUND);
        lineTwo.setStrokeLineJoin(StrokeLineJoin.ROUND);
        lineTwo.getStyleClass().add(styleClass);

        return new Group(lineOne, lineTwo);
    }

    private void startForwardPreview() {
        if (!interactionModel.beginPendingPreview()) {
            return;
        }

        stopAllAnimations();
        holdThresholdReached = false;
        transitionBusy = false;
        progress.set(0.0);
        updateRingVisibility();
        updateStateClasses();

        holdTransition = new PauseTransition(HOLD_THRESHOLD);
        holdTransition.setOnFinished(event -> holdThresholdReached = true);
        holdTransition.playFromStart();

        forwardTimeline = createProgressTimeline(progress.get(), 1.0, FORWARD_DURATION, () -> {
            if (interactionModel.finishForwardPreview()) {
                completePendingInteraction();
            }
        });
        forwardTimeline.playFromStart();
    }

    private void handlePendingRelease() {
        if (holdTransition != null) {
            holdTransition.stop();
        }

        ScheduleStatusInteractionModel.ReleaseDecision decision = interactionModel.releasePending(
            holdThresholdReached,
            progress.get() >= 0.999
        );
        if (decision == ScheduleStatusInteractionModel.ReleaseDecision.START_ROLLBACK) {
            startRollback();
        }
        updateRingVisibility();
        updateStateClasses();
    }

    private void startRollback() {
        stopTimeline(forwardTimeline);
        stopTransition(completionTransition);
        transitionBusy = false;

        rollbackTimeline = createProgressTimeline(progress.get(), 0.0, ROLLBACK_DURATION, () -> {
            interactionModel.finishRollback();
            progress.set(0.0);
            holdThresholdReached = false;
            updateRingVisibility();
            updateStateClasses();
        });
        rollbackTimeline.playFromStart();
    }

    private void completePendingInteraction() {
        completed = true;
        holdThresholdReached = false;
        suppressCompletedClick = true;
        showCompletedCheck(true, () -> commitCompletion(true));
        updateRingVisibility();
        updateStateClasses();
    }

    private void startCompletedReverse() {
        if (!interactionModel.beginCompletedReverse()) {
            return;
        }

        stopTransition(completionTransition);
        stopTimeline(forwardTimeline);
        stopTimeline(rollbackTimeline);
        if (holdTransition != null) {
            holdTransition.stop();
        }

        transitionBusy = true;
        progress.set(1.0);
        updateRingVisibility();
        updateStateClasses();

        FadeTransition fadeOut = new FadeTransition(CHECK_POP_DURATION, checkGroup);
        fadeOut.setFromValue(checkGroup.getOpacity());
        fadeOut.setToValue(0.0);

        ScaleTransition shrink = new ScaleTransition(CHECK_POP_DURATION, checkGroup);
        shrink.setFromX(checkGroup.getScaleX());
        shrink.setFromY(checkGroup.getScaleY());
        shrink.setToX(0.82);
        shrink.setToY(0.82);

        rollbackTimeline = createProgressTimeline(1.0, 0.0, ROLLBACK_DURATION, () -> {
            if (interactionModel.finishCompletedReverse()) {
                completed = false;
                checkGroup.setVisible(false);
                transitionBusy = false;
                commitCompletion(false);
                updateRingVisibility();
                updateStateClasses();
            }
        });

        reverseTransition = new ParallelTransition(fadeOut, shrink);
        reverseTransition.setOnFinished(event -> checkGroup.setVisible(false));
        reverseTransition.playFromStart();
        rollbackTimeline.playFromStart();
    }

    private void showCompletedCheck(boolean animate, Runnable onFinished) {
        checkGroup.setVisible(true);

        if (!animate) {
            checkGroup.setOpacity(1.0);
            checkGroup.setScaleX(1.0);
            checkGroup.setScaleY(1.0);
            progress.set(0.0);
            transitionBusy = false;
            updateRingVisibility();
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        transitionBusy = true;
        progress.set(0.0);
        checkGroup.setOpacity(0.0);
        checkGroup.setScaleX(0.82);
        checkGroup.setScaleY(0.82);
        rippleCircle.setVisible(true);
        rippleCircle.setOpacity(0.55);
        rippleCircle.setScaleX(0.65);
        rippleCircle.setScaleY(0.65);
        updateRingVisibility();

        FadeTransition checkFade = new FadeTransition(CHECK_POP_DURATION, checkGroup);
        checkFade.setFromValue(0.0);
        checkFade.setToValue(1.0);

        ScaleTransition checkScale = new ScaleTransition(CHECK_POP_DURATION, checkGroup);
        checkScale.setFromX(0.82);
        checkScale.setFromY(0.82);
        checkScale.setToX(1.0);
        checkScale.setToY(1.0);

        FadeTransition rippleFade = new FadeTransition(CHECK_SETTLE_DURATION, rippleCircle);
        rippleFade.setFromValue(0.55);
        rippleFade.setToValue(0.0);

        ScaleTransition rippleScale = new ScaleTransition(CHECK_SETTLE_DURATION, rippleCircle);
        rippleScale.setFromX(0.65);
        rippleScale.setFromY(0.65);
        rippleScale.setToX(1.35);
        rippleScale.setToY(1.35);

        completionTransition = new ParallelTransition(checkFade, checkScale, rippleFade, rippleScale);
        completionTransition.setOnFinished(event -> {
            rippleCircle.setVisible(false);
            rippleCircle.setScaleX(1.0);
            rippleCircle.setScaleY(1.0);
            transitionBusy = false;
            updateRingVisibility();
            updateStateClasses();
            if (onFinished != null) {
                onFinished.run();
            }
        });
        completionTransition.playFromStart();
    }

    private void commitCompletion(boolean targetCompleted) {
        boolean success = commitHandler == null || commitHandler.test(targetCompleted);
        if (!success) {
            syncCompleted(!targetCompleted);
            return;
        }
        interactionModel.reset(targetCompleted);
        updateRingVisibility();
        updateStateClasses();
    }

    private Timeline createProgressTimeline(double from, double to, Duration duration, Runnable onFinished) {
        return new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(progress, from)),
            new KeyFrame(duration, event -> onFinished.run(), new KeyValue(progress, to))
        );
    }

    private void updateProgressSegments(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        List<Arc> segments = List.of(progressSegmentA, progressSegmentB, progressSegmentC);
        double remainingDegrees = clamped * 360.0;
        for (Arc arc : segments) {
            double segmentDegrees = Math.max(0.0, Math.min(SEGMENT_SWEEP, remainingDegrees));
            arc.setLength(-segmentDegrees);
            remainingDegrees -= SEGMENT_SWEEP;
        }
    }

    private void updateRingVisibility() {
        ScheduleStatusInteractionModel.VisualState visualState = interactionModel.getVisualState();
        boolean showStaticRing =
            visualState == ScheduleStatusInteractionModel.VisualState.PENDING_IDLE
                || visualState == ScheduleStatusInteractionModel.VisualState.COMPLETED_IDLE;
        boolean showProgressRing =
            visualState == ScheduleStatusInteractionModel.VisualState.FORWARD_PREVIEW
                || visualState == ScheduleStatusInteractionModel.VisualState.ROLLBACK
                || visualState == ScheduleStatusInteractionModel.VisualState.REVERSE_TO_PENDING;

        staticSegmentA.setVisible(showStaticRing);
        staticSegmentB.setVisible(showStaticRing);
        staticSegmentC.setVisible(showStaticRing);

        for (Arc arc : List.of(progressSegmentA, progressSegmentB, progressSegmentC)) {
            arc.setVisible(showProgressRing && Math.abs(arc.getLength()) > 0.01);
        }
    }

    private void updateStateClasses() {
        getStyleClass().removeAll(
            "schedule-status-state-pending",
            "schedule-status-state-completed",
            "schedule-status-state-busy"
        );
        getStyleClass().add(completed ? "schedule-status-state-completed" : "schedule-status-state-pending");
        if (interactionModel.isBusy() || transitionBusy) {
            getStyleClass().add("schedule-status-state-busy");
        }
        setAccessibleText(completed ? "已完成日程" : "待办日程");
    }

    private void stopAllAnimations() {
        if (holdTransition != null) {
            holdTransition.stop();
        }
        stopTimeline(forwardTimeline);
        stopTimeline(rollbackTimeline);
        stopTransition(completionTransition);
        stopTransition(reverseTransition);
    }

    private void stopTimeline(Timeline timeline) {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void stopTransition(ParallelTransition transition) {
        if (transition != null) {
            transition.stop();
        }
    }
}
