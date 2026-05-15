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

    private boolean uiInitialized = false;
    private SidebarManager sidebarManager;
    private final ExecutorService scheduleCompletionExecutor;
    private final ScheduleCompletionCoordinator scheduleCompletionCoordinator;
    private ScheduleHandler scheduleHandler;
    private ReminderNotificationService reminderNotificationService;

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
        // 创建左侧导航栏
        sidebarManager = new SidebarManager(
            themeCoordinator,
            loc,
            scheduleItemService,
            applicationContext.getPreferencesStore(),
            this::showView,
            this::showSettingsDialog,
            themeCoordinator::toggleThemeAppearance,
            Platform::exit,
            collapsed -> themeCoordinator.setSidebarCollapsed(collapsed),
            keyword -> {
                scheduleListView.searchSchedules(keyword);
                showView(scheduleListView);
            },
            () -> {
                if (scheduleListView != null) {
                    scheduleListView.clearSearch();
                }
            }
        );
        VBox sidebar = sidebarManager.build();
        sidebarManager.startHeaderClock();
        themeCoordinator.setOnSidebarIconsChanged(this::refreshViewIcons);
        appShell.setLeft(sidebar);

        // 创建右侧信息面板
        createInfoPanel();

        // 初始化各个视图
        scheduleListView = new ScheduleListView(this);
        timelineView = new TimelineView(this);
        heatmapView = new HeatmapView(this);
        //infoPanelView = new InfoPanelView(this);

        // 设置SidebarManager视图引用
        sidebarManager.setScheduleView(scheduleListView);
        sidebarManager.setTimelineView(timelineView);
        sidebarManager.setHeatmapView(heatmapView);

        // 默认显示日程列表视图
        showView(scheduleListView);
        themeCoordinator.updateMacaronPresentation();
        uiInitialized = true;
    }


    public Pane createSvgIcon(String resourcePath, String title, double size) {
        return themeCoordinator.createSvgIcon(resourcePath, title, size);
    }

    public Pane createSvgIcon(IconKey iconKey, String title, double size) {
        return themeCoordinator.createSvgIcon(iconKey, title, size);
    }

    public Label createHeaderClockLabel() {
        return sidebarManager.createHeaderClockLabel();
    }

    public ReadOnlyStringProperty headerClockTextProperty() {
        return sidebarManager.headerClockTextProperty();
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
        sidebarManager.updatePendingCountBadge();
        themeCoordinator.requestGlassRefresh();
    }

    public void refreshCurrentViewAndPendingCount() {
        if (currentView != null) {
            currentView.refresh();
        }
        sidebarManager.updatePendingCountBadge();
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
        sidebarManager.updatePendingCountBadge();
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
        sidebarManager.adjustPendingCountOptimistically(mutation.pendingCountDelta());
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
        sidebarManager.adjustPendingCountOptimistically(-mutation.pendingCountDelta());
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
        sidebarManager.setPendingCountListener(listener);
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
        sidebarManager.stopHeaderClock();
        scheduleCompletionExecutor.shutdownNow();
        if (reminderNotificationService != null) {
            reminderNotificationService.shutdown();
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
