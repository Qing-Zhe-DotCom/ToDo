package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScheduleListViewLandingTargetTest {

    @Test
    void expandedGroupPrefersCardHandoff() {
        ScheduleListView.LandingTargetDecision decision =
            ScheduleListView.resolveLandingTargetDecision(false, true, true);

        assertEquals(ScheduleListView.LandingTargetKind.EXPANDED_CARD, decision.kind());
    }

    @Test
    void collapsedGroupUsesHeaderAbsorb() {
        ScheduleListView.LandingTargetDecision decision =
            ScheduleListView.resolveLandingTargetDecision(true, true, true);

        assertEquals(ScheduleListView.LandingTargetKind.COLLAPSED_HEADER, decision.kind());
    }

    @Test
    void missingTargetFallsBackCleanly() {
        ScheduleListView.LandingTargetDecision decision =
            ScheduleListView.resolveLandingTargetDecision(false, false, false);

        assertEquals(ScheduleListView.LandingTargetKind.FALLBACK, decision.kind());
    }
}
