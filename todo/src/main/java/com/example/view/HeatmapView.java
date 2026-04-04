package com.example.view;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class HeatmapView implements View, ScheduleCompletionParticipant {
    private static final double GRID_GAP = 3;
    private static final double CELL_CHROME = 4;
    private static final double CALENDAR_LAYOUT_SAFETY = 2;
    private static final double MONTH_HEADER_HEIGHT = 28;
    private static final double WEEK_EXTRA_HEIGHT = 58;
    private static final int YEAR_MONTH_COLUMNS = 4;
    private static final int YEAR_MONTH_ROWS = 3;
    private static final int YEAR_MONTH_WEEKS = 6;
    private static final double YEAR_CARD_GAP = 16;
    private static final double SIDEBAR_EXPANDED_WIDTH = 280;
    private static final double SIDEBAR_COLLAPSED_WIDTH = 40;
    private static final String COMPLETED_PROXY_STYLE = "heatmap-completed-proxy";
    private static final String COMPLETED_PROXY_HOST_STYLE = "heatmap-completed-proxy-host";

    private MainController controller;
    private ScheduleDAO scheduleDAO;

    private VBox root;
    private HBox metaBar;
    private HBox body;
    private StackPane heatmapPane;
    private GridPane heatmapGrid;
    private ScrollPane scrollPane;
    private Label statsLabel;
    private HBox legend;
    private Button prevBtn;
    private Button nextBtn;
    private VBox sidebarShell;
    private VBox daySchedulePanel;
    private VBox dayScheduleRail;
    private Label dayScheduleTitle;
    private Label dayScheduleCountLabel;
    private Label dayScheduleRailDateLabel;
    private Label dayScheduleRailCountLabel;
    private VBox dayScheduleCardsBox;
    private HBox completedDropZone;
    private final List<Schedule> loadedSchedules = new ArrayList<>();
    private Map<LocalDate, List<Schedule>> schedulesByDate = new LinkedHashMap<>();
    private boolean redrawQueued;
    private boolean layoutRefreshQueued;
    private boolean sidebarCollapsed;
    private String lastLayoutSignature;
    private LocalDate visibleStartDate;
    private LocalDate visibleEndDate;
    private ScheduleCollapsePopAnimator.MotionHandle completedProxyHandle;
    private StackPane completedProxyHost;
    private StackPane completedProxyShell;
    private ChangeListener<Bounds> viewportReadyListener;

    private String currentViewMode = "month"; // week, month, year
    private LocalDate currentDate = LocalDate.now();
    private LocalDate selectedDate;

    public HeatmapView(MainController controller) {
        this.controller = controller;
        this.scheduleDAO = new ScheduleDAO();

        initializeUI();
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
        legend = createLegend();
        metaBar = createMetaBar();

        // 热力图容器
        scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("heatmap-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        configureScrollPoliciesForCurrentView();

        heatmapGrid = new GridPane();
        heatmapGrid.getStyleClass().add("heatmap-grid");
        heatmapGrid.setHgap(GRID_GAP);
        heatmapGrid.setVgap(GRID_GAP);
        heatmapGrid.setPadding(new Insets(10));
        heatmapGrid.setAlignment(Pos.CENTER);

        scrollPane.setContent(heatmapGrid);
        scrollPane.setMinWidth(0);

        heatmapPane = new StackPane(scrollPane);
        heatmapPane.getStyleClass().add("heatmap-main-pane");
        heatmapPane.setMinWidth(0);
        HBox.setHgrow(heatmapPane, Priority.ALWAYS);
        heatmapPane.widthProperty().addListener((obs, oldValue, newValue) -> queueLayoutRefresh());
        heatmapPane.heightProperty().addListener((obs, oldValue, newValue) -> queueLayoutRefresh());

        // 图例
        sidebarShell = createDayScheduleSidebar();

        body = new HBox(16, heatmapPane, sidebarShell);
        body.getStyleClass().add("heatmap-body");
        body.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(body, Priority.ALWAYS);

        applySidebarState();

        root.getChildren().addAll(header, metaBar, body);
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
        HBox legend = new HBox(8);
        legend.getStyleClass().add("heatmap-legend");
        legend.setAlignment(Pos.CENTER_RIGHT);
        updateLegend();
        return legend;
    }

    private HBox createMetaBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("heatmap-meta-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(statsLabel, spacer, legend);
        return bar;
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

    private VBox createDayScheduleSidebar() {
        VBox shell = new VBox();
        shell.getStyleClass().addAll("heatmap-sidebar-shell", "heatmap-sidebar");
        shell.setFillWidth(true);

        daySchedulePanel = new VBox(12);
        daySchedulePanel.getStyleClass().addAll("heatmap-day-panel", "heatmap-day-panel-content");

        HBox panelHeader = new HBox(10);
        panelHeader.getStyleClass().add("heatmap-day-panel-header");
        panelHeader.setAlignment(Pos.CENTER_LEFT);

        dayScheduleTitle = new Label("已选日期");
        dayScheduleTitle.getStyleClass().add("heatmap-day-title");

        dayScheduleCountLabel = new Label(buildScheduleCountText(0));
        dayScheduleCountLabel.getStyleClass().add("heatmap-day-count");

        VBox titleGroup = new VBox(6, dayScheduleTitle, dayScheduleCountLabel);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        panelHeader.getChildren().addAll(titleGroup, headerSpacer, createSidebarToggleButton(true));

        ScrollPane dayScheduleScrollPane = new ScrollPane();
        dayScheduleScrollPane.getStyleClass().add("heatmap-day-scroll");
        dayScheduleScrollPane.setFitToWidth(true);
        dayScheduleScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(dayScheduleScrollPane, Priority.ALWAYS);

        dayScheduleCardsBox = new VBox(10);
        dayScheduleCardsBox.getStyleClass().add("heatmap-day-cards");
        dayScheduleCardsBox.setFillWidth(true);

        completedDropZone = new HBox();
        completedDropZone.setAlignment(Pos.CENTER_LEFT);
        completedDropZone.getStyleClass().add("heatmap-completed-zone");
        Label completedDropLabel = new Label("已完成归档区");
        completedDropLabel.getStyleClass().add("heatmap-completed-zone-label");
        completedDropZone.getChildren().add(completedDropLabel);

        dayScheduleScrollPane.setContent(dayScheduleCardsBox);
        daySchedulePanel.getChildren().addAll(panelHeader, dayScheduleScrollPane, completedDropZone);

        dayScheduleRail = new VBox(0);
        dayScheduleRail.getStyleClass().add("heatmap-day-rail");
        dayScheduleRail.setAlignment(Pos.TOP_CENTER);
        dayScheduleRail.getChildren().add(createSidebarToggleButton(false));

        shell.getChildren().addAll(daySchedulePanel, dayScheduleRail);
        VBox.setVgrow(daySchedulePanel, Priority.ALWAYS);
        return shell;
    }

    private Button createSidebarToggleButton(boolean collapseTarget) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button", "heatmap-sidebar-toggle");
        button.setGraphic(controller.createSvgIcon(
            collapseTarget ? "/icons/macaron_arrow-right_icon.svg" : "/icons/macaron_arrow-left_icon.svg",
            null,
            18
        ));
        button.setTooltip(new Tooltip(collapseTarget ? "收起当日日程" : "展开当日日程"));
        button.setOnAction(event -> setSidebarCollapsed(collapseTarget));
        return button;
    }

    private void setSidebarCollapsed(boolean collapsed) {
        if (sidebarCollapsed == collapsed) {
            return;
        }
        sidebarCollapsed = collapsed;
        applySidebarState();
        queueRefresh();
    }

    private void applySidebarState() {
        if (sidebarShell == null || daySchedulePanel == null || dayScheduleRail == null) {
            return;
        }

        daySchedulePanel.setManaged(!sidebarCollapsed);
        daySchedulePanel.setVisible(!sidebarCollapsed);
        dayScheduleRail.setManaged(sidebarCollapsed);
        dayScheduleRail.setVisible(sidebarCollapsed);

        double width = resolveSidebarWidth(sidebarCollapsed);
        sidebarShell.setMinWidth(width);
        sidebarShell.setPrefWidth(width);
        sidebarShell.setMaxWidth(width);

        sidebarShell.getStyleClass().removeAll("heatmap-sidebar-collapsed", "heatmap-sidebar-expanded");
        if (sidebarCollapsed) {
            sidebarShell.getStyleClass().add("heatmap-sidebar-collapsed");
        } else {
            sidebarShell.getStyleClass().add("heatmap-sidebar-expanded");
        }

        sidebarShell.requestLayout();
        if (body != null) {
            body.requestLayout();
        }
        if (heatmapPane != null) {
            heatmapPane.requestLayout();
        }
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void refresh() {
        queueRefresh();
    }

    private void queueLayoutRefresh() {
        if (layoutRefreshQueued) {
            return;
        }
        layoutRefreshQueued = true;
        Platform.runLater(() -> {
            layoutRefreshQueued = false;
            String signature = buildCurrentLayoutSignature();
            if (signature == null || signature.equals(lastLayoutSignature)) {
                return;
            }
            queueRefresh();
        });
    }

    private void queueRefresh() {
        configureScrollPoliciesForCurrentView();
        if (!ensureViewportReady()) {
            return;
        }
        if (redrawQueued) {
            return;
        }
        redrawQueued = true;
        Platform.runLater(() -> {
            redrawQueued = false;
            configureScrollPoliciesForCurrentView();
            if (!ensureViewportReady()) {
                return;
            }
            try {
                drawHeatmap();
            } catch (SQLException e) {
                controller.showError("加载热力图失败", e.getMessage());
            }
        });
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
            rows = YEAR_MONTH_ROWS;
            cols = YEAR_MONTH_COLUMNS;
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

        heatmapGrid.getChildren().clear();
        configureGridForCurrentView();
        updateNavigationButtons();
        updateLegend();
        schedulesByDate = buildSchedulesByDate(loadedSchedules, visibleStartDate, visibleEndDate);
        ensureSelectedDate(visibleStartDate, visibleEndDate);

        Map<LocalDate, Integer> stats = buildDailyCompletionStats(loadedSchedules, visibleStartDate, visibleEndDate);
        int totalCompleted = stats.values().stream().mapToInt(Integer::intValue).sum();
        int activeDays = (int) stats.values().stream().filter(v -> v > 0).count();
        statsLabel.setText(String.format(
            "%s：共完成 %d 项，活跃 %d 天",
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
        lastLayoutSignature = buildCurrentLayoutSignature();
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

    private void configureGridForCurrentView() {
        if ("year".equals(currentViewMode)) {
            heatmapGrid.setHgap(YEAR_CARD_GAP);
            heatmapGrid.setVgap(YEAR_CARD_GAP);
            heatmapGrid.setAlignment(Pos.TOP_LEFT);
            return;
        }
        heatmapGrid.setHgap(GRID_GAP);
        heatmapGrid.setVgap(GRID_GAP);
        heatmapGrid.setAlignment(Pos.CENTER);
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
        for (int month = 1; month <= 12; month++) {
            LocalDate monthStart = LocalDate.of(startDate.getYear(), month, 1);
            VBox monthCard = createYearMonthCard(monthStart, stats, cellSize);
            heatmapGrid.add(monthCard, resolveYearMonthColumn(month), resolveYearMonthRow(month));
        }
    }

    private VBox createYearMonthCard(LocalDate monthStart, Map<LocalDate, Integer> stats, double cellSize) {
        VBox monthCard = new VBox(8);
        monthCard.getStyleClass().add("heatmap-year-card");

        Label monthTitle = new Label(buildYearMonthTitle(monthStart));
        monthTitle.getStyleClass().add("heatmap-year-title");

        GridPane monthGrid = new GridPane();
        monthGrid.getStyleClass().add("heatmap-year-month-grid");
        monthGrid.setHgap(GRID_GAP);
        monthGrid.setVgap(GRID_GAP);

        String[] weekdayNames = {"日", "一", "二", "三", "四", "五", "六"};
        for (int column = 0; column < weekdayNames.length; column++) {
            Label weekdayLabel = new Label(weekdayNames[column]);
            weekdayLabel.getStyleClass().addAll("label-hint", "heatmap-year-weekday");
            monthGrid.add(weekdayLabel, column, 0);
        }

        int firstDayOffset = monthStart.getDayOfWeek().getValue() % 7;
        int daysInMonth = monthStart.lengthOfMonth();
        for (int slot = 0; slot < YEAR_MONTH_WEEKS * 7; slot++) {
            int row = slot / 7 + 1;
            int column = slot % 7;
            int dayOfMonth = slot - firstDayOffset + 1;
            if (dayOfMonth < 1 || dayOfMonth > daysInMonth) {
                monthGrid.add(createHeatmapPlaceholder(cellSize), column, row);
                continue;
            }

            LocalDate date = monthStart.withDayOfMonth(dayOfMonth);
            int count = stats.getOrDefault(date, 0);
            monthGrid.add(createHeatmapCell(date, count, cellSize, true, true), column, row);
        }

        monthCard.getChildren().addAll(monthTitle, monthGrid);
        return monthCard;
    }

    private Region createHeatmapPlaceholder(double cellSize) {
        Region placeholder = new Region();
        placeholder.getStyleClass().add("heatmap-cell-placeholder");
        double size = cellSize + CELL_CHROME;
        placeholder.setMinSize(size, size);
        placeholder.setPrefSize(size, size);
        placeholder.setMaxSize(size, size);
        return placeholder;
    }

    private StackPane createHeatmapCell(LocalDate date, int count, double cellSize, boolean activeInCurrentPeriod) {
        return createHeatmapCell(date, count, cellSize, activeInCurrentPeriod, false);
    }

    private StackPane createHeatmapCell(
        LocalDate date,
        int count,
        double cellSize,
        boolean activeInCurrentPeriod,
        boolean yearCell
    ) {
        Rectangle rect = new Rectangle(cellSize, cellSize);
        rect.getStyleClass().add("heatmap-cell");
        if (yearCell) {
            rect.getStyleClass().add("heatmap-year-cell");
        }
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
        if (dayScheduleTitle == null || dayScheduleCardsBox == null) {
            return;
        }

        dayScheduleTitle.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) + " 的日程");
        removeCompletedDropProxy();
        dayScheduleCardsBox.getChildren().clear();

        List<Schedule> schedules = schedulesByDate.getOrDefault(selectedDate, List.of());
        if (dayScheduleCountLabel != null) {
            dayScheduleCountLabel.setText(buildScheduleCountText(schedules.size()));
        }
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
        card.getStyleClass().addAll("heatmap-day-card", "heatmap-day-card-compact");
        card.setMaxWidth(Double.MAX_VALUE);
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
        cardShell.setMaxWidth(Double.MAX_VALUE);

        StackPane cardMotionHost = new StackPane(cardShell);
        cardMotionHost.getStyleClass().add("schedule-card-motion-host");
        cardMotionHost.setPickOnBounds(false);
        cardMotionHost.setMaxWidth(Double.MAX_VALUE);
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
            targetCompleted -> handleStatusToggle(cardMotionHost, schedule, statusControlRef[0], targetCompleted)
        );
        statusControlRef[0] = statusControl;

        HBox header = new HBox(8);
        header.getStyleClass().add("heatmap-day-card-top");
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(schedule.getName());
        titleLabel.getStyleClass().addAll("schedule-title", "schedule-card-title-text");
        titleLabel.setWrapText(false);
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        HBox tagsBox = new HBox(6);
        tagsBox.getStyleClass().add("heatmap-day-card-tags");
        tagsBox.setAlignment(Pos.CENTER_RIGHT);

        if (hasText(schedule.getPriority())) {
            Label priorityLabel = new Label(schedule.getPriority());
            priorityLabel.getStyleClass().add("priority-" + getPriorityClass(schedule.getPriority()));
            tagsBox.getChildren().add(priorityLabel);
        }

        if (hasText(schedule.getCategory())) {
            Label categoryLabel = new Label(schedule.getCategory());
            categoryLabel.getStyleClass().add("category-tag");
            tagsBox.getChildren().add(categoryLabel);
        }

        header.getChildren().addAll(statusControl, colorMark, titleLabel);
        if (!tagsBox.getChildren().isEmpty()) {
            header.getChildren().add(tagsBox);
        }

        HBox metaLine = new HBox(8);
        metaLine.getStyleClass().add("heatmap-day-card-meta");
        metaLine.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label(getScheduleDateText(schedule));
        dateLabel.getStyleClass().addAll("schedule-date", "schedule-card-subtitle-text");
        dateLabel.setWrapText(false);
        dateLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        dateLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(dateLabel, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(getScheduleStatusText(schedule));
        statusLabel.getStyleClass().addAll("label-hint", "schedule-card-subtitle-text");

        metaLine.getChildren().addAll(dateLabel, spacer, statusLabel);
        card.getChildren().addAll(header, metaLine);

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
            ScheduleReflowAnimator.captureVisibleCardBounds(dayScheduleCardsBox, schedule.getId());

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
        ScheduleReflowAnimator.playVerticalReflow(dayScheduleCardsBox, beforeBounds, null);
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

    private String buildCurrentLayoutSignature() {
        return buildLayoutSignature(
            currentViewMode,
            currentDate,
            sidebarCollapsed,
            resolveLayoutContentWidth(),
            resolveLayoutContentHeight()
        );
    }

    private void configureScrollPoliciesForCurrentView() {
        if (scrollPane == null) {
            return;
        }
        ScrollPane.ScrollBarPolicy policy = "year".equals(currentViewMode)
            ? ScrollPane.ScrollBarPolicy.AS_NEEDED
            : ScrollPane.ScrollBarPolicy.NEVER;
        scrollPane.setHbarPolicy(policy);
        scrollPane.setVbarPolicy(policy);
    }

    private boolean ensureViewportReady() {
        if (hasRenderableViewport(resolveViewportWidth(), resolveViewportHeight())) {
            clearViewportReadyListener();
            return true;
        }
        registerViewportReadyListener();
        return false;
    }

    private void registerViewportReadyListener() {
        if (scrollPane == null || viewportReadyListener != null) {
            return;
        }
        viewportReadyListener = (obs, oldBounds, newBounds) -> {
            if (!hasRenderableViewport(newBounds)) {
                return;
            }
            clearViewportReadyListener();
            queueRefresh();
        };
        scrollPane.viewportBoundsProperty().addListener(viewportReadyListener);
        if (hasRenderableViewport(scrollPane.getViewportBounds())) {
            clearViewportReadyListener();
            queueRefresh();
        }
    }

    private void clearViewportReadyListener() {
        if (scrollPane == null || viewportReadyListener == null) {
            return;
        }
        scrollPane.viewportBoundsProperty().removeListener(viewportReadyListener);
        viewportReadyListener = null;
    }

    private double resolveLayoutContentWidth() {
        if ("year".equals(currentViewMode)) {
            return resolveHeatmapPaneWidth();
        }
        return resolveViewportWidth();
    }

    private double resolveLayoutContentHeight() {
        if ("year".equals(currentViewMode)) {
            return resolveHeatmapPaneHeight();
        }
        return resolveViewportHeight();
    }

    private double resolveViewportWidth() {
        return scrollPane != null ? scrollPane.getViewportBounds().getWidth() : 0;
    }

    private double resolveViewportHeight() {
        return scrollPane != null ? scrollPane.getViewportBounds().getHeight() : 0;
    }

    private double resolveHeatmapPaneWidth() {
        double width = heatmapPane != null ? heatmapPane.getWidth() : 0;
        if (width <= 0 && scrollPane != null) {
            width = scrollPane.getWidth();
        }
        return width;
    }

    private double resolveHeatmapPaneHeight() {
        double height = heatmapPane != null ? heatmapPane.getHeight() : 0;
        if (height <= 0 && scrollPane != null) {
            height = scrollPane.getHeight();
        }
        return height;
    }

    private double calculateCellSize(int cols, int rows) {
        if ("year".equals(currentViewMode)) {
            return calculateYearCellSize();
        }

        double viewportWidth = resolveViewportWidth();
        double viewportHeight = resolveViewportHeight();
        double horizontalPadding = heatmapGrid.getPadding().getLeft() + heatmapGrid.getPadding().getRight();
        double verticalPadding = heatmapGrid.getPadding().getTop() + heatmapGrid.getPadding().getBottom();
        double reservedHeaderHeight = "week".equals(currentViewMode) ? WEEK_EXTRA_HEIGHT : MONTH_HEADER_HEIGHT;
        double minimumSize = "week".equals(currentViewMode) ? 28 : 18;

        return calculateCalendarCellSize(
            viewportWidth,
            viewportHeight,
            horizontalPadding,
            verticalPadding,
            reservedHeaderHeight,
            cols,
            rows,
            GRID_GAP,
            CELL_CHROME,
            CALENDAR_LAYOUT_SAFETY,
            minimumSize,
            90
        );
    }

    private double calculateYearCellSize() {
        double availableWidth = Math.max(resolveHeatmapPaneWidth() - getReservedWidth(), 320);
        double availableHeight = Math.max(resolveHeatmapPaneHeight() - getReservedHeight(), 240);

        double monthCardWidth = (availableWidth - Math.max(0, YEAR_MONTH_COLUMNS - 1) * YEAR_CARD_GAP)
            / YEAR_MONTH_COLUMNS;
        double monthCardHeight = (availableHeight - Math.max(0, YEAR_MONTH_ROWS - 1) * YEAR_CARD_GAP)
            / YEAR_MONTH_ROWS;

        double widthBasedSize = (monthCardWidth - 28 - Math.max(0, 6) * GRID_GAP) / 7.0;
        double heightBasedSize = (monthCardHeight - 42 - Math.max(0, YEAR_MONTH_WEEKS) * GRID_GAP) / 7.0;
        return clamp(Math.floor(Math.min(widthBasedSize, heightBasedSize)), 12, 26);
    }

    private double getReservedWidth() {
        double horizontalPadding = heatmapGrid.getPadding().getLeft() + heatmapGrid.getPadding().getRight();
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

    private double clamp(double value, double min, double max) {
        return clampValue(value, min, max);
    }

    static String buildLayoutSignature(
        String viewMode,
        LocalDate focusDate,
        boolean sidebarCollapsed,
        double availableWidth,
        double availableHeight
    ) {
        if (viewMode == null || focusDate == null) {
            return null;
        }
        int width = quantizeLayoutSize(availableWidth);
        int height = quantizeLayoutSize(availableHeight);
        if (width <= 0 || height <= 0) {
            return null;
        }
        return viewMode + "|" + focusDate + "|" + sidebarCollapsed + "|" + width + "x" + height;
    }

    static int quantizeLayoutSize(double size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.round(size);
    }

    static boolean hasRenderableViewport(Bounds bounds) {
        if (bounds == null) {
            return false;
        }
        return hasRenderableViewport(bounds.getWidth(), bounds.getHeight());
    }

    static boolean hasRenderableViewport(double width, double height) {
        return width >= 1 && height >= 1;
    }

    static double calculateCalendarCellSize(
        double viewportWidth,
        double viewportHeight,
        double horizontalPadding,
        double verticalPadding,
        double reservedHeaderHeight,
        int columns,
        int rows,
        double gridGap,
        double cellChrome,
        double safetyBuffer,
        double minSize,
        double maxSize
    ) {
        if (!hasRenderableViewport(viewportWidth, viewportHeight) || columns <= 0 || rows <= 0) {
            return minSize;
        }

        double usableWidth = viewportWidth
            - horizontalPadding
            - safetyBuffer
            - Math.max(0, columns - 1) * gridGap
            - columns * cellChrome;
        double usableHeight = viewportHeight
            - verticalPadding
            - safetyBuffer
            - reservedHeaderHeight
            - Math.max(0, rows - 1) * gridGap
            - rows * cellChrome;

        double rawSize = Math.floor(Math.min(usableWidth / columns, usableHeight / rows));
        return clampValue(rawSize, minSize, maxSize);
    }

    static double calculateCalendarFootprintWidth(
        double cellSize,
        double horizontalPadding,
        int columns,
        double gridGap,
        double cellChrome,
        double safetyBuffer
    ) {
        return horizontalPadding
            + safetyBuffer
            + columns * (cellSize + cellChrome)
            + Math.max(0, columns - 1) * gridGap;
    }

    static double resolveSidebarWidth(boolean collapsed) {
        return collapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;
    }

    private static double clampValue(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
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

    private String buildScheduleCountText(int count) {
        return count + " 项日程";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
            renderHeatmapFromLoadedSchedules(resolveCurrentRows(), resolveCurrentColumns());
        }
    }

    @Override
    public void confirmCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null || loadedSchedules.isEmpty()) {
            return;
        }
        if (visibleStartDate != null && visibleEndDate != null) {
            renderHeatmapFromLoadedSchedules(resolveCurrentRows(), resolveCurrentColumns());
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
            renderHeatmapFromLoadedSchedules(resolveCurrentRows(), resolveCurrentColumns());
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

    static int resolveYearMonthColumn(int month) {
        int normalizedMonth = clampMonth(month);
        return (normalizedMonth - 1) % YEAR_MONTH_COLUMNS;
    }

    static int resolveYearMonthRow(int month) {
        int normalizedMonth = clampMonth(month);
        return (normalizedMonth - 1) / YEAR_MONTH_COLUMNS;
    }

    static String buildYearMonthTitle(LocalDate monthStart) {
        return monthStart.getMonthValue() + "月";
    }

    private static int clampMonth(int month) {
        return Math.max(1, Math.min(12, month));
    }

    private int resolveCurrentRows() {
        if ("week".equals(currentViewMode)) {
            return 1;
        }
        if ("year".equals(currentViewMode)) {
            return YEAR_MONTH_ROWS;
        }
        return 6;
    }

    private int resolveCurrentColumns() {
        if ("year".equals(currentViewMode)) {
            return YEAR_MONTH_COLUMNS;
        }
        return 7;
    }
}
