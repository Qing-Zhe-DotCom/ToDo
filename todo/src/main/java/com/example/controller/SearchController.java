package com.example.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.example.application.IconKey;
import com.example.application.ScheduleItemService;
import com.example.config.UserPreferencesStore;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class SearchController {

    private static final String SEARCH_HISTORY_PREFERENCE_KEY = "todo.search.history";
    private static final int SEARCH_HISTORY_MAX_ENTRIES = 20;
    private static final int SEARCH_SUGGESTION_HISTORY_WHEN_EMPTY_LIMIT = 8;
    private static final int SEARCH_SUGGESTION_TOTAL_LIMIT = 12;
    private static final int SEARCH_SUGGESTION_BUCKET_LIMIT = 3;
    private static final Duration SEARCH_SUGGESTION_DEBOUNCE_DURATION = Duration.millis(120);
    private static final Pattern SEARCH_HISTORY_WHITESPACE = Pattern.compile("\\s+");

    private final ScheduleItemService scheduleItemService;
    private final UserPreferencesStore preferencesStore;
    private final TextField searchField;
    private final Consumer<String> onSearch;
    private final Runnable onClearSearch;
    private final Supplier<Boolean> isSidebarCollapsed;
    private final Runnable onSearchStateChanged;
    private final BiFunction<IconKey, String, Pane> iconFactory;
    private final Function<String, String> categoryDisplayNameFunc;

    private ContextMenu searchSuggestionMenu;
    private PauseTransition searchSuggestionDebounce;
    private final List<String> searchHistory = new ArrayList<>();

    public SearchController(
            ScheduleItemService scheduleItemService,
            UserPreferencesStore preferencesStore,
            TextField searchField,
            Consumer<String> onSearch,
            Runnable onClearSearch,
            Supplier<Boolean> isSidebarCollapsed,
            Runnable onSearchStateChanged,
            BiFunction<IconKey, String, Pane> iconFactory,
            Function<String, String> categoryDisplayNameFunc) {
        this.scheduleItemService = scheduleItemService;
        this.preferencesStore = preferencesStore;
        this.searchField = searchField;
        this.onSearch = onSearch;
        this.onClearSearch = onClearSearch;
        this.isSidebarCollapsed = isSidebarCollapsed;
        this.onSearchStateChanged = onSearchStateChanged;
        this.iconFactory = iconFactory;
        this.categoryDisplayNameFunc = categoryDisplayNameFunc;
        this.searchHistory.addAll(loadSearchHistory(preferencesStore));
        installSidebarSearchSuggestions();
    }

    public boolean hasSearchHistory() {
        return searchHistory != null && !searchHistory.isEmpty();
    }

    private void installSidebarSearchSuggestions() {
        if (searchField == null) {
            return;
        }
        if (searchSuggestionMenu == null) {
            searchSuggestionMenu = new ContextMenu();
            searchSuggestionMenu.getStyleClass().add("search-suggestion-menu");
            searchSuggestionMenu.setAutoHide(true);
            searchSuggestionMenu.setHideOnEscape(true);
        }
        if (searchSuggestionDebounce == null) {
            searchSuggestionDebounce = new PauseTransition(SEARCH_SUGGESTION_DEBOUNCE_DURATION);
            searchSuggestionDebounce.setOnFinished(event -> refreshSidebarSearchSuggestions());
        }

        searchField.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (focused) {
                refreshSidebarSearchSuggestions();
            } else {
                // Defer to allow context-menu clicks to register before hiding.
                Platform.runLater(this::hideSidebarSearchSuggestions);
            }
        });
        searchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hideSidebarSearchSuggestions();
            }
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (shouldClearSearchResults(oldValue, newValue)) {
                onClearSearch.run();
            }
            queueSidebarSearchSuggestionRefresh();
            onSearchStateChanged.run();
        });
        searchField.setOnAction(e -> performSearch(searchField.getText()));
    }

    private void queueSidebarSearchSuggestionRefresh() {
        if (searchSuggestionDebounce == null || searchField == null) {
            return;
        }
        if (!searchField.isFocused()) {
            return;
        }
        searchSuggestionDebounce.playFromStart();
    }

    private void refreshSidebarSearchSuggestions() {
        if (searchSuggestionMenu == null || searchField == null) {
            return;
        }
        if (!searchField.isFocused() || !searchField.isVisible() || isSidebarCollapsed.get()) {
            hideSidebarSearchSuggestions();
            return;
        }

        String rawInput = searchField.getText();
        String trimmedInput = rawInput == null ? "" : rawInput.trim();

        List<SearchSuggestion> suggestions = buildSearchSuggestions(trimmedInput);
        if (suggestions.isEmpty()) {
            hideSidebarSearchSuggestions();
            return;
        }

        List<MenuItem> items = new ArrayList<>();
        for (SearchSuggestion suggestion : suggestions) {
            MenuItem item = new MenuItem(suggestion.displayText);
            item.setGraphic(createSvgIcon(suggestion.iconKey, suggestion.displayText, 16));
            item.setOnAction(event -> {
                searchField.setText(suggestion.insertText);
                searchField.positionCaret(suggestion.insertText.length());
                performSearch(suggestion.insertText);
            });
            items.add(item);
        }
        searchSuggestionMenu.getItems().setAll(items);

        if (!searchSuggestionMenu.isShowing()) {
            searchSuggestionMenu.show(searchField, Side.BOTTOM, 0, 4);
        }
    }

    private List<SearchSuggestion> buildSearchSuggestions(String trimmedInput) {
        List<SearchSuggestion> suggestions = new ArrayList<>();
        LinkedHashSet<String> dedupeKeys = new LinkedHashSet<>();

        if (trimmedInput == null || trimmedInput.isEmpty()) {
            int count = 0;
            for (String entry : searchHistory) {
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                String insert = entry;
                String key = insert.toLowerCase(Locale.ROOT);
                if (!dedupeKeys.add(key)) {
                    continue;
                }
                suggestions.add(new SearchSuggestion(insert, insert, IconKey.RESET));
                if (++count >= SEARCH_SUGGESTION_HISTORY_WHEN_EMPTY_LIMIT) {
                    break;
                }
            }
            return suggestions;
        }

        String needle = trimmedInput.toLowerCase(Locale.ROOT);
        addSuggestionsFromHistory(suggestions, dedupeKeys, needle, SEARCH_SUGGESTION_BUCKET_LIMIT);

        int remaining = SEARCH_SUGGESTION_TOTAL_LIMIT - suggestions.size();
        if (remaining <= 0) {
            return suggestions;
        }

        try {
            addSuggestionsFromValues(
                suggestions,
                dedupeKeys,
                scheduleItemService.suggestActiveScheduleTitles(trimmedInput, SEARCH_SUGGESTION_BUCKET_LIMIT),
                IconKey.NOTES,
                remaining
            );
            remaining = SEARCH_SUGGESTION_TOTAL_LIMIT - suggestions.size();
            if (remaining <= 0) {
                return suggestions;
            }

            addSuggestionsFromValues(
                suggestions,
                dedupeKeys,
                scheduleItemService.suggestActiveTagNames(trimmedInput, SEARCH_SUGGESTION_BUCKET_LIMIT),
                IconKey.TAG,
                remaining
            );
            remaining = SEARCH_SUGGESTION_TOTAL_LIMIT - suggestions.size();
            if (remaining <= 0) {
                return suggestions;
            }

            List<String> categories = scheduleItemService.suggestActiveCategories(trimmedInput, SEARCH_SUGGESTION_BUCKET_LIMIT);
            for (String rawCategory : categories) {
                if (rawCategory == null || rawCategory.isBlank()) {
                    continue;
                }
                String insert = rawCategory;
                String key = insert.toLowerCase(Locale.ROOT);
                if (!dedupeKeys.add(key)) {
                    continue;
                }
                suggestions.add(new SearchSuggestion(categoryDisplayName(rawCategory), insert, IconKey.FOLDER));
                if (suggestions.size() >= SEARCH_SUGGESTION_TOTAL_LIMIT) {
                    break;
                }
            }
        } catch (SQLException ignored) {
            // Suggestions are best-effort. The main search path should remain usable.
        }

        return suggestions;
    }

    private void addSuggestionsFromHistory(
        List<SearchSuggestion> suggestions,
        LinkedHashSet<String> dedupeKeys,
        String needle,
        int limit
    ) {
        int count = 0;
        for (String entry : searchHistory) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if (!entry.toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            String insert = entry;
            String key = insert.toLowerCase(Locale.ROOT);
            if (!dedupeKeys.add(key)) {
                continue;
            }
            suggestions.add(new SearchSuggestion(insert, insert, IconKey.RESET));
            if (++count >= limit || suggestions.size() >= SEARCH_SUGGESTION_TOTAL_LIMIT) {
                break;
            }
        }
    }

    private static void addSuggestionsFromValues(
        List<SearchSuggestion> suggestions,
        LinkedHashSet<String> dedupeKeys,
        List<String> values,
        IconKey iconKey,
        int remaining
    ) {
        if (values == null || values.isEmpty() || remaining <= 0) {
            return;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String insert = value;
            String key = insert.toLowerCase(Locale.ROOT);
            if (!dedupeKeys.add(key)) {
                continue;
            }
            suggestions.add(new SearchSuggestion(insert, insert, iconKey));
            if (--remaining <= 0 || suggestions.size() >= SEARCH_SUGGESTION_TOTAL_LIMIT) {
                break;
            }
        }
    }

    public void hideSidebarSearchSuggestions() {
        if (searchSuggestionDebounce != null) {
            searchSuggestionDebounce.stop();
        }
        if (searchSuggestionMenu != null) {
            searchSuggestionMenu.hide();
        }
    }

    public void clearSidebarSearchHistory() {
        hideSidebarSearchSuggestions();
        clearSearchHistory(preferencesStore, searchHistory);
        onSearchStateChanged.run();
        refreshSidebarSearchSuggestions();
        searchField.requestFocus();
    }

    public void performSearch(String keyword) {
        hideSidebarSearchSuggestions();
        if (isBlankSearchText(keyword)) {
            onClearSearch.run();
            return;
        }
        rememberSearchHistory(preferencesStore, searchHistory, keyword);
        onSearchStateChanged.run();
        onSearch.accept(keyword.trim());
    }

    private Pane createSvgIcon(IconKey iconKey, String title, double size) {
        return iconFactory.apply(iconKey, title);
    }

    private String categoryDisplayName(String category) {
        return categoryDisplayNameFunc.apply(category);
    }

    static boolean shouldClearSearchResults(String previousText, String currentText) {
        return !isBlankSearchText(previousText) && isBlankSearchText(currentText);
    }

    private static boolean isBlankSearchText(String text) {
        return text == null || text.trim().isEmpty();
    }

    static List<String> loadSearchHistory(UserPreferencesStore preferencesStore) {
        if (preferencesStore == null) {
            return List.of();
        }
        return parseSearchHistory(preferencesStore.get(SEARCH_HISTORY_PREFERENCE_KEY, ""));
    }

    static void rememberSearchHistory(
        UserPreferencesStore preferencesStore,
        List<String> historyBuffer,
        String rawKeyword
    ) {
        if (preferencesStore == null || historyBuffer == null) {
            return;
        }

        List<String> updated = appendSearchHistory(historyBuffer, rawKeyword, SEARCH_HISTORY_MAX_ENTRIES);
        if (updated.equals(historyBuffer)) {
            return;
        }

        historyBuffer.clear();
        historyBuffer.addAll(updated);
        if (historyBuffer.isEmpty()) {
            preferencesStore.remove(SEARCH_HISTORY_PREFERENCE_KEY);
        } else {
            preferencesStore.put(SEARCH_HISTORY_PREFERENCE_KEY, String.join("\n", historyBuffer));
        }
    }

    static void clearSearchHistory(UserPreferencesStore preferencesStore, List<String> historyBuffer) {
        if (historyBuffer != null) {
            historyBuffer.clear();
        }
        if (preferencesStore != null) {
            preferencesStore.remove(SEARCH_HISTORY_PREFERENCE_KEY);
        }
    }

    static List<String> appendSearchHistory(List<String> existing, String rawEntry, int maxEntries) {
        if (maxEntries <= 0) {
            return List.of();
        }

        String normalizedEntry = normalizeSearchHistoryEntry(rawEntry);
        if (normalizedEntry == null) {
            return existing == null ? List.of() : List.copyOf(existing);
        }

        List<String> updated = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        updated.add(normalizedEntry);
        seen.add(normalizedEntry.toLowerCase(Locale.ROOT));

        if (existing != null) {
            for (String entry : existing) {
                String normalizedExisting = normalizeSearchHistoryEntry(entry);
                if (normalizedExisting == null) {
                    continue;
                }
                String key = normalizedExisting.toLowerCase(Locale.ROOT);
                if (!seen.add(key)) {
                    continue;
                }
                updated.add(normalizedExisting);
                if (updated.size() >= maxEntries) {
                    break;
                }
            }
        }

        return updated;
    }

    static List<String> parseSearchHistory(String rawHistory) {
        if (rawHistory == null || rawHistory.isBlank()) {
            return List.of();
        }

        String[] lines = rawHistory.replace("\r", "").split("\n");
        List<String> parsed = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String line : lines) {
            String normalized = normalizeSearchHistoryEntry(line);
            if (normalized == null) {
                continue;
            }
            String key = normalized.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }
            parsed.add(normalized);
            if (parsed.size() >= SEARCH_HISTORY_MAX_ENTRIES) {
                break;
            }
        }
        return parsed;
    }

    static String normalizeSearchHistoryEntry(String rawEntry) {
        if (rawEntry == null) {
            return null;
        }
        String sanitized = rawEntry.replace('\r', ' ').replace('\n', ' ').trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        return SEARCH_HISTORY_WHITESPACE.matcher(sanitized).replaceAll(" ");
    }

    static final class SearchSuggestion {
        private final String displayText;
        private final String insertText;
        private final IconKey iconKey;

        private SearchSuggestion(String displayText, String insertText, IconKey iconKey) {
            this.displayText = displayText;
            this.insertText = insertText;
            this.iconKey = iconKey;
        }
    }
}
