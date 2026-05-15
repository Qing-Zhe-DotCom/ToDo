package com.example.controller;

import java.sql.SQLException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.IntConsumer;

import com.example.application.ApplicationContext;
import com.example.application.AppFontWeight;
import com.example.application.AppLanguage;
import com.example.application.ClassicThemePalette;
import com.example.application.CustomOptionsService;
import com.example.application.ExperimentalFeaturesService;
import com.example.application.FontService;
import com.example.application.GlassBackdropCoordinator;
import com.example.application.HeatmapColorScheme;
import com.example.application.IconKey;
import com.example.application.IconPack;
import com.example.application.IconService;
import com.example.application.LocalizationService;
import com.example.application.MainViewModel;
import com.example.application.NavigationService;
import com.example.application.ReminderNotificationService;
import com.example.application.ScheduleItemService;
import com.example.application.ShortcutSpec;
import com.example.application.ThemeAppearance;
import com.example.application.ThemeFamily;
import com.example.application.ThemeService;
import com.example.application.WheelModifier;
import com.example.config.UserPreferencesStore;
import com.example.model.ScheduleItem;
import com.example.model.RecurrenceRule;
import com.example.view.HeatmapView;
import com.example.view.InfoPanelView;
import com.example.view.LabeledTextAutoFit;
import com.example.view.ScheduleCardStyleSupport;
import com.example.view.ScheduleCompletionParticipant;
import com.example.view.ScheduleListView;
import com.example.view.TimelineView;
import com.example.view.View;

import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.Group;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.scene.transform.Rotate;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.geometry.VPos;
import javafx.util.Duration;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MainController {
    private final ApplicationContext applicationContext;
    private final MainViewModel mainViewModel;
    private final ScheduleItemService scheduleItemService;
    private final CustomOptionsService customOptionsService;
    private final NavigationService navigationService;
    private final ThemeService themeService;
    private final IconService iconService;
    private final ExperimentalFeaturesService experimentalFeaturesService;
    private final LocalizationService localizationService;
    private final LocalizationFacade loc;
    private final FontService fontService;

    @FXML
    private StackPane root;
    private BorderPane appShell;
    private Pane macaronBackgroundLayer;
    private Scene scene;
    
    // 各个视图
    private ScheduleListView scheduleListView;
    private TimelineView timelineView;
    private HeatmapView heatmapView;
    private InfoPanelView infoPanelView;
    
    // 当前选中的视图
    private View currentView;

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
    private Button themeButton;
    private Pane themeIcon;
    private boolean sidebarCollapsed = false;
    private boolean featurePanelExpanded = false;
    private boolean uiInitialized = false;
    private static final DateTimeFormatter HEADER_CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final StringProperty headerClockText = new SimpleStringProperty("");
    private Timeline headerClockTimeline;
    private final Map<Labeled, String[]> collapsibleLabels = new LinkedHashMap<>();
    private final ExecutorService scheduleCompletionExecutor;
    private final ScheduleCompletionCoordinator scheduleCompletionCoordinator;
    private ScheduleHandler scheduleHandler;
    private ReminderNotificationService reminderNotificationService;
    private IntConsumer pendingCountListener;
    private int lastKnownPendingCount = -1;

    private ThemeCoordinator themeCoordinator;
    private SettingsDialog settingsDialog;

    private static final String PREF_TIMELINE_ZOOM_WHEEL_MODIFIER_KEY = "todo.shortcut.timeline.zoom.wheelModifier";
    private static final String PREF_TIMELINE_ZOOM_IN_KEY = "todo.shortcut.timeline.zoom.in";
    private static final String PREF_TIMELINE_ZOOM_OUT_KEY = "todo.shortcut.timeline.zoom.out";

    private WheelModifier timelineZoomWheelModifier = WheelModifier.CTRL;
    private ShortcutSpec timelineZoomInShortcut = ShortcutSpec.of(true, false, false, false, KeyCode.EQUALS);
    private ShortcutSpec timelineZoomOutShortcut = ShortcutSpec.of(true, false, false, false, KeyCode.MINUS);
    
    public MainController() {
        this(ApplicationContext.createDefault());
    }

    public MainController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.mainViewModel = applicationContext.getMainViewModel();
        this.scheduleItemService = applicationContext.getScheduleItemService();
        this.customOptionsService = applicationContext.getCustomOptionsService();
        this.navigationService = mainViewModel.getNavigationService();
        this.themeService = mainViewModel.getThemeService();
        this.iconService = mainViewModel.getIconService();
        this.experimentalFeaturesService = applicationContext.getExperimentalFeaturesService();
        this.localizationService = mainViewModel.getLocalizationService();
        this.loc = new LocalizationFacade(localizationService);
        this.fontService = mainViewModel.getFontService();
        this.themeCoordinator = new ThemeCoordinator(
            themeService,
            iconService,
            experimentalFeaturesService,
            applicationContext.getPreferencesStore(),
            fontService,
            localizationService
        );
        loadTimelineShortcutPreferences();
        scheduleCompletionExecutor = Executors.newSingleThreadExecutor(createCompletionThreadFactory());
        scheduleCompletionCoordinator = new ScheduleCompletionCoordinator(
            scheduleItemService::updateScheduleItemCompletion,
            new ScheduleCompletionCoordinator.MutationApplier() {
                @Override
                public void applyOptimistic(ScheduleCompletionMutation mutation) {
                    applyCompletionMutationLocally(mutation);
                }

                @Override
                public void confirm(ScheduleCompletionMutation mutation) {
                    confirmCompletionMutationLocally(mutation);
                }

                @Override
                public void revert(ScheduleCompletionMutation mutation) {
                    revertCompletionMutationLocally(mutation);
                }
            },
            this::reportCompletionPersistenceFailure,
            scheduleCompletionExecutor,
            Platform::runLater
        );
        this.scheduleHandler = new ScheduleHandler(
            scheduleItemService,
            scheduleCompletionCoordinator,
            this::refreshAllViews,
            this::requestReminderResync
        );
    }

    @FXML
    private void initialize() {
        settingsDialog = createSettingsDialog();
        initializeUI();
    }

    private SettingsDialog createSettingsDialog() {
        UserPreferencesStore prefsStore = applicationContext.getPreferencesStore();
        return new SettingsDialog(
            loc,
            themeCoordinator,
            themeService,
            iconService,
            fontService,
            scheduleHandler,
            customOptionsService,
            experimentalFeaturesService,
            prefsStore,
            localizationService,
            applicationContext.getAppProperties(),
            this::refreshAllViews,
            this::refreshDataViews,
            this::applyCurrentFont,
            this::loadTimelineShortcutPreferences,
            timelineZoomWheelModifier,
            timelineZoomInShortcut,
            timelineZoomOutShortcut
        );
    }
    
    private void initializeUI() {
        if (uiInitialized) {
            return;
        }
        if (root == null) {
            root = new StackPane();
        }
        if (!root.getStyleClass().contains("root")) {
            root.getStyleClass().add("root");
        }
        if (appShell == null) {
            appShell = new BorderPane();
            appShell.getStyleClass().add("app-shell");
        }
        if (macaronBackgroundLayer == null) {
            themeCoordinator.setRoot(root);
            macaronBackgroundLayer = themeCoordinator.createMacaronBackgroundLayer();
            themeCoordinator.setMacaronBackgroundLayer(macaronBackgroundLayer);
        }
        if (root.getChildren().isEmpty()) {
            root.getChildren().setAll(macaronBackgroundLayer, appShell);
        }
        startHeaderClock();

        // 创建左侧导航栏
        createSidebar();
        
        // 创建右侧信息面板
        createInfoPanel();
        
        // 初始化各个视图
        scheduleListView = new ScheduleListView(this);
        timelineView = new TimelineView(this);
        heatmapView = new HeatmapView(this);
        //infoPanelView = new InfoPanelView(this);
        
        // 默认显示日程列表视图
        showView(scheduleListView);
        themeCoordinator.updateMacaronPresentation();
        uiInitialized = true;
    }

    private void createSidebar() {
        sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        collapseToggle = new ToggleButton();
        collapseToggle.getStyleClass().addAll("nav-button", "sidebar-collapse-button");
        collapseToggle.setMaxWidth(Double.MAX_VALUE);
        String collapseTooltip = text("sidebar.collapse.toggle");
        Pane collapseToggleIcon = createSvgIcon(IconKey.ARROW_RIGHT, collapseTooltip, 24);
        collapseToggle.setGraphic(collapseToggleIcon);
        collapseToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        collapseToggle.setAccessibleText(collapseTooltip);
        collapseToggle.setTooltip(new Tooltip(collapseTooltip));
        collapseToggle.setOnAction(e -> {
            sidebarCollapsed = collapseToggle.isSelected();
            updateSidebarCollapseState();
        });

        searchField = new TextField();
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.getStyleClass().addAll("search-field", "search-field-with-actions");
        searchField.setPromptText(text("sidebar.search.prompt"));
        searchController = new SearchController(
            scheduleItemService,
            applicationContext.getPreferencesStore(),
            searchField,
            keyword -> {
                scheduleListView.searchSchedules(keyword);
                showView(scheduleListView);
            },
            () -> {
                if (scheduleListView != null) {
                    scheduleListView.clearSearch();
                }
            },
            () -> sidebarCollapsed,
            this::updateSidebarSearchActionButtons,
            (iconKey, title) -> themeCoordinator.createSvgIcon(iconKey, title, 16),
            this::categoryDisplayName
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

        themeCoordinator.updateSidebarIcons();
        updateSidebarSearchActionButtons();

        // 导航按钮
        ToggleGroup navGroup = new ToggleGroup();

        scheduleNavButton = createNavButton(IconKey.CALENDAR, text("nav.schedule"), navGroup);
        scheduleNavButton.setSelected(true);
        scheduleNavButton.setOnAction(e -> showView(scheduleListView));

        timelineNavButton = createNavButton(IconKey.TIMELINE, text("nav.timeline"), navGroup);
        timelineNavButton.setOnAction(e -> showView(timelineView));

        heatmapNavButton = createNavButton(IconKey.GRID_HEATMAP, text("nav.heatmap"), navGroup);
        heatmapNavButton.setOnAction(e -> showView(heatmapView));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        functionTitle = new Label(text("sidebar.functions"));
        functionTitle.getStyleClass().addAll("label-hint", "sidebar-function-title");

        settingsActionButton = createActionButton(IconKey.SETTINGS, text("sidebar.settings"), this::showSettingsDialog);

        appearanceToggle = new ToggleButton(text("sidebar.appearance.darkMode"));
        appearanceToggle.getStyleClass().addAll("nav-button", "sidebar-action-button", "sidebar-appearance-toggle");
        appearanceToggle.setMaxWidth(Double.MAX_VALUE);
        appearanceToggle.setGraphicTextGap(8);
        appearanceToggle.setContentDisplay(ContentDisplay.LEFT);
        appearanceToggle.setTextOverrun(OverrunStyle.CLIP);
        appearanceToggle.setWrapText(false);
        appearanceToggle.setOnAction(e -> themeCoordinator.toggleThemeAppearance());
        registerCollapsibleControl(appearanceToggle, text("sidebar.appearance.darkMode"), "", text("sidebar.appearance.darkMode"));
        themeCoordinator.updateAppearanceTogglePresentation();

        exitActionButton = createActionButton(IconKey.LOGOUT, text("sidebar.exit"), Platform::exit);

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
        String featureTooltip = text("sidebar.feature.toggle");
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

        // 设置主题协调器的侧边栏组件引用
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
        themeCoordinator.setOnSidebarIconsChanged(this::refreshViewIcons);

        updateSidebarCollapseState();
        appShell.setLeft(sidebar);
    }

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
        collapsibleLabels.put(control, new String[] { expandedText, collapsedText });
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
                collapseToggle.setText(text("sidebar.title"));
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
                control.setPadding(new Insets(8, 0, 8, 0)); // Center padding
                control.setMinSize(40, 40); // Make it a square for centering
                control.setPrefSize(40, 40);
                control.setMaxSize(40, 40);
            } else {
                control.setText(texts[0]);
                control.setContentDisplay(ContentDisplay.LEFT);
                control.setStyle("-fx-padding: 10 12 10 12; -fx-alignment: center-left; -fx-min-height: 40; -fx-pref-height: 40; -fx-max-height: 40;");
                control.setAlignment(Pos.CENTER_LEFT);
                control.setPadding(new Insets(10, 12, 10, 12)); // Original nav-button padding
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

    public Pane createSvgIcon(String resourcePath, String title, double size) {
        return themeCoordinator.createSvgIcon(resourcePath, title, size);
    }

    public Pane createSvgIcon(IconKey iconKey, String title, double size) {
        return themeCoordinator.createSvgIcon(iconKey, title, size);
    }

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

    private void startHeaderClock() {
        if (headerClockTimeline != null) {
            return;
        }
        updateHeaderClockText();
        headerClockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateHeaderClockText()));
        headerClockTimeline.setCycleCount(Animation.INDEFINITE);
        headerClockTimeline.play();
    }

    private void stopHeaderClock() {
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

    public double parseDoublePreference(String key, double fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        UserPreferencesStore preferencesStore = applicationContext.getPreferencesStore();
        String raw = preferencesStore.get(key, null);
        return themeCoordinator.parseDoubleOrDefault(raw, fallback);
    }

    public void putPreference(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        applicationContext.getPreferencesStore().put(key, value != null ? value : "");
    }

    public WheelModifier getTimelineZoomWheelModifier() {
        return timelineZoomWheelModifier;
    }

    public ShortcutSpec getTimelineZoomInShortcut() {
        return timelineZoomInShortcut;
    }

    public ShortcutSpec getTimelineZoomOutShortcut() {
        return timelineZoomOutShortcut;
    }

    private void loadTimelineShortcutPreferences() {
        UserPreferencesStore preferencesStore = applicationContext.getPreferencesStore();
        timelineZoomWheelModifier = WheelModifier.fromPreference(preferencesStore.get(PREF_TIMELINE_ZOOM_WHEEL_MODIFIER_KEY, null));

        ShortcutSpec storedZoomIn = ShortcutSpec.parsePreference(preferencesStore.get(PREF_TIMELINE_ZOOM_IN_KEY, null));
        if (storedZoomIn != null) {
            timelineZoomInShortcut = storedZoomIn;
        }

        ShortcutSpec storedZoomOut = ShortcutSpec.parsePreference(preferencesStore.get(PREF_TIMELINE_ZOOM_OUT_KEY, null));
        if (storedZoomOut != null) {
            timelineZoomOutShortcut = storedZoomOut;
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
                featurePanelToggle.setText(text("sidebar.more"));
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

    private void createInfoPanel() {
        infoPanelView = new InfoPanelView(this);
        infoPanelView.hideImmediately();
        appShell.setRight(infoPanelView.getView());
    }
    
    private void showView(View view) {
        currentView = view;
        navigationService.setCurrentScreen(resolveScreen(view));
        appShell.setCenter(view.getView());
        view.refresh();
        themeCoordinator.requestGlassRefresh();
    }
    
    public void showScheduleDetails(ScheduleItem item) {
        if (item == null) {
            return;
        }
        ScheduleItem detailsItem = item;
        if (item.hasRecurrence()
            && item.getViewKey() != null
            && item.getId() != null
            && !item.getId().equals(item.getViewKey())) {
            try {
                ScheduleItem baseItem = findScheduleById(item.getId());
                if (baseItem != null) {
                    detailsItem = baseItem;
                }
            } catch (SQLException ignored) {
            }
        }
        navigationService.setSelectedScheduleItem(detailsItem);
        infoPanelView.setSchedule(detailsItem);
        infoPanelView.showWithAnimation();
        themeCoordinator.requestGlassRefresh();
    }

    public void showScheduleDetailsAndFocusTitle(ScheduleItem item) {
        showScheduleDetails(item);
        infoPanelView.focusTitleEditor();
    }

    public void closeScheduleDetails() {
        infoPanelView.hideWithAnimation();
        themeCoordinator.requestGlassRefresh();
    }

    public boolean isScheduleSelected(ScheduleItem item) {
        ScheduleItem selectedItem = navigationService.getSelectedScheduleItem();
        if (selectedItem == null || item == null) {
            return false;
        }
        if (selectedItem.getId() != null && !selectedItem.getId().isBlank()
            && item.getId() != null && !item.getId().isBlank()) {
            return selectedItem.getId().equals(item.getId());
        }
        return selectedItem == item;
    }
    
    public ScheduleItem getSelectedScheduleItem() {
        return navigationService.getSelectedScheduleItem();
    }

    public ScheduleCompletionCoordinator.PendingCompletion prepareScheduleCompletion(
        ScheduleItem item,
        boolean targetCompleted
    ) {
        return scheduleHandler.prepareCompletion(item, targetCompleted);
    }

    public boolean updateScheduleCompletion(ScheduleItem item, boolean targetCompleted) {
        return scheduleHandler.updateCompletion(item, targetCompleted);
    }
    
    public void refreshAllViews() {
        if (currentView != null) {
            currentView.refresh();
        }
        infoPanelView.refresh();
        updatePendingCountBadge();
        themeCoordinator.requestGlassRefresh();
    }

    public void refreshCurrentViewAndPendingCount() {
        if (currentView != null) {
            currentView.refresh();
        }
        updatePendingCountBadge();
        themeCoordinator.requestGlassRefresh();
    }

    public void refreshDataViews() {
        if (scheduleListView != null) {
            scheduleListView.refresh();
        }
        if (timelineView != null) {
            timelineView.refresh();
        }
        if (heatmapView != null) {
            heatmapView.refresh();
        }
        if (currentView != null && currentView != scheduleListView && currentView != timelineView && currentView != heatmapView) {
            currentView.refresh();
        }
        updatePendingCountBadge();
        themeCoordinator.requestGlassRefresh();
    }

    public String createSchedule(ScheduleItem item) throws SQLException {
        return scheduleHandler.createSchedule(item);
    }

    public ScheduleItem quickCreateSchedule(String rawTitle) throws SQLException {
        return scheduleHandler.quickCreateSchedule(rawTitle);
    }

    public boolean saveSchedule(ScheduleItem item) throws SQLException {
        return scheduleHandler.saveSchedule(item);
    }

    public boolean removeSchedule(String scheduleId) throws SQLException {
        return scheduleHandler.removeSchedule(scheduleId);
    }

    public ScheduleItem findScheduleById(String scheduleId) throws SQLException {
        return scheduleHandler.findScheduleById(scheduleId);
    }

    public List<ScheduleItem> loadAllSchedules() throws SQLException {
        return scheduleHandler.loadAllSchedules();
    }

    public List<ScheduleItem> searchSchedules(String keyword) throws SQLException {
        return scheduleHandler.searchSchedules(keyword);
    }

    public List<ScheduleItem> loadDeletedSchedules() throws SQLException {
        return scheduleHandler.loadDeletedSchedules();
    }

    public boolean restoreDeletedSchedule(String scheduleId) throws SQLException {
        return scheduleHandler.restoreDeletedSchedule(scheduleId);
    }

    public boolean permanentlyDeleteSchedule(String scheduleId) throws SQLException {
        return scheduleHandler.permanentlyDeleteSchedule(scheduleId);
    }

    private void ensureReminderNotificationService() {
        if (reminderNotificationService != null) {
            return;
        }
        reminderNotificationService = new ReminderNotificationService(
            scheduleItemService,
            localizationService,
            message -> Platform.runLater(() -> showInfo(text("notification.reminder.unavailable.title"), message))
        );
    }

    private void requestReminderResync() {
        if (reminderNotificationService == null) {
            return;
        }
        reminderNotificationService.requestResync();
    }

    public void focusScheduleQuickAdd() {
        showView(scheduleListView);
        if (scheduleListView != null) {
            scheduleListView.focusQuickAddInput();
        }
    }

    public List<ScheduleItem> applyPendingCompletionMutations(List<ScheduleItem> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return schedules;
        }
        for (ScheduleCompletionMutation mutation : scheduleCompletionCoordinator.snapshotCommittedMutations()) {
            for (ScheduleItem item : schedules) {
                mutation.applyTo(item);
            }
        }
        return schedules;
    }

    private void applyCompletionMutationLocally(ScheduleCompletionMutation mutation) {
        mutation.applyTo(navigationService.getSelectedScheduleItem());
        for (ScheduleCompletionParticipant participant : collectCompletionParticipants()) {
            participant.applyCompletionMutation(mutation);
        }
        adjustPendingCountOptimistically(mutation.pendingCountDelta());
        requestReminderResync();
    }

    private void confirmCompletionMutationLocally(ScheduleCompletionMutation mutation) {
        mutation.applyTo(navigationService.getSelectedScheduleItem());
        for (ScheduleCompletionParticipant participant : collectCompletionParticipants()) {
            participant.confirmCompletionMutation(mutation);
        }
        requestReminderResync();
    }

    private void revertCompletionMutationLocally(ScheduleCompletionMutation mutation) {
        mutation.revertOn(navigationService.getSelectedScheduleItem());
        for (ScheduleCompletionParticipant participant : collectCompletionParticipants()) {
            participant.revertCompletionMutation(mutation);
        }
        adjustPendingCountOptimistically(-mutation.pendingCountDelta());
        requestReminderResync();
    }

    private List<ScheduleCompletionParticipant> collectCompletionParticipants() {
        List<ScheduleCompletionParticipant> participants = new ArrayList<>();
        if (scheduleListView instanceof ScheduleCompletionParticipant) {
            participants.add(scheduleListView);
        }
        if (timelineView instanceof ScheduleCompletionParticipant) {
            participants.add(timelineView);
        }
        if (heatmapView instanceof ScheduleCompletionParticipant) {
            participants.add(heatmapView);
        }
        if (infoPanelView instanceof ScheduleCompletionParticipant) {
            participants.add(infoPanelView);
        }
        return participants;
    }

    private void adjustPendingCountOptimistically(int delta) {
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

    /* private void reportCompletionPersistenceFailure(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()
            ? cause.getMessage()
            : "鏈兘淇濆瓨鏃ョ▼鐘舵€佸彉鏇淬€?";
        showError("鏇存柊鐘舵€佸け璐?, message);
    }

    }*/

    private void reportCompletionPersistenceFailure(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()
            ? cause.getMessage()
            : text("error.scheduleUpdate.message");
        showError(text("error.scheduleUpdate.title"), message);
    }

    private ThreadFactory createCompletionThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "todo-schedule-completion");
            thread.setDaemon(true);
            return thread;
        };
    }

    private void refreshViewIcons() {
        if (scheduleListView != null) {
            scheduleListView.refreshIcons();
        }
        if (timelineView != null) {
            timelineView.refreshIcons();
        }
        if (heatmapView != null) {
            heatmapView.refreshIcons();
        }
        if (infoPanelView != null) {
            infoPanelView.refreshIcons();
        }
    }

    private static boolean isBlankSearchText(String text) {
        return text == null || text.trim().isEmpty();
    }

    public void openNewScheduleDialog() {
        focusScheduleQuickAdd();
    }
    
    public void openEditScheduleDialog(ScheduleItem item) {
        showScheduleDetailsAndFocusTitle(item);
    }
    
    private void showSettingsDialog() {
        settingsDialog.show();
    }

    public boolean isTimeTextInputEnabled() {
        return customOptionsService != null && customOptionsService.isTimeTextInputEnabled();
    }






    public String text(String key, Object... args) {
        return loc.text(key, args);
    }

    public String format(String patternKey, TemporalAccessor value) {
        return loc.format(patternKey, value);
    }

    public String themeFamilyDisplayName(ThemeFamily family) {
        return loc.themeFamilyDisplayName(family);
    }

    public String classicPaletteDisplayName(ClassicThemePalette palette) {
        return loc.classicPaletteDisplayName(palette);
    }

    public String iconPackDisplayName(IconPack iconPack) {
        return loc.iconPackDisplayName(iconPack);
    }

    public String currentThemeDisplayName(ThemeFamily family, ClassicThemePalette palette) {
        return loc.currentThemeDisplayName(family, palette);
    }

    public String currentIconDisplayName(IconPack iconPack, boolean bindingEnabled) {
        return loc.currentIconDisplayName(iconPack, bindingEnabled);
    }

    public String scheduleCardStyleDisplayName(String styleId) {
        return loc.scheduleCardStyleDisplayName(styleId);
    }

    public String priorityDisplayName(String priority) {
        return loc.priorityDisplayName(priority);
    }

    public String categoryDisplayName(String category) {
        return loc.categoryDisplayName(category);
    }

    public String recurrenceSummary(RecurrenceRule rule) {
        return loc.recurrenceSummary(rule);
    }

    public String weekdayShort(DayOfWeek dayOfWeek) {
        return loc.weekdayShort(dayOfWeek);
    }

    public String weekdayNarrow(DayOfWeek dayOfWeek) {
        return loc.weekdayNarrow(dayOfWeek);
    }

    public AppLanguage getActiveLanguage() {
        return localizationService.getActiveLanguage();
    }

    public AppLanguage getPreferredLanguage() {
        return localizationService.getPreferredLanguage();
    }

    public AppFontWeight getCurrentFontWeight() {
        return fontService.getCurrentFontWeight();
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public CustomOptionsService getCustomOptionsService() {
        return customOptionsService;
    }

    public ThemeService getThemeService() {
        return themeService;
    }

    public FontService getFontService() {
        return fontService;
    }

    public void applyDialogPreferences(DialogPane pane) {
        if (pane == null) {
            return;
        }
        pane.getStylesheets().setAll(getCurrentThemeStylesheets());
        fontService.applyTo(pane, localizationService.getActiveLanguage());
        themeCoordinator.updateDialogGlass(pane);
    }

    private void applyCurrentFont() {
        Node target = scene != null ? scene.getRoot() : root;
        if (target != null) {
            fontService.applyTo(target, localizationService.getActiveLanguage());
        }
    }


    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        applyDialogPreferences(alert.getDialogPane());
        alert.setTitle(text("alert.error.title"));
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        applyDialogPreferences(alert.getDialogPane());
        alert.setTitle(text("alert.info.title"));
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public StackPane getRoot() {
        return root;
    }

    public String getCurrentTheme() {
        return themeCoordinator.getCurrentTheme();
    }

    public String getCurrentScheduleCardStyle() {
        return themeCoordinator.getCurrentScheduleCardStyle();
    }

    public String getCurrentTimelineCardStyle() {
        return themeCoordinator.getCurrentTimelineCardStyle();
    }

    public List<String> getCurrentThemeStylesheets() {
        return themeCoordinator.getCurrentThemeStylesheets();
    }

    public void setPendingCountListener(IntConsumer listener) {
        pendingCountListener = listener;
        updatePendingCountBadge();
    }

    public void setScene(Scene scene) {
        this.scene = scene;
        themeCoordinator.setScene(scene);
        themeCoordinator.applySavedThemeIfNeeded();
        applyCurrentFont();
        themeCoordinator.updateMacaronPresentation();
        setupGlobalInfoPanelInteractions();
    }

    private void setupGlobalInfoPanelInteractions() {
        if (scene == null) {
            return;
        }

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!infoPanelView.isPanelVisible()) {
                return;
            }

            Object target = event.getTarget();
            if (target instanceof Node && isDescendant((Node) target, infoPanelView.getView())) {
                return;
            }

            closeScheduleDetails();
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && infoPanelView.isPanelVisible()) {
                closeScheduleDetails();
                event.consume();
                return;
            }

            if (currentView == timelineView && timelineView != null && handleTimelineZoomKeyPress(event)) {
                event.consume();
            }
        });
    }

    private boolean handleTimelineZoomKeyPress(KeyEvent event) {
        if (event == null || scene == null) {
            return false;
        }
        if (isTextInputFocused()) {
            return false;
        }

        if (matchesTimelineZoomIn(event)) {
            timelineView.zoomIn();
            return true;
        }
        if (matchesTimelineZoomOut(event)) {
            timelineView.zoomOut();
            return true;
        }
        return false;
    }

    private boolean isTextInputFocused() {
        if (scene == null) {
            return false;
        }
        Node focusOwner = scene.getFocusOwner();
        return focusOwner instanceof TextInputControl;
    }

    private boolean matchesTimelineZoomIn(KeyEvent event) {
        return matchesShortcutWithZoomAliases(timelineZoomInShortcut, event);
    }

    private boolean matchesTimelineZoomOut(KeyEvent event) {
        return matchesShortcutWithZoomAliases(timelineZoomOutShortcut, event);
    }

    private boolean matchesShortcutWithZoomAliases(ShortcutSpec spec, KeyEvent event) {
        if (spec == null || event == null) {
            return false;
        }

        KeyCode stored = spec.getKeyCode();
        KeyCode incoming = event.getCode();
        boolean keyMatches = switch (stored) {
            case EQUALS, ADD -> incoming == KeyCode.EQUALS || incoming == KeyCode.ADD;
            case MINUS, SUBTRACT -> incoming == KeyCode.MINUS || incoming == KeyCode.SUBTRACT;
            default -> incoming == stored;
        };
        if (!keyMatches) {
            return false;
        }

        if (spec.matchesModifiers(event)) {
            return true;
        }

        // Allow extra Shift when the stored shortcut doesn't require it (Ctrl+= vs Ctrl++).
        if ((stored == KeyCode.EQUALS || stored == KeyCode.ADD)
            && !spec.isShift()
            && event.isShiftDown()
            && event.isControlDown() == spec.isCtrl()
            && event.isAltDown() == spec.isAlt()
            && event.isMetaDown() == spec.isMeta()) {
            return true;
        }

        return false;
    }

    private boolean isDescendant(Node node, Node possibleAncestor) {
        Node current = node;
        while (current != null) {
            if (current == possibleAncestor) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
    
    public void initializeApplication() {
        refreshAllViews();
        ensureReminderNotificationService();
        requestReminderResync();
    }
    
    public void shutdown() {
        stopHeaderClock();
        scheduleCompletionExecutor.shutdownNow();
        if (reminderNotificationService != null) {
            reminderNotificationService.shutdown();
        }
    }

    private void updatePendingCountBadge() {
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

    private NavigationService.Screen resolveScreen(View view) {
        if (view instanceof TimelineView) {
            return NavigationService.Screen.TIMELINE;
        }
        if (view instanceof HeatmapView) {
            return NavigationService.Screen.HEATMAP;
        }
        return NavigationService.Screen.LIST;
    }
}
