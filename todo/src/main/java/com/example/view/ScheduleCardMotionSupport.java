package com.example.view;

import java.util.function.BooleanSupplier;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public final class ScheduleCardMotionSupport {
    public interface ArchiveAnimationListener {
        default void onDropStart() {
        }

        default void onCommitPoint() {
        }

        default void onFinished(boolean success) {
        }
    }

    public interface StagedArchiveAnimationListener extends ArchiveAnimationListener {
        default void onReadyToSettle(StagedLanding landing) {
        }
    }

    public static final class StagedLanding {
        private final Pane overlayHost;
        private final ImageView ghost;
        private final Node sourceNode;
        private final ArchiveAnimationListener listener;
        private final Runnable failureRunnable;
        private boolean resolved;

        private StagedLanding(
            Pane overlayHost,
            ImageView ghost,
            Node sourceNode,
            ArchiveAnimationListener listener,
            Runnable failureRunnable
        ) {
            this.overlayHost = overlayHost;
            this.ghost = ghost;
            this.sourceNode = sourceNode;
            this.listener = listener;
            this.failureRunnable = failureRunnable;
        }

        public Pane getOverlayHost() {
            return overlayHost;
        }

        public ImageView getGhost() {
            return ghost;
        }

        public Point2D getGhostSceneCenter() {
            Bounds ghostBounds = ghost != null ? ghost.localToScene(ghost.getBoundsInLocal()) : null;
            if (ghostBounds == null) {
                return null;
            }
            return new Point2D(ghostBounds.getCenterX(), ghostBounds.getCenterY());
        }

        public boolean isResolved() {
            return resolved;
        }

        public void finishSuccess() {
            if (resolved) {
                return;
            }
            resolved = true;
            if (overlayHost != null && ghost != null) {
                overlayHost.getChildren().remove(ghost);
            }
            hideCommittedNode(sourceNode);
            listener.onFinished(true);
        }

        public void finishFailure() {
            if (resolved) {
                return;
            }
            resolved = true;
            if (overlayHost != null && ghost != null) {
                overlayHost.getChildren().remove(ghost);
            }
            restoreNode(sourceNode);
            listener.onFinished(false);
            if (failureRunnable != null) {
                failureRunnable.run();
            }
        }
    }

    private static final ArchiveAnimationListener NO_OP_LISTENER = new ArchiveAnimationListener() {
    };

    private static final Duration SHAKE_DURATION = Duration.millis(140);
    private static final Duration DROP_DURATION = Duration.millis(220);
    private static final Duration ABSORB_DURATION = Duration.millis(160);
    private static final Duration COMMIT_AFTER_DROP_START = Duration.millis(140);
    private static final Interpolator SHAKE_INTERPOLATOR = Interpolator.SPLINE(0.28, 0.0, 0.24, 1.0);
    private static final Interpolator DROP_INTERPOLATOR = Interpolator.SPLINE(0.16, 0.76, 0.22, 1.0);
    private static final Interpolator ABSORB_INTERPOLATOR = Interpolator.SPLINE(0.36, 0.0, 0.2, 1.0);
    private static final Color GHOST_SHADOW = Color.rgb(15, 23, 42, 0.12);

    private ScheduleCardMotionSupport() {
    }

    public static void playDropToCompleted(
        Node node,
        Point2D targetScenePoint,
        BooleanSupplier commitAction,
        Runnable onFailure
    ) {
        playDropToCompleted(node, targetScenePoint, NO_OP_LISTENER, commitAction, onFailure);
    }

    public static void playDropToCompleted(
        Node node,
        Point2D targetScenePoint,
        ArchiveAnimationListener listener,
        BooleanSupplier commitAction,
        Runnable onFailure
    ) {
        ArchiveAnimationListener safeListener = listener != null ? listener : NO_OP_LISTENER;
        if (node == null) {
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (!success && onFailure != null) {
                onFailure.run();
            }
            safeListener.onFinished(success);
            return;
        }

        if (node.getScene() == null || targetScenePoint == null || !(node.getScene().getRoot() instanceof Pane)) {
            playLocalFallback(node, safeListener, commitAction, onFailure);
            return;
        }

        Pane overlayHost = (Pane) node.getScene().getRoot();
        Bounds sourceSceneBounds = node.localToScene(node.getBoundsInLocal());
        if (sourceSceneBounds == null) {
            playLocalFallback(node, safeListener, commitAction, onFailure);
            return;
        }

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        WritableImage snapshot = node.snapshot(parameters, null);

        Point2D sourceRootPoint = overlayHost.sceneToLocal(sourceSceneBounds.getMinX(), sourceSceneBounds.getMinY());
        Point2D targetRootCenter = overlayHost.sceneToLocal(targetScenePoint);

        double sourceX = sourceRootPoint.getX();
        double sourceY = sourceRootPoint.getY();
        double targetX = targetRootCenter.getX() - sourceSceneBounds.getWidth() / 2.0;
        double targetY = targetRootCenter.getY() - sourceSceneBounds.getHeight() / 2.0;
        double rawDeltaX = targetX - sourceX;
        double lateralMerge = clamp(rawDeltaX, -26.0, 26.0);
        double deltaY = Math.max(sourceSceneBounds.getHeight() + 18.0, targetY - sourceY);
        double dropMidX = sourceX + clamp(lateralMerge * 0.04, -1.1, 1.1);
        double dropLateX = sourceX + clamp(lateralMerge * 0.18, -4.8, 4.8);
        double absorbX = sourceX + lateralMerge;
        double settleScale = clamp(0.9 - Math.max(sourceSceneBounds.getWidth(), sourceSceneBounds.getHeight()) / 1800.0, 0.84, 0.9);

        ImageView ghost = new ImageView(snapshot);
        ghost.setManaged(false);
        ghost.setMouseTransparent(true);
        ghost.setLayoutX(sourceX);
        ghost.setLayoutY(sourceY);
        ghost.setFitWidth(sourceSceneBounds.getWidth());
        ghost.setFitHeight(sourceSceneBounds.getHeight());
        ghost.setPreserveRatio(false);
        ghost.setSmooth(true);
        ghost.setCache(true);
        ghost.setEffect(new DropShadow(10.0, 0.0, 4.0, GHOST_SHADOW));
        overlayHost.getChildren().add(ghost);
        ghost.toFront();

        node.setMouseTransparent(true);

        boolean[] commitSucceeded = {false};
        boolean[] commitResolved = {false};
        boolean[] failureHandled = {false};

        Timeline sourceRelease = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(node.opacityProperty(), node.getOpacity()),
                new KeyValue(node.translateXProperty(), node.getTranslateX()),
                new KeyValue(node.translateYProperty(), node.getTranslateY()),
                new KeyValue(node.rotateProperty(), node.getRotate()),
                new KeyValue(node.scaleXProperty(), node.getScaleX()),
                new KeyValue(node.scaleYProperty(), node.getScaleY())
            ),
            new KeyFrame(
                SHAKE_DURATION,
                new KeyValue(node.opacityProperty(), 0.04, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateXProperty(), 0.0, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 3.4, SHAKE_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), 0.0, SHAKE_INTERPOLATOR),
                new KeyValue(node.scaleXProperty(), 0.998, SHAKE_INTERPOLATOR),
                new KeyValue(node.scaleYProperty(), 0.998, SHAKE_INTERPOLATOR)
            )
        );

        Timeline shake = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(ghost.layoutXProperty(), sourceX),
                new KeyValue(ghost.layoutYProperty(), sourceY),
                new KeyValue(ghost.rotateProperty(), 0.0),
                new KeyValue(ghost.scaleXProperty(), 1.0),
                new KeyValue(ghost.scaleYProperty(), 1.0),
                new KeyValue(ghost.opacityProperty(), 1.0)
            ),
            new KeyFrame(
                Duration.millis(72),
                new KeyValue(ghost.layoutXProperty(), sourceX - 1.8, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), sourceY + 0.8, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), -0.9, SHAKE_INTERPOLATOR)
            ),
            new KeyFrame(
                Duration.millis(148),
                new KeyValue(ghost.layoutXProperty(), sourceX + 1.8, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), sourceY + 1.9, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), 1.1, SHAKE_INTERPOLATOR)
            ),
            new KeyFrame(
                SHAKE_DURATION,
                new KeyValue(ghost.layoutXProperty(), sourceX + 0.4, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), sourceY + 3.8, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), 0.25, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.scaleXProperty(), 0.998, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.scaleYProperty(), 0.998, SHAKE_INTERPOLATOR)
            )
        );

        Timeline drop = new Timeline(
            new KeyFrame(
                Duration.millis(DROP_DURATION.toMillis() * 0.72),
                new KeyValue(ghost.layoutXProperty(), dropMidX, DROP_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), sourceY + deltaY * 0.72, DROP_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), 0.5, DROP_INTERPOLATOR),
                new KeyValue(ghost.scaleXProperty(), 0.993, DROP_INTERPOLATOR),
                new KeyValue(ghost.scaleYProperty(), 0.993, DROP_INTERPOLATOR),
                new KeyValue(ghost.opacityProperty(), 0.92, DROP_INTERPOLATOR)
            ),
            new KeyFrame(
                DROP_DURATION,
                new KeyValue(ghost.layoutXProperty(), dropLateX, DROP_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), sourceY + deltaY * 0.92, DROP_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), 0.85, DROP_INTERPOLATOR),
                new KeyValue(ghost.scaleXProperty(), 0.972, DROP_INTERPOLATOR),
                new KeyValue(ghost.scaleYProperty(), 0.972, DROP_INTERPOLATOR),
                new KeyValue(ghost.opacityProperty(), 0.6, DROP_INTERPOLATOR)
            )
        );

        Timeline absorb = new Timeline(
            new KeyFrame(
                ABSORB_DURATION,
                new KeyValue(ghost.layoutXProperty(), absorbX, ABSORB_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), targetY, ABSORB_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), 0.42, ABSORB_INTERPOLATOR),
                new KeyValue(ghost.scaleXProperty(), settleScale, ABSORB_INTERPOLATOR),
                new KeyValue(ghost.scaleYProperty(), settleScale, ABSORB_INTERPOLATOR),
                new KeyValue(ghost.opacityProperty(), 0.0, ABSORB_INTERPOLATOR)
            )
        );

        shake.setOnFinished(event -> safeListener.onDropStart());

        SequentialTransition ghostSequence = new SequentialTransition(shake, drop, absorb);
        PauseTransition commitTransition = new PauseTransition(Duration.millis(
            SHAKE_DURATION.toMillis() + COMMIT_AFTER_DROP_START.toMillis()
        ));

        Runnable cleanupFailure = () -> {
            if (failureHandled[0]) {
                return;
            }
            failureHandled[0] = true;
            overlayHost.getChildren().remove(ghost);
            restoreNode(node);
            safeListener.onFinished(false);
            if (onFailure != null) {
                onFailure.run();
            }
        };

        commitTransition.setOnFinished(event -> {
            commitResolved[0] = true;
            safeListener.onCommitPoint();
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (success) {
                commitSucceeded[0] = true;
                detachSourceNode(node);
                return;
            }
            ghostSequence.stop();
            cleanupFailure.run();
        });

        ParallelTransition sequence = new ParallelTransition(ghostSequence, sourceRelease, commitTransition);
        sequence.setOnFinished(event -> {
            overlayHost.getChildren().remove(ghost);
            if (commitSucceeded[0]) {
                hideCommittedNode(node);
                safeListener.onFinished(true);
                return;
            }
            if (failureHandled[0]) {
                return;
            }
            if (!commitResolved[0]) {
                commitResolved[0] = true;
                safeListener.onCommitPoint();
                boolean success = commitAction == null || commitAction.getAsBoolean();
                if (success) {
                    hideCommittedNode(node);
                    safeListener.onFinished(true);
                    return;
                }
            }
            cleanupFailure.run();
        });
        sequence.playFromStart();
    }

    public static void playShakeOnly(
        Node node,
        ArchiveAnimationListener listener,
        BooleanSupplier commitAction,
        Runnable onFailure
    ) {
        ArchiveAnimationListener safeListener = listener != null ? listener : NO_OP_LISTENER;
        if (node == null) {
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (!success && onFailure != null) {
                onFailure.run();
            }
            safeListener.onFinished(success);
            return;
        }

        node.setMouseTransparent(true);
        safeListener.onDropStart();

        Timeline shakeOnly = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(node.translateXProperty(), 0.0),
                new KeyValue(node.translateYProperty(), 0.0),
                new KeyValue(node.rotateProperty(), 0.0),
                new KeyValue(node.scaleXProperty(), 1.0),
                new KeyValue(node.scaleYProperty(), 1.0),
                new KeyValue(node.opacityProperty(), 1.0)
            ),
            new KeyFrame(
                Duration.millis(72),
                new KeyValue(node.translateXProperty(), -1.8, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 0.8, SHAKE_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), -0.9, SHAKE_INTERPOLATOR)
            ),
            new KeyFrame(
                Duration.millis(148),
                new KeyValue(node.translateXProperty(), 1.8, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 1.9, SHAKE_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), 1.1, SHAKE_INTERPOLATOR)
            ),
            new KeyFrame(
                SHAKE_DURATION,
                new KeyValue(node.translateXProperty(), 0.4, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 3.8, SHAKE_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), 0.25, SHAKE_INTERPOLATOR),
                new KeyValue(node.scaleXProperty(), 0.998, SHAKE_INTERPOLATOR),
                new KeyValue(node.scaleYProperty(), 0.998, SHAKE_INTERPOLATOR),
                new KeyValue(node.opacityProperty(), 0.92, SHAKE_INTERPOLATOR)
            )
        );

        shakeOnly.setOnFinished(event -> {
            safeListener.onCommitPoint();
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (success) {
                hideCommittedNode(node);
                safeListener.onFinished(true);
                return;
            }
            restoreNode(node);
            safeListener.onFinished(false);
            if (onFailure != null) {
                onFailure.run();
            }
        });
        shakeOnly.playFromStart();
    }

    public static void playDropToCompleted(Node node, BooleanSupplier commitAction, Runnable onFailure) {
        playLocalFallback(node, NO_OP_LISTENER, commitAction, onFailure);
    }

    public static void playDropToStaging(
        Node node,
        Point2D stagingScenePoint,
        StagedArchiveAnimationListener listener,
        BooleanSupplier commitAction,
        Runnable onFailure
    ) {
        StagedArchiveAnimationListener safeListener = listener != null ? listener : new StagedArchiveAnimationListener() {
        };
        if (node == null) {
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (!success && onFailure != null) {
                onFailure.run();
            }
            safeListener.onFinished(success);
            return;
        }

        if (node.getScene() == null || stagingScenePoint == null || !(node.getScene().getRoot() instanceof Pane)) {
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (!success && onFailure != null) {
                onFailure.run();
            }
            safeListener.onFinished(success);
            return;
        }

        Pane overlayHost = (Pane) node.getScene().getRoot();
        Bounds sourceSceneBounds = node.localToScene(node.getBoundsInLocal());
        if (sourceSceneBounds == null) {
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (!success && onFailure != null) {
                onFailure.run();
            }
            safeListener.onFinished(success);
            return;
        }

        WritableImage snapshot = snapshotNode(node);
        Point2D sourceRootPoint = overlayHost.sceneToLocal(sourceSceneBounds.getMinX(), sourceSceneBounds.getMinY());
        Point2D targetRootCenter = overlayHost.sceneToLocal(stagingScenePoint);

        double sourceX = sourceRootPoint.getX();
        double sourceY = sourceRootPoint.getY();
        double targetX = targetRootCenter.getX() - sourceSceneBounds.getWidth() / 2.0;
        double targetY = targetRootCenter.getY() - sourceSceneBounds.getHeight() / 2.0;
        double rawDeltaX = targetX - sourceX;
        double lateralMerge = clamp(rawDeltaX, -22.0, 22.0);
        double dropMidX = sourceX + clamp(lateralMerge * 0.05, -1.1, 1.1);
        double deltaY = Math.max(sourceSceneBounds.getHeight() + 18.0, targetY - sourceY);

        ImageView ghost = createGhostView(snapshot, sourceSceneBounds, sourceRootPoint);
        overlayHost.getChildren().add(ghost);
        ghost.toFront();

        node.setMouseTransparent(true);

        boolean[] commitSucceeded = {false};
        boolean[] commitResolved = {false};
        boolean[] failureHandled = {false};

        Timeline sourceRelease = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(node.opacityProperty(), node.getOpacity()),
                new KeyValue(node.translateXProperty(), node.getTranslateX()),
                new KeyValue(node.translateYProperty(), node.getTranslateY()),
                new KeyValue(node.rotateProperty(), node.getRotate()),
                new KeyValue(node.scaleXProperty(), node.getScaleX()),
                new KeyValue(node.scaleYProperty(), node.getScaleY())
            ),
            new KeyFrame(
                SHAKE_DURATION,
                new KeyValue(node.opacityProperty(), 0.04, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateXProperty(), 0.0, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 3.4, SHAKE_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), 0.0, SHAKE_INTERPOLATOR),
                new KeyValue(node.scaleXProperty(), 0.998, SHAKE_INTERPOLATOR),
                new KeyValue(node.scaleYProperty(), 0.998, SHAKE_INTERPOLATOR)
            )
        );

        Timeline shake = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(ghost.layoutXProperty(), sourceX),
                new KeyValue(ghost.layoutYProperty(), sourceY),
                new KeyValue(ghost.rotateProperty(), 0.0),
                new KeyValue(ghost.scaleXProperty(), 1.0),
                new KeyValue(ghost.scaleYProperty(), 1.0),
                new KeyValue(ghost.opacityProperty(), 1.0)
            ),
            new KeyFrame(
                Duration.millis(72),
                new KeyValue(ghost.layoutXProperty(), sourceX - 1.8, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), sourceY + 0.8, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), -0.9, SHAKE_INTERPOLATOR)
            ),
            new KeyFrame(
                Duration.millis(148),
                new KeyValue(ghost.layoutXProperty(), sourceX + 1.8, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), sourceY + 1.9, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), 1.1, SHAKE_INTERPOLATOR)
            ),
            new KeyFrame(
                SHAKE_DURATION,
                new KeyValue(ghost.layoutXProperty(), sourceX + 0.4, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), sourceY + 3.8, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), 0.25, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.scaleXProperty(), 0.998, SHAKE_INTERPOLATOR),
                new KeyValue(ghost.scaleYProperty(), 0.998, SHAKE_INTERPOLATOR)
            )
        );

        Timeline drop = new Timeline(
            new KeyFrame(
                Duration.millis(DROP_DURATION.toMillis() * 0.72),
                new KeyValue(ghost.layoutXProperty(), dropMidX, DROP_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), sourceY + deltaY * 0.72, DROP_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), 0.48, DROP_INTERPOLATOR),
                new KeyValue(ghost.scaleXProperty(), 0.993, DROP_INTERPOLATOR),
                new KeyValue(ghost.scaleYProperty(), 0.993, DROP_INTERPOLATOR),
                new KeyValue(ghost.opacityProperty(), 0.94, DROP_INTERPOLATOR)
            ),
            new KeyFrame(
                DROP_DURATION,
                new KeyValue(ghost.layoutXProperty(), targetX, DROP_INTERPOLATOR),
                new KeyValue(ghost.layoutYProperty(), targetY, DROP_INTERPOLATOR),
                new KeyValue(ghost.rotateProperty(), 0.22, DROP_INTERPOLATOR),
                new KeyValue(ghost.scaleXProperty(), 0.986, DROP_INTERPOLATOR),
                new KeyValue(ghost.scaleYProperty(), 0.986, DROP_INTERPOLATOR),
                new KeyValue(ghost.opacityProperty(), 1.0, DROP_INTERPOLATOR)
            )
        );

        shake.setOnFinished(event -> safeListener.onDropStart());

        SequentialTransition ghostSequence = new SequentialTransition(shake, drop);
        PauseTransition commitTransition = new PauseTransition(Duration.millis(
            SHAKE_DURATION.toMillis() + COMMIT_AFTER_DROP_START.toMillis()
        ));

        Runnable cleanupFailure = () -> {
            if (failureHandled[0]) {
                return;
            }
            failureHandled[0] = true;
            overlayHost.getChildren().remove(ghost);
            restoreNode(node);
            safeListener.onFinished(false);
            if (onFailure != null) {
                onFailure.run();
            }
        };

        commitTransition.setOnFinished(event -> {
            commitResolved[0] = true;
            safeListener.onCommitPoint();
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (success) {
                commitSucceeded[0] = true;
                detachSourceNode(node);
                return;
            }
            ghostSequence.stop();
            cleanupFailure.run();
        });

        ParallelTransition sequence = new ParallelTransition(ghostSequence, sourceRelease, commitTransition);
        sequence.setOnFinished(event -> {
            if (failureHandled[0]) {
                return;
            }
            if (commitSucceeded[0]) {
                safeListener.onReadyToSettle(new StagedLanding(
                    overlayHost,
                    ghost,
                    node,
                    safeListener,
                    onFailure
                ));
                return;
            }
            if (!commitResolved[0]) {
                commitResolved[0] = true;
                safeListener.onCommitPoint();
                boolean success = commitAction == null || commitAction.getAsBoolean();
                if (success) {
                    safeListener.onReadyToSettle(new StagedLanding(
                        overlayHost,
                        ghost,
                        node,
                        safeListener,
                        onFailure
                    ));
                    return;
                }
            }
            cleanupFailure.run();
        });
        sequence.playFromStart();
    }

    private static void playLocalFallback(
        Node node,
        ArchiveAnimationListener listener,
        BooleanSupplier commitAction,
        Runnable onFailure
    ) {
        if (node == null) {
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (!success && onFailure != null) {
                onFailure.run();
            }
            listener.onFinished(success);
            return;
        }

        node.setMouseTransparent(true);

        boolean[] commitSucceeded = {false};
        boolean[] commitResolved = {false};
        boolean[] failureHandled = {false};

        Timeline motion = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(node.translateXProperty(), 0.0),
                new KeyValue(node.translateYProperty(), 0.0),
                new KeyValue(node.rotateProperty(), 0.0),
                new KeyValue(node.scaleXProperty(), 1.0),
                new KeyValue(node.scaleYProperty(), 1.0),
                new KeyValue(node.opacityProperty(), 1.0)
            ),
            new KeyFrame(
                Duration.millis(72),
                new KeyValue(node.translateXProperty(), -1.8, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 0.8, SHAKE_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), -0.9, SHAKE_INTERPOLATOR)
            ),
            new KeyFrame(
                Duration.millis(148),
                new KeyValue(node.translateXProperty(), 1.8, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 1.9, SHAKE_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), 1.1, SHAKE_INTERPOLATOR)
            ),
            new KeyFrame(
                SHAKE_DURATION,
                new KeyValue(node.translateXProperty(), 0.4, SHAKE_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 3.8, SHAKE_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), 0.25, SHAKE_INTERPOLATOR),
                new KeyValue(node.opacityProperty(), 0.92, SHAKE_INTERPOLATOR)
            ),
            new KeyFrame(
                Duration.millis(SHAKE_DURATION.toMillis() + DROP_DURATION.toMillis()),
                new KeyValue(node.translateXProperty(), 0.0, DROP_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 28.0, DROP_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), 0.55, DROP_INTERPOLATOR),
                new KeyValue(node.scaleXProperty(), 0.972, DROP_INTERPOLATOR),
                new KeyValue(node.scaleYProperty(), 0.972, DROP_INTERPOLATOR),
                new KeyValue(node.opacityProperty(), 0.44, DROP_INTERPOLATOR)
            ),
            new KeyFrame(
                Duration.millis(SHAKE_DURATION.toMillis() + DROP_DURATION.toMillis() + ABSORB_DURATION.toMillis()),
                new KeyValue(node.translateXProperty(), 0.0, ABSORB_INTERPOLATOR),
                new KeyValue(node.translateYProperty(), 36.0, ABSORB_INTERPOLATOR),
                new KeyValue(node.rotateProperty(), 0.2, ABSORB_INTERPOLATOR),
                new KeyValue(node.scaleXProperty(), 0.88, ABSORB_INTERPOLATOR),
                new KeyValue(node.scaleYProperty(), 0.88, ABSORB_INTERPOLATOR),
                new KeyValue(node.opacityProperty(), 0.0, ABSORB_INTERPOLATOR)
            )
        );

        PauseTransition dropStart = new PauseTransition(SHAKE_DURATION);
        dropStart.setOnFinished(event -> listener.onDropStart());

        Runnable cleanupFailure = () -> {
            if (failureHandled[0]) {
                return;
            }
            failureHandled[0] = true;
            restoreNode(node);
            listener.onFinished(false);
            if (onFailure != null) {
                onFailure.run();
            }
        };

        PauseTransition commitTransition = new PauseTransition(Duration.millis(
            SHAKE_DURATION.toMillis() + COMMIT_AFTER_DROP_START.toMillis()
        ));
        commitTransition.setOnFinished(event -> {
            commitResolved[0] = true;
            listener.onCommitPoint();
            boolean success = commitAction == null || commitAction.getAsBoolean();
            if (success) {
                commitSucceeded[0] = true;
                return;
            }
            motion.stop();
            cleanupFailure.run();
        });

        ParallelTransition sequence = new ParallelTransition(motion, dropStart, commitTransition);
        sequence.setOnFinished(event -> {
            if (commitSucceeded[0]) {
                hideCommittedNode(node);
                listener.onFinished(true);
                return;
            }
            if (failureHandled[0]) {
                return;
            }
            if (!commitResolved[0]) {
                commitResolved[0] = true;
                listener.onCommitPoint();
                boolean success = commitAction == null || commitAction.getAsBoolean();
                if (success) {
                    hideCommittedNode(node);
                    listener.onFinished(true);
                    return;
                }
            }
            cleanupFailure.run();
        });
        sequence.playFromStart();
    }

    private static void restoreNode(Node node) {
        node.setVisible(true);
        node.setManaged(true);
        node.setTranslateX(0.0);
        node.setTranslateY(0.0);
        node.setRotate(0.0);
        node.setOpacity(1.0);
        node.setScaleX(1.0);
        node.setScaleY(1.0);
        node.setMouseTransparent(false);
    }

    private static void hideCommittedNode(Node node) {
        detachSourceNode(node);
        node.setMouseTransparent(false);
    }

    private static void detachSourceNode(Node node) {
        node.setVisible(false);
        node.setManaged(false);
        node.setOpacity(0.0);
        node.setTranslateX(0.0);
        node.setTranslateY(0.0);
        node.setRotate(0.0);
        node.setScaleX(1.0);
        node.setScaleY(1.0);
        node.setMouseTransparent(true);
    }

    private static WritableImage snapshotNode(Node node) {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return node.snapshot(parameters, null);
    }

    private static ImageView createGhostView(WritableImage snapshot, Bounds sourceSceneBounds, Point2D sourceRootPoint) {
        ImageView ghost = new ImageView(snapshot);
        ghost.setManaged(false);
        ghost.setMouseTransparent(true);
        ghost.setLayoutX(sourceRootPoint.getX());
        ghost.setLayoutY(sourceRootPoint.getY());
        ghost.setFitWidth(sourceSceneBounds.getWidth());
        ghost.setFitHeight(sourceSceneBounds.getHeight());
        ghost.setPreserveRatio(false);
        ghost.setSmooth(true);
        ghost.setCache(true);
        ghost.setEffect(new DropShadow(10.0, 0.0, 4.0, GHOST_SHADOW));
        return ghost;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
