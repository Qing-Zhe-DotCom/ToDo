package com.example.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.example.model.Schedule;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Duration;

public final class ScheduleReflowAnimator {
    private static final String CARD_ID_KEY = "schedule.reflow.cardId";
    private static final String CARD_COMPLETED_KEY = "schedule.reflow.completed";
    private static final String SHIFT_TIMELINE_KEY = "schedule.reflow.shiftTimeline";
    private static final String TARGET_TIMELINE_KEY = "schedule.reflow.targetTimeline";
    private static final Duration REFLOW_DURATION = Duration.millis(320);
    private static final Duration TARGET_FEEDBACK_DURATION = Duration.millis(320);
    private static final Duration COLLAPSED_RECEIVE_DURATION = Duration.millis(320);
    private static final Interpolator LIVE_SHIFT_INTERPOLATOR = Interpolator.SPLINE(0.14, 0.78, 0.2, 1.0);
    private static final Interpolator REFLOW_INTERPOLATOR = Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0);
    private static final Interpolator TARGET_INTERPOLATOR = Interpolator.SPLINE(0.2, 0.76, 0.22, 1.0);

    private ScheduleReflowAnimator() {
    }

    public static final class VisibleCard {
        private final int scheduleId;
        private final Node node;
        private final Bounds bounds;
        private final boolean completed;

        VisibleCard(int scheduleId, Node node, Bounds bounds, boolean completed) {
            this.scheduleId = scheduleId;
            this.node = node;
            this.bounds = bounds;
            this.completed = completed;
        }

        public int getScheduleId() {
            return scheduleId;
        }

        public Node getNode() {
            return node;
        }

        public Bounds getBounds() {
            return bounds;
        }

        public boolean isCompleted() {
            return completed;
        }
    }

    public static void bindCard(Node node, Schedule schedule) {
        if (node == null || schedule == null || schedule.getId() <= 0) {
            return;
        }
        node.getProperties().put(CARD_ID_KEY, schedule.getId());
        node.getProperties().put(CARD_COMPLETED_KEY, schedule.isCompleted());
    }

    public static Map<Integer, VisibleCard> captureVisibleCards(Node container) {
        Map<Integer, VisibleCard> cardsById = new LinkedHashMap<>();
        collectVisibleCards(container, cardsById);
        return cardsById;
    }

    public static Map<Integer, Bounds> captureVisibleCardBounds(Node container, int excludedScheduleId) {
        Map<Integer, Bounds> boundsById = new LinkedHashMap<>();
        for (VisibleCard card : captureVisibleCards(container).values()) {
            if (card.getScheduleId() != excludedScheduleId) {
                boundsById.put(card.getScheduleId(), card.getBounds());
            }
        }
        return boundsById;
    }

    public static VisibleCard findVisibleCardById(Node container, int scheduleId) {
        if (scheduleId <= 0) {
            return null;
        }
        return findVisibleCard(container, scheduleId, null);
    }

    public static VisibleCard findVisibleCardByIdAndCompletion(Node container, int scheduleId, boolean completed) {
        if (scheduleId <= 0) {
            return null;
        }
        return findVisibleCard(container, scheduleId, completed);
    }

    public static void animateLiveShift(Node node, double targetTranslateY, Duration duration) {
        if (node == null) {
            return;
        }

        stopTimeline(node, SHIFT_TIMELINE_KEY);
        Timeline timeline = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(node.translateYProperty(), node.getTranslateY())
            ),
            new KeyFrame(
                duration,
                new KeyValue(node.translateYProperty(), targetTranslateY, LIVE_SHIFT_INTERPOLATOR)
            )
        );
        timeline.setOnFinished(event -> {
            node.setTranslateY(targetTranslateY);
            node.getProperties().remove(SHIFT_TIMELINE_KEY);
        });
        node.getProperties().put(SHIFT_TIMELINE_KEY, timeline);
        timeline.playFromStart();
    }

    public static void playVerticalReflow(
        Node container,
        Map<Integer, Bounds> beforeBounds,
        Supplier<Node> targetSupplier
    ) {
        playVerticalReflow(
            container,
            beforeBounds,
            targetSupplier,
            REFLOW_DURATION,
            TARGET_FEEDBACK_DURATION,
            null
        );
    }

    public static void playVerticalReflow(
        Node container,
        Map<Integer, Bounds> beforeBounds,
        Supplier<Node> targetSupplier,
        Duration reflowDuration,
        Duration targetFeedbackDuration
    ) {
        playVerticalReflow(container, beforeBounds, targetSupplier, reflowDuration, targetFeedbackDuration, null);
    }

    public static void playVerticalReflow(
        Node container,
        Map<Integer, Bounds> beforeBounds,
        Supplier<Node> targetSupplier,
        Duration reflowDuration,
        Duration targetFeedbackDuration,
        Runnable onFinished
    ) {
        if (container == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        Duration safeReflowDuration = reflowDuration != null ? reflowDuration : REFLOW_DURATION;
        Duration safeTargetDuration = targetFeedbackDuration != null ? targetFeedbackDuration : TARGET_FEEDBACK_DURATION;

        Platform.runLater(() -> Platform.runLater(() -> {
            Map<Integer, VisibleCard> visibleCards = captureVisibleCards(container);
            Map<VisibleCard, Timeline> reflowTimelines = new LinkedHashMap<>();
            for (VisibleCard card : visibleCards.values()) {
                Bounds before = beforeBounds.get(card.getScheduleId());
                if (before == null) {
                    continue;
                }

                Bounds after = card.getBounds();
                if (after == null) {
                    continue;
                }

                double deltaY = before.getMinY() - after.getMinY();
                if (Math.abs(deltaY) < 1.0) {
                    continue;
                }

                stopTimeline(card.getNode(), SHIFT_TIMELINE_KEY);
                card.getNode().setTranslateY(deltaY);
                Timeline reflow = new Timeline(
                    new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(card.getNode().translateYProperty(), deltaY)
                    ),
                    new KeyFrame(
                        safeReflowDuration,
                        new KeyValue(card.getNode().translateYProperty(), 0.0, REFLOW_INTERPOLATOR)
                    )
                );
                reflow.setOnFinished(event -> {
                    card.getNode().setTranslateY(0.0);
                    card.getNode().getProperties().remove(SHIFT_TIMELINE_KEY);
                });
                card.getNode().getProperties().put(SHIFT_TIMELINE_KEY, reflow);
                reflowTimelines.put(card, reflow);
            }

            Node targetNode = targetSupplier != null ? targetSupplier.get() : null;
            int completionCount = reflowTimelines.size() + (targetNode != null ? 1 : 0);
            if (completionCount == 0) {
                if (onFinished != null) {
                    onFinished.run();
                }
                return;
            }

            final int[] pendingCallbacks = {completionCount};
            Runnable completeOne = () -> {
                pendingCallbacks[0]--;
                if (pendingCallbacks[0] == 0 && onFinished != null) {
                    onFinished.run();
                }
            };

            for (Map.Entry<VisibleCard, Timeline> entry : reflowTimelines.entrySet()) {
                VisibleCard card = entry.getKey();
                Timeline reflowTimeline = entry.getValue();
                reflowTimeline.setOnFinished(event -> {
                    card.getNode().setTranslateY(0.0);
                    card.getNode().getProperties().remove(SHIFT_TIMELINE_KEY);
                    completeOne.run();
                });
                reflowTimeline.playFromStart();
            }

            if (targetNode != null) {
                playTargetPulse(targetNode, safeTargetDuration, completeOne);
            }
        }));
    }

    public static void playTargetPulse(Node node) {
        playTargetPulse(node, TARGET_FEEDBACK_DURATION);
    }

    public static void playTargetPulse(Node node, Duration duration) {
        playTargetPulse(node, duration, null);
    }

    public static void playTargetPulse(Node node, Duration duration, Runnable onFinished) {
        if (node == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        Duration safeDuration = duration != null ? duration : TARGET_FEEDBACK_DURATION;
        double midpointMillis = safeDuration.toMillis() * 0.46;

        stopTimeline(node, TARGET_TIMELINE_KEY);
        double baseScaleX = node.getScaleX();
        double baseScaleY = node.getScaleY();
        double baseOpacity = node.getOpacity();

        Timeline pulse = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(node.scaleXProperty(), baseScaleX),
                new KeyValue(node.scaleYProperty(), baseScaleY),
                new KeyValue(node.opacityProperty(), baseOpacity)
            ),
            new KeyFrame(
                Duration.millis(midpointMillis),
                new KeyValue(node.scaleXProperty(), baseScaleX * 1.012, TARGET_INTERPOLATOR),
                new KeyValue(node.scaleYProperty(), baseScaleY * 1.012, TARGET_INTERPOLATOR),
                new KeyValue(node.opacityProperty(), Math.max(0.92, baseOpacity * 0.97), TARGET_INTERPOLATOR)
            ),
            new KeyFrame(
                safeDuration,
                new KeyValue(node.scaleXProperty(), baseScaleX, TARGET_INTERPOLATOR),
                new KeyValue(node.scaleYProperty(), baseScaleY, TARGET_INTERPOLATOR),
                new KeyValue(node.opacityProperty(), baseOpacity, TARGET_INTERPOLATOR)
            )
        );
        pulse.setOnFinished(event -> {
            node.setScaleX(baseScaleX);
            node.setScaleY(baseScaleY);
            node.setOpacity(baseOpacity);
            node.getProperties().remove(TARGET_TIMELINE_KEY);
            if (onFinished != null) {
                onFinished.run();
            }
        });
        node.getProperties().put(TARGET_TIMELINE_KEY, pulse);
        pulse.playFromStart();
    }

    public static void playCollapsedReceive(Node node) {
        playCollapsedReceive(node, COLLAPSED_RECEIVE_DURATION);
    }

    public static void playCollapsedReceive(Node node, Duration duration) {
        playCollapsedReceive(node, duration, null);
    }

    public static void playCollapsedReceive(Node node, Duration duration, Runnable onFinished) {
        if (node == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        Duration safeDuration = duration != null ? duration : COLLAPSED_RECEIVE_DURATION;
        double midpointMillis = safeDuration.toMillis() * 0.46;

        stopTimeline(node, TARGET_TIMELINE_KEY);
        double baseTranslateY = node.getTranslateY();
        double baseOpacity = node.getOpacity();

        Timeline receive = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(node.translateYProperty(), baseTranslateY),
                new KeyValue(node.opacityProperty(), baseOpacity)
            ),
            new KeyFrame(
                Duration.millis(midpointMillis),
                new KeyValue(node.translateYProperty(), baseTranslateY + 4.0, TARGET_INTERPOLATOR),
                new KeyValue(node.opacityProperty(), Math.max(0.88, baseOpacity * 0.94), TARGET_INTERPOLATOR)
            ),
            new KeyFrame(
                safeDuration,
                new KeyValue(node.translateYProperty(), baseTranslateY, TARGET_INTERPOLATOR),
                new KeyValue(node.opacityProperty(), baseOpacity, TARGET_INTERPOLATOR)
            )
        );
        receive.setOnFinished(event -> {
            node.setTranslateY(baseTranslateY);
            node.setOpacity(baseOpacity);
            node.getProperties().remove(TARGET_TIMELINE_KEY);
            if (onFinished != null) {
                onFinished.run();
            }
        });
        node.getProperties().put(TARGET_TIMELINE_KEY, receive);
        receive.playFromStart();
    }

    private static void collectVisibleCards(Node node, Map<Integer, VisibleCard> cardsById) {
        if (node == null) {
            return;
        }

        int cardId = extractCardId(node);
        if (cardId > 0 && node.isVisible() && node.isManaged()) {
            Bounds bounds = resolveCardBounds(node);
            if (bounds != null) {
                cardsById.putIfAbsent(cardId, new VisibleCard(cardId, node, bounds, extractCompleted(node)));
            }
        }

        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                collectVisibleCards(child, cardsById);
            }
        }
    }

    private static VisibleCard findVisibleCard(Node node, int scheduleId, Boolean completed) {
        if (node == null) {
            return null;
        }

        int cardId = extractCardId(node);
        if (cardId == scheduleId && node.isVisible() && node.isManaged()) {
            boolean nodeCompleted = extractCompleted(node);
            if (completed == null || nodeCompleted == completed.booleanValue()) {
                Bounds bounds = resolveCardBounds(node);
                if (bounds != null) {
                    return new VisibleCard(cardId, node, bounds, nodeCompleted);
                }
            }
        }

        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                VisibleCard found = findVisibleCard(child, scheduleId, completed);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int extractCardId(Node node) {
        Object rawValue = node.getProperties().get(CARD_ID_KEY);
        if (rawValue instanceof Integer) {
            return (Integer) rawValue;
        }
        if (rawValue instanceof Long) {
            return ((Long) rawValue).intValue();
        }
        return -1;
    }

    private static boolean extractCompleted(Node node) {
        Object rawValue = node.getProperties().get(CARD_COMPLETED_KEY);
        if (rawValue instanceof Boolean) {
            return (Boolean) rawValue;
        }
        return false;
    }

    private static Bounds resolveCardBounds(Node node) {
        Bounds sceneBounds = node.localToScene(node.getBoundsInLocal());
        if (sceneBounds != null) {
            return sceneBounds;
        }
        return node.getBoundsInParent();
    }

    private static void stopTimeline(Node node, String key) {
        Object timeline = node.getProperties().get(key);
        if (timeline instanceof Timeline) {
            ((Timeline) timeline).stop();
        }
    }
}
