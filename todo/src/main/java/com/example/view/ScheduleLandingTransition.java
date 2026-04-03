package com.example.view;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

final class ScheduleLandingTransition {
    static final String COMPLETED_PREVIEW_CLASS = "schedule-card-transition-completed";
    static final String TARGET_PREPARED_CLASS = "schedule-card-transition-receiving";

    private static final String TARGET_BASE_OPACITY_KEY = "schedule.landing.targetBaseOpacity";
    private static final String TARGET_BASE_MOUSE_TRANSPARENT_KEY = "schedule.landing.targetBaseMouseTransparent";
    private static final String TARGET_PREPARED_KEY = "schedule.landing.targetPrepared";
    private static final Interpolator SETTLE_INTERPOLATOR = Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0);
    private static final Interpolator MORPH_INTERPOLATOR = Interpolator.SPLINE(0.22, 0.7, 0.2, 1.0);
    private static final Interpolator ABSORB_INTERPOLATOR = Interpolator.SPLINE(0.32, 0.0, 0.18, 1.0);

    private ScheduleLandingTransition() {
    }

    static void prepareTargetNodeForHandoff(Node node, double preparedOpacity) {
        if (node == null) {
            return;
        }
        if (!Boolean.TRUE.equals(node.getProperties().get(TARGET_PREPARED_KEY))) {
            node.getProperties().put(TARGET_BASE_OPACITY_KEY, node.getOpacity());
            node.getProperties().put(TARGET_BASE_MOUSE_TRANSPARENT_KEY, node.isMouseTransparent());
        }
        node.getProperties().put(TARGET_PREPARED_KEY, Boolean.TRUE);
        if (!node.getStyleClass().contains(TARGET_PREPARED_CLASS)) {
            node.getStyleClass().add(TARGET_PREPARED_CLASS);
        }
        node.setOpacity(preparedOpacity);
        node.setMouseTransparent(true);
    }

    static void finishTargetHandoff(Node node) {
        if (node == null) {
            return;
        }
        node.setOpacity(resolveTargetOpacity(node));
        node.setMouseTransparent(resolveTargetMouseTransparent(node));
        node.getStyleClass().remove(TARGET_PREPARED_CLASS);
        clearPreparedTargetState(node);
    }

    static void cancelTargetPreparation(Node node) {
        finishTargetHandoff(node);
    }

    static WritableImage snapshotNode(Node node) {
        return snapshotNode(node, null);
    }

    static WritableImage snapshotNode(Node node, Double opacityOverride) {
        if (node == null) {
            return null;
        }

        double originalOpacity = node.getOpacity();
        if (opacityOverride != null) {
            node.setOpacity(opacityOverride);
        }

        try {
            if (node.getParent() != null) {
                node.getParent().applyCss();
                node.getParent().layout();
            }
            node.applyCss();
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            return node.snapshot(parameters, null);
        } finally {
            if (opacityOverride != null) {
                node.setOpacity(originalOpacity);
            }
        }
    }

    static WritableImage snapshotNodeWithTemporaryClass(Node node, String temporaryClass) {
        if (node == null || temporaryClass == null || temporaryClass.isBlank()) {
            return snapshotNode(node);
        }

        boolean added = false;
        if (!node.getStyleClass().contains(temporaryClass)) {
            node.getStyleClass().add(temporaryClass);
            added = true;
        }
        try {
            return snapshotNode(node);
        } finally {
            if (added) {
                node.getStyleClass().remove(temporaryClass);
            }
        }
    }

    static void handoffToExpandedTarget(
        ScheduleCardMotionSupport.StagedLanding landing,
        Node targetNode,
        Duration settleDuration,
        Duration morphDuration,
        Duration revealDuration,
        Runnable onFinished
    ) {
        if (landing == null || landing.isResolved()) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        Pane overlayHost = landing.getOverlayHost();
        ImageView pendingGhost = landing.getGhost();
        Bounds targetSceneBounds = targetNode != null ? targetNode.localToScene(targetNode.getBoundsInLocal()) : null;
        if (overlayHost == null || pendingGhost == null || targetNode == null || targetSceneBounds == null) {
            finishTargetHandoff(targetNode);
            if (onFinished != null) {
                onFinished.run();
            }
            landing.finishSuccess();
            return;
        }

        double targetOpacity = resolveTargetOpacity(targetNode);
        WritableImage completedSnapshot = snapshotPreparedTargetNode(targetNode, targetOpacity);
        if (completedSnapshot == null) {
            finishTargetHandoff(targetNode);
            if (onFinished != null) {
                onFinished.run();
            }
            landing.finishSuccess();
            return;
        }

        ImageView completedGhost = createGhostFromSnapshot(
            completedSnapshot,
            pendingGhost.getLayoutX(),
            pendingGhost.getLayoutY(),
            pendingGhost.getFitWidth(),
            pendingGhost.getFitHeight()
        );
        completedGhost.setOpacity(0.0);
        completedGhost.setScaleX(0.986);
        completedGhost.setScaleY(0.986);
        overlayHost.getChildren().add(completedGhost);
        completedGhost.toFront();
        pendingGhost.toFront();

        Point2D targetRootPoint = overlayHost.sceneToLocal(targetSceneBounds.getMinX(), targetSceneBounds.getMinY());

        Timeline settle = new Timeline(
            new KeyFrame(
                settleDuration,
                new KeyValue(pendingGhost.layoutXProperty(), targetRootPoint.getX(), SETTLE_INTERPOLATOR),
                new KeyValue(pendingGhost.layoutYProperty(), targetRootPoint.getY(), SETTLE_INTERPOLATOR),
                new KeyValue(pendingGhost.fitWidthProperty(), targetSceneBounds.getWidth(), SETTLE_INTERPOLATOR),
                new KeyValue(pendingGhost.fitHeightProperty(), targetSceneBounds.getHeight(), SETTLE_INTERPOLATOR),
                new KeyValue(completedGhost.layoutXProperty(), targetRootPoint.getX(), SETTLE_INTERPOLATOR),
                new KeyValue(completedGhost.layoutYProperty(), targetRootPoint.getY(), SETTLE_INTERPOLATOR),
                new KeyValue(completedGhost.fitWidthProperty(), targetSceneBounds.getWidth(), SETTLE_INTERPOLATOR),
                new KeyValue(completedGhost.fitHeightProperty(), targetSceneBounds.getHeight(), SETTLE_INTERPOLATOR)
            )
        );

        Timeline morph = new Timeline(
            new KeyFrame(
                morphDuration,
                new KeyValue(pendingGhost.opacityProperty(), 0.0, MORPH_INTERPOLATOR),
                new KeyValue(pendingGhost.scaleXProperty(), 0.986, MORPH_INTERPOLATOR),
                new KeyValue(pendingGhost.scaleYProperty(), 0.986, MORPH_INTERPOLATOR),
                new KeyValue(completedGhost.opacityProperty(), 1.0, MORPH_INTERPOLATOR),
                new KeyValue(completedGhost.scaleXProperty(), 1.0, MORPH_INTERPOLATOR),
                new KeyValue(completedGhost.scaleYProperty(), 1.0, MORPH_INTERPOLATOR)
            )
        );

        Timeline handoff = new Timeline(
            new KeyFrame(
                revealDuration,
                new KeyValue(targetNode.opacityProperty(), targetOpacity, MORPH_INTERPOLATOR),
                new KeyValue(completedGhost.opacityProperty(), 0.0, MORPH_INTERPOLATOR)
            )
        );

        SequentialTransition transition = new SequentialTransition(settle, morph, handoff);
        transition.setOnFinished(event -> {
            overlayHost.getChildren().remove(completedGhost);
            finishTargetHandoff(targetNode);
            if (onFinished != null) {
                onFinished.run();
            }
            landing.finishSuccess();
        });
        transition.playFromStart();
    }

    static void morphIntoCollapsedEntry(
        ScheduleCardMotionSupport.StagedLanding landing,
        WritableImage completedSnapshot,
        Point2D entryScenePoint,
        Duration settleDuration,
        Duration morphDuration,
        Duration absorbDuration,
        Runnable onFinished
    ) {
        if (landing == null || landing.isResolved()) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        Pane overlayHost = landing.getOverlayHost();
        ImageView pendingGhost = landing.getGhost();
        if (overlayHost == null || pendingGhost == null || completedSnapshot == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            landing.finishSuccess();
            return;
        }

        Point2D safeEntryPoint = entryScenePoint != null ? entryScenePoint : landing.getGhostSceneCenter();
        if (safeEntryPoint == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            landing.finishSuccess();
            return;
        }

        ImageView completedGhost = createGhostFromSnapshot(
            completedSnapshot,
            pendingGhost.getLayoutX(),
            pendingGhost.getLayoutY(),
            pendingGhost.getFitWidth(),
            pendingGhost.getFitHeight()
        );
        completedGhost.setOpacity(0.0);
        completedGhost.setScaleX(0.986);
        completedGhost.setScaleY(0.986);
        overlayHost.getChildren().add(completedGhost);
        completedGhost.toFront();
        pendingGhost.toFront();

        Point2D entryRootPoint = overlayHost.sceneToLocal(
            safeEntryPoint.getX() - pendingGhost.getFitWidth() / 2.0,
            safeEntryPoint.getY() - pendingGhost.getFitHeight() / 2.0
        );

        Timeline settle = new Timeline(
            new KeyFrame(
                settleDuration,
                new KeyValue(pendingGhost.layoutXProperty(), entryRootPoint.getX(), SETTLE_INTERPOLATOR),
                new KeyValue(pendingGhost.layoutYProperty(), entryRootPoint.getY(), SETTLE_INTERPOLATOR),
                new KeyValue(completedGhost.layoutXProperty(), entryRootPoint.getX(), SETTLE_INTERPOLATOR),
                new KeyValue(completedGhost.layoutYProperty(), entryRootPoint.getY(), SETTLE_INTERPOLATOR)
            )
        );

        Timeline morph = new Timeline(
            new KeyFrame(
                morphDuration,
                new KeyValue(pendingGhost.opacityProperty(), 0.0, MORPH_INTERPOLATOR),
                new KeyValue(completedGhost.opacityProperty(), 1.0, MORPH_INTERPOLATOR),
                new KeyValue(pendingGhost.scaleXProperty(), 0.985, MORPH_INTERPOLATOR),
                new KeyValue(pendingGhost.scaleYProperty(), 0.985, MORPH_INTERPOLATOR),
                new KeyValue(completedGhost.scaleXProperty(), 1.0, MORPH_INTERPOLATOR),
                new KeyValue(completedGhost.scaleYProperty(), 1.0, MORPH_INTERPOLATOR)
            )
        );

        Timeline absorb = new Timeline(
            new KeyFrame(
                absorbDuration,
                new KeyValue(completedGhost.layoutYProperty(), entryRootPoint.getY() + 6.0, ABSORB_INTERPOLATOR),
                new KeyValue(completedGhost.opacityProperty(), 0.0, ABSORB_INTERPOLATOR),
                new KeyValue(completedGhost.scaleXProperty(), 0.9, ABSORB_INTERPOLATOR),
                new KeyValue(completedGhost.scaleYProperty(), 0.9, ABSORB_INTERPOLATOR)
            )
        );

        SequentialTransition transition = new SequentialTransition(settle, morph, absorb);
        transition.setOnFinished(event -> {
            overlayHost.getChildren().remove(completedGhost);
            if (onFinished != null) {
                onFinished.run();
            }
            landing.finishSuccess();
        });
        transition.playFromStart();
    }

    private static double resolveTargetOpacity(Node node) {
        Object rawOpacity = node.getProperties().get(TARGET_BASE_OPACITY_KEY);
        if (rawOpacity instanceof Number) {
            return ((Number) rawOpacity).doubleValue();
        }
        return node != null ? node.getOpacity() : 1.0;
    }

    private static boolean resolveTargetMouseTransparent(Node node) {
        Object rawValue = node.getProperties().get(TARGET_BASE_MOUSE_TRANSPARENT_KEY);
        if (rawValue instanceof Boolean) {
            return (Boolean) rawValue;
        }
        return false;
    }

    private static void clearPreparedTargetState(Node node) {
        node.getProperties().remove(TARGET_BASE_OPACITY_KEY);
        node.getProperties().remove(TARGET_BASE_MOUSE_TRANSPARENT_KEY);
        node.getProperties().remove(TARGET_PREPARED_KEY);
    }

    private static WritableImage snapshotPreparedTargetNode(Node node, double opacityOverride) {
        if (node == null) {
            return null;
        }
        boolean removed = node.getStyleClass().remove(TARGET_PREPARED_CLASS);
        try {
            return snapshotNode(node, opacityOverride);
        } finally {
            if (removed) {
                node.getStyleClass().add(TARGET_PREPARED_CLASS);
            }
        }
    }

    private static ImageView createGhostFromSnapshot(
        WritableImage snapshot,
        double layoutX,
        double layoutY,
        double fitWidth,
        double fitHeight
    ) {
        ImageView ghost = new ImageView(snapshot);
        ghost.setManaged(false);
        ghost.setMouseTransparent(true);
        ghost.setLayoutX(layoutX);
        ghost.setLayoutY(layoutY);
        ghost.setFitWidth(fitWidth);
        ghost.setFitHeight(fitHeight);
        ghost.setPreserveRatio(false);
        ghost.setSmooth(true);
        ghost.setCache(true);
        return ghost;
    }
}
