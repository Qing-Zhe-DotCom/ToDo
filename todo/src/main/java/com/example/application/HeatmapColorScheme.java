package com.example.application;

import java.util.List;

import javafx.scene.paint.Color;

/**
 * Named heatmap color schemes. Each scheme provides five intensity levels
 * (0 = empty, 1–4 = increasing intensity) for both light and dark appearances.
 *
 * <p>Call {@link #interpolate(int, int, int, int, boolean)} to get a smooth
 * sub-level color for a given completion count.</p>
 */
public enum HeatmapColorScheme {
    GREEN("green", "heatmap.scheme.green",
        new String[]{"#ebedf0", "#c6e48b", "#7bc96f", "#239a3b", "#196127"},
        new String[]{"#1a1f27", "#123d2a", "#1f6b3f", "#2ea54d", "#3ddc84"}),
    BLUE("blue", "heatmap.scheme.blue",
        new String[]{"#ebedf0", "#bde0fe", "#6cb4ee", "#2b86c5", "#14517a"},
        new String[]{"#1a1f27", "#152f4a", "#1d5a8c", "#3a8fd4", "#6cb4ee"}),
    PURPLE("purple", "heatmap.scheme.purple",
        new String[]{"#ebedf0", "#d4c4f0", "#a678dc", "#7340b0", "#4a1d78"},
        new String[]{"#1a1f27", "#2a1848", "#462a78", "#7340b0", "#b48ce0"}),
    ORANGE("orange", "heatmap.scheme.orange",
        new String[]{"#ebedf0", "#fed8a8", "#fdab5a", "#e8751a", "#b35509"},
        new String[]{"#1a1f27", "#3d2610", "#6e4218", "#e8751a", "#fdab5a"}),
    PINK("pink", "heatmap.scheme.pink",
        new String[]{"#ebedf0", "#fcc2d7", "#f783ac", "#e64980", "#a61e4d"},
        new String[]{"#1a1f27", "#3d1428", "#6e2346", "#e64980", "#f783ac"}),
    TEAL("teal", "heatmap.scheme.teal",
        new String[]{"#ebedf0", "#a8e6cf", "#5ccba5", "#20a077", "#0e6a4e"},
        new String[]{"#1a1f27", "#0e3930", "#1a6b55", "#20a077", "#5ccba5"});

    private final String id;
    private final String labelKey;
    private final Color[] lightColors;
    private final Color[] darkColors;

    HeatmapColorScheme(String id, String labelKey, String[] lightHexes, String[] darkHexes) {
        this.id = id;
        this.labelKey = labelKey;
        this.lightColors = parseColors(lightHexes);
        this.darkColors = parseColors(darkHexes);
    }

    public String getId() {
        return id;
    }

    public String getLabelKey() {
        return labelKey;
    }

    /** Returns the base color for the given discrete level (0–4). */
    public Color getColor(int level, boolean dark) {
        Color[] colors = dark ? darkColors : lightColors;
        return colors[Math.max(0, Math.min(level, 4))];
    }

    /**
     * Returns all 5 base colors for the given appearance.
     * Index 0 = empty, 1–4 = increasing intensity.
     */
    public Color[] getColors(boolean dark) {
        Color[] src = dark ? darkColors : lightColors;
        return src.clone();
    }

    /**
     * Returns an interpolated color for the given completion count, using the
     * supplied thresholds. Within each level range the color smoothly transitions
     * from the lower level's color to the current level's color.
     *
     * @param count completed count (≥ 0)
     * @param t1    level-1 upper bound
     * @param t2    level-2 upper bound
     * @param t3    level-3 upper bound
     * @param dark  true for dark-mode colors
     * @return interpolated {@link Color}
     */
    public Color interpolate(int count, int t1, int t2, int t3, boolean dark) {
        Color[] colors = dark ? darkColors : lightColors;
        if (count <= 0) {
            return colors[0];
        }
        if (count <= t1) {
            double fraction = (double) count / Math.max(1, t1);
            return colors[0].interpolate(colors[1], fraction);
        }
        if (count <= t2) {
            double fraction = (double) (count - t1) / Math.max(1, t2 - t1);
            return colors[1].interpolate(colors[2], fraction);
        }
        if (count <= t3) {
            double fraction = (double) (count - t2) / Math.max(1, t3 - t2);
            return colors[2].interpolate(colors[3], fraction);
        }
        double fraction = Math.min(1.0, (double) (count - t3) / Math.max(1, t3 - t2));
        return colors[3].interpolate(colors[4], fraction);
    }

    public static List<HeatmapColorScheme> supportedValues() {
        return List.of(values());
    }

    public static HeatmapColorScheme fromId(String value) {
        if (value == null || value.isBlank()) {
            return GREEN;
        }
        for (HeatmapColorScheme scheme : values()) {
            if (scheme.id.equalsIgnoreCase(value)) {
                return scheme;
            }
        }
        return GREEN;
    }

    private static Color[] parseColors(String[] hexes) {
        Color[] colors = new Color[hexes.length];
        for (int i = 0; i < hexes.length; i++) {
            colors[i] = Color.web(hexes[i]);
        }
        return colors;
    }
}
