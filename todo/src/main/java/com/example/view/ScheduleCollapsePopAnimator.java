package com.example.view;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.util.Duration;

final class ScheduleCollapsePopAnimator {
    private static final String MOTION_HANDLE_KEY = "schedule.collapsePop.motionHandle";
    private static final Duration TARGET_LOOKUP_DELAY = Duration.millis(
        CollapsePopKeyframePreset.TARGET_APPEAR.toMillis() - CollapsePopKeyframePreset.COMMIT_POINT.toMillis()
    );
    private static final int TARGET_LOOKUP_RETRIES = 6;
    private static final Interpolator COLLAPSE_EASE_IN = Interpolator.SPLINE(0.42, 0.0, 1.0, 1.0);
    private static final Interpolator POP_SPRING = Interpolator.SPLINE(0.2, 0.88, 0.18, 1.0);
    private static final Interpolator POP_SETTLE = Interpolator.SPLINE(0.24, 0.84, 0.22, 1.0);

    static final class MotionHandle {
        private final Region host;
        private final Node shell;
        private final Runnable layoutPulse;
        private double naturalHeight = Double.NaN;

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

        double ensureNaturalHeight() {
            if (!Double.isNaN(naturalHeight) && naturalHeight > 1.0) {
                return naturalHeight;
            }
            double measured = host.getLayoutBounds() != null ? host.getLayoutBounds().getHeight() : 0.0;
            if (measured <= 1.0) {
                measured = host.prefHeight(-1);
            }
            if (measured <= 1.0) {
                host.applyCss();
                host.layout();
                measured = host.prefHeight(-1);
            }
            naturalHeight = Math.max(1.0, measured);
            return naturalHeight;
        }

        void setAnimatedHeight(double height) {
            double safeHeight = Math.max(0.0, height);
            host.setMinHeight(safeHeight);
            host.setPrefHeight(safeHeight);
            host.setMaxHeight(safeHeight);
            pulseLayout();
        }

        void restoreComputedHeight() {
            host.setMinHeight(Region.USE_COMPUTED_SIZE);
            host.setPrefHeight(Region.USE_COMPUTED_SIZE);
            host.setMaxHeight(Region.USE_COMPUTED_SIZE);
            pulseLayout();
        }

        void pulseLayout() {
            host.requestLayout();
            if (shell instanceof Parent) {
                ((Parent) shell).requestLayout();
            }
            if (layoutPulse != null) {
                layoutPulse.run();
            }
        }

        void restoreSteadyState() {
            host.setManaged(true);
            host.setVisible(true);
            host.setMouseTransparent(false);
            shell.setMouseTransparent(false);
            shell.setOpacity(1.0);
            shell.setScaleX(1.0);
            shell.setScaleY(1.0);
            shell.setTranslateY(0.0);
            shell.setRotate(0.0);
            restoreComputedHeight();
        }

        void hideCommittedSource() {
            host.setVisible(false);
            host.setManaged(false);
            host.setMouseTransparent(true);
            shell.setMouseTransparent(true);
            shell.setOpacity(0.0);
            shell.setScaleX(1.0);
            shell.setScaleY(1.0);
            shell.setTranslateY(0.0);
            shell.setRotate(0.0);
            setAnimatedHeight(0.0);
        }

        void prepareTargetPopState() {
            double height = ensureNaturalHeight();
            double tinyScale = CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_APPEAR);
            host.setManaged(true);
            host.setVisible(true);
            host.setMouseTransparent(true);
            shell.setMouseTransparent(true);
            shell.setOpacity(0.0);
            shell.setScaleX(tinyScale);
            shell.setScaleY(tinyScale);
            shell.setTranslateY(-10.0);
            shell.setRotate(0.0);
            setAnimatedHeight(Math.max(2.0, height * 0.12));
        }

        void finishTargetPop() {
            host.setManaged(true);
            host.setVisible(true);
            host.setMouseTransparent(false);
            shell.setMouseTransparent(false);
            shell.setOpacity(1.0);
            shell.setScaleX(1.0);
            shell.setScaleY(1.0);
            shell.setTranslateY(0.0);
            shell.setRotate(0.0);
            restoreComputedHeight();
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

    static void playListComplete(
        MotionHandle sourceHandle,
        BooleanSupplier commitAction,
        Supplier<MotionHandle> expandedTargetSupplier,
        Supplier<Node> collapsedHeaderSupplier,
        Runnable onFailure,
        Runnable onFinished
    ) {
        playCompletion(
            sourceHandle,
            commitAction,
            () -> scheduleListFollowUp(expandedTargetSupplier, collapsedHeaderSupplier, onFinished),
            onFailure,
            onFinished
        );
    }

    static void playHeatmapComplete(
        MotionHandle sourceHandle,
        BooleanSupplier commitAction,
        Supplier<MotionHandle> completedProxySupplier,
        Supplier<Node> zoneSupplier,
        Runnable onFailure,
        Runnable onFinished
    ) {
        playCompletion(
            sourceHandle,
            commitAction,
            () -> scheduleHeatmapFollowUp(completedProxySupplier, zoneSupplier, onFinished),
            onFailure,
            onFinished
        );
    }

    private static void playCompletion(
        MotionHandle sourceHandle,
        BooleanSupplier commitAction,
        Runnable successFollowUp,
        Runnable onFailure,
        Runnable onFinished
    ) {
        if (sourceHandle == null) {
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (!success) {
                if (onFailure != null) {
                    onFailure.run();
                }
                if (onFinished != null) {
                    onFinished.run();
                }
                return;
            }
            if (successFollowUp != null) {
                successFollowUp.run();
            } else if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        double naturalHeight = sourceHandle.ensureNaturalHeight();
        sourceHandle.getHost().setMouseTransparent(true);
        sourceHandle.getShell().setMouseTransparent(true);

        Timeline collapseTimeline = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(sourceHandle.getShell().scaleXProperty(), 1.0),
                new KeyValue(sourceHandle.getShell().scaleYProperty(), 1.0),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 1.0),
                new KeyValue(sourceHandle.getShell().translateYProperty(), 0.0),
                new KeyValue(sourceHandle.getHost().minHeightProperty(), naturalHeight),
                new KeyValue(sourceHandle.getHost().prefHeightProperty(), naturalHeight),
                new KeyValue(sourceHandle.getHost().maxHeightProperty(), naturalHeight)
            ),
            new KeyFrame(
                CollapsePopKeyframePreset.SOURCE_STAGE_ONE,
                event -> sourceHandle.pulseLayout(),
                new KeyValue(sourceHandle.getShell().scaleXProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_ONE), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().scaleYProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_ONE), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 0.84, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().translateYProperty(), 2.0, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getHost().minHeightProperty(), naturalHeight * 0.82, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getHost().prefHeightProperty(), naturalHeight * 0.82, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getHost().maxHeightProperty(), naturalHeight * 0.82, COLLAPSE_EASE_IN)
            ),
            new KeyFrame(
                CollapsePopKeyframePreset.SOURCE_STAGE_TWO,
                event -> sourceHandle.pulseLayout(),
                new KeyValue(sourceHandle.getShell().scaleXProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_TWO), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().scaleYProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_TWO), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 0.38, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().translateYProperty(), 5.0, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getHost().minHeightProperty(), naturalHeight * 0.4, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getHost().prefHeightProperty(), naturalHeight * 0.4, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getHost().maxHeightProperty(), naturalHeight * 0.4, COLLAPSE_EASE_IN)
            ),
            new KeyFrame(
                CollapsePopKeyframePreset.SOURCE_STAGE_THREE,
                event -> sourceHandle.pulseLayout(),
                new KeyValue(sourceHandle.getShell().scaleXProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_THREE), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().scaleYProperty(), CollapsePopKeyframePreset.sourceScaleAt(CollapsePopKeyframePreset.SOURCE_STAGE_THREE), COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 0.0, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getShell().translateYProperty(), 8.0, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getHost().minHeightProperty(), 0.0, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getHost().prefHeightProperty(), 0.0, COLLAPSE_EASE_IN),
                new KeyValue(sourceHandle.getHost().maxHeightProperty(), 0.0, COLLAPSE_EASE_IN)
            ),
            new KeyFrame(
                CollapsePopKeyframePreset.COMMIT_POINT,
                event -> sourceHandle.pulseLayout(),
                new KeyValue(sourceHandle.getShell().opacityProperty(), 0.0),
                new KeyValue(sourceHandle.getHost().minHeightProperty(), 0.0),
                new KeyValue(sourceHandle.getHost().prefHeightProperty(), 0.0),
                new KeyValue(sourceHandle.getHost().maxHeightProperty(), 0.0)
            )
        );

        PauseTransition commitPause = new PauseTransition(CollapsePopKeyframePreset.COMMIT_POINT);
        commitPause.setOnFinished(event -> {
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (!success) {
                collapseTimeline.stop();
                sourceHandle.restoreSteadyState();
                if (onFailure != null) {
                    onFailure.run();
                }
                if (onFinished != null) {
                    onFinished.run();
                }
                return;
            }
            sourceHandle.hideCommittedSource();
            if (successFollowUp != null) {
                successFollowUp.run();
            } else if (onFinished != null) {
                onFinished.run();
            }
        });

        collapseTimeline.playFromStart();
        commitPause.playFromStart();
    }

    private static void scheduleListFollowUp(
        Supplier<MotionHandle> expandedTargetSupplier,
        Supplier<Node> collapsedHeaderSupplier,
        Runnable onFinished
    ) {
        PauseTransition afterCommitDelay = new PauseTransition(TARGET_LOOKUP_DELAY);
        afterCommitDelay.setOnFinished(event -> {
            if (expandedTargetSupplier != null) {
                pollForPreparedTarget(expandedTargetSupplier, TARGET_LOOKUP_RETRIES, onFinished);
                return;
            }
            Node header = collapsedHeaderSupplier != null ? collapsedHeaderSupplier.get() : null;
            if (header != null) {
                ScheduleReflowAnimator.playCollapsedReceive(
                    header,
                    CollapsePopKeyframePreset.targetPopDuration(),
                    onFinished
                );
                return;
            }
            if (onFinished != null) {
                onFinished.run();
            }
        });
        afterCommitDelay.playFromStart();
    }

    private static void scheduleHeatmapFollowUp(
        Supplier<MotionHandle> completedProxySupplier,
        Supplier<Node> zoneSupplier,
        Runnable onFinished
    ) {
        PauseTransition afterCommitDelay = new PauseTransition(TARGET_LOOKUP_DELAY);
        afterCommitDelay.setOnFinished(event -> {
            MotionHandle proxyHandle = completedProxySupplier != null ? completedProxySupplier.get() : null;
            if (proxyHandle != null) {
                playPreparedTargetPop(proxyHandle, onFinished);
                return;
            }
            Node zone = zoneSupplier != null ? zoneSupplier.get() : null;
            if (zone != null) {
                ScheduleReflowAnimator.playTargetPulse(zone, CollapsePopKeyframePreset.targetPopDuration(), onFinished);
                return;
            }
            if (onFinished != null) {
                onFinished.run();
            }
        });
        afterCommitDelay.playFromStart();
    }

    private static void pollForPreparedTarget(
        Supplier<MotionHandle> targetSupplier,
        int retriesRemaining,
        Runnable onFinished
    ) {
        MotionHandle handle = targetSupplier != null ? targetSupplier.get() : null;
        if (handle != null) {
            playPreparedTargetPop(handle, onFinished);
            return;
        }
        if (retriesRemaining <= 0) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        Platform.runLater(() -> pollForPreparedTarget(targetSupplier, retriesRemaining - 1, onFinished));
    }

    private static void playPreparedTargetPop(MotionHandle handle, Runnable onFinished) {
        if (handle == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        double naturalHeight = handle.ensureNaturalHeight();
        double popDurationMillis = CollapsePopKeyframePreset.targetPopDuration().toMillis();

        Timeline popTimeline = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_APPEAR)),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_APPEAR)),
                new KeyValue(handle.getShell().opacityProperty(), 0.0),
                new KeyValue(handle.getShell().translateYProperty(), -10.0),
                new KeyValue(handle.getHost().minHeightProperty(), Math.max(2.0, naturalHeight * 0.12)),
                new KeyValue(handle.getHost().prefHeightProperty(), Math.max(2.0, naturalHeight * 0.12)),
                new KeyValue(handle.getHost().maxHeightProperty(), Math.max(2.0, naturalHeight * 0.12))
            ),
            new KeyFrame(
                Duration.millis(CollapsePopKeyframePreset.TARGET_STAGE_ONE.toMillis() - CollapsePopKeyframePreset.TARGET_APPEAR.toMillis()),
                event -> handle.pulseLayout(),
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_ONE), POP_SPRING),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_ONE), POP_SPRING),
                new KeyValue(handle.getShell().opacityProperty(), 0.78, POP_SPRING),
                new KeyValue(handle.getShell().translateYProperty(), -3.0, POP_SPRING),
                new KeyValue(handle.getHost().minHeightProperty(), naturalHeight * 0.82, POP_SPRING),
                new KeyValue(handle.getHost().prefHeightProperty(), naturalHeight * 0.82, POP_SPRING),
                new KeyValue(handle.getHost().maxHeightProperty(), naturalHeight * 0.82, POP_SPRING)
            ),
            new KeyFrame(
                Duration.millis(CollapsePopKeyframePreset.TARGET_STAGE_TWO.toMillis() - CollapsePopKeyframePreset.TARGET_APPEAR.toMillis()),
                event -> handle.pulseLayout(),
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_TWO), POP_SPRING),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_TWO), POP_SPRING),
                new KeyValue(handle.getShell().opacityProperty(), 1.0, POP_SPRING),
                new KeyValue(handle.getShell().translateYProperty(), 1.0, POP_SPRING),
                new KeyValue(handle.getHost().minHeightProperty(), naturalHeight * 1.06, POP_SPRING),
                new KeyValue(handle.getHost().prefHeightProperty(), naturalHeight * 1.06, POP_SPRING),
                new KeyValue(handle.getHost().maxHeightProperty(), naturalHeight * 1.06, POP_SPRING)
            ),
            new KeyFrame(
                Duration.millis(CollapsePopKeyframePreset.TARGET_STAGE_THREE.toMillis() - CollapsePopKeyframePreset.TARGET_APPEAR.toMillis()),
                event -> handle.pulseLayout(),
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_THREE), POP_SETTLE),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_STAGE_THREE), POP_SETTLE),
                new KeyValue(handle.getShell().translateYProperty(), -0.8, POP_SETTLE),
                new KeyValue(handle.getHost().minHeightProperty(), naturalHeight * 0.97, POP_SETTLE),
                new KeyValue(handle.getHost().prefHeightProperty(), naturalHeight * 0.97, POP_SETTLE),
                new KeyValue(handle.getHost().maxHeightProperty(), naturalHeight * 0.97, POP_SETTLE)
            ),
            new KeyFrame(
                Duration.millis(popDurationMillis),
                event -> handle.pulseLayout(),
                new KeyValue(handle.getShell().scaleXProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_FINISH), POP_SETTLE),
                new KeyValue(handle.getShell().scaleYProperty(), CollapsePopKeyframePreset.targetScaleAt(CollapsePopKeyframePreset.TARGET_FINISH), POP_SETTLE),
                new KeyValue(handle.getShell().opacityProperty(), 1.0, POP_SETTLE),
                new KeyValue(handle.getShell().translateYProperty(), 0.0, POP_SETTLE),
                new KeyValue(handle.getHost().minHeightProperty(), naturalHeight, POP_SETTLE),
                new KeyValue(handle.getHost().prefHeightProperty(), naturalHeight, POP_SETTLE),
                new KeyValue(handle.getHost().maxHeightProperty(), naturalHeight, POP_SETTLE)
            )
        );
        popTimeline.setOnFinished(event -> {
            handle.finishTargetPop();
            if (onFinished != null) {
                onFinished.run();
            }
        });
        popTimeline.playFromStart();
    }
}
