package com.example.view;

import com.example.controller.MainController;
import com.example.databaseutil.ScheduleDAO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;

public class HeatmapView implements View {

    private MainController controller;
    private ScheduleDAO scheduleDAO;

    private VBox root;
    private GridPane heatmapGrid;
    private Label statsLabel;

    private String currentViewMode = "month"; // week, month, year
    private LocalDate currentDate = LocalDate.now();

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
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);

        heatmapGrid = new GridPane();
        heatmapGrid.setHgap(3);
        heatmapGrid.setVgap(3);
        heatmapGrid.setPadding(new Insets(10));

        scrollPane.setContent(heatmapGrid);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 图例
        HBox legend = createLegend();

        root.getChildren().addAll(header, statsLabel, scrollPane, legend);
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
            refresh();
        });

        ToggleButton monthBtn = new ToggleButton("月");
        monthBtn.setToggleGroup(viewGroup);
        monthBtn.setSelected(true);
        monthBtn.setOnAction(e -> {
            currentViewMode = "month";
            refresh();
        });

        ToggleButton yearBtn = new ToggleButton("年");
        yearBtn.setToggleGroup(viewGroup);
        yearBtn.setOnAction(e -> {
            currentViewMode = "year";
            refresh();
        });

        // 导航按钮
        Button prevBtn = new Button("< 上一" + getPeriodName());
        prevBtn.setOnAction(e -> navigate(-1));

        Button nextBtn = new Button("下一" + getPeriodName() + " >");
        nextBtn.setOnAction(e -> navigate(1));

        Button todayBtn = new Button("今天");
        todayBtn.setOnAction(e -> {
            currentDate = LocalDate.now();
            refresh();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(
            titleLabel,
            new Label("视图:"),
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
        refresh();
    }

    private HBox createLegend() {
        HBox legend = new HBox(10);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(10));

        legend.getChildren().add(new Label("完成数量:"));

        String[] labels = {"0", "1-2", "3-5", "6-8", "9+"};
        String[] colors = {"#ebedf0", "#c6e48b", "#7bc96f", "#239a3b", "#196127"};

        for (int i = 0; i < labels.length; i++) {
            Rectangle rect = new Rectangle(15, 15);
            rect.setFill(Color.web(colors[i]));
            rect.getStyleClass().add("heatmap-cell");

            Label label = new Label(labels[i]);
            label.getStyleClass().add("label-hint");

            legend.getChildren().addAll(rect, label);
        }

        return legend;
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void refresh() {
        Platform.runLater(() -> {
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
            rows = 7;
            cols = 53;
        } else {
            startDate = currentDate.withDayOfMonth(1);
            endDate = startDate.plusMonths(1).minusDays(1);
            rows = 7;
            cols = 6;
        }

        // 获取统计数据
        Map<LocalDate, Integer> stats = scheduleDAO.getDailyCompletionStats(startDate, endDate);

        // 更新统计标签
        int totalCompleted = stats.values().stream().mapToInt(Integer::intValue).sum();
        int activeDays = (int) stats.values().stream().filter(v -> v > 0).count();
        statsLabel.setText(String.format("%s: 共完成 %d 项任务，活跃天数 %d 天",
            currentDate.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
            totalCompleted, activeDays));

        // 绘制热力图
        if ("week".equals(currentViewMode)) {
            drawWeekView(startDate, stats);
        } else if ("year".equals(currentViewMode)) {
            drawYearView(startDate, stats);
        } else {
            drawMonthView(startDate, endDate, stats);
        }
    }

    private void drawWeekView(LocalDate startDate, Map<LocalDate, Integer> stats) {
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            int count = stats.getOrDefault(date, 0);

            // 星期标签
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.getStyleClass().add("label-hint");
            heatmapGrid.add(dayLabel, i, 0);

            // 日期单元格
            StackPane cell = createHeatmapCell(date, count);
            heatmapGrid.add(cell, i, 1);

            // 日期数字
            Label dateLabel = new Label(date.format(DateTimeFormatter.ofPattern("MM/dd")));
            dateLabel.getStyleClass().add("label-hint");
            heatmapGrid.add(dateLabel, i, 2);
        }
    }

    private void drawMonthView(LocalDate startDate, LocalDate endDate, Map<LocalDate, Integer> stats) {
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

                StackPane cell = createHeatmapCell(current, count);

                // 淡化非本月日期
                if (current.getMonth() != startDate.getMonth()) {
                    cell.setOpacity(0.3);
                }

                heatmapGrid.add(cell, col, row);
                current = current.plusDays(1);
            }
            row++;
        }
    }

    private void drawYearView(LocalDate startDate, Map<LocalDate, Integer> stats) {
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
                    StackPane cell = createHeatmapCell(current, count);
                    heatmapGrid.add(cell, weekCol, day);
                    current = current.plusDays(1);
                }
            }
            weekCol++;
        }
    }

    private StackPane createHeatmapCell(LocalDate date, int count) {
        Rectangle rect = new Rectangle(25, 25);
        rect.getStyleClass().add("heatmap-cell");
        rect.setFill(getColorForCount(count));

        StackPane cell = new StackPane(rect);
        cell.setPadding(new Insets(2));

        // 悬停提示
        String tooltipText = String.format("%s\n完成: %d 项",
            date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            count);
        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(cell, tooltip);

        return cell;
    }

    private Color getColorForCount(int count) {
        if (count == 0) return Color.web("#ebedf0");
        if (count <= 2) return Color.web("#c6e48b");
        if (count <= 5) return Color.web("#7bc96f");
        if (count <= 8) return Color.web("#239a3b");
        return Color.web("#196127");
    }
}
