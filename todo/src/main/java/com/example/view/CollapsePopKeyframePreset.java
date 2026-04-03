package com.example.view;

import javafx.util.Duration;

final class CollapsePopKeyframePreset {
    private static final double TIME_SCALE = 1.5;

    static final Duration SOURCE_STAGE_ONE = scaledMillis(100.0);
    static final Duration SOURCE_STAGE_TWO = scaledMillis(200.0);
    static final Duration SOURCE_STAGE_THREE = scaledMillis(300.0);
    static final Duration COMMIT_POINT = scaledMillis(400.0);
    static final Duration TARGET_APPEAR = scaledMillis(450.0);
    static final Duration TARGET_STAGE_ONE = scaledMillis(550.0);
    static final Duration TARGET_STAGE_TWO = scaledMillis(650.0);
    static final Duration TARGET_STAGE_THREE = scaledMillis(725.0);
    static final Duration TARGET_FINISH = scaledMillis(800.0);

    private static final double[] SOURCE_TIMES = {
        0.0,
        SOURCE_STAGE_ONE.toMillis(),
        SOURCE_STAGE_TWO.toMillis(),
        SOURCE_STAGE_THREE.toMillis(),
        COMMIT_POINT.toMillis()
    };
    private static final double[] SOURCE_SCALES = {1.0, 0.8, 0.5, 0.12, 0.0};
    private static final double[] TARGET_TIMES = {
        TARGET_APPEAR.toMillis(),
        TARGET_STAGE_ONE.toMillis(),
        TARGET_STAGE_TWO.toMillis(),
        TARGET_STAGE_THREE.toMillis(),
        TARGET_FINISH.toMillis()
    };
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

    private static Duration scaledMillis(double baseMillis) {
        return Duration.millis(baseMillis * TIME_SCALE);
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
