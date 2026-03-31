package com.example.view;

import com.example.controller.MainController;
import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TimelineView implements View {
    
    private MainController controller;
    private ScheduleDAO scheduleDAO;
    
    private VBox root;
    private StackPane timelineContainer;
    private Pane timelinePane;
    private ScrollPane scrollPane;
    
    private static final double DAY_WIDTH = 80;
    private static final double TIMELINE_Y = 200;
    private static final double NODE_RADIUS = 6;
    
    private AnimationTimer autoScrollTimer;
    private long lastUpdate = 0;
    
    public TimelineView(MainController controller) {
        this.controller = controller;
        this.scheduleDAO = new ScheduleDAO();
        
        initializeUI();
        startAutoScroll();
    }
    
    private void initializeUI() {
        root = new VBox(10);
        root.getStyleClass().add("main-content");
        root.setPadding(new Insets(15));
        
        // 标题栏
        HBox header = createHeader();
        
        // 时间轴容器
        timelineContainer = new StackPane();
        timelineContainer.getStyleClass().add("timeline-container");
        VBox.setVgrow(timelineContainer, Priority.ALWAYS);
        
        // 时间轴绘制区域
        timelinePane = new Pane();
        
        scrollPane = new ScrollPane(timelinePane);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        timelineContainer.getChildren().add(scrollPane);
        
        // 新建按钮
        Button newScheduleBtn = new Button("+ 新建日程");
        newScheduleBtn.getStyleClass().add("button");
        newScheduleBtn.setOnAction(e -> controller.openNewScheduleDialog());
        
        HBox buttonBox = new HBox(newScheduleBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        root.getChildren().addAll(header, timelineContainer, buttonBox);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("日程时间轴");
        titleLabel.getStyleClass().add("label-title");
        
        Label hintLabel = new Label("水平滚动查看不同时间段");
        hintLabel.getStyleClass().add("label-hint");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(titleLabel, hintLabel, spacer);
        
        return header;
    }
    
    @Override
    public Node getView() {
        return root;
    }
    
    @Override
    public void refresh() {
        Platform.runLater(() -> {
            try {
                drawTimeline();
            } catch (SQLException e) {
                controller.showError("加载时间轴失败", e.getMessage());
            }
        });
    }
    
    private void drawTimeline() throws SQLException {
        timelinePane.getChildren().clear();
        
        List<Schedule> schedules = scheduleDAO.getAllSchedules();
        
        if (schedules.isEmpty()) {
            Label emptyLabel = new Label("暂无日程，点击 新建日程 添加");
            emptyLabel.getStyleClass().add("label-subtitle");
            emptyLabel.setLayoutX(100);
            emptyLabel.setLayoutY(TIMELINE_Y - 20);
            timelinePane.getChildren().add(emptyLabel);
            return;
        }
        
        // 计算时间范围
        LocalDate rawMinDate = schedules.stream()
            .filter(s -> s.getStartDate() != null)
            .map(Schedule::getStartDate)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now().minusDays(7));
        
        LocalDate rawMaxDate = schedules.stream()
            .filter(s -> s.getDueDate() != null)
            .map(Schedule::getDueDate)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now().plusDays(30));
        
        // 扩展范围
        final LocalDate minDate = rawMinDate.minusDays(3);
        final LocalDate maxDate = rawMaxDate.plusDays(7);
        
        long daysBetween = ChronoUnit.DAYS.between(minDate, maxDate);
        final double totalWidth = daysBetween * DAY_WIDTH + 200;
        timelinePane.setPrefWidth(totalWidth);
        timelinePane.setPrefHeight(400);
        
        // 绘制时间轴线
        Line timelineLine = new Line(50, TIMELINE_Y, totalWidth - 50, TIMELINE_Y);
        timelineLine.getStyleClass().add("timeline-line");
        timelinePane.getChildren().add(timelineLine);
        
        // 绘制日期标记
        LocalDate current = minDate;
        int dayIndex = 0;
        
        while (!current.isAfter(maxDate)) {
            double x = 50 + dayIndex * DAY_WIDTH;
            
            // 日期刻度线
            Line tick = new Line(x, TIMELINE_Y - 10, x, TIMELINE_Y + 10);
            tick.setStroke(Color.GRAY);
            timelinePane.getChildren().add(tick);
            
            // 日期标签
            Label dateLabel = new Label(current.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd")));
            dateLabel.setLayoutX(x - 15);
            dateLabel.setLayoutY(TIMELINE_Y + 15);
            dateLabel.getStyleClass().add("schedule-date");
            
            // 高亮今天
            if (current.equals(LocalDate.now())) {
                dateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
                
                // 今天的特殊标记
                Rectangle todayMarker = new Rectangle(x - 2, TIMELINE_Y - 15, 4, 30);
                todayMarker.setFill(Color.web("#2196F3"));
                timelinePane.getChildren().add(todayMarker);
            }
            
            timelinePane.getChildren().add(dateLabel);
            
            current = current.plusDays(1);
            dayIndex++;
        }
        
        // 绘制日程节点
        for (Schedule schedule : schedules) {
            if (schedule.getStartDate() == null || schedule.getDueDate() == null) {
                continue;
            }
            
            long startOffset = ChronoUnit.DAYS.between(minDate, schedule.getStartDate());
            long duration = ChronoUnit.DAYS.between(schedule.getStartDate(), schedule.getDueDate()) + 1;
            
            double startX = 50 + startOffset * DAY_WIDTH;
            double width = Math.max(duration * DAY_WIDTH - 10, 60);
            
            // 计算Y位置（避免重叠）
            double yPos = calculateYPosition(schedule, schedules, minDate);
            
            // 日程条
            Rectangle scheduleBar = new Rectangle(width, 30);
            scheduleBar.setArcWidth(5);
            scheduleBar.setArcHeight(5);
            
            if (schedule.isCompleted()) {
                scheduleBar.setFill(Color.web("#4caf50"));
                scheduleBar.setOpacity(0.7);
            } else if (schedule.isOverdue()) {
                scheduleBar.setFill(Color.web("#f44336"));
            } else {
                scheduleBar.setFill(Color.web(schedule.getColor()));
            }
            
            scheduleBar.setLayoutX(startX);
            scheduleBar.setLayoutY(yPos);
            
            // 日程标题
            Label titleLabel = new Label(schedule.getName());
            titleLabel.setLayoutX(startX + 5);
            titleLabel.setLayoutY(yPos + 5);
            titleLabel.setMaxWidth(width - 10);
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            
            // 点击事件
            scheduleBar.setOnMouseClicked(e -> {
                controller.showScheduleDetails(schedule);
                if (e.getClickCount() == 2) {
                    controller.openEditScheduleDialog(schedule);
                }
            });
            
            titleLabel.setOnMouseClicked(e -> {
                controller.showScheduleDetails(schedule);
                if (e.getClickCount() == 2) {
                    controller.openEditScheduleDialog(schedule);
                }
            });
            
            // 悬停提示
            Tooltip tooltip = new Tooltip(
                schedule.getName() + "\n" +
                "开始: " + schedule.getStartDate() + "\n" +
                "截止: " + schedule.getDueDate() + "\n" +
                "状态: " + (schedule.isCompleted() ? "已完成" : "未完成")
            );
            Tooltip.install(scheduleBar, tooltip);
            
            timelinePane.getChildren().addAll(scheduleBar, titleLabel);
        }
        
        // 滚动到今天位置（黄金分割点左侧）
        Platform.runLater(() -> {
            long todayOffset = ChronoUnit.DAYS.between(minDate, LocalDate.now());
            double todayX = 50 + todayOffset * DAY_WIDTH;
            double scrollPosition = todayX - timelineContainer.getWidth() * 0.382;
            scrollPane.setHvalue(scrollPosition / (totalWidth - timelineContainer.getWidth()));
        });
    }
    
    private double calculateYPosition(Schedule schedule, List<Schedule> allSchedules, LocalDate minDate) {
        // 简单的防重叠算法
        double baseY = TIMELINE_Y - 80;
        long startOffset = ChronoUnit.DAYS.between(minDate, schedule.getStartDate());
        
        int overlapCount = 0;
        for (Schedule other : allSchedules) {
            if (other == schedule || other.getStartDate() == null || other.getDueDate() == null) {
                continue;
            }
            
            long otherStart = ChronoUnit.DAYS.between(minDate, other.getStartDate());
            long otherEnd = ChronoUnit.DAYS.between(minDate, other.getDueDate());
            
            // 检查是否有重叠
            if (startOffset >= otherStart && startOffset <= otherEnd) {
                overlapCount++;
            }
        }
        
        return baseY - (overlapCount % 3) * 40;
    }
    
    private void startAutoScroll() {
        autoScrollTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 每分钟更新一次
                if (now - lastUpdate > 60_000_000_000L) { // 60秒
                    lastUpdate = now;
                    // 可以在这里添加自动滚动逻辑
                }
            }
        };
        autoScrollTimer.start();
    }
    
    public void stopAutoScroll() {
        if (autoScrollTimer != null) {
            autoScrollTimer.stop();
        }
    }
}
