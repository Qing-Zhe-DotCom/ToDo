package com.example.application;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.example.config.UserPreferencesStore;

public final class ProductivityConfigService {
    static final String PREF_SEARCH_FIELDS_KEY = "todo.productivity.search.fields";
    static final String PREF_DEFAULT_SORT_MODE_KEY = "todo.productivity.sort.default";
    static final String PREF_HEATMAP_PRESET_KEY = "todo.productivity.heatmap.preset";
    static final String PREF_HEATMAP_THRESHOLDS_KEY = "todo.productivity.heatmap.thresholds";
    static final String PREF_HEATMAP_COLORS_KEY = "todo.productivity.heatmap.colors";
    static final String PREF_PRESET_TAGS_KEY = "todo.productivity.tags.preset";
    static final String PREF_CLOCK_ENABLED_KEY = "todo.productivity.clock.enabled";
    static final String PREF_CLOCK_24H_KEY = "todo.productivity.clock.use24h";
    static final String PREF_CLOCK_SECONDS_KEY = "todo.productivity.clock.show-seconds";
    static final String PREF_SEARCH_HISTORY_KEY = "todo.productivity.search.history";
    static final String PREF_SEARCH_SUGGESTIONS_KEY = "todo.productivity.search.suggestions";

    private final UserPreferencesStore preferencesStore;

    public ProductivityConfigService(UserPreferencesStore preferencesStore) {
        this.preferencesStore = java.util.Objects.requireNonNull(preferencesStore, "preferencesStore");
    }

    public ProductivityConfig load() {
        ProductivityConfig defaults = ProductivityConfig.defaultValue();
        Set<ProductivityConfig.SearchField> searchFields = parseSearchFields(
            preferencesStore.get(PREF_SEARCH_FIELDS_KEY, null)
        );
        ProductivityConfig.SortMode sortMode = ProductivityConfig.SortMode.fromId(
            preferencesStore.get(PREF_DEFAULT_SORT_MODE_KEY, defaults.defaultSortMode().getId())
        );
        String preset = preferencesStore.get(PREF_HEATMAP_PRESET_KEY, defaults.heatmapPreset());
        List<Integer> thresholds = parseIntegerList(
            preferencesStore.get(PREF_HEATMAP_THRESHOLDS_KEY, null),
            defaults.heatmapThresholds()
        );
        List<String> colors = parseStringList(
            preferencesStore.get(PREF_HEATMAP_COLORS_KEY, null),
            defaults.heatmapColors()
        );
        List<String> presetTags = parseStringList(
            preferencesStore.get(PREF_PRESET_TAGS_KEY, null),
            defaults.presetTags()
        );
        ProductivityConfig.ClockDisplay clockDisplay = new ProductivityConfig.ClockDisplay(
            parseBoolean(preferencesStore.get(PREF_CLOCK_ENABLED_KEY, null), defaults.clockDisplay().enabled()),
            parseBoolean(preferencesStore.get(PREF_CLOCK_24H_KEY, null), defaults.clockDisplay().use24Hour()),
            parseBoolean(preferencesStore.get(PREF_CLOCK_SECONDS_KEY, null), defaults.clockDisplay().showSeconds())
        );
        List<String> searchHistory = parseStringList(
            preferencesStore.get(PREF_SEARCH_HISTORY_KEY, null),
            defaults.searchHistory()
        );
        List<String> suggestionSeeds = parseStringList(
            preferencesStore.get(PREF_SEARCH_SUGGESTIONS_KEY, null),
            defaults.suggestionSeeds()
        );
        return new ProductivityConfig(
            searchFields,
            sortMode,
            preset,
            thresholds,
            colors,
            presetTags,
            clockDisplay,
            searchHistory,
            suggestionSeeds
        );
    }

    public void save(ProductivityConfig config) {
        ProductivityConfig normalized = config != null ? config : ProductivityConfig.defaultValue();
        preferencesStore.put(PREF_SEARCH_FIELDS_KEY, encodeStringList(
            normalized.searchFields().stream().map(ProductivityConfig.SearchField::getId).toList()
        ));
        preferencesStore.put(PREF_DEFAULT_SORT_MODE_KEY, normalized.defaultSortMode().getId());
        preferencesStore.put(PREF_HEATMAP_PRESET_KEY, normalized.heatmapPreset());
        preferencesStore.put(PREF_HEATMAP_THRESHOLDS_KEY, encodeIntegerList(normalized.heatmapThresholds()));
        preferencesStore.put(PREF_HEATMAP_COLORS_KEY, encodeStringList(normalized.heatmapColors()));
        preferencesStore.put(PREF_PRESET_TAGS_KEY, encodeStringList(normalized.presetTags()));
        preferencesStore.put(PREF_CLOCK_ENABLED_KEY, Boolean.toString(normalized.clockDisplay().enabled()));
        preferencesStore.put(PREF_CLOCK_24H_KEY, Boolean.toString(normalized.clockDisplay().use24Hour()));
        preferencesStore.put(PREF_CLOCK_SECONDS_KEY, Boolean.toString(normalized.clockDisplay().showSeconds()));
        preferencesStore.put(PREF_SEARCH_HISTORY_KEY, encodeStringList(normalized.searchHistory()));
        preferencesStore.put(PREF_SEARCH_SUGGESTIONS_KEY, encodeStringList(normalized.suggestionSeeds()));
    }

    public ProductivityConfig update(UnaryOperator<ProductivityConfig> updater) {
        UnaryOperator<ProductivityConfig> action = updater != null ? updater : UnaryOperator.identity();
        ProductivityConfig next = action.apply(load());
        save(next);
        return load();
    }

    public ProductivityConfig reset() {
        preferencesStore.remove(PREF_SEARCH_FIELDS_KEY);
        preferencesStore.remove(PREF_DEFAULT_SORT_MODE_KEY);
        preferencesStore.remove(PREF_HEATMAP_PRESET_KEY);
        preferencesStore.remove(PREF_HEATMAP_THRESHOLDS_KEY);
        preferencesStore.remove(PREF_HEATMAP_COLORS_KEY);
        preferencesStore.remove(PREF_PRESET_TAGS_KEY);
        preferencesStore.remove(PREF_CLOCK_ENABLED_KEY);
        preferencesStore.remove(PREF_CLOCK_24H_KEY);
        preferencesStore.remove(PREF_CLOCK_SECONDS_KEY);
        preferencesStore.remove(PREF_SEARCH_HISTORY_KEY);
        preferencesStore.remove(PREF_SEARCH_SUGGESTIONS_KEY);
        return load();
    }

    private Set<ProductivityConfig.SearchField> parseSearchFields(String raw) {
        List<String> ids = decodeStringList(raw);
        if (ids.isEmpty()) {
            return EnumSet.allOf(ProductivityConfig.SearchField.class);
        }
        EnumSet<ProductivityConfig.SearchField> fields = EnumSet.noneOf(ProductivityConfig.SearchField.class);
        for (String id : ids) {
            ProductivityConfig.SearchField field = ProductivityConfig.SearchField.fromId(id);
            if (field != null) {
                fields.add(field);
            }
        }
        if (fields.isEmpty()) {
            return EnumSet.allOf(ProductivityConfig.SearchField.class);
        }
        return fields;
    }

    private static List<Integer> parseIntegerList(String raw, List<Integer> fallback) {
        List<String> tokens = decodeStringList(raw);
        if (tokens.isEmpty()) {
            return fallback;
        }
        List<Integer> values = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            try {
                values.add(Integer.parseInt(token));
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }
        return values;
    }

    private static List<String> parseStringList(String raw, List<String> fallback) {
        if (raw == null) {
            return fallback;
        }
        List<String> values = decodeStringList(raw);
        return values;
    }

    private static boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        return fallback;
    }

    private static String encodeIntegerList(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return encodeStringList(values.stream().map(String::valueOf).toList());
    }

    private static String encodeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        List<String> encoded = new ArrayList<>(values.size());
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            encoded.add(URLEncoder.encode(trimmed, StandardCharsets.UTF_8));
        }
        return String.join(",", encoded);
    }

    private static List<String> decodeStringList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] tokens = raw.split(",");
        List<String> decoded = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            String value = URLDecoder.decode(token, StandardCharsets.UTF_8).trim();
            if (!value.isEmpty()) {
                decoded.add(value);
            }
        }
        return decoded;
    }
}
