package com.example.application;

import com.example.config.UserPreferencesStore;
import com.example.model.ScheduleItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages user-customizable task (category) and tag candidate libraries.
 *
 * <p>Tasks map to {@link com.example.model.ScheduleItem#getCategory()} and are single-select.
 * Tags map to {@link com.example.model.ScheduleItem#getTags()} and are multi-select.</p>
 */
public final class CustomOptionsService {
    static final String PREF_TASKS_KEY = "todo.custom.tasks";
    static final String PREF_TAGS_KEY = "todo.custom.tags";
    public static final String PREF_TIME_TEXT_INPUT_ENABLED_KEY = "todo.custom.time-text-input.enabled";

    public static final int MAX_TASKS = 100;
    public static final int MAX_TAGS = 100;

    private static final List<String> DEFAULT_TASKS = List.of(
        "工作",
        "生活",
        "学习",
        "健康",
        "购物清单",
        "财务",
        "社交",
        "灵感",
        "旅游"
    );

    private final UserPreferencesStore preferencesStore;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private final List<String> tasks = new ArrayList<>();
    private final List<String> tags = new ArrayList<>();

    public CustomOptionsService(UserPreferencesStore preferencesStore) {
        this.preferencesStore = Objects.requireNonNull(preferencesStore, "preferencesStore");
        load();
    }

    public List<String> getTasks() {
        return List.copyOf(tasks);
    }

    public List<String> getTags() {
        return List.copyOf(tags);
    }

    public List<String> suggestTasks(String query, int limit) {
        return suggestFrom(tasks, query, limit);
    }

    public List<String> suggestTags(String query, int limit) {
        return suggestFrom(tags, query, limit);
    }

    public boolean isTimeTextInputEnabled() {
        return Boolean.parseBoolean(preferencesStore.get(PREF_TIME_TEXT_INPUT_ENABLED_KEY, Boolean.FALSE.toString()));
    }

    public void setTimeTextInputEnabled(boolean enabled) {
        preferencesStore.put(PREF_TIME_TEXT_INPUT_ENABLED_KEY, Boolean.toString(enabled));
    }

    /**
     * Ensures a task exists in the candidate library. If it doesn't exist, it will be added at the end.
     *
     * @return false when the library is at capacity and the new task cannot be added.
     */
    public boolean ensureTaskExists(String task) {
        String normalized = normalizeOption(task);
        if (normalized == null) {
            return true;
        }

        String key = keyOf(normalized);
        if (containsKey(tasks, key)) {
            return true;
        }
        if (tasks.size() >= MAX_TASKS) {
            return false;
        }
        tasks.add(normalized);
        saveTasks();
        notifyChanged();
        return true;
    }

    /**
     * Ensures all tags exist in the candidate library. New tags will be appended in encounter order.
     *
     * @return false when the library would exceed capacity and no changes are applied.
     */
    public boolean ensureTagsExist(Collection<String> inputTags) {
        if (inputTags == null || inputTags.isEmpty()) {
            return true;
        }

        LinkedHashSet<String> existingKeys = new LinkedHashSet<>();
        for (String existing : tags) {
            String normalized = normalizeOption(existing);
            if (normalized != null) {
                existingKeys.add(keyOf(normalized));
            }
        }

        List<String> toAdd = new ArrayList<>();
        for (String raw : inputTags) {
            String normalized = normalizeOption(raw);
            if (normalized == null) {
                continue;
            }
            String key = keyOf(normalized);
            if (existingKeys.add(key)) {
                toAdd.add(normalized);
            }
        }

        if (toAdd.isEmpty()) {
            return true;
        }
        if (tags.size() + toAdd.size() > MAX_TAGS) {
            return false;
        }

        tags.addAll(toAdd);
        saveTags();
        notifyChanged();
        return true;
    }

    public void replaceTasks(List<String> updatedTasks) {
        List<String> normalized = normalizeAndDedupe(updatedTasks, MAX_TASKS);
        tasks.clear();
        tasks.addAll(normalized);
        saveTasks();
        notifyChanged();
    }

    public void replaceTags(List<String> updatedTags) {
        List<String> normalized = normalizeAndDedupe(updatedTags, MAX_TAGS);
        tags.clear();
        tags.addAll(normalized);
        saveTags();
        notifyChanged();
    }

    /**
     * Imports tasks and tags from existing schedule items (best-effort, capped by MAX_*).
     * This helps existing users get suggestions without retyping their historical values.
     */
    public void importFromScheduleItems(List<? extends ScheduleItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        LinkedHashSet<String> taskKeys = new LinkedHashSet<>();
        for (String existing : tasks) {
            String normalized = normalizeOption(existing);
            if (normalized != null) {
                taskKeys.add(keyOf(normalized));
            }
        }

        LinkedHashSet<String> tagKeys = new LinkedHashSet<>();
        for (String existing : tags) {
            String normalized = normalizeOption(existing);
            if (normalized != null) {
                tagKeys.add(keyOf(normalized));
            }
        }

        boolean changed = false;

        for (ScheduleItem item : items) {
            if (item == null) {
                continue;
            }

            String category = ScheduleItem.normalizeCategory(item.getCategory());
            if (!ScheduleItem.isDefaultCategory(category)) {
                String normalizedCategory = normalizeOption(category);
                if (normalizedCategory != null
                    && tasks.size() < MAX_TASKS
                    && taskKeys.add(keyOf(normalizedCategory))) {
                    tasks.add(normalizedCategory);
                    changed = true;
                }
            }

            for (String tag : item.getTagNames()) {
                String normalizedTag = normalizeOption(tag);
                if (normalizedTag == null) {
                    continue;
                }
                if (tags.size() >= MAX_TAGS) {
                    continue;
                }
                if (tagKeys.add(keyOf(normalizedTag))) {
                    tags.add(normalizedTag);
                    changed = true;
                }
            }
        }

        if (changed) {
            saveTasks();
            saveTags();
            notifyChanged();
        }
    }

    public Runnable addChangeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void load() {
        String rawTasks = preferencesStore.get(PREF_TASKS_KEY, null);
        if (rawTasks == null) {
            tasks.clear();
            tasks.addAll(DEFAULT_TASKS);
            saveTasks();
        } else {
            tasks.clear();
            tasks.addAll(parseOptions(rawTasks, MAX_TASKS));
        }

        String rawTags = preferencesStore.get(PREF_TAGS_KEY, null);
        tags.clear();
        if (rawTags != null) {
            tags.addAll(parseOptions(rawTags, MAX_TAGS));
        }
    }

    private void saveTasks() {
        preferencesStore.put(PREF_TASKS_KEY, serializeOptions(tasks));
    }

    private void saveTags() {
        preferencesStore.put(PREF_TAGS_KEY, serializeOptions(tags));
    }

    private void notifyChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    private static List<String> suggestFrom(List<String> source, String query, int limit) {
        if (source == null || source.isEmpty() || limit <= 0) {
            return List.of();
        }

        String needle = query == null ? "" : query.trim();
        if (needle.isEmpty()) {
            return source.size() <= limit ? List.copyOf(source) : List.copyOf(source.subList(0, limit));
        }

        String lowerNeedle = needle.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String option : source) {
            if (option == null || option.isBlank()) {
                continue;
            }
            if (!option.toLowerCase(Locale.ROOT).contains(lowerNeedle)) {
                continue;
            }
            results.add(option);
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    private static List<String> normalizeAndDedupe(List<String> raw, int max) {
        List<String> normalized = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return normalized;
        }

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String value : raw) {
            String cleaned = normalizeOption(value);
            if (cleaned == null) {
                continue;
            }
            String key = keyOf(cleaned);
            if (!keys.add(key)) {
                continue;
            }
            normalized.add(cleaned);
            if (normalized.size() > max) {
                throw new IllegalArgumentException("Custom option list exceeds max: " + max);
            }
        }
        return normalized;
    }

    private static List<String> parseOptions(String raw, int max) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        String[] lines = raw.replace("\r", "").split("\n");
        List<String> parsed = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String line : lines) {
            String normalized = normalizeOption(line);
            if (normalized == null) {
                continue;
            }
            String key = keyOf(normalized);
            if (!seen.add(key)) {
                continue;
            }
            parsed.add(normalized);
            if (parsed.size() >= max) {
                break;
            }
        }
        return parsed;
    }

    private static String serializeOptions(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String value : values) {
            String normalized = normalizeOption(value);
            if (normalized == null) {
                continue;
            }
            if (!first) {
                builder.append('\n');
            }
            first = false;
            builder.append(normalized);
        }
        return builder.toString();
    }

    private static String normalizeOption(String raw) {
        if (raw == null) {
            return null;
        }
        // Keep it single-line to avoid breaking preference serialization.
        String sanitized = raw.replace('\r', ' ').replace('\n', ' ').strip();
        return sanitized.isEmpty() ? null : sanitized;
    }

    private static String keyOf(String normalized) {
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static boolean containsKey(List<String> values, String key) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            String normalized = normalizeOption(value);
            if (normalized == null) {
                continue;
            }
            if (keyOf(normalized).equals(key)) {
                return true;
            }
        }
        return false;
    }
}
