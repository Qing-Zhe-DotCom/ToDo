package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import javafx.util.Duration;

class CollapsePopKeyframePresetTest {

    @Test
    void sourceCollapseKeyframesStayPinnedToApprovedScales() {
        assertEquals(0.8, CollapsePopKeyframePreset.sourceScaleAt(Duration.millis(100)), 0.0001);
        assertEquals(0.5, CollapsePopKeyframePreset.sourceScaleAt(Duration.millis(200)), 0.0001);
    }

    @Test
    void targetPopKeyframesStayPinnedToApprovedScales() {
        assertEquals(0.8, CollapsePopKeyframePreset.targetScaleAt(Duration.millis(550)), 0.0001);
        assertEquals(1.1, CollapsePopKeyframePreset.targetScaleAt(Duration.millis(650)), 0.0001);
        assertEquals(0.95, CollapsePopKeyframePreset.targetScaleAt(Duration.millis(725)), 0.0001);
        assertEquals(1.0, CollapsePopKeyframePreset.targetScaleAt(Duration.millis(800)), 0.0001);
    }
}
