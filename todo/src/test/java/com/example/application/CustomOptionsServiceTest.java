package com.example.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.config.UserPreferencesStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CustomOptionsServiceTest {

    @Test
    void seedsDefaultTasksWhenPreferenceMissing() {
        MapPreferencesStore store = new MapPreferencesStore();

        CustomOptionsService service = new CustomOptionsService(store);

        assertEquals(
            List.of("工作", "生活", "学习", "健康", "购物清单", "财务", "社交", "灵感", "旅游"),
            service.getTasks()
        );
        assertTrue(service.getTags().isEmpty());

        assertNotNull(store.get(CustomOptionsService.PREF_TASKS_KEY, null));
    }

    @Test
    void ensureTaskExistsAddsWhenUnderLimitAndRejectsWhenAtLimit() {
        MapPreferencesStore store = new MapPreferencesStore();
        CustomOptionsService service = new CustomOptionsService(store);

        assertTrue(service.ensureTaskExists("测试任务"));
        assertTrue(service.getTasks().contains("测试任务"));

        List<String> filled = new ArrayList<>();
        for (int i = 0; i < CustomOptionsService.MAX_TASKS; i++) {
            filled.add("T" + i);
        }
        service.replaceTasks(filled);

        assertFalse(service.ensureTaskExists("T-EXTRA"));
    }

    @Test
    void ensureTagsExistDeduplicatesAndRespectsLimit() {
        MapPreferencesStore store = new MapPreferencesStore();
        CustomOptionsService service = new CustomOptionsService(store);

        assertTrue(service.ensureTagsExist(List.of("Work", "work", "Life")));
        assertEquals(List.of("Work", "Life"), service.getTags());

        List<String> filled = new ArrayList<>();
        for (int i = 0; i < CustomOptionsService.MAX_TAGS; i++) {
            filled.add("Tag" + i);
        }
        service.replaceTags(filled);

        assertFalse(service.ensureTagsExist(List.of("Another")));
    }

    @Test
    void replaceTasksRejectsOverMaxUniqueEntries() {
        MapPreferencesStore store = new MapPreferencesStore();
        CustomOptionsService service = new CustomOptionsService(store);

        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < CustomOptionsService.MAX_TASKS + 1; i++) {
            tooMany.add("Task" + i);
        }

        assertThrows(IllegalArgumentException.class, () -> service.replaceTasks(tooMany));
    }

    private static final class MapPreferencesStore implements UserPreferencesStore {
        private final Map<String, String> values = new HashMap<>();

        @Override
        public String get(String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }

        @Override
        public void put(String key, String value) {
            values.put(key, value);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }
    }
}

