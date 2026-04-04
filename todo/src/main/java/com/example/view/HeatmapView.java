package com.example.view;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.controller.MainController;
import com.example.controller.ScheduleCompletionCoordinator;
import com.example.controller.ScheduleCompletionMutation;
import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class HeatmapView implements View, ScheduleCompletionParticipant {
    private static final double GRID_GAP = 4;
    private static final double HEATMAP_GRID_PADDING = 8;
    private static final double YEAR_GRID_GAP = 14;
    private static final double DAY_PANEL_SIDE_WIDTH = 332;
    private static final double DAY_PANEL_BOTTOM_HEIGHT = 132;
    private static final double DAY_PANEL_BOTTOM_MAX_HEIGHT = 144;
    private static final String COMPLETED_PROXY_STYLE = "heatmap-completed-proxy";
    private static final String COMPLETED_PROXY_HOST_STYLE = "heatmap-completed-proxy-host";

    private enum PanelLayoutMode {
        SIDE,
        BOTTOM
    }

    private enum HeatmapCellVariant {
        STANDARD,
        YEAR_CARD
    }

    private final MainController controller;
    private final ScheduleDAO scheduleDAO;
    private final List<Schedule> loadedSchedules = new ArrayList<>();

    private VBox root;
    private HBox metaBar;
    private BorderPane workspaceHost;
    private VBox heatmapPane;
    private StackPane heatmapCanvasHost;
    private GridPane heatmapGrid;
    private GridPane yearMonthsGrid;
    private ScrollPane scrollPane;
    private Label statsLabel;
    private HBox legend;
    private Button prevBtn;
    private Button nextBtn;
    private VBox daySchedulePanel;
    private Label dayScheduleTitle;
    private VBox dayScheduleCardsBox;
    private Label dayScheduleCountLabel;
    private ScrollPane dayScheduleScrollPane;
    private VBox dayScheduleCardsColumn;
    private HBox dayScheduleCardsRow;
    private Pane currentDayScheduleCardsContainer;
    private HBox completedDropZone;
    private Map<LocalDate, List<Schedule>> schedulesByDate = new LinkedHashMap<>();
    private boolean redrawQueued;
    private LocalDate visibleStartDate;
    private LocalDate visibleEndDate;
    private ScheduleCollapsePopAnimator.MotionHandle completedProxyHandle;
    private StackPane completedProxyHost;
    private StackPane completedProxyShell;

    private PanelLayoutMode panelLayoutMode = PanelLayoutMode.SIDE;
    private String currentViewMode = "month";
    private LocalDate currentDate = LocalDate.now();
    private LocalDate selectedDate;

    public HeatmapView(MainController controller) {
        this.controller = controller;
        this.scheduleDAO = new ScheduleDAO();

        initializeHeatmapWorkspace();
    }

    private void initializeHeatmapWorkspace() {
        root = new VBox(12);
        root.getStyleClass().addAll("main-content", "heatmap-root");
        root.setPadding(new Insets(14));

        HBox header = buildHeatmapHeader();

        statsLabel = new Label();
        statsLabel.getStyleClass().addAll("label-subtitle", "heatmap-stats-label");

        legend = buildHeatmapLegend();
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        metaBar = new HBox(12, statsLabel, metaSpacer, legend);
        metaBar.setAlignment(Pos.CENTER_LEFT);
        metaBar.getStyleClass().add("heatmap-meta-bar");

        scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("heatmap-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> queueRefresh());

        heatmapGrid = new GridPane();
        heatmapGrid.getStyleClass().add("heatmap-grid");
        heatmapGrid.setHgap(GRID_GAP);
        heatmapGrid.setVgap(GRID_GAP);
        heatmapGrid.setPadding(new Insets(HEATMAP_GRID_PADDING));
        heatmapGrid.setAlignment(Pos.CENTER);
        heatmapGrid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        yearMonthsGrid = new GridPane();
        yearMonthsGrid.getStyleClass().add("heatmap-year-grid");
        yearMonthsGrid.setAlignment(Pos.TOP_CENTER);
        yearMonthsGrid.setHgap(YEAR_GRID_GAP);
        yearMonthsGrid.setVgap(YEAR_GRID_GAP);
        yearMonthsGrid.setPadding(new Insets(6, 2, 8, 2));
        yearMonthsGrid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        heatmapCanvasHost = new StackPane(heatmapGrid, yearMonthsGrid);
        heatmapCanvasHost.getStyleClass().add("heatmap-canvas-host");
        heatmapCanvasHost.setAlignment(Pos.TOP_CENTER);
        scrollPane.setContent(heatmapCanvasHost);

        heatmapPane = new VBox(scrollPane);
        heatmapPane.getStyleClass().add("heatmap-visual-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        daySchedulePanel = buildDaySchedulePanel();

        workspaceHost = new BorderPane();
        workspaceHost.getStyleClass().add("heatmap-workspace");
        workspaceHost.setCenter(heatmapPane);
        VBox.setVgrow(workspaceHost, Priority.ALWAYS);
        workspaceHost.widthProperty().addListener((obs, oldValue, newValue) -> queueRefresh());
        workspaceHost.heightProperty().addListener((obs, oldValue, newValue) -> queueRefresh());

        root.widthProperty().addListener((obs, oldValue, newValue) -> queueRefresh());
        root.heightProperty().addListener((obs, oldValue, newValue) -> queueRefresh());

        applyCurrentPanelLayout(false);
        root.getChildren().addAll(header, metaBar, workspaceHost);
    }

    private HBox buildHeatmapHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("heatmap-toolbar");

        Label titleLabel = new Label("日程热力图");
        titleLabel.getStyleClass().addAll("label-title", "heatmap-title");

        ToggleGroup viewGroup = new ToggleGroup();
        HBox viewModes = new HBox(8);
        viewModes.setAlignment(Pos.CENTER_LEFT);
        viewModes.getStyleClass().add("heatmap-mode-group");
        viewModes.getChildren().addAll(
            createViewModeButton(viewGroup, "/icons/macaron_week_icon.svg", "周视图", false, "week"),
            createViewModeButton(viewGroup, "/icons/macaron_month_icon.svg", "月视图", true, "month"),
            createViewModeButton(viewGroup, "/icons/macaron_year_icon.svg", "年视图", false, "year")
        );

        ToggleGroup layoutGroup = new ToggleGroup();
        HBox layoutModes = new HBox(0);
        layoutModes.setAlignment(Pos.CENTER_LEFT);
        layoutModes.getStyleClass().add("heatmap-layout-group");
        layoutModes.getChildren().addAll(
            createLayoutModeButton(layoutGroup, "侧栏", "切换到侧栏布局", true, PanelLayoutMode.SIDE),
            createLayoutModeButton(layoutGroup, "底栏", "切换到底栏布局", false, PanelLayoutMode.BOTTOM)
        );

        HBox navButtons = new HBox(8);
        navButtons.setAlignment(Pos.CENTER_RIGHT);
        navButtons.getStyleClass().add("heatmap-nav-group");

        prevBtn = createNavigationButton("/icons/macaron_prev_icon.svg", "上一周期", () -> navigate(-1));
        Button todayBtn = createNavigationButton("/icons/macaron_today_icon.svg", "回到今天", () -> {
            currentDate = LocalDate.now();
            queueRefresh();
        });
        nextBtn = createNavigationButton("/icons/macaron_next_icon.svg", "下一周期", () -> navigate(1));
        navButtons.getChildren().addAll(prevBtn, todayBtn, nextBtn);

        updateNavigationButtons();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(titleLabel, spacer, viewModes, layoutModes, navButtons);
        return header;
    }

    private ToggleButton createViewModeButton(
        ToggleGroup viewGroup,
        String iconPath,
        String tooltipText,
        boolean selected,
        String targetMode
    ) {
        ToggleButton button = new ToggleButton();
        button.setToggleGroup(viewGroup);
        button.setGraphic(controller.createSvgIcon(iconPath, tooltipText, 46));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip(tooltipText));
        button.setAccessibleText(tooltipText);
        button.getStyleClass().addAll("heatmap-toolbar-button", "heatmap-toolbar-icon-button");
        button.setSelected(selected);
        button.setOnAction(event -> {
            if (!button.isSelected()) {
                button.setSelected(true);
                return;
            }
            currentViewMode = targetMode;
            queueRefresh();
        });
        return button;
    }

    private ToggleButton createLayoutModeButton(
        ToggleGroup layoutGroup,
        String text,
        String tooltipText,
        boolean selected,
        PanelLayoutMode targetMode
    ) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(layoutGroup);
        button.setTooltip(new Tooltip(tooltipText));
        button.setAccessibleText(text);
        button.getStyleClass().addAll("heatmap-toolbar-button", "heatmap-layout-button");
        button.setSelected(selected);
        button.setOnAction(event -> {
            if (!button.isSelected()) {
                button.setSelected(true);
                return;
            }
            panelLayoutMode = targetMode;
            applyCurrentPanelLayout(true);
        });
        return button;
    }

    private Button createNavigationButton(String iconPath, String tooltipText, Runnable action) {
        Button button = new Button();
        button.setGraphic(controller.createSvgIcon(iconPath, tooltipText, 46));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip(tooltipText));
        button.setAccessibleText(tooltipText);
        button.getStyleClass().addAll("heatmap-toolbar-button", "heatmap-toolbar-icon-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private HBox buildHeatmapLegend() {
        HBox legendBox = new HBox(8);
        legendBox.setAlignment(Pos.CENTER_LEFT);
        legendBox.getStyleClass().add("heatmap-legend");
        return legendBox;
    }

    private VBox buildDaySchedulePanel() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("heatmap-day-panel");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("heatmap-day-header");

        dayScheduleTitle = new Label("选择日期");
        dayScheduleTitle.getStyleClass().add("heatmap-day-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        dayScheduleCountLabel = new Label("0 项");
        dayScheduleCountLabel.getStyleClass().add("heatmap-day-count");
        header.getChildren().addAll(dayScheduleTitle, spacer, dayScheduleCountLabel);

        dayScheduleScrollPane = new ScrollPane();
        dayScheduleScrollPane.getStyleClass().add("heatmap-day-scroll");
        dayScheduleScrollPane.setPannable(true);
        VBox.setVgrow(dayScheduleScrollPane, Priority.ALWAYS);

        dayScheduleCardsBox = new VBox(8);
        dayScheduleCardsBox.getStyleClass().addAll("heatmap-day-cards", "heatmap-day-cards-column");
        dayScheduleCardsColumn = dayScheduleCardsBox;

        dayScheduleCardsRow = new HBox(10);
        dayScheduleCardsRow.setAlignment(Pos.CENTER_LEFT);
        dayScheduleCardsRow.getStyleClass().addAll("heatmap-day-cards", "heatmap-day-cards-row");

        switchDayCardsContainer(dayScheduleCardsColumn);

        completedDropZone = new HBox();
        completedDropZone.setAlignment(Pos.CENTER_LEFT);
        completedDropZone.getStyleClass().add("heatmap-completed-zone");
        Label completedDropLabel = new Label("已完成归档区");
        completedDropLabel.getStyleClass().add("heatmap-completed-zone-label");
        completedDropZone.getChildren().add(completedDropLabel);

        panel.getChildren().addAll(header, dayScheduleScrollPane, completedDropZone);
        return panel;
    }

    private void switchDayCardsContainer(Pane targetContainer) {
        if (targetContainer == null || targetContainer == currentDayScheduleCardsContainer) {
            return;
        }
        currentDayScheduleCardsContainer = targetContainer;
        if (dayScheduleScrollPane != null) {
            dayScheduleScrollPane.setContent(targetContainer);
        }
    }

    private void applyCurrentPanelLayout(boolean refreshAfter) {
        if (workspaceHost == null || daySchedulePanel == null || dayScheduleScrollPane == null) {
            return;
        }

        workspaceHost.getStyleClass().removeAll("heatmap-workspace-side", "heatmap-workspace-bottom");
        daySchedulePanel.getStyleClass().removeAll("heatmap-day-panel-side", "heatmap-day-panel-bottom");
        workspaceHost.setRight(null);
        workspaceHost.setBottom(null);

        if (panelLayoutMode == PanelLayoutMode.SIDE) {
            workspaceHost.getStyleClass().add("heatmap-workspace-side");
            workspaceHost.setRight(daySchedulePanel);
            BorderPane.setMargin(daySchedulePanel, new Insets(0, 0, 0, 14));

            daySchedulePanel.getStyleClass().add("heatmap-day-panel-side");
            daySchedulePanel.setMinWidth(320);
            daySchedulePanel.setPrefWidth(determineDayPanelPreferredExtent(true));
            daySchedulePanel.setMaxWidth(340);
            daySchedulePanel.setMinHeight(0);
            daySchedulePanel.setMaxHeight(Double.MAX_VALUE);

            dayScheduleScrollPane.setFitToWidth(true);
            dayScheduleScrollPane.setFitToHeight(false);
            dayScheduleScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            dayScheduleScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            dayScheduleScrollPane.setPrefViewportHeight(360);
            switchDayCardsContainer(dayScheduleCardsColumn);
        } else {
            workspaceHost.getStyleClass().add("heatmap-workspace-bottom");
            workspaceHost.setBottom(daySchedulePanel);
            BorderPane.setMargin(daySchedulePanel, new Insets(12, 0, 0, 0));

            daySchedulePanel.getStyleClass().add("heatmap-day-panel-bottom");
            daySchedulePanel.setMinWidth(0);
            daySchedulePanel.setMaxWidth(Double.MAX_VALUE);
            daySchedulePanel.setMinHeight(124);
            daySchedulePanel.setPrefHeight(determineDayPanelPreferredExtent(false));
            daySchedulePanel.setMaxHeight(DAY_PANEL_BOTTOM_MAX_HEIGHT);

            dayScheduleScrollPane.setFitToWidth(false);
            dayScheduleScrollPane.setFitToHeight(true);
            dayScheduleScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            dayScheduleScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            dayScheduleScrollPane.setPrefViewportHeight(76);
            switchDayCardsContainer(dayScheduleCardsRow);
        }

        if (refreshAfter) {
            updateDaySchedulePanelContent();
            queueRefresh();
        }
    }

    private void initializeUI() {
        root = new VBox(15);
        root.getStyleClass().add("main-content");
        root.setPadding(new Insets(15));

        // 标题和工具栏
        HBox header = createHeader();

        // 统计信息
        statsLabel = new Label();
        statsLabel.getStyleClass().add("label-subtitle");

        // 热力图容器
        scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("heatmap-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> queueRefresh());

        heatmapGrid = new GridPane();
        heatmapGrid.getStyleClass().add("heatmap-grid");
        heatmapGrid.setHgap(GRID_GAP);
        heatmapGrid.setVgap(GRID_GAP);
        heatmapGrid.setPadding(new Insets(10));
        heatmapGrid.setAlignment(Pos.CENTER);

        scrollPane.setContent(heatmapGrid);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.widthProperty().addListener((obs, oldValue, newValue) -> queueRefresh());
        root.heightProperty().addListener((obs, oldValue, newValue) -> queueRefresh());

        // 图例
        legend = createLegend();

        daySchedulePanel = createDaySchedulePanel();

        root.getChildren().addAll(header, statsLabel, scrollPane, daySchedulePanel, legend);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("日程热力图");
        titleLabel.getStyleClass().add("label-title");

        // 视图模式选择
        ToggleGroup viewGroup = new ToggleGroup();
        HBox viewModes = new HBox(24); // Increase spacing to 24px (1.5x of 16px)
        viewModes.setAlignment(Pos.CENTER_LEFT);

        ToggleButton weekBtn = new ToggleButton();
        weekBtn.setGraphic(controller.createSvgIcon("/icons/macaron_week_icon.svg", null, 48));
        weekBtn.setToggleGroup(viewGroup);
        weekBtn.getStyleClass().setAll("icon-button");
        weekBtn.setTooltip(new Tooltip("周视图"));
        weekBtn.setOnAction(e -> {
            currentViewMode = "week";
            queueRefresh();
        });

        ToggleButton monthBtn = new ToggleButton();
        monthBtn.setGraphic(controller.createSvgIcon("/icons/macaron_month_icon.svg", null, 48));
        monthBtn.setToggleGroup(viewGroup);
        monthBtn.setSelected(true);
        monthBtn.getStyleClass().setAll("icon-button");
        monthBtn.setTooltip(new Tooltip("月视图"));
        monthBtn.setOnAction(e -> {
            currentViewMode = "month";
            queueRefresh();
        });

        ToggleButton yearBtn = new ToggleButton();
        yearBtn.setGraphic(controller.createSvgIcon("/icons/macaron_year_icon.svg", null, 48));
        yearBtn.setToggleGroup(viewGroup);
        yearBtn.getStyleClass().setAll("icon-button");
        yearBtn.setTooltip(new Tooltip("年视图"));
        yearBtn.setOnAction(e -> {
            currentViewMode = "year";
            queueRefresh();
        });
        
        viewModes.getChildren().addAll(weekBtn, monthBtn, yearBtn);

        // 导航按钮
        HBox navButtons = new HBox(24); // Increase spacing to 24px (1.5x of 16px)
        navButtons.setAlignment(Pos.CENTER_RIGHT);
        navButtons.setPadding(new Insets(0, 15, 0, 0)); // Keep the 15px right padding from edge

        prevBtn = new Button();
        prevBtn.setGraphic(controller.createSvgIcon("/icons/macaron_prev_icon.svg", null, 48));
        prevBtn.getStyleClass().setAll("icon-button");
        prevBtn.setOnAction(e -> navigate(-1));

        Button todayBtn = new Button();
        todayBtn.setGraphic(controller.createSvgIcon("/icons/macaron_today_icon.svg", null, 48));
        todayBtn.getStyleClass().setAll("icon-button");
        todayBtn.setTooltip(new Tooltip("回到今天"));
        todayBtn.setOnAction(e -> {
            currentDate = LocalDate.now();
            queueRefresh();
        });

        nextBtn = new Button();
        nextBtn.setGraphic(controller.createSvgIcon("/icons/macaron_next_icon.svg", null, 48));
        nextBtn.getStyleClass().setAll("icon-button");
        nextBtn.setOnAction(e -> navigate(1));

        navButtons.getChildren().addAll(prevBtn, todayBtn, nextBtn);
        updateNavigationButtons();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label viewLabel = new Label("视图:");
        viewLabel.getStyleClass().add("label-subtitle");

        header.getChildren().addAll(
            titleLabel,
            viewLabel,
            viewModes,
            spacer,
            navButtons
        );

        return header;
    }

    private String getPeriodName() {
        if ("week".equals(currentViewMode)) return "周";
        if ("year".equals(currentViewMode)) return "年";
        return "月";
    }

    private void navigate(int direction) {
        if ("week".equals(currentViewMode)) {
            currentDate = currentDate.plusWeeks(direction);
        } else if ("year".equals(currentViewMode)) {
            currentDate = currentDate.plusYears(direction);
        } else {
            currentDate = currentDate.plusMonths(direction);
        }
        queueRefresh();
    }

    private HBox createLegend() {
        HBox legend = new HBox(10);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(10));
        updateLegend();
        return legend;
    }

    private VBox createDaySchedulePanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("heatmap-day-panel");

        dayScheduleTitle = new Label("所选日期日程");
        dayScheduleTitle.getStyleClass().add("heatmap-day-title");

        ScrollPane dayScheduleScrollPane = new ScrollPane();
        dayScheduleScrollPane.getStyleClass().add("heatmap-day-scroll");
        dayScheduleScrollPane.setFitToWidth(true);
        dayScheduleScrollPane.setPrefViewportHeight(180);
        dayScheduleScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        dayScheduleCardsBox = new VBox(10);
        dayScheduleCardsBox.getStyleClass().add("heatmap-day-cards");

        completedDropZone = new HBox();
        completedDropZone.setAlignment(Pos.CENTER_LEFT);
        completedDropZone.getStyleClass().add("heatmap-completed-zone");
        Label completedDropLabel = new Label("已完成归档区");
        completedDropLabel.getStyleClass().add("heatmap-completed-zone-label");
        completedDropZone.getChildren().add(completedDropLabel);

        dayScheduleScrollPane.setContent(dayScheduleCardsBox);
        panel.getChildren().addAll(dayScheduleTitle, dayScheduleScrollPane, completedDropZone);
        return panel;
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void refresh() {
        queueRefresh();
    }

    private void queueRefresh() {
        if (redrawQueued) {
            return;
        }
        redrawQueued = true;
        Platform.runLater(() -> {
            redrawQueued = false;
            try {
                renderHeatmapSurface();
            } catch (SQLException e) {
                controller.showError("加载热力图失败", e.getMessage());
            }
        });
    }

    private void renderHeatmapSurface() throws SQLException {
        int rows = 0;
        int cols = 0;

        if ("week".equals(currentViewMode)) {
            visibleStartDate = currentDate.with(DayOfWeek.MONDAY);
            visibleEndDate = visibleStartDate.plusDays(6);
            rows = 1;
            cols = 7;
        } else if ("year".equals(currentViewMode)) {
            visibleStartDate = currentDate.withDayOfYear(1);
            visibleEndDate = visibleStartDate.plusYears(1).minusDays(1);
        } else {
            visibleStartDate = currentDate.withDayOfMonth(1);
            visibleEndDate = visibleStartDate.plusMonths(1).minusDays(1);
            rows = calculateMonthRowCount(visibleStartDate, visibleEndDate);
            cols = 7;
        }

        loadedSchedules.clear();
        loadedSchedules.addAll(controller.applyPendingCompletionMutations(scheduleDAO.getAllSchedules()));
        renderLoadedSchedules(rows, cols);
    }

    private void renderLoadedSchedules(int rows, int cols) {
        if (visibleStartDate == null || visibleEndDate == null) {
            return;
        }

        updateNavigationButtons();
        updateHeatmapLegend();
        schedulesByDate = buildSchedulesByDate(loadedSchedules, visibleStartDate, visibleEndDate);
        ensureSelectedDate(visibleStartDate, visibleEndDate);

        Map<LocalDate, Integer> stats = buildDailyCompletionStats(loadedSchedules, visibleStartDate, visibleEndDate);
        int totalCompleted = stats.values().stream().mapToInt(Integer::intValue).sum();
        int activeDays = (int) stats.values().stream().filter(value -> value > 0).count();
        statsLabel.setText(String.format(
            "%s：完成 %d 项，活跃 %d 天",
            getStatsPeriodLabel(visibleStartDate, visibleEndDate),
            totalCompleted,
            activeDays
        ));

        if ("year".equals(currentViewMode)) {
            renderYearOverview(visibleStartDate, stats);
        } else {
            showStandardGridSurface();
            double cellSize = calculateAdaptiveCellSize(cols, rows);
            if ("week".equals(currentViewMode)) {
                renderWeekHeatmap(visibleStartDate, stats, cellSize);
            } else {
                renderMonthHeatmap(visibleStartDate, visibleEndDate, stats, cellSize);
            }
        }

        updateDaySchedulePanelContent();
    }

    private void showStandardGridSurface() {
        heatmapGrid.setManaged(true);
        heatmapGrid.setVisible(true);
        yearMonthsGrid.setManaged(false);
        yearMonthsGrid.setVisible(false);
        yearMonthsGrid.getChildren().clear();
    }

    private void showYearOverviewSurface() {
        heatmapGrid.setManaged(false);
        heatmapGrid.setVisible(false);
        heatmapGrid.getChildren().clear();
        yearMonthsGrid.setManaged(true);
        yearMonthsGrid.setVisible(true);
    }

    private void renderWeekHeatmap(LocalDate startDate, Map<LocalDate, Integer> stats, double cellSize) {
        heatmapGrid.getChildren().clear();
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        for (int index = 0; index < 7; index++) {
            LocalDate date = startDate.plusDays(index);
            Label dayLabel = new Label(dayNames[index]);
            dayLabel.getStyleClass().addAll("label-hint", "heatmap-grid-label");
            heatmapGrid.add(dayLabel, index, 0);

            StackPane cell = buildHeatmapCell(date, stats.getOrDefault(date, 0), cellSize, true, HeatmapCellVariant.STANDARD);
            heatmapGrid.add(cell, index, 1);

            Label dateLabel = new Label(date.format(DateTimeFormatter.ofPattern("MM/dd")));
            dateLabel.getStyleClass().addAll("label-hint", "heatmap-grid-label");
            heatmapGrid.add(dateLabel, index, 2);
        }
    }

    private void renderMonthHeatmap(LocalDate startDate, LocalDate endDate, Map<LocalDate, Integer> stats, double cellSize) {
        heatmapGrid.getChildren().clear();

        String[] dayNames = {"日", "一", "二", "三", "四", "五", "六"};
        for (int index = 0; index < dayNames.length; index++) {
            Label label = new Label(dayNames[index]);
            label.getStyleClass().addAll("label-hint", "heatmap-grid-label");
            heatmapGrid.add(label, index, 0);
        }

        populateMonthGridCells(heatmapGrid, startDate, endDate, stats, cellSize, HeatmapCellVariant.STANDARD, 1);
    }

    private void populateMonthGridCells(
        GridPane targetGrid,
        LocalDate monthStart,
        LocalDate monthEnd,
        Map<LocalDate, Integer> stats,
        double cellSize,
        HeatmapCellVariant variant,
        int rowOffset
    ) {
        LocalDate current = calculateMonthGridStart(monthStart);
        LocalDate gridEnd = calculateMonthGridEnd(monthEnd);
        int row = rowOffset;

        while (!current.isAfter(gridEnd)) {
            for (int col = 0; col < 7; col++) {
                boolean active = current.getMonth() == monthStart.getMonth();
                int count = active ? stats.getOrDefault(current, 0) : 0;
                StackPane cell = buildHeatmapCell(current, count, cellSize, active, variant);
                targetGrid.add(cell, col, row);
                current = current.plusDays(1);
            }
            row++;
        }
    }

    private void renderYearOverview(LocalDate startDate, Map<LocalDate, Integer> stats) {
        showYearOverviewSurface();
        yearMonthsGrid.getChildren().clear();

        double availableWidth = Math.max(resolveViewportWidth() - 8.0, 320.0);
        int columns = determineYearMonthColumns(availableWidth);
        double cardWidth = calculateYearMonthCardWidth(availableWidth, columns);

        for (int month = 1; month <= 12; month++) {
            LocalDate monthStart = startDate.withMonth(month).withDayOfMonth(1);
            VBox monthCard = buildYearMonthCard(monthStart, stats, cardWidth);
            int columnIndex = (month - 1) % columns;
            int rowIndex = (month - 1) / columns;
            yearMonthsGrid.add(monthCard, columnIndex, rowIndex);
        }
    }

    private VBox buildYearMonthCard(LocalDate monthStart, Map<LocalDate, Integer> stats, double cardWidth) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.TOP_LEFT);
        card.setMinWidth(cardWidth);
        card.setPrefWidth(cardWidth);
        card.setMaxWidth(cardWidth);
        card.getStyleClass().add("heatmap-year-card");

        Label monthLabel = new Label(monthStart.format(DateTimeFormatter.ofPattern("M月")));
        monthLabel.getStyleClass().add("heatmap-year-card-title");

        GridPane monthGrid = new GridPane();
        monthGrid.setHgap(3);
        monthGrid.setVgap(3);
        monthGrid.setAlignment(Pos.TOP_CENTER);
        monthGrid.getStyleClass().add("heatmap-year-card-grid");

        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        int monthRows = calculateMonthRowCount(monthStart, monthEnd);
        double cellSize = calculateYearMonthCardCellSize(cardWidth, monthRows);
        populateMonthGridCells(monthGrid, monthStart, monthEnd, stats, cellSize, HeatmapCellVariant.YEAR_CARD, 0);

        card.getChildren().addAll(monthLabel, monthGrid);
        return card;
    }

    private StackPane buildHeatmapCell(
        LocalDate date,
        int count,
        double cellSize,
        boolean activeInCurrentPeriod,
        HeatmapCellVariant variant
    ) {
        double inset = variant == HeatmapCellVariant.YEAR_CARD
            ? Math.max(1.2, cellSize * 0.14)
            : Math.max(2.0, cellSize * 0.12);
        double fillSize = Math.max(variant == HeatmapCellVariant.YEAR_CARD ? 4.0 : 6.0, cellSize - inset * 2.0);

        Rectangle shell = new Rectangle(cellSize, cellSize);
        shell.getStyleClass().add("heatmap-cell-shell");
        if (variant == HeatmapCellVariant.YEAR_CARD) {
            shell.getStyleClass().add("heatmap-cell-shell-compact");
        }
        applyPebbleShape(shell, cellSize, cellSize);

        Rectangle fill = new Rectangle(fillSize, fillSize);
        fill.getStyleClass().add("heatmap-cell-fill");
        applyPebbleShape(fill, fillSize, fillSize);
        updateHeatmapCellColor(fill, getLevelForCount(count));

        StackPane cell = new StackPane(shell, fill);
        cell.setMinSize(cellSize, cellSize);
        cell.setPrefSize(cellSize, cellSize);
        cell.setMaxSize(cellSize, cellSize);
        cell.setAlignment(Pos.CENTER);
        cell.getStyleClass().add("heatmap-cell");
        cell.getStyleClass().add(variant == HeatmapCellVariant.YEAR_CARD ? "heatmap-cell-year" : "heatmap-cell-standard");

        if (!activeInCurrentPeriod) {
            cell.getStyleClass().add("heatmap-cell-inactive");
        }
        if (date != null && date.equals(selectedDate)) {
            cell.getStyleClass().add("heatmap-cell-selected");
        }
        if (activeInCurrentPeriod) {
            cell.setOnMouseClicked(event -> {
                selectedDate = date;
                renderLoadedSchedules(resolveCurrentRows(), resolveCurrentColumns());
            });
        }

        Tooltip.install(cell, new Tooltip(buildTooltipText(date, count, schedulesByDate.getOrDefault(date, List.of()))));
        return cell;
    }

    private void applyPebbleShape(Rectangle rectangle, double width, double height) {
        double pebbleRadius = Math.max(12.0, Math.min(width, height) * 0.86);
        rectangle.setArcWidth(pebbleRadius);
        rectangle.setArcHeight(pebbleRadius);
    }

    private void drawHeatmap() throws SQLException {
        heatmapGrid.getChildren().clear();

        LocalDate startDate;
        LocalDate endDate;
        int rows, cols;

        if ("week".equals(currentViewMode)) {
            startDate = currentDate.with(DayOfWeek.MONDAY);
            endDate = startDate.plusDays(6);
            rows = 1;
            cols = 7;
        } else if ("year".equals(currentViewMode)) {
            startDate = currentDate.withDayOfYear(1);
            endDate = startDate.plusYears(1).minusDays(1);
            rows = 7;
            cols = 53;
        } else {
            startDate = currentDate.withDayOfMonth(1);
            endDate = startDate.plusMonths(1).minusDays(1);
            rows = 6;
            cols = 7;
        }

        visibleStartDate = startDate;
        visibleEndDate = endDate;
        loadedSchedules.clear();
        loadedSchedules.addAll(controller.applyPendingCompletionMutations(scheduleDAO.getAllSchedules()));
        renderHeatmapFromLoadedSchedules(rows, cols);
        return;
    }
        /* Map<LocalDate, Integer> stats = scheduleDAO.getDailyCompletionStats(startDate, endDate);
        List<Schedule> schedules = scheduleDAO.getAllSchedules();
        schedulesByDate = buildSchedulesByDate(schedules, startDate, endDate);
        ensureSelectedDate(startDate, endDate);

        int totalCompleted = stats.values().stream().mapToInt(Integer::intValue).sum();
        int activeDays = (int) stats.values().stream().filter(v -> v > 0).count();
        statsLabel.setText(String.format("%s: 共完成 %d 项任务，活跃天数 %d 天",
            getStatsPeriodLabel(startDate, endDate),
            totalCompleted, activeDays));

        double cellSize = calculateCellSize(cols, rows);

        // 绘制热力图
        if ("week".equals(currentViewMode)) {
            drawWeekView(startDate, stats, cellSize);
        } else if ("year".equals(currentViewMode)) {
            drawYearView(startDate, stats, cellSize);
        } else {
            drawMonthView(startDate, endDate, stats, cellSize);
        }

        updateDaySchedulePanel();
    }

    */

    private void renderHeatmapFromLoadedSchedules(int rows, int cols) {
        if (visibleStartDate == null || visibleEndDate == null) {
            return;
        }

        updateNavigationButtons();
        updateLegend();
        schedulesByDate = buildSchedulesByDate(loadedSchedules, visibleStartDate, visibleEndDate);
        ensureSelectedDate(visibleStartDate, visibleEndDate);

        Map<LocalDate, Integer> stats = buildDailyCompletionStats(loadedSchedules, visibleStartDate, visibleEndDate);
        int totalCompleted = stats.values().stream().mapToInt(Integer::intValue).sum();
        int activeDays = (int) stats.values().stream().filter(v -> v > 0).count();
        statsLabel.setText(String.format(
            "%s: completed %d items across %d active days",
            getStatsPeriodLabel(visibleStartDate, visibleEndDate),
            totalCompleted,
            activeDays
        )); /*
        statsLabel.setText(String.format("%s: 鍏卞畬鎴?%d 椤逛换鍔★紝娲昏穬澶╂暟 %d 澶?,
            getStatsPeriodLabel(visibleStartDate, visibleEndDate),
            totalCompleted,
            activeDays)); */

        double cellSize = calculateCellSize(cols, rows);
        if ("week".equals(currentViewMode)) {
            drawWeekView(visibleStartDate, stats, cellSize);
        } else if ("year".equals(currentViewMode)) {
            drawYearView(visibleStartDate, stats, cellSize);
        } else {
            drawMonthView(visibleStartDate, visibleEndDate, stats, cellSize);
        }

        updateDaySchedulePanel();
    }

    private void drawWeekView(LocalDate startDate, Map<LocalDate, Integer> stats, double cellSize) {
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            int count = stats.getOrDefault(date, 0);

            // 星期标签
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.getStyleClass().add("label-hint");
            heatmapGrid.add(dayLabel, i, 0);

            // 日期单元格
            StackPane cell = createHeatmapCell(date, count, cellSize, true);
            heatmapGrid.add(cell, i, 1);

            // 日期数字
            Label dateLabel = new Label(date.format(DateTimeFormatter.ofPattern("MM/dd")));
            dateLabel.getStyleClass().add("label-hint");
            heatmapGrid.add(dateLabel, i, 2);
        }
    }

    private void drawMonthView(LocalDate startDate, LocalDate endDate, Map<LocalDate, Integer> stats, double cellSize) {
        // 添加星期标题
        String[] dayNames = {"日", "一", "二", "三", "四", "五", "六"};
        for (int i = 0; i < 7; i++) {
            Label label = new Label(dayNames[i]);
            label.getStyleClass().add("label-hint");
            heatmapGrid.add(label, i, 0);
        }

        // 计算月初是星期几
        int firstDayOfWeek = startDate.getDayOfWeek().getValue() % 7;

        LocalDate current = startDate.minusDays(firstDayOfWeek);
        int row = 1;

        while (current.isBefore(endDate.plusDays(7))) {
            for (int col = 0; col < 7; col++) {
                int count = 0;
                if (!current.isBefore(startDate) && !current.isAfter(endDate)) {
                    count = stats.getOrDefault(current, 0);
                }

                boolean inCurrentMonth = current.getMonth() == startDate.getMonth();
                StackPane cell = createHeatmapCell(current, count, cellSize, inCurrentMonth);

                heatmapGrid.add(cell, col, row);
                current = current.plusDays(1);
            }
            row++;
        }
    }

    private void drawYearView(LocalDate startDate, Map<LocalDate, Integer> stats, double cellSize) {
        // 添加月份标签
        for (int month = 1; month <= 12; month++) {
            Label label = new Label(month + "月");
            label.getStyleClass().add("label-hint");
            label.setRotate(-90);
            heatmapGrid.add(label, 0, month);
        }

        // 绘制每周数据
        LocalDate current = startDate;
        int weekCol = 1;

        while (current.getYear() == startDate.getYear()) {
            for (int day = 1; day <= 7; day++) {
                DayOfWeek dow = DayOfWeek.of(day % 7 == 0 ? 7 : day);

                if (current.getDayOfWeek() == dow && current.getYear() == startDate.getYear()) {
                    int count = stats.getOrDefault(current, 0);
                    StackPane cell = createHeatmapCell(current, count, cellSize, true);
                    heatmapGrid.add(cell, weekCol, day);
                    current = current.plusDays(1);
                }
            }
            weekCol++;
        }
    }

    private StackPane createHeatmapCell(LocalDate date, int count, double cellSize, boolean activeInCurrentPeriod) {
        Rectangle rect = new Rectangle(cellSize, cellSize);
        rect.getStyleClass().add("heatmap-cell");
        updateHeatmapCellColor(rect, getLevelForCount(count));

        StackPane cell = new StackPane(rect);
        cell.setPadding(new Insets(2));
        if (!activeInCurrentPeriod) {
            cell.getStyleClass().add("heatmap-cell-inactive");
        }
        if (date != null && date.equals(selectedDate)) {
            cell.getStyleClass().add("heatmap-cell-selected");
        }
        if (activeInCurrentPeriod) {
            cell.setOnMouseClicked(e -> {
                selectedDate = date;
                updateDaySchedulePanel();
                queueRefresh();
            });
        }

        List<Schedule> schedules = schedulesByDate.getOrDefault(date, List.of());
        String tooltipText = buildTooltipText(date, count, schedules);
        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(cell, tooltip);

        return cell;
    }

    private String buildTooltipText(LocalDate date, int completedCount, List<Schedule> schedules) {
        StringBuilder builder = new StringBuilder();
        builder.append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        builder.append("\n完成: ").append(completedCount).append(" 项");
        builder.append("\n日程: ").append(schedules.size()).append(" 项");

        int previewCount = Math.min(3, schedules.size());
        for (int i = 0; i < previewCount; i++) {
            builder.append("\n• ").append(schedules.get(i).getName());
        }
        if (schedules.size() > previewCount) {
            builder.append("\n…");
        }
        return builder.toString();
    }

    private void updateHeatmapLegend() {
        if (legend == null) {
            return;
        }

        legend.getChildren().clear();

        Label title = new Label("完成强度");
        title.getStyleClass().addAll("label-hint", "heatmap-legend-title");
        legend.getChildren().add(title);

        String[] labels = {"0", "1-2", "3-5", "6-8", "9+"};
        for (int level = 0; level < labels.length; level++) {
            HBox item = new HBox(4);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("heatmap-legend-item");

            item.getChildren().addAll(buildLegendSwatch(level), new Label(labels[level]));
            ((Label) item.getChildren().get(1)).getStyleClass().addAll("label-hint", "heatmap-legend-label");
            legend.getChildren().add(item);
        }
    }

    private StackPane buildLegendSwatch(int level) {
        Rectangle shell = new Rectangle(18, 18);
        shell.getStyleClass().addAll("heatmap-cell-shell", "heatmap-legend-shell");
        applyPebbleShape(shell, 18, 18);

        Rectangle fill = new Rectangle(12, 12);
        fill.getStyleClass().add("heatmap-cell-fill");
        applyPebbleShape(fill, 12, 12);
        updateHeatmapCellColor(fill, level);

        StackPane swatch = new StackPane(shell, fill);
        swatch.setMinSize(18, 18);
        swatch.setPrefSize(18, 18);
        swatch.setMaxSize(18, 18);
        swatch.getStyleClass().add("heatmap-legend-swatch");
        return swatch;
    }

    private void updateDaySchedulePanelContent() {
        if (dayScheduleTitle == null || dayScheduleCountLabel == null || currentDayScheduleCardsContainer == null) {
            return;
        }

        currentDayScheduleCardsContainer.getChildren().clear();
        removeCompletedDropProxy();

        if (selectedDate == null) {
            dayScheduleTitle.setText("选择日期");
            dayScheduleCountLabel.setText("0 项");
            return;
        }

        List<Schedule> schedules = schedulesByDate.getOrDefault(selectedDate, List.of());
        dayScheduleTitle.setText(selectedDate.format(DateTimeFormatter.ofPattern("M月d日")));
        dayScheduleCountLabel.setText(schedules.size() + " 项");

        if (schedules.isEmpty()) {
            Label emptyLabel = new Label("该日期暂无日程");
            emptyLabel.getStyleClass().add("heatmap-day-empty");
            currentDayScheduleCardsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Schedule schedule : schedules) {
            currentDayScheduleCardsContainer.getChildren().add(buildDayScheduleCard(schedule));
        }
    }

    private StackPane buildDayScheduleCard(Schedule schedule) {
        VBox card = new VBox(panelLayoutMode == PanelLayoutMode.SIDE ? 8 : 6);
        card.getStyleClass().addAll(
            "heatmap-day-card",
            panelLayoutMode == PanelLayoutMode.SIDE ? "heatmap-day-card-side" : "heatmap-day-card-bottom"
        );
        card.setMaxWidth(Double.MAX_VALUE);
        ScheduleCardStyleSupport.applyCardPresentation(
            card,
            schedule,
            controller.getCurrentScheduleCardStyle(),
            "schedule-card-role-heatmap",
            "heatmap-day-card-surface"
        );
        if (controller.isScheduleSelected(schedule)) {
            card.getStyleClass().addAll("heatmap-day-card-selected", "schedule-card-state-selected");
        }

        StackPane cardShell = new StackPane(card);
        cardShell.getStyleClass().add("schedule-card-motion-shell");
        cardShell.setPickOnBounds(false);

        StackPane cardMotionHost = new StackPane(cardShell);
        cardMotionHost.getStyleClass().add("schedule-card-motion-host");
        cardMotionHost.setPickOnBounds(false);

        if (panelLayoutMode == PanelLayoutMode.BOTTOM) {
            double cardWidth = resolveDayCardWidth();
            card.setMinWidth(cardWidth);
            card.setPrefWidth(cardWidth);
            card.setMaxWidth(cardWidth);
            cardMotionHost.setMinWidth(cardWidth);
            cardMotionHost.setPrefWidth(cardWidth);
            cardMotionHost.setMaxWidth(cardWidth);
        } else {
            cardMotionHost.setMaxWidth(Double.MAX_VALUE);
        }

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(cardMotionHost.widthProperty());
        clip.heightProperty().bind(cardMotionHost.heightProperty());
        cardMotionHost.setClip(clip);
        ScheduleCollapsePopAnimator.bindMotionHandle(
            cardMotionHost,
            cardShell,
            () -> {
                cardMotionHost.requestLayout();
                getActiveDayCardsContainer().requestLayout();
                if (daySchedulePanel != null) {
                    daySchedulePanel.requestLayout();
                }
            }
        );
        ScheduleReflowAnimator.bindCard(cardMotionHost, schedule);

        Rectangle colorMark = new Rectangle(12, 12);
        colorMark.getStyleClass().add("schedule-color-mark");
        applyPebbleShape(colorMark, 12, 12);

        String color = schedule.getColor();
        if (color != null && !color.isBlank()) {
            colorMark.setStyle("-fx-fill: " + color + ";");
        } else {
            colorMark.getStyleClass().add("mark-default");
        }

        ScheduleStatusControl[] statusControlRef = new ScheduleStatusControl[1];
        ScheduleStatusControl statusControl = new ScheduleStatusControl(
            schedule.isCompleted(),
            ScheduleStatusControl.SizePreset.HEATMAP,
            "schedule-status-role-heatmap",
            targetCompleted -> handleStatusToggle(cardMotionHost, card, schedule, statusControlRef[0], targetCompleted)
        );
        statusControlRef[0] = statusControl;

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("heatmap-day-card-header");

        VBox textGroup = new VBox(3);
        Label titleLabel = new Label(schedule.getName());
        titleLabel.getStyleClass().addAll("schedule-title", "schedule-card-title-text");
        titleLabel.setWrapText(true);

        Label dateLabel = new Label(getScheduleDateText(schedule));
        dateLabel.getStyleClass().addAll("schedule-date", "schedule-card-subtitle-text");
        dateLabel.setWrapText(true);
        textGroup.getChildren().addAll(titleLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label priorityLabel = new Label(schedule.getPriority());
        priorityLabel.getStyleClass().add("priority-" + getPriorityClass(schedule.getPriority()));

        Label categoryLabel = new Label(schedule.getCategory());
        categoryLabel.getStyleClass().add("category-tag");

        header.getChildren().addAll(statusControl, colorMark, textGroup, spacer, priorityLabel, categoryLabel);

        Label descriptionLabel = new Label(getScheduleDescriptionText(schedule));
        descriptionLabel.getStyleClass().addAll("schedule-description", "schedule-card-body-text");
        descriptionLabel.setWrapText(true);

        Label statusLabel = new Label(getScheduleStatusText(schedule));
        statusLabel.getStyleClass().addAll("label-hint", "schedule-card-subtitle-text", "heatmap-day-card-status");

        card.getChildren().addAll(header, descriptionLabel, statusLabel);

        card.setOnMouseClicked(event -> {
            controller.showScheduleDetails(schedule);
            updateDaySchedulePanelContent();
            if (event.getClickCount() == 2) {
                controller.openEditScheduleDialog(schedule);
            }
        });

        return cardMotionHost;
    }

    private Pane getActiveDayCardsContainer() {
        return currentDayScheduleCardsContainer != null ? currentDayScheduleCardsContainer : dayScheduleCardsColumn;
    }

    private double resolveDayCardWidth() {
        double availableWidth = root != null ? root.getWidth() : 0.0;
        if (availableWidth <= 0 && scrollPane != null) {
            availableWidth = scrollPane.getViewportBounds().getWidth();
        }
        return clamp(availableWidth * 0.24, 236.0, 278.0);
    }

    private void updateNavigationButtons() {
        String periodName = getPeriodName();
        if (prevBtn != null) {
            prevBtn.setText("");
            prevBtn.setTooltip(new Tooltip("上一" + periodName));
        }
        if (nextBtn != null) {
            nextBtn.setText("");
            nextBtn.setTooltip(new Tooltip("下一" + periodName));
        }
    }

    private void updateLegend() {
        if (legend == null) {
            return;
        }

        legend.getChildren().clear();

        Label title = new Label("完成数量:");
        title.getStyleClass().add("label-hint");
        legend.getChildren().add(title);

        String[] labels = {"0", "1-2", "3-5", "6-8", "9+"};

        for (int i = 0; i < labels.length; i++) {
            Rectangle rect = new Rectangle(15, 15);
            rect.getStyleClass().add("heatmap-cell");
            updateHeatmapCellColor(rect, i);

            Label label = new Label(labels[i]);
            label.getStyleClass().add("label-hint");

            legend.getChildren().addAll(rect, label);
        }
    }

    private void ensureSelectedDate(LocalDate startDate, LocalDate endDate) {
        if (selectedDate != null && !selectedDate.isBefore(startDate) && !selectedDate.isAfter(endDate)) {
            return;
        }

        LocalDate firstScheduledDate = schedulesByDate.keySet().stream()
            .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
            .findFirst()
            .orElse(null);

        if (firstScheduledDate != null) {
            selectedDate = firstScheduledDate;
            return;
        }

        if (currentDate.isBefore(startDate)) {
            selectedDate = startDate;
        } else if (currentDate.isAfter(endDate)) {
            selectedDate = endDate;
        } else {
            selectedDate = currentDate;
        }
    }

    private void updateDaySchedulePanel() {
        if (dayScheduleTitle == null || dayScheduleCardsBox == null || selectedDate == null) {
            return;
        }

        dayScheduleTitle.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) + " 的日程");
        removeCompletedDropProxy();
        dayScheduleCardsBox.getChildren().clear();

        List<Schedule> schedules = schedulesByDate.getOrDefault(selectedDate, List.of());
        if (schedules.isEmpty()) {
            Label emptyLabel = new Label("该日期暂无日程");
            emptyLabel.getStyleClass().add("heatmap-day-empty");
            dayScheduleCardsBox.getChildren().add(emptyLabel);
            return;
        }

        for (Schedule schedule : schedules) {
            dayScheduleCardsBox.getChildren().add(createDayScheduleCard(schedule));
        }
    }

    private StackPane createDayScheduleCard(Schedule schedule) {
        VBox card = new VBox(8);
        card.getStyleClass().add("heatmap-day-card");
        ScheduleCardStyleSupport.applyCardPresentation(
            card,
            schedule,
            controller.getCurrentScheduleCardStyle(),
            "schedule-card-role-heatmap"
        );
        if (controller.isScheduleSelected(schedule)) {
            card.getStyleClass().addAll("heatmap-day-card-selected", "schedule-card-state-selected");
        }

        StackPane cardShell = new StackPane(card);
        cardShell.getStyleClass().add("schedule-card-motion-shell");
        cardShell.setPickOnBounds(false);

        StackPane cardMotionHost = new StackPane(cardShell);
        cardMotionHost.getStyleClass().add("schedule-card-motion-host");
        cardMotionHost.setPickOnBounds(false);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(cardMotionHost.widthProperty());
        clip.heightProperty().bind(cardMotionHost.heightProperty());
        cardMotionHost.setClip(clip);
        ScheduleCollapsePopAnimator.bindMotionHandle(
            cardMotionHost,
            cardShell,
            () -> {
                cardMotionHost.requestLayout();
                dayScheduleCardsBox.requestLayout();
                if (daySchedulePanel != null) {
                    daySchedulePanel.requestLayout();
                }
            }
        );
        ScheduleReflowAnimator.bindCard(cardMotionHost, schedule);

        Rectangle colorMark = new Rectangle(10, 10);
        colorMark.setArcWidth(10);
        colorMark.setArcHeight(10);
        colorMark.getStyleClass().add("schedule-color-mark");

        String color = schedule.getColor();
        if (color != null && !color.isBlank()) {
            colorMark.setStyle("-fx-fill: " + color + ";");
        } else {
            colorMark.getStyleClass().add("mark-default");
        }

        ScheduleStatusControl[] statusControlRef = new ScheduleStatusControl[1];
        ScheduleStatusControl statusControl = new ScheduleStatusControl(
            schedule.isCompleted(),
            ScheduleStatusControl.SizePreset.HEATMAP,
            "schedule-status-role-heatmap",
            targetCompleted -> handleStatusToggle(cardMotionHost, card, schedule, statusControlRef[0], targetCompleted)
        );
        statusControlRef[0] = statusControl;

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox textGroup = new VBox(4);
        Label titleLabel = new Label(schedule.getName());
        titleLabel.getStyleClass().addAll("schedule-title", "schedule-card-title-text");
        Label dateLabel = new Label(getScheduleDateText(schedule));
        dateLabel.getStyleClass().addAll("schedule-date", "schedule-card-subtitle-text");
        textGroup.getChildren().addAll(titleLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label priorityLabel = new Label(schedule.getPriority());
        priorityLabel.getStyleClass().add("priority-" + getPriorityClass(schedule.getPriority()));

        Label categoryLabel = new Label(schedule.getCategory());
        categoryLabel.getStyleClass().add("category-tag");

        header.getChildren().addAll(statusControl, colorMark, textGroup, spacer, priorityLabel, categoryLabel);

        Label descriptionLabel = new Label(getScheduleDescriptionText(schedule));
        descriptionLabel.getStyleClass().addAll("schedule-description", "schedule-card-body-text");
        descriptionLabel.setWrapText(true);

        Label statusLabel = new Label(getScheduleStatusText(schedule));
        statusLabel.getStyleClass().addAll("label-hint", "schedule-card-subtitle-text");

        card.getChildren().addAll(header, descriptionLabel, statusLabel);

        card.setOnMouseClicked(e -> {
            controller.showScheduleDetails(schedule);
            updateDaySchedulePanel();
            if (e.getClickCount() == 2) {
                controller.openEditScheduleDialog(schedule);
            }
        });

        return cardMotionHost;
    }

    private boolean handleStatusToggle(
        Node cardNode,
        Node snapshotNode,
        Schedule schedule,
        ScheduleStatusControl control,
        boolean targetCompleted
    ) {
        if (!targetCompleted) {
            return controller.updateScheduleCompletion(schedule, false);
        }

        ScheduleCompletionCoordinator.PendingCompletion pendingCompletion =
            controller.prepareScheduleCompletion(schedule, true);
        if (pendingCompletion == null) {
            return false;
        }

        Map<Integer, Bounds> beforeBounds =
            ScheduleReflowAnimator.captureVisibleCardBounds(getActiveDayCardsContainer(), schedule.getId());

        ScheduleCollapsePopAnimator.playCollapseSource(
            ScheduleCollapsePopAnimator.resolveMotionHandle(cardNode),
            pendingCompletion::commit,
            () -> handleCommittedHeatmapCompletion(beforeBounds),
            () -> {
                if (control != null) {
                    control.syncCompleted(false);
                }
            },
            null
        );
        return true;
    }

    /* private ScheduleCollapsePopAnimator.MotionHandle createCompletedProxyHandle(WritableImage completedSnapshot) {
        if (completedDropZone == null) {
            return null;
        }

        removeCompletedDropProxy();

        StackPane proxyShell = new StackPane();
        proxyShell.getStyleClass().add(COMPLETED_PROXY_STYLE);
        proxyShell.setPickOnBounds(false);

        if (completedSnapshot != null) {
            ImageView imageView = new ImageView(completedSnapshot);
            imageView.setPreserveRatio(true);
            imageView.setFitHeight(Math.min(60.0, completedSnapshot.getHeight()));
            imageView.setSmooth(true);
            proxyShell.getChildren().add(imageView);
        } else {
            Label fallbackLabel = new Label("已完成");
            fallbackLabel.getStyleClass().add("heatmap-completed-zone-label");
            proxyShell.getChildren().add(fallbackLabel);
        }

        StackPane proxyHost = new StackPane(proxyShell);
        proxyHost.getStyleClass().addAll("schedule-card-motion-host", COMPLETED_PROXY_STYLE);
        proxyHost.setPickOnBounds(false);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(proxyHost.widthProperty());
        clip.heightProperty().bind(proxyHost.heightProperty());
        proxyHost.setClip(clip);

        completedDropZone.getChildren().add(proxyHost);

        ScheduleCollapsePopAnimator.MotionHandle handle = ScheduleCollapsePopAnimator.bindMotionHandle(
            proxyHost,
            proxyShell,
            () -> {
                proxyHost.requestLayout();
                completedDropZone.requestLayout();
            }
        );
        ScheduleCollapsePopAnimator.prepareTargetPopState(handle);
        proxyHost.getProperties().put(COMPLETED_PROXY_STYLE, Boolean.TRUE);
        return handle;
    }

    private void removeCompletedDropProxy() {
        if (completedDropZone == null) {
            return;
        }
        completedDropZone.getChildren().removeIf(node -> Boolean.TRUE.equals(node.getProperties().get(COMPLETED_PROXY_STYLE)));
    }

    }*/

    private void handleCommittedHeatmapCompletion(Map<Integer, Bounds> beforeBounds) {
        ScheduleReflowAnimator.playVerticalReflow(getActiveDayCardsContainer(), beforeBounds, null);
        ScheduleCollapsePopAnimator.MotionHandle proxyHandle = createCompletedProxyHandle();
        if (proxyHandle != null) {
            ScheduleCollapsePopAnimator.prepareTargetPopState(proxyHandle);
            ScheduleCollapsePopAnimator.playPreparedTargetPop(proxyHandle, null);
            return;
        }
        if (completedDropZone != null) {
            ScheduleReflowAnimator.playTargetPulse(
                completedDropZone,
                CollapsePopKeyframePreset.targetPopDuration(),
                null
            );
        }
    }

    private ScheduleCollapsePopAnimator.MotionHandle createCompletedProxyHandle() {
        if (completedDropZone == null) {
            return null;
        }

        if (completedProxyHost == null || completedProxyShell == null || completedProxyHandle == null) {
            completedProxyShell = new StackPane();
            completedProxyShell.getStyleClass().add(COMPLETED_PROXY_STYLE);
            completedProxyShell.setPickOnBounds(false);

            Label proxyLabel = new Label("已完成");
            proxyLabel.getStyleClass().add("heatmap-completed-zone-label");
            completedProxyShell.getChildren().add(proxyLabel);

            completedProxyHost = new StackPane(completedProxyShell);
            completedProxyHost.getStyleClass().addAll(
                "schedule-card-motion-host",
                COMPLETED_PROXY_STYLE,
                COMPLETED_PROXY_HOST_STYLE
            );
            completedProxyHost.setPickOnBounds(false);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(completedProxyHost.widthProperty());
            clip.heightProperty().bind(completedProxyHost.heightProperty());
            completedProxyHost.setClip(clip);

            completedProxyHandle = ScheduleCollapsePopAnimator.bindMotionHandle(
                completedProxyHost,
                completedProxyShell,
                () -> {
                    completedProxyHost.requestLayout();
                    completedDropZone.requestLayout();
                }
            );
        }

        if (!completedDropZone.getChildren().contains(completedProxyHost)) {
            completedDropZone.getChildren().add(completedProxyHost);
        }
        completedProxyHost.setManaged(true);
        completedProxyHost.setVisible(true);
        return completedProxyHandle;
    }

    private void removeCompletedDropProxy() {
        if (completedDropZone == null || completedProxyHost == null) {
            return;
        }
        completedDropZone.getChildren().remove(completedProxyHost);
    }

    private String getStatsPeriodLabel(LocalDate startDate, LocalDate endDate) {
        if ("week".equals(currentViewMode)) {
            return startDate.format(DateTimeFormatter.ofPattern("MM月dd日"))
                + " - "
                + endDate.format(DateTimeFormatter.ofPattern("MM月dd日"));
        }
        if ("year".equals(currentViewMode)) {
            return currentDate.format(DateTimeFormatter.ofPattern("yyyy年"));
        }
        return currentDate.format(DateTimeFormatter.ofPattern("yyyy年MM月"));
    }

    private double calculateAdaptiveCellSize(int cols, int rows) {
        double viewportWidth = resolveViewportWidth();
        double viewportHeight = resolveViewportHeight();
        double horizontalPadding = heatmapGrid.getPadding().getLeft() + heatmapGrid.getPadding().getRight();
        double verticalPadding = heatmapGrid.getPadding().getTop() + heatmapGrid.getPadding().getBottom();
        double reservedHeight = "week".equals(currentViewMode) ? 78.0 : 24.0;

        double availableWidth = Math.max(viewportWidth - horizontalPadding - 8.0, 120.0);
        double availableHeight = Math.max(viewportHeight - verticalPadding - reservedHeight, 120.0);

        double widthBasedSize = (availableWidth - Math.max(0, cols - 1) * GRID_GAP) / cols;
        double heightBasedSize = (availableHeight - Math.max(0, rows - 1) * GRID_GAP) / rows;
        return clampGridCellSize(currentViewMode, panelLayoutMode == PanelLayoutMode.SIDE, Math.min(widthBasedSize, heightBasedSize));
    }

    private double resolveViewportWidth() {
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0) {
            viewportWidth = scrollPane.getWidth();
        }
        if (viewportWidth <= 0 && heatmapPane != null) {
            viewportWidth = heatmapPane.getWidth();
        }
        return Math.max(viewportWidth, 120.0);
    }

    private double resolveViewportHeight() {
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        if (viewportHeight <= 0) {
            viewportHeight = scrollPane.getHeight();
        }
        if (viewportHeight <= 0 && heatmapPane != null) {
            viewportHeight = heatmapPane.getHeight();
        }
        return Math.max(viewportHeight, 120.0);
    }

    static int determineYearMonthColumns(double availableWidth) {
        if (availableWidth >= 1200.0) {
            return 4;
        }
        if (availableWidth >= 900.0) {
            return 3;
        }
        if (availableWidth >= 620.0) {
            return 2;
        }
        return 1;
    }

    static double determineDayPanelPreferredExtent(boolean sideLayout) {
        return sideLayout ? DAY_PANEL_SIDE_WIDTH : DAY_PANEL_BOTTOM_HEIGHT;
    }

    static double clampGridCellSize(String viewMode, boolean sideLayout, double proposedSize) {
        if ("week".equals(viewMode)) {
            return clamp(proposedSize, sideLayout ? 44.0 : 36.0, 118.0);
        }
        if ("year".equals(viewMode)) {
            return clamp(proposedSize, 12.0, 22.0);
        }
        return clamp(proposedSize, sideLayout ? 28.0 : 22.0, sideLayout ? 106.0 : 92.0);
    }

    private double calculateYearMonthCardWidth(double availableWidth, int columns) {
        double rawWidth = (availableWidth - Math.max(0, columns - 1) * YEAR_GRID_GAP) / columns;
        return clamp(rawWidth, 208.0, 420.0);
    }

    private double calculateYearMonthCardCellSize(double cardWidth, int monthRows) {
        double gridWidth = Math.max(cardWidth - 24.0, 120.0);
        double widthBased = (gridWidth - 6.0 * 3.0) / 7.0;
        double heightBased = (monthRows <= 0 ? widthBased : (gridWidth * 0.8) / Math.max(monthRows, 1));
        return clampGridCellSize("year", true, Math.min(widthBased, heightBased));
    }

    private static LocalDate calculateMonthGridStart(LocalDate monthStart) {
        int leadingDays = monthStart.getDayOfWeek().getValue() % 7;
        return monthStart.minusDays(leadingDays);
    }

    private static LocalDate calculateMonthGridEnd(LocalDate monthEnd) {
        int trailingDays = 6 - (monthEnd.getDayOfWeek().getValue() % 7);
        return monthEnd.plusDays(trailingDays);
    }

    private static int calculateMonthRowCount(LocalDate monthStart, LocalDate monthEnd) {
        LocalDate gridStart = calculateMonthGridStart(monthStart);
        LocalDate gridEnd = calculateMonthGridEnd(monthEnd);
        long totalDays = ChronoUnit.DAYS.between(gridStart, gridEnd) + 1L;
        return (int) Math.ceil(totalDays / 7.0);
    }

    private double calculateCellSize(int cols, int rows) {
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();

        if (viewportWidth <= 0) {
            viewportWidth = scrollPane.getWidth();
        }
        if (viewportHeight <= 0) {
            viewportHeight = scrollPane.getHeight();
        }

        double availableWidth = Math.max(viewportWidth - getReservedWidth(), 120);
        double availableHeight = Math.max(viewportHeight - getReservedHeight(), 120);

        double widthBasedSize = (availableWidth - Math.max(0, cols - 1) * GRID_GAP) / cols;
        double heightBasedSize = (availableHeight - Math.max(0, rows - 1) * GRID_GAP) / rows;
        double cellSize = Math.min(widthBasedSize, heightBasedSize);

        if ("week".equals(currentViewMode)) {
            return clamp(cellSize, 28, 90);
        }
        if ("year".equals(currentViewMode)) {
            return clamp(cellSize, 10, 18);
        }
        return clamp(cellSize, 18, 90);
    }

    private double getReservedWidth() {
        double horizontalPadding = heatmapGrid.getPadding().getLeft() + heatmapGrid.getPadding().getRight();
        if ("year".equals(currentViewMode)) {
            return horizontalPadding + 35;
        }
        return horizontalPadding + 20;
    }

    private double getReservedHeight() {
        double verticalPadding = heatmapGrid.getPadding().getTop() + heatmapGrid.getPadding().getBottom();
        if ("week".equals(currentViewMode)) {
            return verticalPadding + 90;
        }
        if ("month".equals(currentViewMode)) {
            return verticalPadding + 40;
        }
        return verticalPadding + 20;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static Map<LocalDate, List<Schedule>> buildSchedulesByDate(List<Schedule> schedules, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<Schedule>> groupedSchedules = new LinkedHashMap<>();
        List<Schedule> sortedSchedules = new ArrayList<>(schedules);
        sortedSchedules.sort(Comparator
            .comparing(HeatmapView::getComparableStartDate)
            .thenComparing(HeatmapView::getComparableEndDate)
            .thenComparing(Schedule::getPriorityValue, Comparator.reverseOrder())
            .thenComparing(Schedule::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

        for (Schedule schedule : sortedSchedules) {
            LocalDate scheduleStart = resolveScheduleStart(schedule);
            LocalDate scheduleEnd = resolveScheduleEnd(schedule);
            if (scheduleStart == null || scheduleEnd == null) {
                continue;
            }

            if (scheduleStart.isAfter(scheduleEnd)) {
                LocalDate temp = scheduleStart;
                scheduleStart = scheduleEnd;
                scheduleEnd = temp;
            }

            LocalDate rangeStart = scheduleStart.isBefore(startDate) ? startDate : scheduleStart;
            LocalDate rangeEnd = scheduleEnd.isAfter(endDate) ? endDate : scheduleEnd;

            if (rangeStart.isAfter(rangeEnd)) {
                continue;
            }

            LocalDate current = rangeStart;
            while (!current.isAfter(rangeEnd)) {
                groupedSchedules.computeIfAbsent(current, key -> new ArrayList<>()).add(schedule);
                current = current.plusDays(1);
            }
        }

        return groupedSchedules;
    }

    static Map<LocalDate, Integer> buildDailyCompletionStats(
        List<Schedule> schedules,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Map<LocalDate, Integer> stats = new LinkedHashMap<>();
        if (schedules == null || startDate == null || endDate == null) {
            return stats;
        }
        for (Schedule schedule : schedules) {
            if (!schedule.isCompleted() || schedule.getUpdatedAt() == null) {
                continue;
            }
            LocalDate completionDate = schedule.getUpdatedAt().toLocalDate();
            if (completionDate.isBefore(startDate) || completionDate.isAfter(endDate)) {
                continue;
            }
            stats.merge(completionDate, 1, Integer::sum);
        }
        return stats;
    }

    static boolean scheduleOccursOnDate(Schedule schedule, LocalDate date) {
        LocalDate startDate = resolveScheduleStart(schedule);
        LocalDate endDate = resolveScheduleEnd(schedule);
        if (startDate == null || endDate == null || date == null) {
            return false;
        }
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private int getLevelForCount(int count) {
        if (count == 0) return 0;
        if (count <= 2) return 1;
        if (count <= 5) return 2;
        if (count <= 8) return 3;
        return 4;
    }

    private void updateHeatmapCellColor(Rectangle rect, int level) {
        rect.getStyleClass().removeAll(
            "level-0", "level-1", "level-2",
            "level-3", "level-4"
        );
        rect.getStyleClass().add("level-" + Math.min(level, 4));
    }

    private String getScheduleDateText(Schedule schedule) {
        LocalDate startDate = schedule.getStartDate();
        LocalDate endDate = schedule.getDueDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");

        if (startDate != null && endDate != null) {
            if (startDate.equals(endDate)) {
                return startDate.format(formatter);
            }
            return startDate.format(formatter) + " - " + endDate.format(formatter);
        }
        if (startDate != null) {
            return "开始于 " + startDate.format(formatter);
        }
        if (endDate != null) {
            return "截止于 " + endDate.format(formatter);
        }
        return "未设置日期";
    }

    private String getScheduleDescriptionText(Schedule schedule) {
        if (schedule.getDescription() == null || schedule.getDescription().isBlank()) {
            return "暂无描述";
        }
        return schedule.getDescription();
    }

    private String getScheduleStatusText(Schedule schedule) {
        if (schedule.isCompleted()) {
            return "状态：已完成";
        }
        if (schedule.isOverdue()) {
            return "状态：已过期";
        }
        if (schedule.isUpcoming()) {
            return "状态：即将到期";
        }
        return "状态：进行中";
    }

    @Override
    public void applyCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null || loadedSchedules.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (Schedule schedule : loadedSchedules) {
            if (!mutation.matches(schedule)) {
                continue;
            }
            mutation.applyTo(schedule);
            changed = true;
        }
        if (changed && visibleStartDate != null && visibleEndDate != null) {
            renderLoadedSchedules(resolveCurrentRows(), resolveCurrentColumns());
        }
    }

    @Override
    public void confirmCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null || loadedSchedules.isEmpty()) {
            return;
        }
        if (visibleStartDate != null && visibleEndDate != null) {
            renderLoadedSchedules(resolveCurrentRows(), resolveCurrentColumns());
        }
    }

    @Override
    public void revertCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null || loadedSchedules.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (Schedule schedule : loadedSchedules) {
            if (!mutation.matches(schedule)) {
                continue;
            }
            mutation.revertOn(schedule);
            changed = true;
        }
        if (changed && visibleStartDate != null && visibleEndDate != null) {
            renderLoadedSchedules(resolveCurrentRows(), resolveCurrentColumns());
        }
    }

    private String getPriorityClass(String priority) {
        if ("高".equals(priority)) {
            return "high";
        }
        if ("低".equals(priority)) {
            return "low";
        }
        return "medium";
    }

    private static LocalDate resolveScheduleStart(Schedule schedule) {
        if (schedule.getStartDate() != null) {
            return schedule.getStartDate();
        }
        return schedule.getDueDate();
    }

    private static LocalDate resolveScheduleEnd(Schedule schedule) {
        if (schedule.getDueDate() != null) {
            return schedule.getDueDate();
        }
        return schedule.getStartDate();
    }

    private static LocalDate getComparableStartDate(Schedule schedule) {
        LocalDate startDate = resolveScheduleStart(schedule);
        return startDate != null ? startDate : LocalDate.MAX;
    }

    private static LocalDate getComparableEndDate(Schedule schedule) {
        LocalDate endDate = resolveScheduleEnd(schedule);
        return endDate != null ? endDate : LocalDate.MAX;
    }

    private int resolveCurrentRows() {
        if ("week".equals(currentViewMode)) {
            return 1;
        }
        if ("year".equals(currentViewMode)) {
            return 0;
        }
        if (visibleStartDate != null && visibleEndDate != null) {
            return calculateMonthRowCount(visibleStartDate, visibleEndDate);
        }
        return 6;
    }

    private int resolveCurrentColumns() {
        if ("year".equals(currentViewMode)) {
            return 0;
        }
        return 7;
    }
}
