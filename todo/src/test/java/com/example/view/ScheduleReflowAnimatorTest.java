package com.example.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.example.model.Schedule;

import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

class ScheduleReflowAnimatorTest {

    @Test
    void completedAwareLookupFindsMatchingNodeWhenIdsOverlap() {
        VBox container = new VBox();
        StackPane pending = createCardNode();
        StackPane completed = createCardNode();
        container.getChildren().addAll(pending, completed);

        Schedule pendingSchedule = new Schedule();
        pendingSchedule.setId(42);
        pendingSchedule.setCompleted(false);
        Schedule completedSchedule = new Schedule();
        completedSchedule.setId(42);
        completedSchedule.setCompleted(true);

        ScheduleReflowAnimator.bindCard(pending, pendingSchedule);
        ScheduleReflowAnimator.bindCard(completed, completedSchedule);

        ScheduleReflowAnimator.VisibleCard pendingCard =
            ScheduleReflowAnimator.findVisibleCardByIdAndCompletion(container, 42, false);
        ScheduleReflowAnimator.VisibleCard completedCard =
            ScheduleReflowAnimator.findVisibleCardByIdAndCompletion(container, 42, true);

        assertNotNull(pendingCard);
        assertNotNull(completedCard);
        assertSame(pending, pendingCard.getNode());
        assertSame(completed, completedCard.getNode());
    }

    @Test
    void detachedSourceNodeNoLongerParticipatesInLookup() {
        VBox container = new VBox();
        StackPane pending = createCardNode();
        StackPane completed = createCardNode();
        container.getChildren().addAll(pending, completed);

        Schedule pendingSchedule = new Schedule();
        pendingSchedule.setId(77);
        pendingSchedule.setCompleted(false);
        Schedule completedSchedule = new Schedule();
        completedSchedule.setId(77);
        completedSchedule.setCompleted(true);

        ScheduleReflowAnimator.bindCard(pending, pendingSchedule);
        ScheduleReflowAnimator.bindCard(completed, completedSchedule);

        pending.setVisible(false);
        pending.setManaged(false);

        assertNull(ScheduleReflowAnimator.findVisibleCardByIdAndCompletion(container, 77, false));
        assertSame(completed, ScheduleReflowAnimator.findVisibleCardById(container, 77).getNode());
    }

    private StackPane createCardNode() {
        StackPane node = new StackPane();
        node.setManaged(true);
        node.setVisible(true);
        node.setPrefSize(120, 32);
        node.resize(120, 32);
        return node;
    }
}
