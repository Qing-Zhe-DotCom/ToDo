package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScheduleStatusInteractionModelTest {

    @Test
    void quickClickKeepsForwardPreviewAndCommitsToCompleted() {
        ScheduleStatusInteractionModel model = new ScheduleStatusInteractionModel(false);

        assertTrue(model.beginPendingPreview());
        assertEquals(
            ScheduleStatusInteractionModel.ReleaseDecision.KEEP_FORWARD_RUNNING,
            model.releasePending(false, false)
        );
        assertTrue(model.finishForwardPreview());
        assertEquals(ScheduleStatusInteractionModel.VisualState.COMPLETED_IDLE, model.getVisualState());
    }

    @Test
    void longPressReleaseBeforeFullDrawStartsRollback() {
        ScheduleStatusInteractionModel model = new ScheduleStatusInteractionModel(false);

        assertTrue(model.beginPendingPreview());
        assertEquals(
            ScheduleStatusInteractionModel.ReleaseDecision.START_ROLLBACK,
            model.releasePending(true, false)
        );
        assertTrue(model.finishRollback());
        assertEquals(ScheduleStatusInteractionModel.VisualState.PENDING_IDLE, model.getVisualState());
    }

    @Test
    void completedDrawCannotBeInterrupted() {
        ScheduleStatusInteractionModel model = new ScheduleStatusInteractionModel(false);

        assertTrue(model.beginPendingPreview());
        assertEquals(
            ScheduleStatusInteractionModel.ReleaseDecision.IGNORE,
            model.releasePending(true, true)
        );
        assertTrue(model.finishForwardPreview());
        assertEquals(ScheduleStatusInteractionModel.VisualState.COMPLETED_IDLE, model.getVisualState());
    }

    @Test
    void completedClickCanReverseBackToPending() {
        ScheduleStatusInteractionModel model = new ScheduleStatusInteractionModel(true);

        assertTrue(model.beginCompletedReverse());
        assertTrue(model.finishCompletedReverse());
        assertEquals(ScheduleStatusInteractionModel.VisualState.PENDING_IDLE, model.getVisualState());
    }

    @Test
    void busyStateRejectsDuplicateStarts() {
        ScheduleStatusInteractionModel model = new ScheduleStatusInteractionModel(false);

        assertTrue(model.beginPendingPreview());
        assertTrue(model.isBusy());
        assertFalse(model.beginPendingPreview());
        assertFalse(model.beginCompletedReverse());
    }
}
