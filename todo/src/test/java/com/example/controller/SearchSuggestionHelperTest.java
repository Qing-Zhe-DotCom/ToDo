package com.example.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.model.ScheduleItem;

import java.util.List;

import org.junit.jupiter.api.Test;

class SearchSuggestionHelperTest {
    @Test
    void buildSuggestionCandidatesDeduplicatesInOrder() {
        List<String> history = List.of("alpha", "beta", "gamma");
        List<String> persisted = List.of("gamma", "delta");
        List<String> local = List.of("epsilon", "beta");

        List<String> result = SearchSuggestionHelper.buildSuggestionCandidates(history, persisted, local);

        assertEquals(List.of("alpha", "beta", "gamma", "delta", "epsilon"), result);
    }

    @Test
    void filterSuggestionsRespectsInputAndLimit() {
        List<String> candidates = List.of("Project", "Plan", "play", "Priority");
        List<String> filtered = SearchSuggestionHelper.filterSuggestions(candidates, "pl", 2);

        assertEquals(List.of("Plan", "play"), filtered);
    }

    @Test
    void updateSearchHistoryMovesLatestToFrontAndRespectsLimit() {
        List<String> history = List.of("one", "two", "three");
        List<String> updated = SearchSuggestionHelper.updateSearchHistory(history, "four", 3);

        assertEquals(List.of("four", "one", "two"), updated);
    }

    @Test
    void extractSeedsFromScheduleItemsUsesTitleCategoryAndTagsInOrder() {
        ScheduleItem first = new ScheduleItem();
        first.setTitle("Daily review");
        first.setCategory("Work");
        first.setTags("team,planning");
        ScheduleItem second = new ScheduleItem();
        second.setTitle("Yoga");
        second.setCategory("Health");
        second.setTags("morning");

        List<String> seeds = SearchSuggestionHelper.extractSeedsFromScheduleItems(List.of(first, second), 5);

        assertEquals(List.of("Daily review", "Work", "team", "planning", "Yoga"), seeds);
    }
}
