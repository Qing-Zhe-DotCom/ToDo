package com.example.view;

import com.example.controller.MainController;
import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class InfoPanelView {

    private MainController controller;
    private ScheduleDAO scheduleDAO;

    private VBox root;
    private Schedule currentSchedule;

    // UI组件
    private Label titleLabel;
    private Label dateLabel;
    private Label priorityLabel;
    private Label categoryLabel;
    private Label statusLabel;
    private Label descriptionArea;
    private Label tagsLabel;
    private Label reminderLabel;

    private Button editButton;
    private Button deleteButton;
    private Button completeButton;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public InfoPanelView(MainController controller) {
        this.controller = controller;
        this.scheduleDAO = new ScheduleDAO();
        initializeUI();
    }

    private void initializeUI() {
        root = new VBox(15);
        root.getStyleClass().add("info-panel");
        root.setPadding(new Insets(15));
        root.setPrefWidth(280);

        // 标题
        Label panelTitle = new Label("日程详情");
        panelTitle.getStyleClass().add("info-panel-title");
        Button closeButton = new Button("✕");
        closeButton.getStyleClass().addAll("button-secondary", "info-panel-close");
        closeButton.setOnAction(e -> controller.hideScheduleDetailsPanel());
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        HBox titleBar = new HBox(8, panelTitle, titleSpacer, closeButton);
        titleBar.setAlignment(Pos.CENTER_LEFT);

        // 日程标题
        titleLabel = new Label("请选择日程");
        titleLabel.getStyleClass().add("label-title");
        titleLabel.setWrapText(true);

        // 状态
        statusLabel = new Label();
        statusLabel.getStyleClass().add("category-tag");

        // 日期信息
        dateLabel = new Label();
        dateLabel.getStyleClass().add("label-subtitle");
        dateLabel.setWrapText(true);

        // 优先级
        priorityLabel = new Label();
        priorityLabel.getStyleClass().add("priority-medium");

        // 分类
        categoryLabel = new Label();
        categoryLabel.getStyleClass().add("category-tag");

        // 标签
        tagsLabel = new Label();
        tagsLabel.getStyleClass().add("label-hint");
        tagsLabel.setWrapText(true);

        // 提醒时间
        reminderLabel = new Label();
        reminderLabel.getStyleClass().add("label-hint");

        // 描述
        Label descTitle = new Label("描述:");
        descTitle.getStyleClass().add("label-subtitle");

        descriptionArea = new Label("暂无描述");
        descriptionArea.getStyleClass().add("schedule-description");
        descriptionArea.setWrapText(true);

        // 操作按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        editButton = new Button("编辑");
        editButton.getStyleClass().addAll("button", "button-secondary");
        editButton.setOnAction(e -> {
            if (currentSchedule != null) {
                controller.openEditScheduleDialog(currentSchedule);
            }
        });

        deleteButton = new Button("删除");
        deleteButton.getStyleClass().addAll("button", "button-danger");
        deleteButton.setOnAction(e -> deleteSchedule());

        completeButton = new Button("完成");
        completeButton.getStyleClass().addAll("button", "button-success");
        completeButton.setOnAction(e -> toggleComplete());

        buttonBox.getChildren().addAll(completeButton, editButton, deleteButton);

        // 分隔线
        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        Separator separator3 = new Separator();

        // 组装
        HBox timeBox = new HBox(10, new Label("时间:"), dateLabel);
        timeBox.setAlignment(Pos.TOP_LEFT);

        root.getChildren().addAll(
            titleBar,
            separator1,
            titleLabel,
            statusLabel,
            separator2,
            timeBox,
            new HBox(10, new Label("优先级:"), priorityLabel),
            new HBox(10, new Label("分类:"), categoryLabel),
            tagsLabel,
            reminderLabel,
            separator3,
            descTitle,
            descriptionArea,
            new Region(), // 弹性空间
            buttonBox
        );

        VBox.setVgrow(root.getChildren().get(root.getChildren().size() - 2), Priority.ALWAYS);

        // 初始状态禁用按钮
        setButtonsEnabled(false);
    }

    public Node getView() {
        return root;
    }

    public void setSchedule(Schedule schedule) {
        this.currentSchedule = schedule;
        updateDisplay();
    }

    private void updateDisplay() {
        if (currentSchedule == null) {
            clearDisplay();
            return;
        }

        Platform.runLater(() -> {
            setButtonsEnabled(true);

            // 标题
            titleLabel.setText(currentSchedule.getName());

            // 状态
            if (currentSchedule.isCompleted()) {
                statusLabel.setText("✓ 已完成");
                statusLabel.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;");
                completeButton.setText("标记未完成");
            } else if (currentSchedule.isOverdue()) {
                statusLabel.setText("⚠ 已过期");
                statusLabel.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828;");
                completeButton.setText("标记完成");
            } else if (currentSchedule.isUpcoming()) {
                statusLabel.setText("⏰ 即将到期");
                statusLabel.setStyle("-fx-background-color: #fff3e0; -fx-text-fill: #ef6c00;");
                completeButton.setText("标记完成");
            } else {
                statusLabel.setText("⏳ 进行中");
                statusLabel.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2;");
                completeButton.setText("标记完成");
            }

            // 日期
            StringBuilder dateText = new StringBuilder();
            if (currentSchedule.getStartDate() != null) {
                dateText.append("开始: ").append(currentSchedule.getStartDate().format(DATE_FORMATTER)).append("\n");
            }
            if (currentSchedule.getDueDate() != null) {
                dateText.append("截止: ").append(currentSchedule.getDueDate().format(DATE_FORMATTER));
            }
            if (dateText.length() == 0) {
                dateText.append("未设置");
            }
            dateLabel.setText(dateText.toString().trim());

            // 优先级
            priorityLabel.setText(currentSchedule.getPriority());
            priorityLabel.getStyleClass().removeAll("priority-high", "priority-medium", "priority-low");
            priorityLabel.getStyleClass().add("priority-" + getPriorityClass(currentSchedule.getPriority()));

            // 分类
            categoryLabel.setText(currentSchedule.getCategory());

            // 标签
            if (currentSchedule.getTags() != null && !currentSchedule.getTags().isEmpty()) {
                tagsLabel.setText("标签: " + currentSchedule.getTags());
            } else {
                tagsLabel.setText("");
            }

            // 提醒
            if (currentSchedule.getReminderTime() != null) {
                reminderLabel.setText("⏰ 提醒: " + currentSchedule.getReminderTime().format(DATETIME_FORMATTER));
            } else {
                reminderLabel.setText("");
            }

            // 描述
            if (currentSchedule.getDescription() != null && !currentSchedule.getDescription().isEmpty()) {
                descriptionArea.setText(currentSchedule.getDescription());
            } else {
                descriptionArea.setText("暂无描述");
            }
        });
    }

    private void clearDisplay() {
        Platform.runLater(() -> {
            titleLabel.setText("请选择日程");
            statusLabel.setText("");
            dateLabel.setText("");
            priorityLabel.setText("");
            categoryLabel.setText("");
            tagsLabel.setText("");
            reminderLabel.setText("");
            descriptionArea.setText("暂无描述");
            setButtonsEnabled(false);
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        editButton.setDisable(!enabled);
        deleteButton.setDisable(!enabled);
        completeButton.setDisable(!enabled);
    }

    private void toggleComplete() {
        if (currentSchedule == null) return;

        try {
            boolean newStatus = !currentSchedule.isCompleted();
            scheduleDAO.updateScheduleStatus(currentSchedule.getId(), newStatus);
            currentSchedule.setCompleted(newStatus);
            updateDisplay();
            controller.refreshAllViews();
        } catch (SQLException e) {
            controller.showError("更新状态失败", e.getMessage());
        }
    }

    private void deleteSchedule() {
        if (currentSchedule == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除日程");
        alert.setContentText("确定要删除日程 \"" + currentSchedule.getName() + "\" 吗？此操作不可恢复。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    scheduleDAO.deleteSchedule(currentSchedule.getId());
                    currentSchedule = null;
                    clearDisplay();
                    controller.refreshAllViews();
                } catch (SQLException e) {
                    controller.showError("删除失败", e.getMessage());
                }
            }
        });
    }

    public void refresh() {
        if (currentSchedule != null) {
            // 重新从数据库加载
            try {
                Schedule updated = scheduleDAO.getScheduleById(currentSchedule.getId());
                if (updated != null) {
                    setSchedule(updated);
                } else {
                    // 日程已被删除
                    currentSchedule = null;
                    clearDisplay();
                }
            } catch (SQLException e) {
                controller.showError("刷新失败", e.getMessage());
            }
        }
    }

    private String getPriorityClass(String priority) {
        if ("高".equals(priority)) return "high";
        if ("低".equals(priority)) return "low";
        return "medium";
    }
}
