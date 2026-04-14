package com.example.view;

import java.util.function.BooleanSupplier;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Duration;

final class ScheduleCollapsePopAnimator {
    private static final String MOTION_HANDLE_KEY = "schedule.collapsePop.motionHandle";
    private static final String ACTIVE_TIMELINE_KEY = "schedule.collapsePop.activeTimeline";
    private static final Interpolator COLLAPSE_EASE_IN = Interpolator.SPLINE(0.42, 0.0, 1.0, 1.0);
    private static final Interpolator POP_SPRING = Interpolator.SPLINE(0.2, 0.88, 0.18, 1.0);
    private static final Interpolator POP_SETTLE = Interpolator.SPLINE(0.24, 0.84, 0.22, 1.0);

    static final class MotionHandle {
        private final Region host;
        private final Node shell;
        private final Runnable layoutPulse;

        private MotionHandle(Region host, Node shell, Runnable layoutPulse) {
            this.host = host;
            this.shell = shell;
            this.layoutPulse = layoutPulse;
        }

        Region getHost() {
            return host;
        }

        Node getShell() {
            return shell;
        }

        void pulseLayout() {
            host.requestLayout();
            if (layoutPulse != null) {
                layoutPulse.run();
            }
        }

        void setAnimationCache(boolean enabled) {
            host.setCache(enabled);
            shell.setCache(enabled);
            CacheHint hint = enabled ? CacheHint.SPEED : CacheHint.DEFAULT;
            host.setCacheHint(hint);
            shell.setCacheHint(hint);
        }

        void stopActiveTimeline() {
            Object activeTimeline = host.getProperties().get(ACTIVE_TIMELINE_KEY);
            if (activeTimeline instanceof Timeline) {
                ((Timeline) activeTimeline).stop();
            }
            host.getProperties().remove(ACTIVE_TIMELINE_KEY);
        }

        void registerTimeline(Timeline timeline) {
            stopActiveTimeline();
            if (timeline != null) {
                host.getProperties().put(ACTIVE_TIMELINE_KEY, timeline);
            }
        }

        void restoreSteadyState() {
            stopActiveTimeline();
            host.setManaged(true);
            host.setVisible(true);
            host.setMouseTransparent(false);
            shell.setMouseTransparent(false);
            shell.setOpacity(1.0);
            shell.setScaleX(1.0);
            shell.setScaleY(1.0);
            shell.setTranslateX(0.0);
            shell.setTranslateY(0.0);
            shell.setRotate(0.0);
            setAnimationCache(false);
            pulseLayout();
        }

        void prepareTargetPopState() {
            stopActiveTimeline();
            host.setManaged(true);
            host.setVisible(true);
            host.setMouseTransparent(true);
            shell.setMouseTransparent(true);
            shell.setOpacity(0.0);
            shell.setScaleX(CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_APPEAR));
            shell.setScaleY(CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_APPEAR));
            shell.setTranslateX(0.0);
            shell.setTranslateY(-10.0);
            shell.setRotate(0.0);
            setAnimationCache(true);
            pulseLayout();
        }

        void finishTargetPop() {
            stopActiveTimeline();
            host.setManaged(true);
            host.setVisible(true);
            host.setMouseTransparent(false);
            shell.setMouseTransparent(false);
            shell.setOpacity(1.0);
            shell.setScaleX(1.0);
            shell.setScaleY(1.0);
            shell.setTranslateX(0.0);
            shell.setTranslateY(0.0);
            shell.setRotate(0.0);
            setAnimationCache(false);
            pulseLayout();
        }
    }

    private ScheduleCollapsePopAnimator() {
    }

    static MotionHandle bindMotionHandle(Region host, Node shell, Runnable layoutPulse) {
        MotionHandle handle = new MotionHandle(host, shell, layoutPulse);
        host.getProperties().put(MOTION_HANDLE_KEY, handle);
        return handle;
    }

    static MotionHandle resolveMotionHandle(Node node) {
        if (node == null) {
            return null;
        }
        Object raw = node.getProperties().get(MOTION_HANDLE_KEY);
        if (raw instanceof MotionHandle) {
            return (MotionHandle) raw;
        }
        return null;
    }

    static void prepareTargetPopState(MotionHandle handle) {
        if (handle != null) {
            handle.prepareTargetPopState();
        }
    }

    static void playCollapseSource(
        MotionHandle sourceHandle,
        BooleanSupplier commitAction,
        Runnable onCommitSuccess,
        Runnable onFailure,
        Runnable onFinished
    ) {
        if (sourceHandle == null) {
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (!success) {
                if (onFailure != null) {
                    onFailure.run();
                }
            } else if (onCommitSuccess != null) {
                onCommitSuccess.run();
            }
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        sourceHandle.stopActiveTimeline();
        sourceHandle.setAnimationCache(true);
        sourceHandle.getHost().setMouseTransparent(true);
        sourceHandle.getShell().setMouseTransparent(true);

        Timeline collapseTimeline = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(sourceHandle.getShell().scaleXProperty(), 1.0),
                new KeyValue(sourceHandle.getShell().scaleYProperty(), 1.0),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 1.0),
                new KeyValue(sourceHandle.getShell().translateYProperty(), 0.0)
            ),
            new KeyFrame(
                CollapsePopKeyframePreset.SOURCE_STAGE_ONE,
                new KeyValue(sourceHandle.getShell().scaleXProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_ONE), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().scaleYProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_ONE), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 0.84, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().translateYProperty(), 2.0, COLLAPSE_EASE_IN)
            ),
            new KeyFrame(
                CollapsePopKeyframePreset.SOURCE_STAGE_TWO,
                new KeyValue(sourceHandle.getShell().scaleXProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_TWO), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().scaleYProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_TWO), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 0.38, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().translateYProperty(), 5.0, COLLAPSE_EASE_IN)
            ),
            new KeyFrame(
                CollapsePopKeyframePreset.SOURCE_STAGE_THREE,
                new KeyValue(sourceHandle.getShell().scaleXProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_THREE), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().scaleYProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_THREE), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 0.0, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().translateYProperty(), 8.0, COLLAPSE_EASE_IN)
            ),
            new KeyFrame(
                CollapsePopKeyframePreset.COMMIT_POINT,
                event -> {
                    boolean success = commitAction == null || commitAction.getAsBoolean();
                    if (!success) {
                        sourceHandle.restoreSteadyState();
                        if (onFailure != null) {
                            onFailure.run();
                        }
                        if (onFinished != null) {
                            onFinished.run();
                        }
                        collapseTimelineStop(event.getSource());
                        return;
                    }
                    if (onCommitSuccess != null) {
                        onCommitSuccess.run();
                    }
                    if (onFinished != null) {
                        onFinished.run();
                    }
                },
                new KeyValue(sourceHandle.getShell().scaleXProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.COMMIT_POINT), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().scaleYProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.COMMIT_POINT), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 0.0, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().translateYProperty(), 10.0, COLLAPSE_EASE_IN)
            )
        );
        sourceHandle.registerTimeline(collapseTimeline);
        collapseTimeline.setOnFinished(event -> sourceHandle.getHost().getProperties().remove(ACTIVE_TIMELINE_KEY));
        collapseTimeline.playFromStart();
    }

    static void playPreparedTargetPop(MotionHandle handle, Runnable onFinished) {
        if (handle == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        handle.stopActiveTimeline();
        handle.setAnimationCache(true);

        Timeline popTimeline = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_APPEAR)),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_APPEAR)),
                new KeyValue(handle.getShell().opacityProperty(), 0.0),
                new KeyValue(handle.getShell().translateYProperty(), -10.0)
            ),
            new KeyFrame(
                Duration.millis(CollapsePopKeyframePreset.TARGET_STAGE_ONE.toMillis() - CollapsePopKeyframePreset.TARGET_APPEAR.toMillis()),
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_ONE), POP_SPRING),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_ONE), POP_SPRING),
                new KeyValue(handle.getShell().opacityProperty(), 0.78, POP_SPRING),
                new KeyValue(handle.getShell().translateYProperty(), -3.0, POP_SPRING)
            ),
            new KeyFrame(
                Duration.millis(CollapsePopKeyframePreset.TARGET_STAGE_TWO.toMillis() - CollapsePopKeyframePreset.TARGET_APPEAR.toMillis()),
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_TWO), POP_SPRING),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_TWO), POP_SPRING),
                new KeyValue(handle.getShell().opacityProperty(), 1.0, POP_SPRING),
                new KeyValue(handle.getShell().translateYProperty(), 1.0, POP_SPRING)
            ),
            new KeyFrame(
                Duration.millis(CollapsePopKeyframePreset.TARGET_STAGE_THREE.toMillis() - CollapsePopKeyframePreset.TARGET_APPEAR.toMillis()),
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_THREE), POP_SETTLE),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_THREE), POP_SETTLE),
                new KeyValue(handle.getShell().translateYProperty(), -0.8, POP_SETTLE)
            ),
            new KeyFrame(
                CollapsePopKeyframePreset.targetPopDuration(),
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_FINISH), POP_SETTLE),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_FINISH), POP_SETTLE),
                new KeyValue(handle.getShell().opacityProperty(), 1.0, POP_SETTLE),
                new KeyValue(handle.getShell().translateYProperty(), 0.0, POP_SETTLE)
            )
        );
        handle.registerTimeline(popTimeline);
        popTimeline.setOnFinished(event -> {
            handle.getHost().getProperties().remove(ACTIVE_TIMELINE_KEY);
            handle.finishTargetPop();
            if (onFinished != null) {
                onFinished.run();
            }
        });
        popTimeline.playFromStart();
    }

    private static void collapseTimelineStop(Object source) {
        if (source instanceof Timeline) {
            ((Timeline) source).stop();
        }
    }
}
