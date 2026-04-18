package com.example.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.example.model.ScheduleItem;

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
    private static final Duration REFLOW_DURATION = Duration.millis(380);
    private static final Duration TARGET_FEEDBACK_DURATION = Duration.millis(420);
    private static final Duration COLLAPSED_RECEIVE_DURATION = Duration.millis(350);
    
    // 更加平滑且带有惯性的流体曲线
    private static final Interpolator LIVE_SHIFT_INTERPOLATOR = Interpolator.SPLINE(0.1, 0.9, 0.2, 1.0);
    
    // 模拟物理质感的重排曲线：快速启动，带有微小的过冲感
    private static final Interpolator REFLOW_INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);
    
    // 重点反馈曲线：使用合法的强力 Ease-Out 曲线 (接近 Overshoot 感官但符合 [0,1] 限制)
    private static final Interpolator TARGET_INTERPOLATOR = Interpolator.SPLINE(0.1, 0.9, 0.2, 1.0);

    private ScheduleReflowAnimator() {
    }

    public static final class VisibleCard {
        private final String scheduleId;
        private final Node node;
        private final Bounds bounds;
        private final boolean completed;

        VisibleCard(String scheduleId, Node node, Bounds bounds, boolean completed) {
            this.scheduleId = scheduleId;
            this.node = node;
            this.bounds = bounds;
            this.completed = completed;
        }

        public String getScheduleId() {
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

    public static void bindCard(Node node, ScheduleItem schedule) {
        if (node == null || schedule == null) {
            return;
        }
        String cardId = schedule.getViewKey() != null && !schedule.getViewKey().isBlank()
            ? schedule.getViewKey()
            : schedule.getId();
        if (cardId == null || cardId.isBlank()) {
            return;
        }
        bindNode(node, cardId, schedule.isCompleted());
    }

    public static void bindNode(Node node, String id, boolean completed) {
        if (node == null || id == null || id.isBlank()) {
            return;
        }
        node.getProperties().put(CARD_ID_KEY, id);
        node.getProperties().put(CARD_COMPLETED_KEY, completed);
    }

    public static Map<String, VisibleCard> captureVisibleCards(Node container) {
        Map<String, VisibleCard> cardsById = new LinkedHashMap<>();
        collectVisibleCards(container, cardsById);
        return cardsById;
    }

    public static Map<String, Bounds> captureVisibleCardBounds(Node container, String excludedScheduleId) {
        Map<String, Bounds> boundsById = new LinkedHashMap<>();
        for (VisibleCard card : captureVisibleCards(container).values()) {
            if (!card.getScheduleId().equals(excludedScheduleId)) {
                boundsById.put(card.getScheduleId(), card.getBounds());
            }
        }
        return boundsById;
    }

    public static VisibleCard findVisibleCardById(Node container, String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return null;
        }
        return findVisibleCard(container, scheduleId, null);
    }

    @Deprecated
    public static VisibleCard findVisibleCardById(Node container, int scheduleId) {
        return findVisibleCardById(container, String.valueOf(scheduleId));
    }

    public static VisibleCard findVisibleCardByIdAndCompletion(Node container, String scheduleId, boolean completed) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return null;
        }
        return findVisibleCard(container, scheduleId, completed);
    }

    @Deprecated
    public static VisibleCard findVisibleCardByIdAndCompletion(Node container, int scheduleId, boolean completed) {
        return findVisibleCardByIdAndCompletion(container, String.valueOf(scheduleId), completed);
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
        Map<String, Bounds> beforeBounds,
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
        Map<String, Bounds> beforeBounds,
        Supplier<Node> targetSupplier,
        Duration reflowDuration,
        Duration targetFeedbackDuration
    ) {
        playVerticalReflow(container, beforeBounds, targetSupplier, reflowDuration, targetFeedbackDuration, null);
    }

    public static void playVerticalReflow(
        Node container,
        Map<String, Bounds> beforeBounds,
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

        if (container instanceof Parent) {
            ((Parent) container).applyCss();
            ((Parent) container).layout();
        }

        Map<String, VisibleCard> visibleCards = captureVisibleCards(container);
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

    private static void collectVisibleCards(Node node, Map<String, VisibleCard> cardsById) {
        if (node == null) {
            return;
        }

        String cardId = extractCardId(node);
        if (cardId != null && !cardId.isBlank() && node.isVisible() && node.isManaged()) {
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

    private static VisibleCard findVisibleCard(Node node, String scheduleId, Boolean completed) {
        if (node == null) {
            return null;
        }

        String cardId = extractCardId(node);
        if (scheduleId.equals(cardId) && node.isVisible() && node.isManaged()) {
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

    private static String extractCardId(Node node) {
        Object rawValue = node.getProperties().get(CARD_ID_KEY);
        if (rawValue instanceof String) {
            return (String) rawValue;
        }
        return null;
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
