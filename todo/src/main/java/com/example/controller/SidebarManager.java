package com.example.controller;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.example.application.IconKey;
import com.example.application.ScheduleItemService;
import com.example.config.UserPreferencesStore;
import com.example.view.LabeledTextAutoFit;
import com.example.view.View;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SidebarManager {

    private final ThemeCoordinator themeCoordinator;
    private final LocalizationFacade loc;
    private final ScheduleItemService scheduleItemService;
    private final UserPreferencesStore preferencesStore;
    private final Consumer<View> onNavigate;
    private final Runnable onSettings;
    private final Runnable onToggleAppearance;
    private final Runnable onExit;
    private final Consumer<Boolean> onCollapsedChanged;
    private final Consumer<String> onSearch;
    private final Runnable onClearSearch;

    // UI component references
    private VBox sidebar;
    private VBox bottomActions;
    private Separator bottomActionsSeparator;
    private Label functionTitle;
    private TextField searchField;
    private StackPane sidebarSearchBox;
    private HBox sidebarSearchActions;
    private Button clearSearchTextButton;
    private Button clearSearchHistoryButton;
    private SearchController searchController;
    private ToggleButton scheduleNavButton;
    private ToggleButton timelineNavButton;
    private ToggleButton heatmapNavButton;
    private Button settingsActionButton;
    private ToggleButton appearanceToggle;
    private Button exitActionButton;
    private ToggleButton collapseToggle;
    private ToggleButton featurePanelToggle;
    private Pane themeIcon;

    private boolean sidebarCollapsed = false;
    private boolean featurePanelExpanded = false;
    private static final DateTimeFormatter HEADER_CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final StringProperty headerClockText = new SimpleStringProperty("");
    private Timeline headerClockTimeline;
    private final Map<Labeled, String[]> collapsibleLabels = new LinkedHashMap<>();

    private IntConsumer pendingCountListener;
    private int lastKnownPendingCount = -1;

    private View scheduleView;
    private View timelineView;
    private View heatmapView;

    public SidebarManager(
            ThemeCoordinator themeCoordinator,
            LocalizationFacade loc,
            ScheduleItemService scheduleItemService,
            UserPreferencesStore preferencesStore,
            Consumer<View> onNavigate,
            Runnable onSettings,
            Runnable onToggleAppearance,
            Runnable onExit,
            Consumer<Boolean> onCollapsedChanged,
            Consumer<String> onSearch,
            Runnable onClearSearch) {
        this.themeCoordinator = themeCoordinator;
        this.loc = loc;
        this.scheduleItemService = scheduleItemService;
        this.preferencesStore = preferencesStore;
        this.onNavigate = onNavigate;
        this.onSettings = onSettings;
        this.onToggleAppearance = onToggleAppearance;
        this.onExit = onExit;
        this.onCollapsedChanged = onCollapsedChanged;
        this.onSearch = onSearch;
        this.onClearSearch = onClearSearch;
    }

    /**
     * Builds the complete sidebar and returns it as a VBox.
     */
    public VBox build() {
        sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        // Collapse toggle button
        collapseToggle = new ToggleButton();
        collapseToggle.getStyleClass().addAll("nav-button", "sidebar-collapse-button");
        collapseToggle.setMaxWidth(Double.MAX_VALUE);
        String collapseTooltip = loc.text("sidebar.collapse.toggle");
        Pane collapseToggleIcon = createSvgIcon(IconKey.ARROW_RIGHT, collapseTooltip, 24);
        collapseToggle.setGraphic(collapseToggleIcon);
        collapseToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        collapseToggle.setAccessibleText(collapseTooltip);
        collapseToggle.setTooltip(new Tooltip(collapseTooltip));
        collapseToggle.setOnAction(e -> {
            sidebarCollapsed = collapseToggle.isSelected();
            updateSidebarCollapseState();
            if (onCollapsedChanged != null) {
                onCollapsedChanged.accept(sidebarCollapsed);
            }
        });

        // Search field
        searchField = new TextField();
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.getStyleClass().addAll("search-field", "search-field-with-actions");
        searchField.setPromptText(loc.text("sidebar.search.prompt"));
        searchController = new SearchController(
                scheduleItemService,
                preferencesStore,
                searchField,
                onSearch,
                onClearSearch,
                () -> sidebarCollapsed,
                this::updateSidebarSearchActionButtons,
                (iconKey, title) -> createSvgIcon(iconKey, title, 16),
                loc::categoryDisplayName
        );

        clearSearchTextButton = new Button();
        clearSearchTextButton.getStyleClass().add("icon-button");
        clearSearchTextButton.setFocusTraversable(false);
        clearSearchTextButton.setOnAction(event -> {
            searchController.hideSidebarSearchSuggestions();
            if (searchField != null) {
                searchField.clear();
                searchField.requestFocus();
            }
        });

        clearSearchHistoryButton = new Button();
        clearSearchHistoryButton.getStyleClass().add("icon-button");
        clearSearchHistoryButton.setFocusTraversable(false);
        clearSearchHistoryButton.setOnAction(event -> searchController.clearSidebarSearchHistory());

        sidebarSearchActions = new HBox(4, clearSearchTextButton, clearSearchHistoryButton);
        sidebarSearchActions.setAlignment(Pos.CENTER_RIGHT);
        sidebarSearchActions.setMaxWidth(Region.USE_PREF_SIZE);
        sidebarSearchActions.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(sidebarSearchActions, Pos.CENTER_RIGHT);
        StackPane.setMargin(sidebarSearchActions, new Insets(0, 6, 0, 0));

        sidebarSearchBox = new StackPane(searchField, sidebarSearchActions);
        sidebarSearchBox.setMaxWidth(Double.MAX_VALUE);
        sidebarSearchBox.setAlignment(Pos.CENTER_LEFT);

        // Navigation buttons
        ToggleGroup navGroup = new ToggleGroup();

        scheduleNavButton = createNavButton(IconKey.CALENDAR, loc.text("nav.schedule"), navGroup);
        scheduleNavButton.setSelected(true);
        scheduleNavButton.setOnAction(e -> {
            if (scheduleView != null) onNavigate.accept(scheduleView);
        });

        timelineNavButton = createNavButton(IconKey.TIMELINE, loc.text("nav.timeline"), navGroup);
        timelineNavButton.setOnAction(e -> {
            if (timelineView != null) onNavigate.accept(timelineView);
        });

        heatmapNavButton = createNavButton(IconKey.GRID_HEATMAP, loc.text("nav.heatmap"), navGroup);
        heatmapNavButton.setOnAction(e -> {
            if (heatmapView != null) onNavigate.accept(heatmapView);
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        functionTitle = new Label(loc.text("sidebar.functions"));
        functionTitle.getStyleClass().addAll("label-hint", "sidebar-function-title");

        settingsActionButton = createActionButton(IconKey.SETTINGS, loc.text("sidebar.settings"), onSettings);

        appearanceToggle = new ToggleButton(loc.text("sidebar.appearance.darkMode"));
        appearanceToggle.getStyleClass().addAll("nav-button", "sidebar-action-button", "sidebar-appearance-toggle");
        appearanceToggle.setMaxWidth(Double.MAX_VALUE);
        appearanceToggle.setGraphicTextGap(8);
        appearanceToggle.setContentDisplay(ContentDisplay.LEFT);
        appearanceToggle.setTextOverrun(OverrunStyle.CLIP);
        appearanceToggle.setWrapText(false);
        appearanceToggle.setOnAction(e -> onToggleAppearance.run());
        registerCollapsibleControl(appearanceToggle, loc.text("sidebar.appearance.darkMode"), "", loc.text("sidebar.appearance.darkMode"));

        exitActionButton = createActionButton(IconKey.LOGOUT, loc.text("sidebar.exit"), onExit);

        bottomActions = new VBox(6);
        bottomActions.getStyleClass().add("sidebar-bottom-actions");
        bottomActions.getChildren().addAll(
                functionTitle,
                settingsActionButton,
                appearanceToggle,
                exitActionButton
        );

        featurePanelToggle = new ToggleButton();
        featurePanelToggle.getStyleClass().addAll("nav-button", "sidebar-feature-toggle");
        featurePanelToggle.setMaxWidth(Double.MAX_VALUE);
        String featureTooltip = loc.text("sidebar.feature.toggle");
        Pane featureToggleIcon = createSvgIcon(IconKey.ARROW_RIGHT, featureTooltip, 24);
        featurePanelToggle.setGraphic(featureToggleIcon);
        featurePanelToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        featurePanelToggle.setAccessibleText(featureTooltip);
        featurePanelToggle.setTooltip(new Tooltip(featureTooltip));
        featurePanelToggle.setOnAction(e -> {
            featurePanelExpanded = featurePanelToggle.isSelected();
            updateFeaturePanelState();
        });

        bottomActionsSeparator = new Separator();

        sidebar.getChildren().addAll(
                collapseToggle,
                sidebarSearchBox,
                new Separator(),
                scheduleNavButton,
                timelineNavButton,
                heatmapNavButton,
                spacer,
                bottomActionsSeparator,
                bottomActions,
                featurePanelToggle
        );

        // Set ThemeCoordinator sidebar component references
        themeCoordinator.setCollapseToggle(collapseToggle);
        themeCoordinator.setFeaturePanelToggle(featurePanelToggle);
        themeCoordinator.setScheduleNavButton(scheduleNavButton);
        themeCoordinator.setTimelineNavButton(timelineNavButton);
        themeCoordinator.setHeatmapNavButton(heatmapNavButton);
        themeCoordinator.setSettingsActionButton(settingsActionButton);
        themeCoordinator.setAppearanceToggle(appearanceToggle);
        themeCoordinator.setExitActionButton(exitActionButton);
        themeCoordinator.setClearSearchTextButton(clearSearchTextButton);
        themeCoordinator.setClearSearchHistoryButton(clearSearchHistoryButton);
        themeCoordinator.setThemeIcon(themeIcon);
        themeCoordinator.setSidebarCollapsed(sidebarCollapsed);
        themeCoordinator.setFeaturePanelExpanded(featurePanelExpanded);

        // Update sidebar icons and appearance now that references are set
        themeCoordinator.updateSidebarIcons();
        updateSidebarSearchActionButtons();
        updateSidebarCollapseState();

        return sidebar;
    }

    // --- View navigation setters (called after views are created) ---

    public void setScheduleView(View view) {
        this.scheduleView = view;
    }

    public void setTimelineView(View view) {
        this.timelineView = view;
    }

    public void setHeatmapView(View view) {
        this.heatmapView = view;
    }

    // --- Public getters for MainController ---

    public TextField getSearchField() {
        return searchField;
    }

    public ToggleButton getScheduleNavButton() {
        return scheduleNavButton;
    }

    public ToggleButton getTimelineNavButton() {
        return timelineNavButton;
    }

    public ToggleButton getHeatmapNavButton() {
        return heatmapNavButton;
    }

    public boolean isSidebarCollapsed() {
        return sidebarCollapsed;
    }

    // --- Pending count badge ---

    public void setPendingCountListener(IntConsumer listener) {
        this.pendingCountListener = listener;
        updatePendingCountBadge();
    }

    public void updatePendingCountBadge() {
        if (pendingCountListener == null) {
            return;
        }
        try {
            lastKnownPendingCount = scheduleItemService.getPendingCount();
            pendingCountListener.accept(lastKnownPendingCount);
        } catch (SQLException ignored) {
            lastKnownPendingCount = 0;
            pendingCountListener.accept(0);
        }
    }

    public void adjustPendingCountOptimistically(int delta) {
        if (pendingCountListener == null || delta == 0) {
            return;
        }
        if (lastKnownPendingCount < 0) {
            updatePendingCountBadge();
            return;
        }
        lastKnownPendingCount = Math.max(0, lastKnownPendingCount + delta);
        pendingCountListener.accept(lastKnownPendingCount);
    }

    // --- Clock methods ---

    public Label createHeaderClockLabel() {
        Label label = new Label();
        label.getStyleClass().add("header-clock");
        label.textProperty().bind(headerClockText);
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.setMouseTransparent(true);
        label.setFocusTraversable(false);
        return label;
    }

    public ReadOnlyStringProperty headerClockTextProperty() {
        return headerClockText;
    }

    public void startHeaderClock() {
        if (headerClockTimeline != null) {
            return;
        }
        updateHeaderClockText();
        headerClockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateHeaderClockText()));
        headerClockTimeline.setCycleCount(Animation.INDEFINITE);
        headerClockTimeline.play();
    }

    public void stopHeaderClock() {
        if (headerClockTimeline == null) {
            return;
        }
        headerClockTimeline.stop();
        headerClockTimeline = null;
    }

    private void updateHeaderClockText() {
        String next = HEADER_CLOCK_FORMATTER.format(LocalTime.now());
        if (!next.equals(headerClockText.get())) {
            headerClockText.set(next);
        }
    }

    // --- Sidebar UI construction helpers ---

    private ToggleButton createNavButton(IconKey iconKey, String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("nav-button");
        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setGraphic(createSvgIcon(iconKey, text, 24));
        button.setGraphicTextGap(8);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setWrapText(false);
        button.setAccessibleText(text);
        button.setTooltip(new Tooltip(text));
        LabeledTextAutoFit.install(button, LabeledTextAutoFit.buttonSpec());
        registerCollapsibleControl(button, text, "", text);
        return button;
    }

    private Button createActionButton(IconKey iconKey, String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("nav-button", "sidebar-action-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setGraphic(createSvgIcon(iconKey, text, 24));
        button.setGraphicTextGap(8);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setWrapText(false);
        button.setAccessibleText(text);
        button.setTooltip(new Tooltip(text));
        LabeledTextAutoFit.install(button, LabeledTextAutoFit.buttonSpec());
        button.setOnAction(e -> action.run());
        registerCollapsibleControl(button, text, "", text);
        return button;
    }

    private void registerCollapsibleControl(Labeled control, String expandedText, String collapsedText, String tooltipText) {
        collapsibleLabels.put(control, new String[]{expandedText, collapsedText});
        control.setTooltip(new Tooltip(tooltipText));
    }

    private void updateSidebarCollapseState() {
        if (sidebar == null) return;

        double collapsedWidth = 52;
        double expandedWidth = 256;
        sidebar.setPrefWidth(sidebarCollapsed ? collapsedWidth : expandedWidth);
        sidebar.setMinWidth(sidebarCollapsed ? collapsedWidth : expandedWidth);
        sidebar.setMaxWidth(sidebarCollapsed ? collapsedWidth : expandedWidth);
        sidebar.setPadding(sidebarCollapsed ? new Insets(6) : new Insets(10));
        sidebar.setSpacing(sidebarCollapsed ? 6 : 8);
        sidebar.setAlignment(sidebarCollapsed ? Pos.TOP_CENTER : Pos.TOP_LEFT);
        sidebar.setFillWidth(!sidebarCollapsed);

        if (collapseToggle != null) {
            if (sidebarCollapsed) {
                collapseToggle.setText("");
                collapseToggle.setStyle("-fx-padding: 8 0 8 0; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-pref-width: 40; -fx-pref-height: 40; -fx-max-width: 40; -fx-max-height: 40;");
                collapseToggle.setAlignment(Pos.CENTER);
                collapseToggle.setPadding(new Insets(8, 0, 8, 0));
            } else {
                collapseToggle.setText(loc.text("sidebar.title"));
                collapseToggle.setStyle("-fx-padding: 10 12 10 12; -fx-alignment: center-left; -fx-min-height: 40; -fx-pref-height: 40; -fx-max-height: 40;");
                collapseToggle.setAlignment(Pos.CENTER_LEFT);
                collapseToggle.setPadding(new Insets(10, 12, 10, 12));
            }
            Node collapseGraphic = collapseToggle.getGraphic();
            if (collapseGraphic != null) {
                collapseGraphic.setRotate(sidebarCollapsed ? 0 : 90);
            }
        }

        for (Map.Entry<Labeled, String[]> entry : collapsibleLabels.entrySet()) {
            Labeled control = entry.getKey();
            String[] texts = entry.getValue();
            if (sidebarCollapsed) {
                control.setText("");
                control.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                control.setStyle("-fx-padding: 8 0 8 0; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-pref-width: 40; -fx-pref-height: 40; -fx-max-width: 40; -fx-max-height: 40;");
                control.setAlignment(Pos.CENTER);
                control.setPadding(new Insets(8, 0, 8, 0));
                control.setMinSize(40, 40);
                control.setPrefSize(40, 40);
                control.setMaxSize(40, 40);
            } else {
                control.setText(texts[0]);
                control.setContentDisplay(ContentDisplay.LEFT);
                control.setStyle("-fx-padding: 10 12 10 12; -fx-alignment: center-left; -fx-min-height: 40; -fx-pref-height: 40; -fx-max-height: 40;");
                control.setAlignment(Pos.CENTER_LEFT);
                control.setPadding(new Insets(10, 12, 10, 12));
                control.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                control.setPrefSize(Region.USE_COMPUTED_SIZE, 40);
                control.setMaxSize(Double.MAX_VALUE, 40);
            }
        }

        if (bottomActions != null) {
            bottomActions.setAlignment(sidebarCollapsed ? Pos.TOP_CENTER : Pos.TOP_LEFT);
            bottomActions.setFillWidth(!sidebarCollapsed);
        }

        if (functionTitle != null) {
            functionTitle.setVisible(!sidebarCollapsed);
            functionTitle.setManaged(!sidebarCollapsed);
        }

        if (searchField != null) {
            searchField.setVisible(!sidebarCollapsed);
            searchField.setManaged(!sidebarCollapsed);
        }

        if (sidebarSearchBox != null) {
            sidebarSearchBox.setVisible(!sidebarCollapsed);
            sidebarSearchBox.setManaged(!sidebarCollapsed);
        }

        if (sidebarCollapsed) {
            searchController.hideSidebarSearchSuggestions();
        }

        updateFeaturePanelState();
    }

    private void updateSidebarSearchActionButtons() {
        if (clearSearchTextButton != null && searchField != null) {
            boolean hasText = !isBlankSearchText(searchField.getText());
            clearSearchTextButton.setVisible(hasText);
            clearSearchTextButton.setManaged(hasText);
        }

        if (clearSearchHistoryButton != null) {
            boolean hasHistory = searchController != null && searchController.hasSearchHistory();
            clearSearchHistoryButton.setVisible(hasHistory);
            clearSearchHistoryButton.setManaged(hasHistory);
        }
    }

    private void updateFeaturePanelState() {
        if (featurePanelToggle != null) {
            if (sidebarCollapsed) {
                featurePanelToggle.setText("");
                featurePanelToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                featurePanelToggle.setStyle("-fx-padding: 8 0 8 0; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-pref-width: 40; -fx-pref-height: 40; -fx-max-width: 40; -fx-max-height: 40;");
                featurePanelToggle.setAlignment(Pos.CENTER);
                featurePanelToggle.setPadding(new Insets(8, 0, 8, 0));
                featurePanelToggle.setMinSize(40, 40);
                featurePanelToggle.setPrefSize(40, 40);
                featurePanelToggle.setMaxSize(40, 40);
            } else {
                featurePanelToggle.setText(loc.text("sidebar.more"));
                featurePanelToggle.setContentDisplay(ContentDisplay.LEFT);
                featurePanelToggle.setStyle("-fx-padding: 10 12 10 12; -fx-alignment: center-left; -fx-min-height: 40; -fx-pref-height: 40; -fx-max-height: 40;");
                featurePanelToggle.setAlignment(Pos.CENTER_LEFT);
                featurePanelToggle.setPadding(new Insets(10, 12, 10, 12));
                featurePanelToggle.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                featurePanelToggle.setPrefSize(Region.USE_COMPUTED_SIZE, 40);
                featurePanelToggle.setMaxSize(Double.MAX_VALUE, 40);
            }
            Node featureGraphic = featurePanelToggle.getGraphic();
            if (featureGraphic != null) {
                featureGraphic.setRotate(featurePanelExpanded ? 90 : 0);
            }
        }
        if (bottomActions != null) {
            bottomActions.setVisible(featurePanelExpanded);
            bottomActions.setManaged(featurePanelExpanded);
        }
        if (bottomActionsSeparator != null) {
            bottomActionsSeparator.setVisible(featurePanelExpanded);
            bottomActionsSeparator.setManaged(featurePanelExpanded);
        }
        themeCoordinator.updateThemeIconState();
    }

    private Pane createSvgIcon(IconKey iconKey, String title, double size) {
        return themeCoordinator.createSvgIcon(iconKey, title, size);
    }

    private static boolean isBlankSearchText(String text) {
        return text == null || text.trim().isEmpty();
    }
}
