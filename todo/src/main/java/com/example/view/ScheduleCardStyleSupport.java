package com.example.view;

import com.example.model.Schedule;

import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScheduleCardStyleSupport {

    public static final String BASE_CARD_CLASS = "schedule-card-surface";

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

    private static final String DEFAULT_STYLE_NAME = "温馨治愈风";

    private static final Map<String, String> STYLE_CLASS_BY_NAME = createStyleClassMap();
    private static final List<String> STYLE_NAMES = Collections.unmodifiableList(new ArrayList<>(STYLE_CLASS_BY_NAME.keySet()));
    private static final List<String> STYLE_CLASSES = Collections.unmodifiableList(new ArrayList<>(STYLE_CLASS_BY_NAME.values()));
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

    public static String getDefaultStyleName() {
        return DEFAULT_STYLE_NAME;
    }

    public static List<String> getStyleNames() {
        return STYLE_NAMES;
    }

    public static String normalizeStyleName(String styleName) {
        if (styleName != null && STYLE_CLASS_BY_NAME.containsKey(styleName)) {
            return styleName;
        }
        return DEFAULT_STYLE_NAME;
    }

    public static void applyCardPresentation(Node node, Schedule schedule, String styleName, String... roleClasses) {
        applyCardStyle(node, styleName, roleClasses);
        applyScheduleState(node, schedule);
    }

    public static void applyCardStyle(Node node, String styleName, String... roleClasses) {
        ObservableList<String> styleClasses = node.getStyleClass();
        if (!styleClasses.contains(BASE_CARD_CLASS)) {
            styleClasses.add(BASE_CARD_CLASS);
        }

        styleClasses.removeAll(STYLE_CLASSES);
        styleClasses.add(STYLE_CLASS_BY_NAME.get(normalizeStyleName(styleName)));

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

        if ("高".equals(schedule.getPriority())) {
            styleClasses.add(STATE_PRIORITY_HIGH);
        } else if ("低".equals(schedule.getPriority())) {
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

    private static Map<String, String> createStyleClassMap() {
        Map<String, String> styleClassMap = new LinkedHashMap<>();
        styleClassMap.put("经典实体卡片", STYLE_CLASSIC);
        styleClassMap.put("清新扁平", STYLE_FRESH);
        styleClassMap.put("温馨治愈风", STYLE_COZY);
        styleClassMap.put("现代高级极简风", STYLE_MODERN_MINIMAL);
        styleClassMap.put("新粗野主义", STYLE_NEO_BRUTALISM);
        styleClassMap.put("Material You", STYLE_MATERIAL_YOU);
        styleClassMap.put("拟物浮雕风", STYLE_NEUMORPHISM);
        return styleClassMap;
    }
}
