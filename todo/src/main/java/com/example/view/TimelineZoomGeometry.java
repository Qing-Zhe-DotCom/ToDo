package com.example.view;

final class TimelineZoomGeometry {
    private TimelineZoomGeometry() {
    }

    static double pixelsPerMinute(double baseCellWidthPx, double zoomFactor, long baseCellMinutes) {
        if (baseCellWidthPx <= 0 || baseCellMinutes <= 0 || Double.isNaN(zoomFactor) || zoomFactor <= 0) {
            return 0.0;
        }
        return (baseCellWidthPx * zoomFactor) / baseCellMinutes;
    }

    static double totalWidth(double leftPadding, double rightPadding, long totalMinutes, double pixelsPerMinute) {
        if (pixelsPerMinute <= 0 || totalMinutes <= 0) {
            return Math.max(0.0, leftPadding + rightPadding);
        }
        return Math.max(0.0, leftPadding + rightPadding + totalMinutes * pixelsPerMinute);
    }

    static double scrollX(double hvalue, double viewportWidth, double contentWidth) {
        double maxScroll = Math.max(0.0, contentWidth - viewportWidth);
        if (maxScroll <= 0) {
            return 0.0;
        }
        return clamp(hvalue, 0.0, 1.0) * maxScroll;
    }

    static double minuteOffsetAtViewportX(
        double viewportX,
        double hvalue,
        double viewportWidth,
        double contentWidth,
        double leftPadding,
        double pixelsPerMinute,
        long totalMinutes
    ) {
        if (pixelsPerMinute <= 0) {
            return 0.0;
        }
        double clampedViewportX = clamp(viewportX, 0.0, Math.max(0.0, viewportWidth));
        double centerX = scrollX(hvalue, viewportWidth, contentWidth) + clampedViewportX;
        double minuteOffset = (centerX - leftPadding) / pixelsPerMinute;
        if (Double.isNaN(minuteOffset) || Double.isInfinite(minuteOffset)) {
            return 0.0;
        }
        return clamp(minuteOffset, 0.0, Math.max(0.0, totalMinutes));
    }

    static double hvalueForAnchor(
        double minuteOffset,
        double viewportX,
        double viewportWidth,
        double contentWidth,
        double leftPadding,
        double pixelsPerMinute
    ) {
        if (pixelsPerMinute <= 0) {
            return 0.0;
        }
        double maxScroll = Math.max(0.0, contentWidth - viewportWidth);
        if (maxScroll <= 0) {
            return 0.0;
        }
        double anchorX = leftPadding + Math.max(0.0, minuteOffset) * pixelsPerMinute;
        double targetScroll = anchorX - clamp(viewportX, 0.0, Math.max(0.0, viewportWidth));
        return clamp(targetScroll / maxScroll, 0.0, 1.0);
    }

    static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
