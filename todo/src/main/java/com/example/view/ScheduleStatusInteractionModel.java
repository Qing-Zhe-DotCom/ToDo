package com.example.view;

public final class ScheduleStatusInteractionModel {

    public enum VisualState {
        PENDING_IDLE,
        FORWARD_PREVIEW,
        ROLLBACK,
        COMPLETED_IDLE,
        REVERSE_TO_PENDING
    }

    public enum ReleaseDecision {
        KEEP_FORWARD_RUNNING,
        START_ROLLBACK,
        IGNORE
    }

    private VisualState visualState;

    public ScheduleStatusInteractionModel(boolean completed) {
        reset(completed);
    }

    public void reset(boolean completed) {
        visualState = completed ? VisualState.COMPLETED_IDLE : VisualState.PENDING_IDLE;
    }

    public VisualState getVisualState() {
        return visualState;
    }

    public boolean isBusy() {
        return visualState != VisualState.PENDING_IDLE && visualState != VisualState.COMPLETED_IDLE;
    }

    public boolean beginPendingPreview() {
        if (visualState != VisualState.PENDING_IDLE) {
            return false;
        }
        visualState = VisualState.FORWARD_PREVIEW;
        return true;
    }

    public ReleaseDecision releasePending(boolean holdThresholdReached, boolean drawCompleted) {
        if (visualState != VisualState.FORWARD_PREVIEW) {
            return ReleaseDecision.IGNORE;
        }
        if (drawCompleted) {
            return ReleaseDecision.IGNORE;
        }
        if (holdThresholdReached) {
            visualState = VisualState.ROLLBACK;
            return ReleaseDecision.START_ROLLBACK;
        }
        return ReleaseDecision.KEEP_FORWARD_RUNNING;
    }

    public boolean finishForwardPreview() {
        if (visualState != VisualState.FORWARD_PREVIEW) {
            return false;
        }
        visualState = VisualState.COMPLETED_IDLE;
        return true;
    }

    public boolean finishRollback() {
        if (visualState != VisualState.ROLLBACK) {
            return false;
        }
        visualState = VisualState.PENDING_IDLE;
        return true;
    }

    public boolean beginCompletedReverse() {
        if (visualState != VisualState.COMPLETED_IDLE) {
            return false;
        }
        visualState = VisualState.REVERSE_TO_PENDING;
        return true;
    }

    public boolean finishCompletedReverse() {
        if (visualState != VisualState.REVERSE_TO_PENDING) {
            return false;
        }
        visualState = VisualState.PENDING_IDLE;
        return true;
    }
}
