package com.example.controller;

import com.example.model.ScheduleItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class SearchSuggestionHelper {
    private SearchSuggestionHelper() {
    }

    public static List<String> buildSuggestionCandidates(
        List<String> history,
        List<String> persistedSeeds,
        List<String> localSeeds
    ) {
        LinkedHashSet<String> combined = new LinkedHashSet<>();
        addCandidates(combined, history);
        addCandidates(combined, persistedSeeds);
        addCandidates(combined, localSeeds);
        return List.copyOf(new ArrayList<>(combined));
    }

    public static List<String> filterSuggestions(List<String> candidates, String input, int limit) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        String normalizedInput = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String normalizedCandidate = candidate.toLowerCase(Locale.ROOT);
            if (normalizedInput.isEmpty() || normalizedCandidate.contains(normalizedInput)) {
                filtered.add(candidate);
                if (limit > 0 && filtered.size() >= limit) {
                    break;
                }
            }
        }
        return List.copyOf(filtered);
    }

    public static List<String> updateSearchHistory(List<String> existing, String keyword, int limit) {
        if (keyword == null) {
            return existing != null ? List.copyOf(existing) : List.of();
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return existing != null ? List.copyOf(existing) : List.of();
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(trimmed);
        addCandidates(ordered, existing);
        List<String> result = new ArrayList<>(ordered);
        return capList(result, limit);
    }

    public static List<String> extractSeedsFromScheduleItems(List<ScheduleItem> items, int limit) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        int capacity = limit > 0 ? limit : Integer.MAX_VALUE;
        for (ScheduleItem item : items) {
            if (item == null) {
                continue;
            }
            addCandidate(tokens, item.getTitle());
            addCandidate(tokens, item.getCategory());
            List<String> tags = item.getTagNames();
            if (tags != null) {
                for (String tag : tags) {
                    addCandidate(tokens, tag);
                    if (tokens.size() >= capacity) {
                        break;
                    }
                }
            }
            if (tokens.size() >= capacity) {
                break;
            }
        }
        List<String> all = new ArrayList<>(tokens);
        return capList(all, limit);
    }

    private static void addCandidates(LinkedHashSet<String> target, List<String> items) {
        if (target == null || items == null || items.isEmpty()) {
            return;
        }
        for (String item : items) {
            addCandidate(target, item);
        }
    }

    private static void addCandidate(LinkedHashSet<String> target, String candidate) {
        if (target == null || candidate == null) {
            return;
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        target.add(trimmed);
    }

    private static List<String> capList(List<String> source, int limit) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        if (limit <= 0 || source.size() <= limit) {
            return List.copyOf(source);
        }
        return List.copyOf(source.subList(0, limit));
    }
}
