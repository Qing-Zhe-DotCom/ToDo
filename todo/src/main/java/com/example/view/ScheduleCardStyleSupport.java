package com.example.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.model.Schedule;

import javafx.collections.ObservableList;
import javafx.scene.Node;

public final class ScheduleCardStyleSupport {
    public static final String BASE_CARD_CLASS = "schedule-card-surface";

    public static final String STYLE_ID_CLASSIC = "classic";
    public static final String STYLE_ID_FRESH = "fresh";
    public static final String STYLE_ID_COZY = "cozy";
    public static final String STYLE_ID_MODERN_MINIMAL = "modern-minimal";
    public static final String STYLE_ID_NEO_BRUTALISM = "neo-brutalism";
    public static final String STYLE_ID_MATERIAL_YOU = "material-you";
    public static final String STYLE_ID_NEUMORPHISM = "neumorphism";

    private static final String STYLE_CLASSIC = "schedule-card-style-classic";
    private static final String STYLE_FRESH = "schedule-card-style-fresh";
    private static final String STYLE_COZY = "schedule-card-style-cozy";
    private static final String STYLE_MODERN_MINIMAL = "schedule-card-style-modern-minimal";
    private static final String STYLE_NEO_BRUTALISM = "schedule-card-style-neo-brutalism";
    private static final String STYLE_MATERIAL_YOU = "schedule-card-style-material-you";
    private static final String STYLE_NEUMORPHISM = "schedule-card-style-neumorphism";

    private static final String STATE_COMPLETED = "schedule-card-state-completed";
    private static final String STATE_OVERDUE = "schedule-card-state-overdue";
    private static final String STATE_UPCOMING = "schedule-card-state-upcoming";
    private static final String STATE_PRIORITY_HIGH = "schedule-card-state-priority-high";
    private static final String STATE_PRIORITY_MEDIUM = "schedule-card-state-priority-medium";
    private static final String STATE_PRIORITY_LOW = "schedule-card-state-priority-low";

    private static final String DEFAULT_STYLE_ID = STYLE_ID_CLASSIC;

    private static final Map<String, StyleMetadata> STYLES_BY_ID = createStyleMap();
    private static final Map<String, String> LEGACY_LABEL_TO_ID = createLegacyLabelMap();
    private static final List<String> STYLE_IDS = Collections.unmodifiableList(new ArrayList<>(STYLES_BY_ID.keySet()));
    private static final List<String> STYLE_CLASSES = Collections.unmodifiableList(
        STYLES_BY_ID.values().stream().map(StyleMetadata::styleClass).toList()
    );
    private static final List<String> STATE_CLASSES = List.of(
        STATE_COMPLETED,
        STATE_OVERDUE,
        STATE_UPCOMING,
        STATE_PRIORITY_HIGH,
        STATE_PRIORITY_MEDIUM,
        STATE_PRIORITY_LOW
    );

    private ScheduleCardStyleSupport() {
    }

    public static String getDefaultStyleId() {
        return DEFAULT_STYLE_ID;
    }

    public static List<String> getStyleIds() {
        return STYLE_IDS;
    }

    public static String getLabelKey(String styleId) {
        return STYLES_BY_ID.get(normalizeStyleId(styleId)).labelKey();
    }

    public static String normalizeStyleId(String styleId) {
        if (styleId != null && STYLES_BY_ID.containsKey(styleId)) {
            return styleId;
        }
        if (styleId != null) {
            String migrated = LEGACY_LABEL_TO_ID.get(styleId);
            if (migrated != null) {
                return migrated;
            }
        }
        return DEFAULT_STYLE_ID;
    }

    public static void applyCardPresentation(Node node, Schedule schedule, String styleId, String... roleClasses) {
        applyCardStyle(node, styleId, roleClasses);
        applyScheduleState(node, schedule);
    }

    public static void applyCardStyle(Node node, String styleId, String... roleClasses) {
        ObservableList<String> styleClasses = node.getStyleClass();
        if (!styleClasses.contains(BASE_CARD_CLASS)) {
            styleClasses.add(BASE_CARD_CLASS);
        }

        styleClasses.removeAll(STYLE_CLASSES);
        styleClasses.add(STYLES_BY_ID.get(normalizeStyleId(styleId)).styleClass());

        if (roleClasses == null) {
            return;
        }
        for (String roleClass : roleClasses) {
            if (roleClass != null && !roleClass.isBlank() && !styleClasses.contains(roleClass)) {
                styleClasses.add(roleClass);
            }
        }
    }

    public static void applyScheduleState(Node node, Schedule schedule) {
        ObservableList<String> styleClasses = node.getStyleClass();
        styleClasses.removeAll(STATE_CLASSES);

        if (schedule == null) {
            return;
        }

        if (Schedule.PRIORITY_HIGH.equals(schedule.getPriority())) {
            styleClasses.add(STATE_PRIORITY_HIGH);
        } else if (Schedule.PRIORITY_LOW.equals(schedule.getPriority())) {
            styleClasses.add(STATE_PRIORITY_LOW);
        } else {
            styleClasses.add(STATE_PRIORITY_MEDIUM);
        }

        if (schedule.isCompleted()) {
            styleClasses.add(STATE_COMPLETED);
        } else if (schedule.isOverdue()) {
            styleClasses.add(STATE_OVERDUE);
        } else if (schedule.isUpcoming()) {
            styleClasses.add(STATE_UPCOMING);
        }
    }

    private static Map<String, StyleMetadata> createStyleMap() {
        Map<String, StyleMetadata> styles = new LinkedHashMap<>();
        styles.put(STYLE_ID_CLASSIC, new StyleMetadata(STYLE_CLASSIC, "style.classic"));
        styles.put(STYLE_ID_FRESH, new StyleMetadata(STYLE_FRESH, "style.fresh"));
        styles.put(STYLE_ID_COZY, new StyleMetadata(STYLE_COZY, "style.cozy"));
        styles.put(STYLE_ID_MODERN_MINIMAL, new StyleMetadata(STYLE_MODERN_MINIMAL, "style.modernMinimal"));
        styles.put(STYLE_ID_NEO_BRUTALISM, new StyleMetadata(STYLE_NEO_BRUTALISM, "style.neoBrutalism"));
        styles.put(STYLE_ID_MATERIAL_YOU, new StyleMetadata(STYLE_MATERIAL_YOU, "style.materialYou"));
        styles.put(STYLE_ID_NEUMORPHISM, new StyleMetadata(STYLE_NEUMORPHISM, "style.neumorphism"));
        return styles;
    }

    private static Map<String, String> createLegacyLabelMap() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("经典实体卡片", STYLE_ID_CLASSIC);
        labels.put("經典實體卡片", STYLE_ID_CLASSIC);
        labels.put("Classic card", STYLE_ID_CLASSIC);
        labels.put("清新扁平", STYLE_ID_FRESH);
        labels.put("Fresh flat", STYLE_ID_FRESH);
        labels.put("温馨治愈风", STYLE_ID_COZY);
        labels.put("溫馨治癒風", STYLE_ID_COZY);
        labels.put("Cozy", STYLE_ID_COZY);
        labels.put("现代高级极简风", STYLE_ID_MODERN_MINIMAL);
        labels.put("現代高級極簡風", STYLE_ID_MODERN_MINIMAL);
        labels.put("Modern minimal", STYLE_ID_MODERN_MINIMAL);
        labels.put("新粗野主义", STYLE_ID_NEO_BRUTALISM);
        labels.put("新粗野主義", STYLE_ID_NEO_BRUTALISM);
        labels.put("Neo brutalism", STYLE_ID_NEO_BRUTALISM);
        labels.put("Material You", STYLE_ID_MATERIAL_YOU);
        labels.put("拟物浮雕风", STYLE_ID_NEUMORPHISM);
        labels.put("擬物浮雕風", STYLE_ID_NEUMORPHISM);
        labels.put("Neumorphism", STYLE_ID_NEUMORPHISM);
        return labels;
    }

    private record StyleMetadata(String styleClass, String labelKey) {
    }
}
