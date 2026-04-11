package com.example.controller;

import java.sql.SQLException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.IntConsumer;

import com.example.application.ApplicationContext;
import com.example.application.AppFontWeight;
import com.example.application.AppLanguage;
import com.example.application.ClassicThemePalette;
import com.example.application.ExperimentalFeaturesService;
import com.example.application.FontService;
import com.example.application.GlassBackdropCoordinator;
import com.example.application.IconKey;
import com.example.application.IconPack;
import com.example.application.IconService;
import com.example.application.LocalizationService;
import com.example.application.MainViewModel;
import com.example.application.NavigationService;
import com.example.application.RecurrenceSummaryFormatter;
import com.example.application.ScheduleItemService;
import com.example.application.ShortcutSpec;
import com.example.application.ThemeAppearance;
import com.example.application.ThemeFamily;
import com.example.application.ThemeService;
import com.example.application.WheelModifier;
import com.example.config.UserPreferencesStore;
import com.example.model.ScheduleItem;
import com.example.model.Schedule;
import com.example.model.RecurrenceRule;
import com.example.view.FlowchartView;
import com.example.view.HeatmapView;
import com.example.view.InfoPanelView;
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
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ReadOnlyStringProperty;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
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
import javafx.geometry.Side;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.util.Duration;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MainController {
    private final ApplicationContext applicationContext;
    private final MainViewModel mainViewModel;
    private final ScheduleItemService scheduleItemService;
    private final NavigationService navigationService;
    private final ThemeService themeService;
    private final IconService iconService;
    private final ExperimentalFeaturesService experimentalFeaturesService;
    private final LocalizationService localizationService;
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
    private FlowchartView flowchartView;
    private InfoPanelView infoPanelView;
    
    // 当前选中的视图
    private View currentView;

    private VBox sidebar;
    private VBox bottomActions;
    private Separator bottomActionsSeparator;
    private Label functionTitle;
    private TextField searchField;
    private ContextMenu searchSuggestionMenu;
    private PauseTransition searchSuggestionDebounce;
    private final List<String> searchHistory = new ArrayList<>();
    private ToggleButton scheduleNavButton;
    private ToggleButton timelineNavButton;
    private ToggleButton heatmapNavButton;
    private ToggleButton flowchartNavButton;
    private Button loginActionButton;
    private Button settingsActionButton;
    private ToggleButton appearanceToggle;
    private Button exitActionButton;
    private ToggleButton collapseToggle;
    private ToggleButton featurePanelToggle;
    private Button themeButton;
    private Pane collapseToggleIcon;
    private Pane featureToggleIcon;
    private Pane themeIcon;
    private boolean sidebarCollapsed = false;
    private boolean featurePanelExpanded = false;
    private boolean uiInitialized = false;
    private static final DateTimeFormatter HEADER_CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final StringProperty headerClockText = new SimpleStringProperty("");
    private Timeline headerClockTimeline;
    private final Map<Labeled, String[]> collapsibleLabels = new LinkedHashMap<>();
    private final List<ThemeFamily> availableThemeFamilies;
    private final List<ClassicThemePalette> classicThemePalettes;
    private final List<IconPack> availableIconPacks;
    private static final String DEFAULT_SCHEDULE_CARD_STYLE = ScheduleCardStyleSupport.getDefaultStyleId();
    private final ExecutorService scheduleCompletionExecutor;
    private final ScheduleCompletionCoordinator scheduleCompletionCoordinator;
    private IntConsumer pendingCountListener;
    private int lastKnownPendingCount = -1;
    
    // 主题管理
    private ThemeFamily currentThemeFamily = ThemeFamily.CLASSIC;
    private ThemeAppearance currentThemeAppearance = ThemeAppearance.LIGHT;
    private ClassicThemePalette currentClassicPalette = ClassicThemePalette.LIGHT;
    private String currentScheduleCardStyle = DEFAULT_SCHEDULE_CARD_STYLE;
    private IconPack currentIconPack = IconPack.CLASSIC;
    private boolean currentThemeIconBinding = true;

    private static final String SEARCH_HISTORY_PREFERENCE_KEY = "todo.search.history";
    private static final int SEARCH_HISTORY_MAX_ENTRIES = 20;
    private static final int SEARCH_SUGGESTION_HISTORY_WHEN_EMPTY_LIMIT = 8;
    private static final int SEARCH_SUGGESTION_TOTAL_LIMIT = 12;
    private static final int SEARCH_SUGGESTION_BUCKET_LIMIT = 3;
    private static final Duration SEARCH_SUGGESTION_DEBOUNCE_DURATION = Duration.millis(120);
    private static final Pattern SEARCH_HISTORY_WHITESPACE = Pattern.compile("\\s+");

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
        this.navigationService = mainViewModel.getNavigationService();
        this.themeService = mainViewModel.getThemeService();
        this.iconService = mainViewModel.getIconService();
        this.experimentalFeaturesService = applicationContext.getExperimentalFeaturesService();
        this.localizationService = mainViewModel.getLocalizationService();
        this.fontService = mainViewModel.getFontService();
        this.availableThemeFamilies = List.copyOf(themeService.getThemeFamilies());
        this.classicThemePalettes = List.copyOf(themeService.getClassicPalettes());
        this.availableIconPacks = List.copyOf(iconService.getAvailableIconPacks());
        syncThemeState();
        syncIconState();
        iconService.addChangeListener(this::refreshIconography);
        searchHistory.addAll(loadSearchHistory(applicationContext.getPreferencesStore()));
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
    }

    @FXML
    private void initialize() {
        initializeUI();
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
            macaronBackgroundLayer = createMacaronBackgroundLayer();
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
        flowchartView = new FlowchartView(this);
        //infoPanelView = new InfoPanelView(this);
        
        // 默认显示日程列表视图
        showView(scheduleListView);
        updateMacaronPresentation();
        uiInitialized = true;
    }

    private Pane createMacaronBackgroundLayer() {
        Pane backgroundLayer = new Pane();
        backgroundLayer.getStyleClass().add("macaron-background-layer");
        backgroundLayer.setManaged(false);
        backgroundLayer.setMouseTransparent(true);
        backgroundLayer.prefWidthProperty().bind(root.widthProperty());
        backgroundLayer.prefHeightProperty().bind(root.heightProperty());

        Region wash = createMacaronBackdropRegion("macaron-background-wash");
        wash.prefWidthProperty().bind(root.widthProperty());
        wash.prefHeightProperty().bind(root.heightProperty());

        Region blueOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-blue");
        blueOrb.setPrefSize(520, 360);
        blueOrb.layoutXProperty().bind(root.widthProperty().multiply(-0.06));
        blueOrb.layoutYProperty().bind(root.heightProperty().multiply(-0.08));

        Region pinkOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-pink");
        pinkOrb.setPrefSize(430, 320);
        pinkOrb.layoutXProperty().bind(root.widthProperty().multiply(0.56));
        pinkOrb.layoutYProperty().bind(root.heightProperty().multiply(0.05));

        Region mintOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-mint");
        mintOrb.setPrefSize(360, 280);
        mintOrb.layoutXProperty().bind(root.widthProperty().multiply(0.18));
        mintOrb.layoutYProperty().bind(root.heightProperty().multiply(0.52));

        Region purpleOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-purple");
        purpleOrb.setPrefSize(300, 240);
        purpleOrb.layoutXProperty().bind(root.widthProperty().multiply(0.72));
        purpleOrb.layoutYProperty().bind(root.heightProperty().multiply(0.48));

        Region creamOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-cream");
        creamOrb.setPrefSize(260, 220);
        creamOrb.layoutXProperty().bind(root.widthProperty().multiply(0.42));
        creamOrb.layoutYProperty().bind(root.heightProperty().multiply(-0.04));

        Region ribbonLeft = createMacaronBackdropRegion("macaron-ribbon", "macaron-ribbon-left");
        ribbonLeft.setPrefSize(760, 220);
        ribbonLeft.layoutXProperty().bind(root.widthProperty().multiply(-0.18));
        ribbonLeft.layoutYProperty().bind(root.heightProperty().multiply(0.34));

        Region ribbonRight = createMacaronBackdropRegion("macaron-ribbon", "macaron-ribbon-right");
        ribbonRight.setPrefSize(620, 200);
        ribbonRight.layoutXProperty().bind(root.widthProperty().multiply(0.44));
        ribbonRight.layoutYProperty().bind(root.heightProperty().multiply(0.68));

        backgroundLayer.getChildren().addAll(
            wash,
            blueOrb,
            pinkOrb,
            mintOrb,
            purpleOrb,
            creamOrb,
            ribbonLeft,
            ribbonRight
        );
        return backgroundLayer;
    }

    private Region createMacaronBackdropRegion(String... styleClasses) {
        Region region = new Region();
        region.setManaged(false);
        region.setMouseTransparent(true);
        region.getStyleClass().addAll(styleClasses);
        return region;
    }

    private void createSidebar() {
        sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        collapseToggle = new ToggleButton();
        collapseToggle.getStyleClass().addAll("nav-button", "sidebar-collapse-button");
        collapseToggle.setMaxWidth(Double.MAX_VALUE);
        String collapseTooltip = text("sidebar.collapse.toggle");
        collapseToggleIcon = createSvgIcon(IconKey.ARROW_RIGHT, collapseTooltip, 24);
        collapseToggle.setGraphic(collapseToggleIcon);
        collapseToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        collapseToggle.setAccessibleText(collapseTooltip);
        collapseToggle.setTooltip(new Tooltip(collapseTooltip));
        collapseToggle.setOnAction(e -> {
            sidebarCollapsed = collapseToggle.isSelected();
            updateSidebarCollapseState();
        });

        searchField = new TextField();
        searchField.getStyleClass().add("search-field");
        searchField.setPromptText(text("sidebar.search.prompt"));
        installSidebarSearchSuggestions();
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (shouldClearSearchResults(oldValue, newValue)) {
                clearScheduleSearch();
            }
            queueSidebarSearchSuggestionRefresh();
        });
        searchField.setOnAction(e -> performSearch(searchField.getText()));

        // 导航按钮
        ToggleGroup navGroup = new ToggleGroup();

        scheduleNavButton = createNavButton(IconKey.CALENDAR, text("nav.schedule"), navGroup);
        scheduleNavButton.setSelected(true);
        scheduleNavButton.setOnAction(e -> showView(scheduleListView));

        timelineNavButton = createNavButton(IconKey.TIMELINE, text("nav.timeline"), navGroup);
        timelineNavButton.setOnAction(e -> showView(timelineView));

        heatmapNavButton = createNavButton(IconKey.GRID_HEATMAP, text("nav.heatmap"), navGroup);
        heatmapNavButton.setOnAction(e -> showView(heatmapView));

        flowchartNavButton = createNavButton(IconKey.FLOWCHART, text("nav.flowchart"), navGroup);
        flowchartNavButton.setOnAction(e -> showView(flowchartView));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        functionTitle = new Label(text("sidebar.functions"));
        functionTitle.getStyleClass().addAll("label-hint", "sidebar-function-title");

        loginActionButton = createActionButton(IconKey.USER, text("sidebar.login"), this::showLoginDialog);
        settingsActionButton = createActionButton(IconKey.SETTINGS, text("sidebar.settings"), this::showSettingsDialog);

        appearanceToggle = new ToggleButton(text("sidebar.appearance.darkMode"));
        appearanceToggle.getStyleClass().addAll("nav-button", "sidebar-action-button", "sidebar-appearance-toggle");
        appearanceToggle.setMaxWidth(Double.MAX_VALUE);
        appearanceToggle.setGraphicTextGap(8);
        appearanceToggle.setContentDisplay(ContentDisplay.LEFT);
        appearanceToggle.setTextOverrun(OverrunStyle.CLIP);
        appearanceToggle.setWrapText(false);
        appearanceToggle.setOnAction(e -> toggleThemeAppearance());
        registerCollapsibleControl(appearanceToggle, text("sidebar.appearance.darkMode"), "", text("sidebar.appearance.darkMode"));
        updateAppearanceTogglePresentation();

        exitActionButton = createActionButton(IconKey.LOGOUT, text("sidebar.exit"), Platform::exit);

        bottomActions = new VBox(6);
        bottomActions.getStyleClass().add("sidebar-bottom-actions");
        bottomActions.getChildren().addAll(
            functionTitle,
            loginActionButton,
            settingsActionButton,
            appearanceToggle,
            exitActionButton
        );

        featurePanelToggle = new ToggleButton();
        featurePanelToggle.getStyleClass().addAll("nav-button", "sidebar-feature-toggle");
        featurePanelToggle.setMaxWidth(Double.MAX_VALUE);
        String featureTooltip = text("sidebar.feature.toggle");
        featureToggleIcon = createSvgIcon(IconKey.ARROW_RIGHT, featureTooltip, 24);
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
            searchField,
            new Separator(),
            scheduleNavButton,
            timelineNavButton,
            heatmapNavButton,
            flowchartNavButton,
            spacer,
            bottomActionsSeparator,
            bottomActions,
            featurePanelToggle
        );

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
            if (collapseToggleIcon != null) {
                collapseToggleIcon.setRotate(sidebarCollapsed ? 0 : 90);
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

        if (sidebarCollapsed) {
            hideSidebarSearchSuggestions();
        }

        updateFeaturePanelState();
    }

    public Pane createSvgIcon(String resourcePath, String title, double size) {
        Group iconGroup = loadSvgGraphic(resourcePath, size);
        Pane container = new Pane(iconGroup);
        container.getStyleClass().add("sidebar-svg-icon");
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        
        // 使用圆角矩形作为裁剪区域，如果不设置圆角则默认为直角
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(size);
        clip.setArcHeight(size);
        container.setClip(clip);
        
        if (title != null && !title.isEmpty()) {
            container.setAccessibleText(title);
            Tooltip.install(container, new Tooltip(title));
        }
        return container;
    }

    public Pane createSvgIcon(IconKey iconKey, String title, double size) {
        return createSvgIcon(iconService.resolveResourcePath(iconKey), title, size);
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

    private Group loadSvgGraphic(String resourcePath, double targetSize) {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return new Group();
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(stream);
            Element svg = document.getDocumentElement();
            Group group = new Group();
            double[] viewBox = parseViewBox(svg.getAttribute("viewBox"));
            parseSvgChildren(svg, group);
            double scale = targetSize / Math.max(viewBox[2], viewBox[3]);
            group.getTransforms().add(new Scale(scale, scale));
            group.getTransforms().add(new Translate(-viewBox[0], -viewBox[1]));
            return group;
        } catch (Exception e) {
            return new Group();
        }
    }

    private void parseSvgChildren(Element parent, Group target) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (!(child instanceof Element)) {
                continue;
            }
            Element element = (Element) child;
            String tag = element.getTagName();
            if ("g".equals(tag)) {
                Group group = new Group();
                applyTransform(group, element.getAttribute("transform"));
                target.getChildren().add(group);
                parseSvgChildren(element, group);
                continue;
            }
            Shape shape = createShapeFromElement(element);
            if (shape == null) {
                continue;
            }
            applyTransform(shape, element.getAttribute("transform"));
            target.getChildren().add(shape);
        }
    }

    private void applyTransform(Node node, String transformStr) {
        if (transformStr == null || transformStr.isEmpty()) return;
        
        Matcher matcher = Pattern.compile("(\\w+)\\s*\\(([^)]+)\\)").matcher(transformStr);
        while (matcher.find()) {
            String type = matcher.group(1);
            String[] args = matcher.group(2).split("[,\\s]+");
            try {
                if ("translate".equals(type)) {
                    double tx = parseDouble(args[0]);
                    double ty = args.length > 1 ? parseDouble(args[1]) : 0;
                    node.getTransforms().add(new Translate(tx, ty));
                } else if ("rotate".equals(type)) {
                    double angle = parseDouble(args[0]);
                    if (args.length >= 3) {
                        double cx = parseDouble(args[1]);
                        double cy = parseDouble(args[2]);
                        node.getTransforms().add(new Rotate(angle, cx, cy));
                    } else {
                        node.getTransforms().add(new Rotate(angle));
                    }
                } else if ("scale".equals(type)) {
                    double sx = parseDouble(args[0]);
                    double sy = args.length > 1 ? parseDouble(args[1]) : sx;
                    node.getTransforms().add(new Scale(sx, sy));
                }
            } catch (Exception e) {
                // ignore parsing errors for a single transform
            }
        }
    }

    private Shape createShapeFromElement(Element element) {
        String tag = element.getTagName();
        if ("circle".equals(tag)) {
            Circle circle = new Circle(
                parseDouble(element.getAttribute("cx")),
                parseDouble(element.getAttribute("cy")),
                parseDouble(element.getAttribute("r"))
            );
            applyShapeStyle(circle, element);
            return circle;
        }
        if ("rect".equals(tag)) {
            Rectangle rectangle = new Rectangle(
                parseDouble(element.getAttribute("x")),
                parseDouble(element.getAttribute("y")),
                parseDouble(element.getAttribute("width")),
                parseDouble(element.getAttribute("height"))
            );
            double rx = parseDouble(element.getAttribute("rx"));
            if (rx > 0) {
                rectangle.setArcWidth(rx * 2);
                rectangle.setArcHeight(rx * 2);
            }
            applyShapeStyle(rectangle, element);
            return rectangle;
        }
        if ("path".equals(tag)) {
            SVGPath path = new SVGPath();
            path.setContent(element.getAttribute("d"));
            applyShapeStyle(path, element);
            return path;
        }
        if ("text".equals(tag)) {
            Text text = new Text(element.getTextContent());
            text.setX(parseDouble(element.getAttribute("x")));
            text.setY(parseDouble(element.getAttribute("y")));
            
            String fontFamily = element.getAttribute("font-family");
            if (fontFamily.isEmpty()) fontFamily = "System";
            else fontFamily = fontFamily.replaceAll("['\"]", "");
            
            double fontSize = parseDoubleOrDefault(element.getAttribute("font-size"), 12);
            String fontWeight = element.getAttribute("font-weight");
            FontWeight fw = "bold".equalsIgnoreCase(fontWeight) ? FontWeight.BOLD : FontWeight.NORMAL;
            text.setFont(Font.font(fontFamily, fw, fontSize));
            
            String textAnchor = element.getAttribute("text-anchor");
            if ("middle".equals(textAnchor)) {
                text.setTextAlignment(TextAlignment.CENTER);
                text.setBoundsType(TextBoundsType.VISUAL);
                double initWidth = text.getLayoutBounds().getWidth();
                if (initWidth > 0) {
                    text.setTranslateX(-initWidth / 2);
                }
                text.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
                    text.setTranslateX(-newB.getWidth() / 2);
                });
            }
            applyShapeStyle(text, element);
            return text;
        }
        return null;
    }

    private void applyShapeStyle(Shape shape, Element element) {
        Color fill = parsePaint(element.getAttribute("fill"));
        Color stroke = parsePaint(element.getAttribute("stroke"));
        if (fill != null) {
            shape.setFill(fill);
        } else {
            shape.setFill(Color.TRANSPARENT);
        }
        if (stroke != null) {
            shape.setStroke(stroke);
            shape.setStrokeWidth(parseDoubleOrDefault(element.getAttribute("stroke-width"), 1));
        }
        String lineCap = element.getAttribute("stroke-linecap");
        if ("round".equalsIgnoreCase(lineCap)) {
            shape.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        String lineJoin = element.getAttribute("stroke-linejoin");
        if ("round".equalsIgnoreCase(lineJoin)) {
            shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }
    }

    private Color parsePaint(String value) {
        if (value == null || value.isEmpty() || "none".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private double[] parseViewBox(String viewBox) {
        if (viewBox == null || viewBox.isEmpty()) {
            return new double[] {0, 0, 100, 100};
        }
        String[] values = viewBox.trim().split("\\s+");
        if (values.length != 4) {
            return new double[] {0, 0, 100, 100};
        }
        return new double[] {
            parseDouble(values[0]),
            parseDouble(values[1]),
            parseDouble(values[2]),
            parseDouble(values[3])
        };
    }

    private double[] parseTranslate(String transform) {
        if (transform == null || !transform.startsWith("translate")) {
            return new double[] {0, 0};
        }
        int start = transform.indexOf('(');
        int end = transform.indexOf(')');
        if (start < 0 || end <= start) {
            return new double[] {0, 0};
        }
        String[] values = transform.substring(start + 1, end).trim().split("[,\\s]+");
        if (values.length == 1) {
            return new double[] {parseDouble(values[0]), 0};
        }
        return new double[] {parseDouble(values[0]), parseDouble(values[1])};
    }

    private double parseDouble(String value) {
        return parseDoubleOrDefault(value, 0);
    }

    private double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public double parseDoublePreference(String key, double fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        UserPreferencesStore preferencesStore = applicationContext.getPreferencesStore();
        String raw = preferencesStore.get(key, null);
        return parseDoubleOrDefault(raw, fallback);
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

    private void saveTimelineShortcutPreferences(WheelModifier modifier, ShortcutSpec zoomIn, ShortcutSpec zoomOut) {
        timelineZoomWheelModifier = modifier != null ? modifier : WheelModifier.CTRL;
        timelineZoomInShortcut = zoomIn != null ? zoomIn : ShortcutSpec.of(true, false, false, false, KeyCode.EQUALS);
        timelineZoomOutShortcut = zoomOut != null ? zoomOut : ShortcutSpec.of(true, false, false, false, KeyCode.MINUS);

        UserPreferencesStore preferencesStore = applicationContext.getPreferencesStore();
        preferencesStore.put(PREF_TIMELINE_ZOOM_WHEEL_MODIFIER_KEY, timelineZoomWheelModifier.getId());
        preferencesStore.put(PREF_TIMELINE_ZOOM_IN_KEY, timelineZoomInShortcut.toPreferenceString());
        preferencesStore.put(PREF_TIMELINE_ZOOM_OUT_KEY, timelineZoomOutShortcut.toPreferenceString());
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
            if (featureToggleIcon != null) {
                featureToggleIcon.setRotate(featurePanelExpanded ? 90 : 0);
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
        updateThemeIconState();
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
        requestGlassRefresh();
    }
    
    public void showScheduleDetails(Schedule schedule) {
        if (schedule == null) {
            return;
        }
        Schedule detailsSchedule = schedule;
        if (schedule.hasRecurrence()
            && schedule.getViewKey() != null
            && schedule.getId() != null
            && !schedule.getId().equals(schedule.getViewKey())) {
            try {
                Schedule baseSchedule = findScheduleById(schedule.getId());
                if (baseSchedule != null) {
                    detailsSchedule = baseSchedule;
                }
            } catch (SQLException ignored) {
            }
        }
        navigationService.setSelectedSchedule(detailsSchedule);
        infoPanelView.setSchedule(detailsSchedule);
        infoPanelView.showWithAnimation();
        requestGlassRefresh();
    }

    public void showScheduleDetailsAndFocusTitle(Schedule schedule) {
        showScheduleDetails(schedule);
        infoPanelView.focusTitleEditor();
    }

    public void closeScheduleDetails() {
        infoPanelView.hideWithAnimation();
        requestGlassRefresh();
    }

    public boolean isScheduleSelected(Schedule schedule) {
        Schedule selectedSchedule = navigationService.getSelectedSchedule();
        if (selectedSchedule == null || schedule == null) {
            return false;
        }
        if (selectedSchedule.getId() != null && !selectedSchedule.getId().isBlank()
            && schedule.getId() != null && !schedule.getId().isBlank()) {
            return selectedSchedule.getId().equals(schedule.getId());
        }
        return selectedSchedule == schedule;
    }
    
    public Schedule getSelectedSchedule() {
        return navigationService.getSelectedSchedule();
    }

    public ScheduleCompletionCoordinator.PendingCompletion prepareScheduleCompletion(
        Schedule schedule,
        boolean targetCompleted
    ) {
        return scheduleCompletionCoordinator.prepare(schedule, targetCompleted);
    }

    public boolean updateScheduleCompletion(Schedule schedule, boolean targetCompleted) {
        if (schedule == null) {
            return false;
        }
        return scheduleCompletionCoordinator.submitImmediate(schedule, targetCompleted);
        /*    boolean updated = scheduleDAO.updateScheduleStatus(schedule.getId(), targetCompleted);
            if (!updated) {
                showError("更新状态失败", "未能保存日程状态变更。");
                return false;
            }

            schedule.setCompleted(targetCompleted);
            if (selectedSchedule != null && isScheduleSelected(schedule)) {
                selectedSchedule.setCompleted(targetCompleted);
            }

            refreshAllViews();
            return true;
        } catch (SQLException e) {
            showError("更新状态失败", e.getMessage());
            return false;
        }*/
    }
    
    public void refreshAllViews() {
        if (currentView != null) {
            currentView.refresh();
        }
        infoPanelView.refresh();
        updatePendingCountBadge();
        requestGlassRefresh();
    }

    public void refreshCurrentViewAndPendingCount() {
        if (currentView != null) {
            currentView.refresh();
        }
        updatePendingCountBadge();
        requestGlassRefresh();
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
        requestGlassRefresh();
    }

    public String createSchedule(Schedule schedule) throws SQLException {
        return scheduleItemService.addScheduleItem(schedule);
    }

    public Schedule quickCreateSchedule(String rawTitle) throws SQLException {
        String title = rawTitle == null ? "" : rawTitle.strip();
        if (title.isEmpty()) {
            return null;
        }

        Schedule schedule = new Schedule();
        schedule.setName(title);
        schedule.setDescription("");
        schedule.setStartAt(null);
        schedule.setDueAt(LocalDate.now().atTime(23, 59));
        schedule.setCompleted(false);
        schedule.setPriority(Schedule.DEFAULT_PRIORITY);
        schedule.setCategory(Schedule.DEFAULT_CATEGORY);
        schedule.setTags("");
        schedule.setReminderTime(null);
        createSchedule(schedule);
        refreshAllViews();
        return schedule;
    }

    public boolean saveSchedule(Schedule schedule) throws SQLException {
        return scheduleItemService.updateScheduleItem(schedule);
    }

    public boolean removeSchedule(String scheduleId) throws SQLException {
        return scheduleItemService.softDeleteScheduleItem(scheduleId);
    }

    public Schedule findScheduleById(String scheduleId) throws SQLException {
        return toLegacySchedule(scheduleItemService.getScheduleItemById(scheduleId));
    }

    public List<Schedule> loadAllSchedules() throws SQLException {
        return toLegacySchedules(scheduleItemService.getActiveScheduleItems());
    }

    public List<Schedule> searchSchedules(String keyword) throws SQLException {
        return toLegacySchedules(scheduleItemService.searchActiveScheduleItems(keyword));
    }

    public List<Schedule> loadDeletedSchedules() throws SQLException {
        return toLegacySchedules(scheduleItemService.getDeletedScheduleItems());
    }

    public boolean restoreDeletedSchedule(String scheduleId) throws SQLException {
        return scheduleItemService.restoreScheduleItem(scheduleId);
    }

    public boolean permanentlyDeleteSchedule(String scheduleId) throws SQLException {
        return scheduleItemService.permanentlyDeleteScheduleItem(scheduleId);
    }

    public void focusScheduleQuickAdd() {
        showView(scheduleListView);
        if (scheduleListView != null) {
            scheduleListView.focusQuickAddInput();
        }
    }

    public List<Schedule> applyPendingCompletionMutations(List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return schedules;
        }
        for (ScheduleCompletionMutation mutation : scheduleCompletionCoordinator.snapshotCommittedMutations()) {
            for (Schedule schedule : schedules) {
                mutation.applyTo(schedule);
            }
        }
        return schedules;
    }

    private void applyCompletionMutationLocally(ScheduleCompletionMutation mutation) {
        mutation.applyTo(navigationService.getSelectedSchedule());
        for (ScheduleCompletionParticipant participant : collectCompletionParticipants()) {
            participant.applyCompletionMutation(mutation);
        }
        adjustPendingCountOptimistically(mutation.pendingCountDelta());
    }

    private void confirmCompletionMutationLocally(ScheduleCompletionMutation mutation) {
        mutation.applyTo(navigationService.getSelectedSchedule());
        for (ScheduleCompletionParticipant participant : collectCompletionParticipants()) {
            participant.confirmCompletionMutation(mutation);
        }
    }

    private void revertCompletionMutationLocally(ScheduleCompletionMutation mutation) {
        mutation.revertOn(navigationService.getSelectedSchedule());
        for (ScheduleCompletionParticipant participant : collectCompletionParticipants()) {
            participant.revertCompletionMutation(mutation);
        }
        adjustPendingCountOptimistically(-mutation.pendingCountDelta());
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

    private void syncThemeState() {
        currentThemeFamily = themeService.getCurrentThemeFamily();
        currentThemeAppearance = themeService.getCurrentAppearance();
        currentClassicPalette = themeService.getCurrentClassicPalette();
        currentScheduleCardStyle = themeService.getCurrentScheduleCardStyle();
        iconService.syncThemeAppearance(currentThemeAppearance);
    }

    private void syncIconState() {
        currentIconPack = iconService.getCurrentIconPack();
        currentThemeIconBinding = iconService.isThemeBindingEnabled();
    }
    
    private void switchTheme(ThemeFamily family) {
        previewThemeSelection(family, currentThemeAppearance, currentClassicPalette, null);
        saveThemePreference();
        updateThemeIconState();
    }

    private void applyThemeStylesheets(List<String> stylesheets) {
        if (scene == null || stylesheets.isEmpty()) {
            return;
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().addAll(stylesheets);
        refreshAllViews();
        updateThemeIconState();
        updateMacaronPresentation();
        updateAppearanceTogglePresentation();
    }

    private void previewThemeSelection(
        ThemeFamily family,
        ThemeAppearance appearance,
        ClassicThemePalette classicPalette,
        DialogPane dialogPane
    ) {
        currentThemeFamily = family != null ? family : ThemeFamily.CLASSIC;
        currentThemeAppearance = appearance != null ? appearance : ThemeAppearance.LIGHT;
        currentClassicPalette = classicPalette != null ? classicPalette : ClassicThemePalette.LIGHT;
        currentScheduleCardStyle = currentThemeFamily.getBoundScheduleCardStyle();
        iconService.syncThemeAppearance(currentThemeAppearance);

        List<String> stylesheets = themeService.resolveStylesheets(
            getClass(),
            currentThemeFamily,
            currentThemeAppearance,
            currentClassicPalette
        );
        applyThemeStylesheets(stylesheets);
        if (dialogPane != null) {
            dialogPane.getStylesheets().setAll(stylesheets);
            fontService.applyTo(dialogPane, localizationService.getActiveLanguage());
            updateDialogGlass(dialogPane);
        }
    }

    private void showThemeMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();

        for (ThemeFamily family : getSelectableThemeFamilies()) {
            String prefix = family == currentThemeFamily ? text("common.selected.prefix") : "";
            MenuItem item = new MenuItem(prefix + text("theme.menu.use", themeFamilyDisplayName(family)));
            item.setOnAction(e -> switchTheme(family));
            menu.getItems().add(item);
        }

        menu.show(anchor, Side.RIGHT, 0, 0);
    }

    private void togglePrimaryTheme() {
        List<ThemeFamily> selectableThemeFamilies = getSelectableThemeFamilies();
        int currentIndex = selectableThemeFamilies.indexOf(currentThemeFamily);
        int nextIndex = (currentIndex + 1) % selectableThemeFamilies.size();
        if (currentIndex < 0) {
            nextIndex = 0;
        }
        switchTheme(selectableThemeFamilies.get(nextIndex));
    }

    private void toggleThemeAppearance() {
        ThemeAppearance next = currentThemeAppearance == ThemeAppearance.DARK ? ThemeAppearance.LIGHT : ThemeAppearance.DARK;
        previewThemeSelection(currentThemeFamily, next, currentClassicPalette, null);
        saveThemePreference();
        updateAppearanceTogglePresentation();
    }

    private void updateAppearanceTogglePresentation() {
        if (appearanceToggle == null) {
            return;
        }

        boolean dark = currentThemeAppearance == ThemeAppearance.DARK;
        if (appearanceToggle.isSelected() != dark) {
            appearanceToggle.setSelected(dark);
        }

        String label = text("sidebar.appearance.darkMode");
        String tooltipText = dark ? text("sidebar.appearance.switchToLight") : text("sidebar.appearance.switchToDark");
        IconKey iconKey = dark ? IconKey.THEME_DARK : IconKey.THEME_LIGHT;

        appearanceToggle.setText(sidebarCollapsed ? "" : label);
        appearanceToggle.setContentDisplay(sidebarCollapsed ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
        appearanceToggle.setGraphic(createSvgIcon(iconKey, label, 24));
        appearanceToggle.setAccessibleText(label);
        appearanceToggle.setTooltip(new Tooltip(tooltipText));
    }

    private void loadThemePreference() {
        syncThemeState();
    }

    private void saveThemePreference() {
        themeService.selectTheme(currentThemeFamily, currentThemeAppearance, currentClassicPalette);
        syncThemeState();
        iconService.syncThemeFamily(currentThemeFamily);
        syncIconState();
    }

    private void applySavedThemeIfNeeded() {
        if (scene == null) {
            return;
        }
        syncThemeState();
        applyThemeStylesheets(getCurrentThemeStylesheets());
    }

    private void updateMacaronPresentation() {
        boolean macaronActive = currentThemeFamily == ThemeFamily.MACARON;
        if (macaronBackgroundLayer != null) {
            macaronBackgroundLayer.setVisible(macaronActive);
            macaronBackgroundLayer.setOpacity(macaronActive ? 1.0 : 0.0);
        }
        updateSceneGlass(scene);
    }

    private List<ThemeFamily> getSelectableThemeFamilies() {
        return filterThemeFamilies(availableThemeFamilies, experimentalFeaturesService.isLabsEnabled());
    }

    static List<ThemeFamily> filterThemeFamilies(List<ThemeFamily> families, boolean labsEnabled) {
        List<ThemeFamily> filtered = new ArrayList<>();
        for (ThemeFamily family : families) {
            if (labsEnabled || family != ThemeFamily.MACARON) {
                filtered.add(family);
            }
        }
        return filtered;
    }

    private void updateSceneGlass(Scene targetScene) {
        if (targetScene == null) {
            return;
        }
        GlassBackdropCoordinator coordinator = GlassBackdropCoordinator.install(targetScene);
        coordinator.setAppearance(currentThemeAppearance);
        coordinator.setActive(currentThemeFamily == ThemeFamily.MACARON);
        if (currentThemeFamily == ThemeFamily.MACARON) {
            coordinator.requestBurstRefresh(Duration.millis(420));
        }
    }

    private void requestGlassRefresh() {
        requestGlassRefresh(scene);
    }

    private void requestGlassRefresh(Scene targetScene) {
        if (targetScene == null || currentThemeFamily != ThemeFamily.MACARON) {
            return;
        }
        GlassBackdropCoordinator coordinator = GlassBackdropCoordinator.install(targetScene);
        coordinator.setAppearance(currentThemeAppearance);
        coordinator.requestBurstRefresh(Duration.millis(260));
    }

    private void updateDialogGlass(DialogPane pane) {
        if (pane == null) {
            return;
        }
        Object marker = pane.getProperties().putIfAbsent("todo.macaron.glass.bound", Boolean.TRUE);
        if (marker != null) {
            updateSceneGlass(pane.getScene());
            return;
        }
        pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                updateSceneGlass(newScene);
            }
        });
        if (pane.getScene() != null) {
            updateSceneGlass(pane.getScene());
        }
    }

    private void updateThemeIconState() {
        if (themeIcon == null) {
            return;
        }
        themeIcon.getStyleClass().removeAll("theme-icon-light", "theme-icon-dark");
        themeIcon.getStyleClass().add("theme-icon-light");
        themeIcon.setRotate(0);
        themeIcon.setOpacity(1.0);
    }

    private void refreshIconography() {
        syncIconState();
        updateSidebarIcons();
        if (scheduleListView != null) {
            scheduleListView.refreshIcons();
        }
        if (timelineView != null) {
            timelineView.refreshIcons();
        }
        if (heatmapView != null) {
            heatmapView.refreshIcons();
        }
        if (flowchartView != null) {
            flowchartView.refreshIcons();
        }
        if (infoPanelView != null) {
            infoPanelView.refreshIcons();
        }
    }

    private void updateSidebarIcons() {
        if (collapseToggle != null) {
            collapseToggle.setGraphic(createSvgIcon(IconKey.ARROW_RIGHT, text("sidebar.collapse.toggle"), 24));
            collapseToggleIcon = (Pane) collapseToggle.getGraphic();
            collapseToggleIcon.setRotate(sidebarCollapsed ? 0 : 90);
        }
        if (featurePanelToggle != null) {
            featurePanelToggle.setGraphic(createSvgIcon(IconKey.ARROW_RIGHT, text("sidebar.feature.toggle"), 24));
            featureToggleIcon = (Pane) featurePanelToggle.getGraphic();
            featureToggleIcon.setRotate(featurePanelExpanded ? 90 : 0);
        }
        if (scheduleNavButton != null) {
            scheduleNavButton.setGraphic(createSvgIcon(IconKey.CALENDAR, text("nav.schedule"), 24));
        }
        if (timelineNavButton != null) {
            timelineNavButton.setGraphic(createSvgIcon(IconKey.TIMELINE, text("nav.timeline"), 24));
        }
        if (heatmapNavButton != null) {
            heatmapNavButton.setGraphic(createSvgIcon(IconKey.GRID_HEATMAP, text("nav.heatmap"), 24));
        }
        if (flowchartNavButton != null) {
            flowchartNavButton.setGraphic(createSvgIcon(IconKey.FLOWCHART, text("nav.flowchart"), 24));
        }
        if (loginActionButton != null) {
            loginActionButton.setGraphic(createSvgIcon(IconKey.USER, text("sidebar.login"), 24));
        }
        if (settingsActionButton != null) {
            settingsActionButton.setGraphic(createSvgIcon(IconKey.SETTINGS, text("sidebar.settings"), 24));
        }
        if (appearanceToggle != null) {
            updateAppearanceTogglePresentation();
        }
        if (exitActionButton != null) {
            exitActionButton.setGraphic(createSvgIcon(IconKey.LOGOUT, text("sidebar.exit"), 24));
        }
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
        if (!searchField.isFocused() || !searchField.isVisible() || sidebarCollapsed) {
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

    private void hideSidebarSearchSuggestions() {
        if (searchSuggestionDebounce != null) {
            searchSuggestionDebounce.stop();
        }
        if (searchSuggestionMenu != null) {
            searchSuggestionMenu.hide();
        }
    }

    private static final class SearchSuggestion {
        private final String displayText;
        private final String insertText;
        private final IconKey iconKey;

        private SearchSuggestion(String displayText, String insertText, IconKey iconKey) {
            this.displayText = displayText;
            this.insertText = insertText;
            this.iconKey = iconKey;
        }
    }
    
    private void performSearch(String keyword) {
        hideSidebarSearchSuggestions();
        if (isBlankSearchText(keyword)) {
            clearScheduleSearch();
            return;
        }
        rememberSearchHistory(applicationContext.getPreferencesStore(), searchHistory, keyword);
        scheduleListView.searchSchedules(keyword.trim());
        showView(scheduleListView);
    }

    private void clearScheduleSearch() {
        if (scheduleListView == null) {
            return;
        }
        scheduleListView.clearSearch();
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
    
    public void openNewScheduleDialog() {
        focusScheduleQuickAdd();
    }
    
    public void openEditScheduleDialog(Schedule schedule) {
        showScheduleDetailsAndFocusTitle(schedule);
    }
    
    private void showLoginDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        applyDialogPreferences(alert.getDialogPane());
        alert.setTitle(text("sidebar.login"));
        alert.setHeaderText(text("login.dialog.title"));
        alert.setContentText(text("login.dialog.message"));
        alert.showAndWait();
    }
    
    private void showSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(text("settings.title"));
        dialog.setHeaderText(null);
        ButtonType saveButtonType = new ButtonType(text("common.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        dialog.getDialogPane().getStylesheets().setAll(getCurrentThemeStylesheets());
        dialog.getDialogPane().getStyleClass().add("settings-dialog-pane");
        applyDialogPreferences(dialog.getDialogPane());
        dialog.getDialogPane().setPrefWidth(940);
        dialog.getDialogPane().setPrefHeight(720);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("settings-shell");
        shell.setPrefWidth(940);
        shell.setPrefHeight(640);
        shell.setMinHeight(640);
        shell.setMaxHeight(640);

        VBox navBar = new VBox(8);
        navBar.getStyleClass().addAll("sidebar", "settings-nav");
        navBar.setPrefWidth(220);
        Label navTitle = new Label(text("settings.title"));
        navTitle.getStyleClass().addAll("label-title", "settings-nav-title");
        Label navSubTitle = new Label(text("settings.subtitle"));
        navSubTitle.getStyleClass().addAll("label-hint", "settings-nav-subtitle");

        ToggleGroup categoryGroup = new ToggleGroup();
        ToggleButton generalTab = new ToggleButton(text("settings.tab.details"));
        generalTab.setGraphic(createSvgIcon(IconKey.DETAIL, text("settings.tab.details"), 20));
        generalTab.setGraphicTextGap(8);
        
        ToggleButton personalizationTab = new ToggleButton(text("settings.tab.personalization"));
        personalizationTab.setGraphic(createSvgIcon(IconKey.STYLE, text("settings.tab.personalization"), 20));
        personalizationTab.setGraphicTextGap(8);
        ToggleButton dataTab = new ToggleButton(text("settings.tab.data"));
        dataTab.setGraphic(createSvgIcon(IconKey.FOLDER, text("settings.tab.data"), 20));
        dataTab.setGraphicTextGap(8);
        for (ToggleButton tab : List.of(generalTab, personalizationTab, dataTab)) {
            tab.getStyleClass().add("nav-button");
            tab.setMaxWidth(Double.MAX_VALUE);
            tab.setContentDisplay(ContentDisplay.LEFT);
            tab.setTextOverrun(OverrunStyle.CLIP);
            tab.setWrapText(false);
            tab.setToggleGroup(categoryGroup);
        }
        navBar.getChildren().addAll(navTitle, navSubTitle, generalTab, personalizationTab, dataTab);

        StackPane contentHost = new StackPane();
        contentHost.getStyleClass().add("settings-content-host");
        contentHost.setMinHeight(0);
        contentHost.setPrefHeight(0);
        contentHost.setMaxHeight(Double.MAX_VALUE);

        String appVersion = applicationContext.getAppProperties().getAppVersion();
        String displayAppVersion = "v" + appVersion;
        ThemeFamily originalThemeFamily = currentThemeFamily;
        ThemeAppearance originalThemeAppearance = currentThemeAppearance;
        ClassicThemePalette originalClassicPalette = currentClassicPalette;
        IconPack originalIconPack = currentIconPack;
        boolean originalThemeIconBinding = currentThemeIconBinding;
        boolean originalLabsEnabled = experimentalFeaturesService.isLabsEnabled();
        WheelModifier originalTimelineZoomWheelModifier = timelineZoomWheelModifier;
        ShortcutSpec originalTimelineZoomInShortcut = timelineZoomInShortcut;
        ShortcutSpec originalTimelineZoomOutShortcut = timelineZoomOutShortcut;

        VBox generalPage = new VBox(18);
        generalPage.getStyleClass().add("settings-page");
        generalPage.setFillWidth(true);
        generalPage.setMinHeight(0);
        VBox aboutCard = createSettingsCard(text("settings.about.title"), text("settings.about.subtitle"));
        Label aboutText = new Label(text("settings.about.body", displayAppVersion));
        aboutText.getStyleClass().add("settings-info-text");
        aboutText.setWrapText(true);
        aboutCard.getChildren().add(aboutText);
        VBox currentCard = createSettingsCard(text("settings.current.title"), text("settings.current.subtitle"));
        Label themeValue = new Label(currentThemeDisplayName(currentThemeFamily, currentClassicPalette));
        themeValue.getStyleClass().add("settings-inline-value");
        Label languageValue = new Label(localizationService.languageLabel(localizationService.getPreferredLanguage()));
        languageValue.getStyleClass().add("settings-inline-value");
        Label fontValue = new Label(localizationService.fontWeightLabel(fontService.getCurrentFontWeight()));
        fontValue.getStyleClass().add("settings-inline-value");
        Label iconValue = new Label(currentIconDisplayName(currentIconPack, currentThemeIconBinding));
        iconValue.getStyleClass().add("settings-inline-value");
        currentCard.getChildren().addAll(
            createSettingRow(text("settings.current.theme.label"), text("settings.current.theme.description"), themeValue),
            createSettingRow(text("settings.current.icon.label"), text("settings.current.icon.description"), iconValue),
            createSettingRow(text("settings.current.language.label"), text("settings.current.language.description"), languageValue),
            createSettingRow(text("settings.current.font.label"), text("settings.current.font.description"), fontValue)
        );
        ThemeFamily[] selectedThemeFamily = new ThemeFamily[] { originalThemeFamily };
        ClassicThemePalette[] selectedClassicPalette = new ClassicThemePalette[] { originalClassicPalette };
        IconPack[] selectedIconPack = new IconPack[] { originalIconPack };
        AppLanguage originalPreferredLanguage = localizationService.getPreferredLanguage();
        AppFontWeight originalFontWeight = fontService.getCurrentFontWeight();
        AppLanguage[] selectedLanguage = new AppLanguage[] { originalPreferredLanguage };
        AppFontWeight[] selectedFontWeight = new AppFontWeight[] { originalFontWeight };
        boolean[] selectedLabsEnabled = new boolean[] { originalLabsEnabled };
        boolean[] selectedThemeIconBinding = new boolean[] { originalThemeIconBinding };
        WheelModifier[] selectedTimelineZoomWheelModifier = new WheelModifier[] { originalTimelineZoomWheelModifier };
        ShortcutSpec[] selectedTimelineZoomInShortcut = new ShortcutSpec[] { originalTimelineZoomInShortcut };
        ShortcutSpec[] selectedTimelineZoomOutShortcut = new ShortcutSpec[] { originalTimelineZoomOutShortcut };

        VBox languageFontCard = createSettingsCard(text("settings.preferences.title"), text("settings.preferences.subtitle"));
        ComboBox<AppLanguage> languageComboBox = new ComboBox<>();
        languageComboBox.getItems().setAll(AppLanguage.supportedValues());
        languageComboBox.setValue(originalPreferredLanguage);
        languageComboBox.setMaxWidth(200);
        languageComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AppLanguage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : localizationService.languageLabel(item));
            }
        });
        languageComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AppLanguage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : localizationService.languageLabel(item));
            }
        });
        languageComboBox.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            selectedLanguage[0] = newValue != null ? newValue : originalPreferredLanguage;
            languageValue.setText(localizationService.languageLabel(selectedLanguage[0]));
        });

        ToggleGroup fontWeightGroup = new ToggleGroup();
        HBox fontChipRow = new HBox(8);
        fontChipRow.setAlignment(Pos.CENTER_LEFT);
        for (AppFontWeight fontWeight : AppFontWeight.supportedValues()) {
            ToggleButton chip = new ToggleButton(localizationService.fontWeightLabel(fontWeight));
            chip.getStyleClass().add("settings-style-chip");
            chip.setToggleGroup(fontWeightGroup);
            chip.setUserData(fontWeight);
            if (fontWeight == originalFontWeight) {
                chip.setSelected(true);
            }
            chip.setOnAction(event -> {
                selectedFontWeight[0] = fontWeight;
                fontValue.setText(localizationService.fontWeightLabel(fontWeight));
            });
            fontChipRow.getChildren().add(chip);
        }

        languageFontCard.getChildren().addAll(
            createSettingRow(text("settings.preferences.language.label"), text("settings.preferences.language.description"), languageComboBox),
            createSettingRow(text("settings.preferences.font.label"), text("settings.preferences.font.description"), fontChipRow)
        );

        VBox shortcutsCard = createSettingsCard(text("settings.shortcuts.title"), text("settings.shortcuts.subtitle"));
        ComboBox<WheelModifier> wheelModifierCombo = new ComboBox<>();
        wheelModifierCombo.getItems().setAll(WheelModifier.supportedValues());
        wheelModifierCombo.setValue(selectedTimelineZoomWheelModifier[0]);
        wheelModifierCombo.setMaxWidth(200);
        wheelModifierCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(WheelModifier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : wheelModifierDisplayName(item));
            }
        });
        wheelModifierCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(WheelModifier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : wheelModifierDisplayName(item));
            }
        });
        wheelModifierCombo.valueProperty().addListener((obs, oldValue, newValue) ->
            selectedTimelineZoomWheelModifier[0] = newValue != null ? newValue : originalTimelineZoomWheelModifier
        );

        Label zoomInValueLabel = new Label(selectedTimelineZoomInShortcut[0].toDisplayString());
        zoomInValueLabel.getStyleClass().add("settings-inline-value");
        Button zoomInSetButton = new Button(text("settings.shortcuts.action.set"));
        zoomInSetButton.getStyleClass().add("button-secondary");
        zoomInSetButton.setCursor(Cursor.HAND);
        zoomInSetButton.setOnAction(event -> {
            ShortcutSpec captured = captureShortcutSpec();
            if (captured != null) {
                selectedTimelineZoomInShortcut[0] = captured;
                zoomInValueLabel.setText(captured.toDisplayString());
            }
        });
        HBox zoomInControl = new HBox(10, zoomInValueLabel, zoomInSetButton);
        zoomInControl.setAlignment(Pos.CENTER_RIGHT);

        Label zoomOutValueLabel = new Label(selectedTimelineZoomOutShortcut[0].toDisplayString());
        zoomOutValueLabel.getStyleClass().add("settings-inline-value");
        Button zoomOutSetButton = new Button(text("settings.shortcuts.action.set"));
        zoomOutSetButton.getStyleClass().add("button-secondary");
        zoomOutSetButton.setCursor(Cursor.HAND);
        zoomOutSetButton.setOnAction(event -> {
            ShortcutSpec captured = captureShortcutSpec();
            if (captured != null) {
                selectedTimelineZoomOutShortcut[0] = captured;
                zoomOutValueLabel.setText(captured.toDisplayString());
            }
        });
        HBox zoomOutControl = new HBox(10, zoomOutValueLabel, zoomOutSetButton);
        zoomOutControl.setAlignment(Pos.CENTER_RIGHT);

        shortcutsCard.getChildren().addAll(
            createSettingRow(
                text("settings.shortcuts.timelineZoomWheel.label"),
                text("settings.shortcuts.timelineZoomWheel.description"),
                wheelModifierCombo
            ),
            createSettingRow(
                text("settings.shortcuts.timelineZoomIn.label"),
                text("settings.shortcuts.timelineZoomIn.description"),
                zoomInControl
            ),
            createSettingRow(
                text("settings.shortcuts.timelineZoomOut.label"),
                text("settings.shortcuts.timelineZoomOut.description"),
                zoomOutControl
            )
        );

        VBox labsCard = createSettingsCard(text("settings.labs.title"), text("settings.labs.subtitle"));
        ToggleButton labsToggle = new ToggleButton();
        labsToggle.getStyleClass().add("modern-toggle-switch");
        labsToggle.setCursor(Cursor.HAND);
        labsToggle.setSelected(originalLabsEnabled);
        if (originalLabsEnabled) {
            labsToggle.getStyleClass().add("on");
        }
        Label labsFootnote = new Label(text("settings.labs.note"));
        labsFootnote.getStyleClass().add("settings-row-desc");
        labsFootnote.setWrapText(true);
        labsCard.getChildren().addAll(
            createSettingRow(text("settings.labs.toggle.label"), text("settings.labs.toggle.description"), labsToggle),
            labsFootnote
        );
        generalPage.getChildren().addAll(aboutCard, currentCard, languageFontCard, shortcutsCard, labsCard);

        VBox personalizationPage = new VBox(18);
        personalizationPage.getStyleClass().add("settings-page");
        personalizationPage.setFillWidth(true);
        personalizationPage.setMinHeight(0);
        VBox themeCard = createSettingsCard(text("settings.theme.title"), text("settings.theme.subtitle"));
        ToggleGroup themeFamilyGroup = new ToggleGroup();
        FlowPane familyChipFlow = new FlowPane();
        familyChipFlow.getStyleClass().add("settings-chip-flow");
        familyChipFlow.setHgap(8);
        familyChipFlow.setVgap(10);
        familyChipFlow.setAlignment(Pos.CENTER_LEFT);
        familyChipFlow.setMaxWidth(Double.MAX_VALUE);
        Map<ThemeFamily, ToggleButton> familyChips = new LinkedHashMap<>();
        for (ThemeFamily family : availableThemeFamilies) {
            ToggleButton familyChip = new ToggleButton(themeFamilyDisplayName(family));
            familyChip.getStyleClass().add("settings-style-chip");
            familyChip.setToggleGroup(themeFamilyGroup);
            familyChip.setWrapText(true);
            familyChip.setTextOverrun(OverrunStyle.CLIP);
            familyChip.setUserData(family);
            if (family == selectedThemeFamily[0]) {
                familyChip.setSelected(true);
            }
            familyChips.put(family, familyChip);
            familyChipFlow.getChildren().add(familyChip);
        }

        FlowPane paletteFlow = new FlowPane();
        paletteFlow.getStyleClass().add("settings-chip-flow");
        paletteFlow.setHgap(10);
        paletteFlow.setVgap(10);
        paletteFlow.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup paletteGroup = new ToggleGroup();
        for (ClassicThemePalette palette : classicThemePalettes) {
            ToggleButton swatch = new ToggleButton();
            swatch.setToggleGroup(paletteGroup);
            swatch.getStyleClass().add("settings-theme-swatch");
            swatch.setStyle("-fx-background-color: " + palette.getPreviewColor() + ";");
            swatch.setTooltip(new Tooltip(classicPaletteDisplayName(palette)));
            swatch.setUserData(palette);
            if (palette == selectedClassicPalette[0]) {
                swatch.setSelected(true);
            }
            paletteFlow.getChildren().add(swatch);
        }
        VBox paletteRow = createStackedSettingRow(
            text("settings.theme.palette.label"),
            text("settings.theme.palette.description"),
            paletteFlow
        );

        VBox iconCard = createSettingsCard(text("settings.icon.title"), text("settings.icon.subtitle"));
        Label iconSummaryValue = new Label(currentIconDisplayName(selectedIconPack[0], selectedThemeIconBinding[0]));
        iconSummaryValue.getStyleClass().add("settings-inline-value");
        ToggleButton iconBindingToggle = new ToggleButton();
        iconBindingToggle.getStyleClass().add("modern-toggle-switch");
        iconBindingToggle.setCursor(Cursor.HAND);
        iconBindingToggle.setSelected(originalThemeIconBinding);
        if (originalThemeIconBinding) {
            iconBindingToggle.getStyleClass().add("on");
        }

        ToggleGroup iconPackGroup = new ToggleGroup();
        FlowPane iconPackFlow = new FlowPane();
        iconPackFlow.getStyleClass().add("settings-chip-flow");
        iconPackFlow.setHgap(8);
        iconPackFlow.setVgap(10);
        iconPackFlow.setAlignment(Pos.CENTER_LEFT);
        iconPackFlow.setMaxWidth(Double.MAX_VALUE);
        Map<IconPack, ToggleButton> iconPackChips = new LinkedHashMap<>();
        for (IconPack iconPack : availableIconPacks) {
            ToggleButton chip = new ToggleButton(iconPackDisplayName(iconPack));
            chip.getStyleClass().add("settings-style-chip");
            chip.setToggleGroup(iconPackGroup);
            chip.setWrapText(true);
            chip.setTextOverrun(OverrunStyle.CLIP);
            chip.setUserData(iconPack);
            if (iconPack == selectedIconPack[0]) {
                chip.setSelected(true);
            }
            iconPackChips.put(iconPack, chip);
            iconPackFlow.getChildren().add(chip);
        }

        Runnable refreshSettingsDialogIcons = () -> {
            generalTab.setGraphic(createSvgIcon(IconKey.DETAIL, text("settings.tab.details"), 20));
            personalizationTab.setGraphic(createSvgIcon(IconKey.STYLE, text("settings.tab.personalization"), 20));
            dataTab.setGraphic(createSvgIcon(IconKey.FOLDER, text("settings.tab.data"), 20));
        };
        Runnable updateThemeSummary = () ->
            themeValue.setText(currentThemeDisplayName(selectedThemeFamily[0], selectedClassicPalette[0]));
        Runnable updateIconSummary = () -> {
            String iconDisplay = currentIconDisplayName(selectedIconPack[0], selectedThemeIconBinding[0]);
            iconValue.setText(iconDisplay);
            iconSummaryValue.setText(iconDisplay);
        };
        Runnable updatePaletteVisibility = () -> {
            boolean visible = selectedThemeFamily[0] != null && selectedThemeFamily[0].supportsClassicPalette();
            paletteRow.setManaged(visible);
            paletteRow.setVisible(visible);
        };
        Runnable updateIconPackInteractivity = () -> {
            boolean manualSelectionEnabled = !selectedThemeIconBinding[0];
            for (ToggleButton chip : iconPackChips.values()) {
                chip.setDisable(!manualSelectionEnabled);
            }
        };
        Runnable updateLabsThemeVisibility = () -> {
            for (Map.Entry<ThemeFamily, ToggleButton> entry : familyChips.entrySet()) {
                boolean visible = selectedLabsEnabled[0] || entry.getKey() != ThemeFamily.MACARON;
                entry.getValue().setManaged(visible);
                entry.getValue().setVisible(visible);
            }
        };
        Runnable previewThemeAndIcons = () -> {
            previewThemeSelection(selectedThemeFamily[0], originalThemeAppearance, selectedClassicPalette[0], dialog.getDialogPane());
            iconService.previewSelection(selectedThemeFamily[0], selectedThemeIconBinding[0], selectedIconPack[0]);
            syncIconState();
            selectedIconPack[0] = currentIconPack;
            ToggleButton selectedPackChip = iconPackChips.get(selectedIconPack[0]);
            if (selectedPackChip != null) {
                selectedPackChip.setSelected(true);
            }
            updateThemeSummary.run();
            updateIconSummary.run();
            updatePaletteVisibility.run();
            updateIconPackInteractivity.run();
            refreshSettingsDialogIcons.run();
        };

        for (javafx.scene.Node node : familyChipFlow.getChildren()) {
            if (node instanceof ToggleButton chip) {
                chip.setOnAction(event -> {
                    selectedThemeFamily[0] = (ThemeFamily) ((ToggleButton) event.getSource()).getUserData();
                    previewThemeAndIcons.run();
                });
            }
        }
        for (javafx.scene.Node node : paletteFlow.getChildren()) {
            if (node instanceof ToggleButton swatch) {
                swatch.setOnAction(event -> {
                    selectedClassicPalette[0] = (ClassicThemePalette) ((ToggleButton) event.getSource()).getUserData();
                    previewThemeAndIcons.run();
                });
            }
        }
        for (javafx.scene.Node node : iconPackFlow.getChildren()) {
            if (node instanceof ToggleButton chip) {
                chip.setOnAction(event -> {
                    if (selectedThemeIconBinding[0]) {
                        return;
                    }
                    selectedIconPack[0] = (IconPack) ((ToggleButton) event.getSource()).getUserData();
                    previewThemeAndIcons.run();
                });
            }
        }

        iconBindingToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            boolean bindingEnabled = Boolean.TRUE.equals(newValue);
            selectedThemeIconBinding[0] = bindingEnabled;
            if (bindingEnabled) {
                iconBindingToggle.getStyleClass().add("on");
                selectedIconPack[0] = IconPack.boundToThemeFamily(selectedThemeFamily[0]);
                ToggleButton selectedPackChip = iconPackChips.get(selectedIconPack[0]);
                if (selectedPackChip != null) {
                    selectedPackChip.setSelected(true);
                }
            } else {
                iconBindingToggle.getStyleClass().remove("on");
            }
            previewThemeAndIcons.run();
        });

        labsToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            boolean labsEnabled = Boolean.TRUE.equals(newValue);
            selectedLabsEnabled[0] = labsEnabled;
            if (labsEnabled) {
                labsToggle.getStyleClass().add("on");
            } else {
                labsToggle.getStyleClass().remove("on");
            }

            if (!labsEnabled && selectedThemeFamily[0] == ThemeFamily.MACARON) {
                selectedThemeFamily[0] = ThemeFamily.CLASSIC;
                selectedClassicPalette[0] = ClassicThemePalette.LIGHT;
                ToggleButton classicChip = familyChips.get(ThemeFamily.CLASSIC);
                if (classicChip != null) {
                    classicChip.setSelected(true);
                }
                for (Node node : paletteFlow.getChildren()) {
                    if (node instanceof ToggleButton swatch
                        && swatch.getUserData() == ClassicThemePalette.LIGHT) {
                        swatch.setSelected(true);
                        break;
                    }
                }
                updateLabsThemeVisibility.run();
                previewThemeAndIcons.run();
                showInfo(
                    text("settings.labs.disabled.title"),
                    text("settings.labs.disabled.message")
                );
                return;
            }

            updateLabsThemeVisibility.run();
            updateThemeSummary.run();
        });

        updateLabsThemeVisibility.run();
        updatePaletteVisibility.run();
        updateIconSummary.run();
        updateIconPackInteractivity.run();
        refreshSettingsDialogIcons.run();
        themeCard.getChildren().addAll(
            createStackedSettingRow(text("settings.theme.family.label"), text("settings.theme.family.description"), familyChipFlow),
            paletteRow
        );
        iconCard.getChildren().addAll(
            createSettingRow(text("settings.icon.summary.label"), text("settings.icon.summary.description"), iconSummaryValue),
            createSettingRow(text("settings.icon.binding.label"), text("settings.icon.binding.description"), iconBindingToggle),
            createStackedSettingRow(text("settings.icon.pack.label"), text("settings.icon.pack.description"), iconPackFlow)
        );
        personalizationPage.getChildren().addAll(themeCard, iconCard);
        VBox dataPage = new VBox(18);
        dataPage.getStyleClass().add("settings-page");
        dataPage.setFillWidth(true);
        dataPage.setMinHeight(0);
        VBox trashCard = createSettingsCard(text("settings.data.title"), text("settings.data.subtitle"));
        Label trashSummary = new Label();
        trashSummary.getStyleClass().add("settings-row-desc");
        trashSummary.setWrapText(true);
        VBox trashItemsBox = new VBox(10);
        trashItemsBox.getStyleClass().add("settings-trash-list");
        populateTrashSettingsList(trashItemsBox, trashSummary);
        trashCard.getChildren().addAll(trashSummary, trashItemsBox);
        dataPage.getChildren().add(trashCard);

        ScrollPane generalPageScroll = createSettingsScrollPane(generalPage);
        ScrollPane personalizationPageScroll = createSettingsScrollPane(personalizationPage);
        ScrollPane dataPageScroll = createSettingsScrollPane(dataPage);

        Map<ToggleButton, Node> pages = new LinkedHashMap<>();
        pages.put(generalTab, generalPageScroll);
        pages.put(personalizationTab, personalizationPageScroll);
        pages.put(dataTab, dataPageScroll);

        Runnable updateNavActive = () -> {
            for (ToggleButton tab : pages.keySet()) {
                tab.getStyleClass().remove("active");
                if (tab.isSelected()) {
                    tab.getStyleClass().add("active");
                }
            }
        };

        ToggleButton[] lastSelectedTab = new ToggleButton[] { generalTab };
        categoryGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                ToggleButton fallbackTab = lastSelectedTab[0] != null ? lastSelectedTab[0] : generalTab;
                Platform.runLater(() -> fallbackTab.setSelected(true));
                return;
            }

            ToggleButton selectedTab = (ToggleButton) newToggle;
            lastSelectedTab[0] = selectedTab;
            if (selectedTab == dataTab) {
                populateTrashSettingsList(trashItemsBox, trashSummary);
            }
            updateNavActive.run();
            switchSettingsPage(contentHost, resolveSettingsPage(selectedTab, pages, generalPageScroll));
        });
        generalTab.setSelected(true);

        shell.setLeft(navBar);
        shell.setCenter(contentHost);
        dialog.getDialogPane().setContent(shell);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        if (saveButton != null) {
            saveButton.getStyleClass().add("primary-save-button");
            saveButton.setDefaultButton(true);
            saveButton.setCursor(Cursor.HAND);
        }
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("ghost-cancel-button");
            cancelButton.setCancelButton(true);
            cancelButton.setCursor(Cursor.HAND);
        }
        dialog.setOnShown(event -> {
            Node buttonBar = dialog.getDialogPane().lookup(".button-bar");
            if (buttonBar != null) {
                buttonBar.toFront();
                buttonBar.setMouseTransparent(false);
            }
            if (saveButton != null) {
                saveButton.toFront();
                saveButton.setMouseTransparent(false);
            }
            if (cancelButton != null) {
                cancelButton.toFront();
                cancelButton.setMouseTransparent(false);
            }
            requestGlassRefresh(dialog.getDialogPane().getScene());
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveButtonType) {
            previewThemeSelection(originalThemeFamily, originalThemeAppearance, originalClassicPalette, null);
            iconService.previewSelection(originalThemeFamily, originalThemeIconBinding, originalIconPack);
            syncIconState();
            return;
        }

        if (selectedLabsEnabled[0] != originalLabsEnabled) {
            experimentalFeaturesService.setLabsEnabled(selectedLabsEnabled[0]);
        }
        if (selectedThemeFamily[0] != originalThemeFamily || selectedClassicPalette[0] != originalClassicPalette) {
            saveThemePreference();
        }
        if (selectedThemeFamily[0] != originalThemeFamily
            || selectedIconPack[0] != originalIconPack
            || selectedThemeIconBinding[0] != originalThemeIconBinding) {
            iconService.commitSelection(selectedThemeFamily[0], selectedThemeIconBinding[0], selectedIconPack[0]);
            syncIconState();
        }
        if (selectedLanguage[0] != null && selectedLanguage[0] != originalPreferredLanguage) {
            localizationService.saveLanguagePreference(selectedLanguage[0]);
        }
        if (selectedFontWeight[0] != null && selectedFontWeight[0] != originalFontWeight) {
            fontService.selectFontWeight(selectedFontWeight[0]);
            applyCurrentFont();
        }
        if (selectedTimelineZoomWheelModifier[0] != originalTimelineZoomWheelModifier
            || !selectedTimelineZoomInShortcut[0].equals(originalTimelineZoomInShortcut)
            || !selectedTimelineZoomOutShortcut[0].equals(originalTimelineZoomOutShortcut)) {
            saveTimelineShortcutPreferences(
                selectedTimelineZoomWheelModifier[0],
                selectedTimelineZoomInShortcut[0],
                selectedTimelineZoomOutShortcut[0]
            );
        }
        refreshAllViews();
        if (selectedLanguage[0] != null && selectedLanguage[0] != originalPreferredLanguage) {
            showRestartLanguageNotice(selectedLanguage[0]);
        }
    }

    private VBox createSettingsCard(String title, String subtitle) {
        VBox card = new VBox(12);
        card.getStyleClass().add("settings-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-card-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("settings-card-subtitle");
        subtitleLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, subtitleLabel);
        return card;
    }

    private ScrollPane createSettingsScrollPane(VBox page) {
        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.getStyleClass().add("settings-page-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setMinHeight(0);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setFocusTraversable(false);
        return scrollPane;
    }

    static <K, V> V resolveSettingsPage(K selectedKey, Map<K, V> pages, V generalPage) {
        V page = pages.get(selectedKey);
        return page != null ? page : generalPage;
    }

    private HBox createSettingRow(String title, String description, Node control) {
        HBox row = new HBox(12);
        row.getStyleClass().add("settings-row");
        VBox textBox = new VBox(4);
        Label rowTitle = new Label(title);
        rowTitle.getStyleClass().add("settings-row-title");
        Label rowDesc = new Label(description);
        rowDesc.getStyleClass().add("settings-row-desc");
        rowDesc.setWrapText(true);
        textBox.getChildren().addAll(rowTitle, rowDesc);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(textBox, spacer, control);
        return row;
    }

    private VBox createStackedSettingRow(String title, String description, Node control) {
        VBox row = new VBox(10);
        row.getStyleClass().addAll("settings-row", "settings-row-stacked");
        VBox textBox = new VBox(4);
        Label rowTitle = new Label(title);
        rowTitle.getStyleClass().add("settings-row-title");
        Label rowDesc = new Label(description);
        rowDesc.getStyleClass().add("settings-row-desc");
        rowDesc.setWrapText(true);
        textBox.getChildren().addAll(rowTitle, rowDesc);
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        row.getChildren().addAll(textBox, control);
        return row;
    }

    private String wheelModifierDisplayName(WheelModifier modifier) {
        WheelModifier resolved = modifier != null ? modifier : WheelModifier.CTRL;
        return switch (resolved) {
            case CTRL -> text("shortcut.modifier.ctrl");
            case ALT -> text("shortcut.modifier.alt");
            case SHIFT -> text("shortcut.modifier.shift");
            case META -> text("shortcut.modifier.meta");
            case NONE -> text("shortcut.modifier.none");
        };
    }

    private ShortcutSpec captureShortcutSpec() {
        Dialog<ShortcutSpec> dialog = new Dialog<>();
        dialog.setTitle(text("settings.shortcuts.capture.title"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType(text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().getStylesheets().setAll(getCurrentThemeStylesheets());
        dialog.getDialogPane().getStyleClass().add("settings-dialog-pane");
        applyDialogPreferences(dialog.getDialogPane());

        Label hint = new Label(text("settings.shortcuts.capture.hint"));
        hint.getStyleClass().add("settings-row-desc");
        hint.setWrapText(true);
        dialog.getDialogPane().setContent(hint);

        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.setResult(null);
                dialog.close();
                event.consume();
                return;
            }

            ShortcutSpec spec = ShortcutSpec.fromKeyEvent(event);
            if (spec == null) {
                if (event.getCode() != null && event.getCode().isModifierKey()) {
                    hint.setText(text("settings.shortcuts.capture.invalidModifier"));
                } else {
                    hint.setText(text("settings.shortcuts.capture.invalid"));
                }
                event.consume();
                return;
            }

            dialog.setResult(spec);
            dialog.close();
            event.consume();
        });

        dialog.setResultConverter(button -> null);
        dialog.setOnShown(event -> Platform.runLater(() -> dialog.getDialogPane().requestFocus()));

        Optional<ShortcutSpec> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void populateTrashSettingsList(VBox host, Label summaryLabel) {
        host.getChildren().clear();
        try {
            List<Schedule> deletedSchedules = loadDeletedSchedules();
            summaryLabel.setText(text("trash.summary", deletedSchedules.size()));
            if (deletedSchedules.isEmpty()) {
                Label emptyLabel = new Label(text("trash.empty"));
                emptyLabel.getStyleClass().add("settings-row-desc");
                host.getChildren().add(emptyLabel);
                return;
            }

            for (Schedule schedule : deletedSchedules) {
                host.getChildren().add(createTrashRow(schedule, host, summaryLabel));
            }
        } catch (SQLException exception) {
            summaryLabel.setText(text("trash.load.failed"));
            Label errorLabel = new Label(exception.getMessage());
            errorLabel.getStyleClass().add("settings-row-desc");
            errorLabel.setWrapText(true);
            host.getChildren().add(errorLabel);
        }
    }

    private Node createTrashRow(Schedule schedule, VBox host, Label summaryLabel) {
        VBox textBox = new VBox(4);
        Label titleLabel = new Label(schedule.getName());
        titleLabel.getStyleClass().add("settings-row-title");
        Label metaLabel = new Label(buildTrashMetaText(schedule));
        metaLabel.getStyleClass().add("settings-row-desc");
        metaLabel.setWrapText(true);
        textBox.getChildren().addAll(titleLabel, metaLabel);

        Button restoreButton = new Button(text("trash.restore"));
        restoreButton.getStyleClass().add("button-secondary");
        restoreButton.setOnAction(e -> {
            try {
                if (restoreDeletedSchedule(schedule.getId())) {
                    populateTrashSettingsList(host, summaryLabel);
                    refreshDataViews();
                }
            } catch (SQLException exception) {
                showError(text("trash.restore.failed.title"), exception.getMessage());
            }
        });

        Button purgeButton = new Button(text("trash.purge"));
        purgeButton.getStyleClass().add("button-secondary");
        purgeButton.setOnAction(e -> {
            try {
                if (!permanentlyDeleteSchedule(schedule.getId())) {
                    showInfo(text("trash.purge.unavailable.title"), text("trash.purge.unavailable.message"));
                    return;
                }
                populateTrashSettingsList(host, summaryLabel);
                refreshDataViews();
            } catch (SQLException exception) {
                showError(text("trash.purge.failed.title"), exception.getMessage());
            }
        });

        HBox actions = new HBox(8, restoreButton, purgeButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, textBox, spacer, actions);
        row.getStyleClass().add("settings-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String buildTrashMetaText(Schedule schedule) {
        LocalDateTime dueAt = schedule.getDueAt();
        LocalDateTime startAt = schedule.getStartAt();
        LocalDateTime deletedAt = schedule.getDeletedAt();
        String timeText;
        if (startAt != null && dueAt != null) {
            timeText = format("format.trash.dateTime", startAt) + " -> " + format("format.trash.dateTime", dueAt);
        } else if (dueAt != null) {
            timeText = text("trash.meta.due", format("format.trash.dateTime", dueAt));
        } else if (startAt != null) {
            timeText = text("trash.meta.start", format("format.trash.dateTime", startAt));
        } else {
            timeText = text("trash.meta.unset");
        }
        String deletedText = deletedAt != null ? format("format.trash.dateTime", deletedAt) : text("trash.meta.none");
        return text("trash.meta.template", timeText, deletedText);
    }

    public String text(String key, Object... args) {
        return localizationService.text(key, args);
    }

    public String format(String patternKey, TemporalAccessor value) {
        return localizationService.format(patternKey, value);
    }

    public String themeFamilyDisplayName(ThemeFamily family) {
        return localizationService.themeFamilyLabel(family);
    }

    public String classicPaletteDisplayName(ClassicThemePalette palette) {
        return localizationService.classicPaletteLabel(palette);
    }

    public String iconPackDisplayName(IconPack iconPack) {
        return localizationService.iconPackLabel(iconPack);
    }

    public String currentThemeDisplayName(ThemeFamily family, ClassicThemePalette palette) {
        ThemeFamily resolvedFamily = family != null ? family : ThemeFamily.CLASSIC;
        if (resolvedFamily.supportsClassicPalette()) {
            return text(
                "settings.current.theme.classicValue",
                themeFamilyDisplayName(resolvedFamily),
                classicPaletteDisplayName(palette)
            );
        }
        return themeFamilyDisplayName(resolvedFamily);
    }

    public String currentIconDisplayName(IconPack iconPack, boolean bindingEnabled) {
        IconPack resolvedPack = iconPack != null ? iconPack : IconPack.CLASSIC;
        return bindingEnabled
            ? text("settings.current.icon.boundValue", iconPackDisplayName(resolvedPack))
            : text("settings.current.icon.unboundValue", iconPackDisplayName(resolvedPack));
    }

    public String scheduleCardStyleDisplayName(String styleId) {
        return localizationService.scheduleCardStyleLabel(styleId);
    }

    public String priorityDisplayName(String priority) {
        return localizationService.priorityLabel(priority);
    }

    public String categoryDisplayName(String category) {
        return localizationService.categoryLabel(category);
    }

    public String recurrenceSummary(RecurrenceRule rule) {
        return RecurrenceSummaryFormatter.describe(rule, localizationService);
    }

    public String weekdayShort(DayOfWeek dayOfWeek) {
        return localizationService.weekdayShort(dayOfWeek);
    }

    public String weekdayNarrow(DayOfWeek dayOfWeek) {
        return localizationService.weekdayNarrow(dayOfWeek);
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

    public FontService getFontService() {
        return fontService;
    }

    public void applyDialogPreferences(DialogPane pane) {
        if (pane == null) {
            return;
        }
        pane.getStylesheets().setAll(getCurrentThemeStylesheets());
        fontService.applyTo(pane, localizationService.getActiveLanguage());
        updateDialogGlass(pane);
    }

    private void applyCurrentFont() {
        Node target = scene != null ? scene.getRoot() : root;
        if (target != null) {
            fontService.applyTo(target, localizationService.getActiveLanguage());
        }
    }

    private void showRestartLanguageNotice(AppLanguage language) {
        showInfo(
            text("settings.language.restart.title"),
            text("settings.language.restart.message", localizationService.languageLabel(language))
        );
    }

    private void switchSettingsPage(StackPane host, Node page) {
        if (host.getChildren().isEmpty()) {
            page.setOpacity(0);
            page.setTranslateX(8);
            host.getChildren().setAll(page);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), page);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            TranslateTransition moveIn = new TranslateTransition(Duration.millis(180), page);
            moveIn.setFromX(8);
            moveIn.setToX(0);
            new ParallelTransition(fadeIn, moveIn).play();
            requestGlassRefresh(host.getScene());
            return;
        }
        Node current = host.getChildren().get(0);
        if (current == page) {
            return;
        }
        FadeTransition fadeOut = new FadeTransition(Duration.millis(130), current);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        TranslateTransition moveOut = new TranslateTransition(Duration.millis(130), current);
        moveOut.setFromX(0);
        moveOut.setToX(-8);
        ParallelTransition out = new ParallelTransition(fadeOut, moveOut);
        out.setOnFinished(evt -> {
            page.setOpacity(0);
            page.setTranslateX(8);
            host.getChildren().setAll(page);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), page);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            TranslateTransition moveIn = new TranslateTransition(Duration.millis(180), page);
            moveIn.setFromX(8);
            moveIn.setToX(0);
            ParallelTransition in = new ParallelTransition(fadeIn, moveIn);
            in.setOnFinished(event -> requestGlassRefresh(host.getScene()));
            in.play();
        });
        out.play();
        requestGlassRefresh(host.getScene());
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
        return currentThemeFamily.getId();
    }

    public String getCurrentScheduleCardStyle() {
        return currentScheduleCardStyle;
    }

    public String getCurrentTimelineCardStyle() {
        return currentScheduleCardStyle;
    }

    public List<String> getCurrentThemeStylesheets() {
        return themeService.resolveStylesheets(getClass(), currentThemeFamily, currentThemeAppearance, currentClassicPalette);
    }

    public void setPendingCountListener(IntConsumer listener) {
        pendingCountListener = listener;
        updatePendingCountBadge();
    }
    
    public void setScene(Scene scene) {
        this.scene = scene;
        applySavedThemeIfNeeded();
        applyCurrentFont();
        updateMacaronPresentation();
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
    }
    
    public void shutdown() {
        stopHeaderClock();
        scheduleCompletionExecutor.shutdownNow();
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

    private Schedule toLegacySchedule(ScheduleItem item) {
        if (item == null) {
            return null;
        }
        return item instanceof Schedule schedule ? new Schedule(schedule) : new Schedule(item);
    }

    private List<Schedule> toLegacySchedules(List<ScheduleItem> items) {
        List<Schedule> schedules = new ArrayList<>();
        if (items == null) {
            return schedules;
        }
        for (ScheduleItem item : items) {
            Schedule schedule = toLegacySchedule(item);
            if (schedule != null) {
                schedules.add(schedule);
            }
        }
        return schedules;
    }

    private NavigationService.Screen resolveScreen(View view) {
        if (view instanceof TimelineView) {
            return NavigationService.Screen.TIMELINE;
        }
        if (view instanceof HeatmapView) {
            return NavigationService.Screen.HEATMAP;
        }
        if (view instanceof FlowchartView) {
            return NavigationService.Screen.FLOWCHART;
        }
        return NavigationService.Screen.LIST;
    }
}
