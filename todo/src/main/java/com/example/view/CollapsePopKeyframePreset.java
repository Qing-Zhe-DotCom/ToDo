package com.example.view;

import javafx.util.Duration;

final class CollapsePopKeyframePreset {
    static final Duration SOURCE_STAGE_ONE = Duration.millis(100);
    static final Duration SOURCE_STAGE_TWO = Duration.millis(200);
    static final Duration SOURCE_STAGE_THREE = Duration.millis(300);
    static final Duration COMMIT_POINT = Duration.millis(400);
    static final Duration TARGET_APPEAR = Duration.millis(450);
    static final Duration TARGET_STAGE_ONE = Duration.millis(550);
    static final Duration TARGET_STAGE_TWO = Duration.millis(650);
    static final Duration TARGET_STAGE_THREE = Duration.millis(725);
    static final Duration TARGET_FINISH = Duration.millis(800);

    private static final double[] SOURCE_TIMES = {0.0, 100.0, 200.0, 300.0, 400.0};
    private static final double[] SOURCE_SCALES = {1.0, 0.8, 0.5, 0.12, 0.0};
    private static final double[] TARGET_TIMES = {450.0, 550.0, 650.0, 725.0, 800.0};
    private static final double[] TARGET_SCALES = {0.12, 0.8, 1.1, 0.95, 1.0};

    private CollapsePopKeyframePreset() {
    }

    static double sourceScaleAt(Duration time) {
        return interpolate(time, SOURCE_TIMES, SOURCE_SCALES);
    }

    static double targetScaleAt(Duration time) {
        return interpolate(time, TARGET_TIMES, TARGET_SCALES);
    }

    static Duration targetPopDuration() {
        return TARGET_FINISH.subtract(TARGET_APPEAR);
    }

    private static double interpolate(Duration time, double[] times, double[] values) {
        double millis = time == null ? 0.0 : time.toMillis();
        if (millis <= times[0]) {
            return values[0];
        }
        int lastIndex = times.length - 1;
        if (millis >= times[lastIndex]) {
            return values[lastIndex];
        }
        for (int i = 1; i < times.length; i++) {
            double right = times[i];
            if (millis > right) {
                continue;
            }
            double left = times[i - 1];
            double progress = (millis - left) / (right - left);
            return values[i - 1] + (values[i] - values[i - 1]) * progress;
        }
        return values[lastIndex];
    }
}
