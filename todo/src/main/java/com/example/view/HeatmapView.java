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
import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

public class HeatmapView implements View {
    private static final double GRID_GAP = 3;

    private MainController controller;
    private ScheduleDAO scheduleDAO;

    private VBox root;
    private GridPane heatmapGrid;
    private ScrollPane scrollPane;
    private Label statsLabel;
    private HBox legend;
    private Button prevBtn;
    private Button nextBtn;
    private VBox daySchedulePanel;
    private Label dayScheduleTitle;
    private VBox dayScheduleCardsBox;
    private Map<LocalDate, List<Schedule>> schedulesByDate = new LinkedHashMap<>();
    private boolean redrawQueued;

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

        ToggleButton weekBtn = new ToggleButton("周");
        weekBtn.setToggleGroup(viewGroup);
        weekBtn.setOnAction(e -> {
            currentViewMode = "week";
            queueRefresh();
        });

        ToggleButton monthBtn = new ToggleButton("月");
        monthBtn.setToggleGroup(viewGroup);
        monthBtn.setSelected(true);
        monthBtn.setOnAction(e -> {
            currentViewMode = "month";
            queueRefresh();
        });

        ToggleButton yearBtn = new ToggleButton("年");
        yearBtn.setToggleGroup(viewGroup);
        yearBtn.setOnAction(e -> {
            currentViewMode = "year";
            queueRefresh();
        });

        // 导航按钮
        prevBtn = new Button();
        updateNavigationButtons();
        prevBtn.setOnAction(e -> navigate(-1));

        nextBtn = new Button();
        updateNavigationButtons();
        nextBtn.setOnAction(e -> navigate(1));

        Button todayBtn = new Button("今天");
        todayBtn.setOnAction(e -> {
            currentDate = LocalDate.now();
            queueRefresh();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label viewLabel = new Label("视图:");
        viewLabel.getStyleClass().add("label-subtitle");

        header.getChildren().addAll(
            titleLabel,
            viewLabel,
            weekBtn, monthBtn, yearBtn,
            spacer,
            prevBtn, todayBtn, nextBtn
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

        dayScheduleScrollPane.setContent(dayScheduleCardsBox);
        panel.getChildren().addAll(dayScheduleTitle, dayScheduleScrollPane);
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
                drawHeatmap();
            } catch (SQLException e) {
                controller.showError("加载热力图失败", e.getMessage());
            }
        });
    }

    private void drawHeatmap() throws SQLException {
        heatmapGrid.getChildren().clear();
        updateNavigationButtons();
        updateLegend();

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

        Map<LocalDate, Integer> stats = scheduleDAO.getDailyCompletionStats(startDate, endDate);
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
        rect.setFill(getColorForCount(count));

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
            prevBtn.setText("< 上一" + periodName);
        }
        if (nextBtn != null) {
            nextBtn.setText("下一" + periodName + " >");
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
        Color[] colors = getHeatmapPalette();

        for (int i = 0; i < labels.length; i++) {
            Rectangle rect = new Rectangle(15, 15);
            rect.setFill(colors[i]);
            rect.getStyleClass().add("heatmap-cell");

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

    private VBox createDayScheduleCard(Schedule schedule) {
        VBox card = new VBox(8);
        card.getStyleClass().add("heatmap-day-card");

        Color accentColor = getScheduleAccentColor(schedule);
        card.setStyle("-fx-border-color: transparent transparent transparent " + toRgb(accentColor) + "; -fx-border-width: 1 1 1 4;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Rectangle colorMark = new Rectangle(10, 10);
        colorMark.setArcWidth(10);
        colorMark.setArcHeight(10);
        colorMark.setFill(accentColor);

        VBox textGroup = new VBox(4);
        Label titleLabel = new Label(schedule.getName());
        titleLabel.getStyleClass().add("schedule-title");
        Label dateLabel = new Label(getScheduleDateText(schedule));
        dateLabel.getStyleClass().add("schedule-date");
        textGroup.getChildren().addAll(titleLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label priorityLabel = new Label(schedule.getPriority());
        priorityLabel.getStyleClass().add("priority-" + getPriorityClass(schedule.getPriority()));

        Label categoryLabel = new Label(schedule.getCategory());
        categoryLabel.getStyleClass().add("category-tag");

        header.getChildren().addAll(colorMark, textGroup, spacer, priorityLabel, categoryLabel);

        Label descriptionLabel = new Label(getScheduleDescriptionText(schedule));
        descriptionLabel.getStyleClass().add("schedule-description");
        descriptionLabel.setWrapText(true);

        Label statusLabel = new Label(getScheduleStatusText(schedule));
        statusLabel.getStyleClass().add("label-hint");

        card.getChildren().addAll(header, descriptionLabel, statusLabel);

        card.setOnMouseClicked(e -> {
            controller.showScheduleDetails(schedule);
            if (e.getClickCount() == 2) {
                controller.openEditScheduleDialog(schedule);
            }
        });

        return card;
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

    private double clamp(double value, double min, double max) {
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

    private Color getColorForCount(int count) {
        Color[] palette = getHeatmapPalette();
        if (count == 0) return palette[0];
        if (count <= 2) return palette[1];
        if (count <= 5) return palette[2];
        if (count <= 8) return palette[3];
        return palette[4];
    }

    private Color[] getHeatmapPalette() {
        if ("dark".equals(controller.getCurrentTheme())) {
            return new Color[] {
                Color.web("#2d333b"),
                Color.web("#0e4429"),
                Color.web("#006d32"),
                Color.web("#26a641"),
                Color.web("#39d353")
            };
        }
        return new Color[] {
            Color.web("#ebedf0"),
            Color.web("#c6e48b"),
            Color.web("#7bc96f"),
            Color.web("#239a3b"),
            Color.web("#196127")
        };
    }

    private Color getScheduleAccentColor(Schedule schedule) {
        if (schedule.isCompleted()) {
            return Color.web("#4caf50");
        }
        if (schedule.isOverdue()) {
            return Color.web("#f44336");
        }
        return Color.web(schedule.getColor() == null || schedule.getColor().isBlank() ? "#2196F3" : schedule.getColor());
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

    private String getPriorityClass(String priority) {
        if ("高".equals(priority)) {
            return "high";
        }
        if ("低".equals(priority)) {
            return "low";
        }
        return "medium";
    }

    private String toRgb(Color color) {
        return String.format("#%02X%02X%02X",
            (int) Math.round(color.getRed() * 255),
            (int) Math.round(color.getGreen() * 255),
            (int) Math.round(color.getBlue() * 255));
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
}
