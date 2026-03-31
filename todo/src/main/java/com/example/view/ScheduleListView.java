package com.example.view;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.example.controller.MainController;
import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ScheduleListView implements View {
    
    private MainController controller;
    private ScheduleDAO scheduleDAO;
    
    private VBox root;
    private ListView<Schedule> scheduleListView;
    private ObservableList<Schedule> schedules;
    
    private CheckBox showPastCheckbox;
    private ComboBox<String> sortComboBox;
    private ComboBox<String> filterComboBox;
    
    private boolean showingSearchResults = false;
    private String currentSearchKeyword = "";
    
    public ScheduleListView(MainController controller) {
        this.controller = controller;
        this.scheduleDAO = new ScheduleDAO();
        this.schedules = FXCollections.observableArrayList();
        
        initializeUI();
    }
    
    private void initializeUI() {
        root = new VBox(10);
        root.getStyleClass().add("main-content");
        root.setPadding(new Insets(15));
        
        // 标题和工具栏
        HBox toolbar = createToolbar();
        
        // 日程列表
        scheduleListView = new ListView<>();
        scheduleListView.getStyleClass().add("schedule-list");
        scheduleListView.setItems(schedules);
        scheduleListView.setCellFactory(lv -> new ScheduleListCell());
        
        // 选择事件
        scheduleListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    controller.showScheduleDetails(newVal);
                }
            }
        );
        
        // 新建按钮
        Button newScheduleBtn = new Button("+ 新建日程");
        newScheduleBtn.getStyleClass().add("button");
        newScheduleBtn.setOnAction(e -> controller.openNewScheduleDialog());
        
        HBox buttonBox = new HBox(newScheduleBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        VBox.setVgrow(scheduleListView, Priority.ALWAYS);
        
        root.getChildren().addAll(toolbar, scheduleListView, buttonBox);
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        // 标题
        Label titleLabel = new Label("日程管理");
        titleLabel.getStyleClass().add("label-title");
        
        // 显示过去日程复选框
        showPastCheckbox = new CheckBox("显示过去日程");
        showPastCheckbox.getStyleClass().add("check-box");
        showPastCheckbox.setOnAction(e -> {
            if (!showingSearchResults) {
                loadSchedules();
            }
        });
        
        // 排序选择
        sortComboBox = new ComboBox<>();
        sortComboBox.getItems().addAll("按日期排序", "按优先级排序", "按分类排序");
        sortComboBox.setValue("按日期排序");
        sortComboBox.setOnAction(e -> {
            if (!showingSearchResults) {
                loadSchedules();
            }
        });
        
        // 筛选选择
        filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll("全部", "未完成", "已完成", "高优先级", "即将到期");
        filterComboBox.setValue("全部");
        filterComboBox.setOnAction(e -> {
            if (!showingSearchResults) {
                loadSchedules();
            }
        });
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        toolbar.getChildren().addAll(titleLabel, showPastCheckbox, sortComboBox, filterComboBox, spacer);
        
        return toolbar;
    }
    
    @Override
    public Node getView() {
        return root;
    }
    
    @Override
    public void refresh() {
        if (!showingSearchResults) {
            loadSchedules();
        }
    }
    
    private void loadSchedules() {
        Platform.runLater(() -> {
            try {
                List<Schedule> allSchedules = scheduleDAO.getAllSchedules();
                
                // 筛选
                List<Schedule> filtered = allSchedules.stream()
                    .filter(this::applyFilter)
                    .collect(Collectors.toList());
                
                // 排序
                filtered.sort(getComparator());
                
                schedules.setAll(filtered);
                
            } catch (SQLException e) {
                controller.showError("加载日程失败", e.getMessage());
            }
        });
    }
    
    private boolean applyFilter(Schedule schedule) {
        // 是否显示过去日程
        if (!showPastCheckbox.isSelected() && schedule.getDueDate() != null) {
            if (schedule.getDueDate().isBefore(LocalDate.now()) && !schedule.isCompleted()) {
                return false;
            }
        }
        
        // 默认只显示未来7天
        if (!showPastCheckbox.isSelected() && schedule.getDueDate() != null) {
            if (schedule.getDueDate().isAfter(LocalDate.now().plusDays(7))) {
                return false;
            }
        }
        
        // 筛选条件
        String filter = filterComboBox.getValue();
        if (filter == null) filter = "全部";
        
        if ("未完成".equals(filter)) return !schedule.isCompleted();
        if ("已完成".equals(filter)) return schedule.isCompleted();
        if ("高优先级".equals(filter)) return "高".equals(schedule.getPriority());
        if ("即将到期".equals(filter)) return schedule.isUpcoming();
        return true;
    }
    
    private Comparator<Schedule> getComparator() {
        String sort = sortComboBox.getValue();
        if (sort == null) sort = "按日期排序";
        
        if ("按日期排序".equals(sort)) {
            return Comparator
                .comparing(Schedule::isCompleted)
                .thenComparing(Schedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("按优先级排序".equals(sort)) {
            return Comparator
                .comparing(Schedule::isCompleted)
                .thenComparing(Schedule::getPriorityValue, Comparator.reverseOrder());
        } else if ("按分类排序".equals(sort)) {
            return Comparator
                .comparing(Schedule::isCompleted)
                .thenComparing(Schedule::getCategory, Comparator.nullsFirst(Comparator.naturalOrder()));
        }
        return Comparator.comparing(Schedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }
    
    public void searchSchedules(String keyword) {
        showingSearchResults = true;
        currentSearchKeyword = keyword;
        
        Platform.runLater(() -> {
            try {
                List<Schedule> results = scheduleDAO.searchSchedules(keyword);
                schedules.setAll(results);
            } catch (SQLException e) {
                controller.showError("搜索失败", e.getMessage());
            }
        });
    }
    
    public void clearSearch() {
        showingSearchResults = false;
        currentSearchKeyword = "";
        loadSchedules();
    }
    
    public void addSchedule(Schedule schedule) throws SQLException {
        scheduleDAO.addSchedule(schedule);
        refresh();
    }
    
    public void updateSchedule(Schedule schedule) throws SQLException {
        scheduleDAO.updateSchedule(schedule);
        refresh();
    }
    
    public void deleteSchedule(Schedule schedule) throws SQLException {
        scheduleDAO.deleteSchedule(schedule.getId());
        refresh();
    }
    
    public void toggleScheduleComplete(Schedule schedule) throws SQLException {
        schedule.setCompleted(!schedule.isCompleted());
        scheduleDAO.updateScheduleStatus(schedule.getId(), schedule.isCompleted());
        refresh();
    }
    
    // 自定义列表单元格
    private class ScheduleListCell extends ListCell<Schedule> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        @Override
        protected void updateItem(Schedule schedule, boolean empty) {
            super.updateItem(schedule, empty);
            
            if (empty || schedule == null) {
                setGraphic(null);
                setText(null);
                getStyleClass().removeAll("completed", "overdue", "upcoming");
                return;
            }
            
            // 创建单元格内容
            HBox cell = new HBox(10);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(8));
            
            // 完成复选框
            CheckBox completeBox = new CheckBox();
            completeBox.setSelected(schedule.isCompleted());
            completeBox.setOnAction(e -> {
                try {
                    toggleScheduleComplete(schedule);
                } catch (SQLException ex) {
                    controller.showError("更新状态失败", ex.getMessage());
                }
            });
            
            // 优先级标签
            Label priorityLabel = new Label(schedule.getPriority());
            priorityLabel.getStyleClass().add("priority-" + getPriorityClass(schedule.getPriority()));
            
            // 标题
            Label titleLabel = new Label(schedule.getName());
            titleLabel.getStyleClass().add("schedule-title");
            if (schedule.isCompleted()) {
                titleLabel.setStyle("-fx-strikethrough: true;");
            }
            
            // 日期
            String dateText = "";
            if (schedule.getDueDate() != null) {
                dateText = schedule.getDueDate().format(formatter);
                if (schedule.isOverdue()) {
                    dateText += " (已过期)";
                }
            }
            Label dateLabel = new Label(dateText);
            dateLabel.getStyleClass().add("schedule-date");
            
            // 分类标签
            Label categoryLabel = new Label(schedule.getCategory());
            categoryLabel.getStyleClass().add("category-tag");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            cell.getChildren().addAll(completeBox, priorityLabel, titleLabel, spacer, dateLabel, categoryLabel);
            
            setGraphic(cell);
            
            // 添加点击事件，确保选择该日程
            cell.setOnMouseClicked(e -> {
                getListView().getSelectionModel().select(schedule);
                // 阻止事件冒泡，避免触发其他选择逻辑
                e.consume();
            });
            
            // 更新样式
            getStyleClass().removeAll("completed", "overdue", "upcoming");
            if (schedule.isCompleted()) {
                getStyleClass().add("completed");
            } else if (schedule.isOverdue()) {
                getStyleClass().add("overdue");
            } else if (schedule.isUpcoming()) {
                getStyleClass().add("upcoming");
            }
        }
        
        private String getPriorityClass(String priority) {
            if ("高".equals(priority)) return "high";
            if ("低".equals(priority)) return "low";
            return "medium";
        }
    }
}
