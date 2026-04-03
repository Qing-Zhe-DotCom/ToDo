package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import javafx.util.Duration;

class CollapsePopKeyframePresetTest {

    @Test
    void sourceCollapseKeyframesStayPinnedToApprovedScales() {
        assertEquals(0.8, CollapsePopKeyframePreset.sourceScaleAt(Duration.millis(150)), 0.0001);
        assertEquals(0.5, CollapsePopKeyframePreset.sourceScaleAt(Duration.millis(300)), 0.0001);
    }

    @Test
    void targetPopKeyframesStayPinnedToApprovedScales() {
        assertEquals(0.8, CollapsePopKeyframePreset.targetScaleAt(Duration.millis(825)), 0.0001);
        assertEquals(1.1, CollapsePopKeyframePreset.targetScaleAt(Duration.millis(975)), 0.0001);
        assertEquals(0.95, CollapsePopKeyframePreset.targetScaleAt(Duration.millis(1087.5)), 0.0001);
        assertEquals(1.0, CollapsePopKeyframePreset.targetScaleAt(Duration.millis(1200)), 0.0001);
    }

    @Test
    void targetPopDurationTracksScaledTimeline() {
        assertEquals(525.0, CollapsePopKeyframePreset.targetPopDuration().toMillis(), 0.0001);
    }
}
