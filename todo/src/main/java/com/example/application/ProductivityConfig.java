package com.example.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public record ProductivityConfig(
    Set<SearchField> searchFields,
    SortMode defaultSortMode,
    String heatmapPreset,
    List<Integer> heatmapThresholds,
    List<String> heatmapColors,
    List<String> presetTags,
    ClockDisplay clockDisplay,
    List<String> searchHistory,
    List<String> suggestionSeeds
) {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    public static final String DEFAULT_HEATMAP_PRESET = "classic-balanced";
    public static final List<Integer> DEFAULT_HEATMAP_THRESHOLDS = List.of(2, 5, 8, 12);
    public static final List<String> DEFAULT_HEATMAP_COLORS = List.of(
        "#edf2f7",
        "#c6e48b",
        "#7bc96f",
        "#239a3b",
        "#196127"
    );

    public ProductivityConfig {
        searchFields = normalizeSearchFields(searchFields);
        defaultSortMode = defaultSortMode != null ? defaultSortMode : SortMode.PRIORITY;
        heatmapPreset = normalizePreset(heatmapPreset);
        heatmapThresholds = normalizeThresholds(heatmapThresholds);
        heatmapColors = normalizeColors(heatmapColors);
        presetTags = normalizeTextList(presetTags, 30);
        clockDisplay = clockDisplay != null ? clockDisplay.normalize() : ClockDisplay.defaultValue();
        searchHistory = normalizeTextList(searchHistory, 30);
        suggestionSeeds = normalizeTextList(suggestionSeeds, 60);
    }

    public static ProductivityConfig defaultValue() {
        return new ProductivityConfig(
            EnumSet.allOf(SearchField.class),
            SortMode.PRIORITY,
            DEFAULT_HEATMAP_PRESET,
            DEFAULT_HEATMAP_THRESHOLDS,
            DEFAULT_HEATMAP_COLORS,
            List.of("Work", "Review", "Meeting"),
            ClockDisplay.defaultValue(),
            List.of(),
            List.of()
        );
    }

    private static Set<SearchField> normalizeSearchFields(Set<SearchField> source) {
        if (source == null || source.isEmpty()) {
            return Collections.unmodifiableSet(EnumSet.allOf(SearchField.class));
        }
        EnumSet<SearchField> normalized = EnumSet.noneOf(SearchField.class);
        for (SearchField field : source) {
            if (field != null) {
                normalized.add(field);
            }
        }
        if (normalized.isEmpty()) {
            normalized = EnumSet.allOf(SearchField.class);
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static String normalizePreset(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_HEATMAP_PRESET;
        }
        return value.trim();
    }

    private static List<Integer> normalizeThresholds(List<Integer> source) {
        List<Integer> fallback = DEFAULT_HEATMAP_THRESHOLDS;
        if (source == null || source.size() != fallback.size()) {
            return fallback;
        }
        List<Integer> values = new ArrayList<>(source.size());
        int previous = 0;
        for (Integer candidate : source) {
            if (candidate == null || candidate < 1) {
                return fallback;
            }
            int value = candidate;
            if (value <= previous) {
                return fallback;
            }
            previous = value;
            values.add(value);
        }
        return List.copyOf(values);
    }

    private static List<String> normalizeColors(List<String> source) {
        List<String> fallback = DEFAULT_HEATMAP_COLORS;
        if (source == null || source.size() != fallback.size()) {
            return fallback;
        }
        List<String> values = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            String candidate = source.get(i);
            if (candidate == null || !HEX_COLOR_PATTERN.matcher(candidate.trim()).matches()) {
                values.add(fallback.get(i));
            } else {
                values.add(candidate.trim().toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(values);
    }

    private static List<String> normalizeTextList(List<String> source, int maxItems) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String value : source) {
            if (value == null) {
                continue;
            }
            String normalized = value.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            ordered.add(normalized);
            if (ordered.size() >= maxItems) {
                break;
            }
        }
        return List.copyOf(ordered);
    }

    public enum SearchField {
        TITLE("title"),
        DESCRIPTION("description"),
        NOTES("notes"),
        CATEGORY("category"),
        TAGS("tags");

        private final String id;

        SearchField(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static SearchField fromId(String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            for (SearchField value : values()) {
                if (value.id.equalsIgnoreCase(id.trim())) {
                    return value;
                }
            }
            return null;
        }
    }

    public enum SortMode {
        PRIORITY("priority"),
        CREATED_TIME("created-time"),
        REMAINING_TIME("remaining-time");

        private final String id;

        SortMode(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static SortMode fromId(String id) {
            if (id == null || id.isBlank()) {
                return PRIORITY;
            }
            for (SortMode mode : values()) {
                if (mode.id.equalsIgnoreCase(id.trim())) {
                    return mode;
                }
            }
            return PRIORITY;
        }
    }

    public record ClockDisplay(boolean enabled, boolean use24Hour, boolean showSeconds) {
        public static ClockDisplay defaultValue() {
            return new ClockDisplay(true, true, false);
        }

        ClockDisplay normalize() {
            return this;
        }
    }
}
