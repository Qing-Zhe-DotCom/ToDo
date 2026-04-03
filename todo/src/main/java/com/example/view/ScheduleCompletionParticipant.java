package com.example.view;

import com.example.controller.ScheduleCompletionMutation;

public interface ScheduleCompletionParticipant {
    void applyCompletionMutation(ScheduleCompletionMutation mutation);

    default void confirmCompletionMutation(ScheduleCompletionMutation mutation) {
    }

    void revertCompletionMutation(ScheduleCompletionMutation mutation);
}
