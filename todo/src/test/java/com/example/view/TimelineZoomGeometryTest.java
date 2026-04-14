package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimelineZoomGeometryTest {

    @Test
    void anchorMathKeepsViewportAnchorStableAcrossZoomLevels() {
        double basePixelsPerMinute = TimelineZoomGeometry.pixelsPerMinute(90.0, 1.0, 360);
        double zoomedPixelsPerMinute = TimelineZoomGeometry.pixelsPerMinute(90.0, 2.0, 360);
        double viewportWidth = 320.0;
        double viewportX = 180.0;
        long totalMinutes = 7L * 24L * 60L;

        double baseContentWidth = TimelineZoomGeometry.totalWidth(36.0, 36.0, totalMinutes, basePixelsPerMinute);
        double zoomedContentWidth = TimelineZoomGeometry.totalWidth(36.0, 36.0, totalMinutes, zoomedPixelsPerMinute);
        double minuteOffset = TimelineZoomGeometry.minuteOffsetAtViewportX(
            viewportX,
            0.33,
            viewportWidth,
            baseContentWidth,
            36.0,
            basePixelsPerMinute,
            totalMinutes
        );

        double targetHvalue = TimelineZoomGeometry.hvalueForAnchor(
            minuteOffset,
            viewportX,
            viewportWidth,
            zoomedContentWidth,
            36.0,
            zoomedPixelsPerMinute
        );
        double roundTripMinuteOffset = TimelineZoomGeometry.minuteOffsetAtViewportX(
            viewportX,
            targetHvalue,
            viewportWidth,
            zoomedContentWidth,
            36.0,
            zoomedPixelsPerMinute,
            totalMinutes
        );

        assertEquals(minuteOffset, roundTripMinuteOffset, 0.0001);
    }

    @Test
    void hvalueIsClampedAtBothEdges() {
        double pixelsPerMinute = TimelineZoomGeometry.pixelsPerMinute(90.0, 1.0, 360);
        double contentWidth = TimelineZoomGeometry.totalWidth(36.0, 36.0, 1_440, pixelsPerMinute);

        assertEquals(
            0.0,
            TimelineZoomGeometry.hvalueForAnchor(-120.0, 80.0, 300.0, contentWidth, 36.0, pixelsPerMinute),
            0.0001
        );
        assertEquals(
            1.0,
            TimelineZoomGeometry.hvalueForAnchor(99_999.0, 220.0, 300.0, contentWidth, 36.0, pixelsPerMinute),
            0.0001
        );
    }

    @Test
    void noScrollableWidthFallsBackToZeroOffsets() {
        double pixelsPerMinute = TimelineZoomGeometry.pixelsPerMinute(90.0, 1.0, 360);
        double contentWidth = TimelineZoomGeometry.totalWidth(36.0, 36.0, 120, pixelsPerMinute);

        assertEquals(
            0.0,
            TimelineZoomGeometry.minuteOffsetAtViewportX(120.0, 0.8, 600.0, contentWidth, 36.0, pixelsPerMinute, 120),
            0.0001
        );
        assertEquals(
            0.0,
            TimelineZoomGeometry.hvalueForAnchor(40.0, 120.0, 600.0, contentWidth, 36.0, pixelsPerMinute),
            0.0001
        );
    }
}
