package com.example.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.example.controller.MainController;
import com.example.model.Schedule;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class ScheduleDialog extends Dialog<Schedule> {

    private MainController controller;
    private Schedule schedule;
    private boolean isEditMode;

    // 表单字段
    private TextField nameField;
    private TextArea descriptionArea;
    private DatePicker startDatePicker;
    private DatePicker dueDatePicker;
    private ComboBox<String> priorityCombo;
    private TextField categoryField;
    private TextField tagsField;
    private CheckBox reminderCheck;
    private DatePicker reminderDatePicker;
    private ComboBox<String> reminderTimeCombo;
    private ColorPicker colorPicker;

    public ScheduleDialog(Schedule schedule, MainController controller) {
        this.controller = controller;
        this.schedule = schedule;
        this.isEditMode = (schedule != null);

        initializeDialog();
        initializeForm();

        if (isEditMode) {
            loadScheduleData();
        }
    }

    private void initializeDialog() {
        setTitle(isEditMode ? "编辑日程" : "新建日程");
        setHeaderText(isEditMode ? "修改日程信息" : "创建新日程");

        // 设置对话框按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane dialogPane = getDialogPane();
        dialogPane.getStyleClass().add("schedule-dialog-pane");
        dialogPane.getButtonTypes().addAll(saveButtonType, cancelButtonType);
        dialogPane.getStylesheets().setAll(controller.getCurrentThemeStylesheets());

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createScheduleFromForm();
            }
            return null;
        });
    }

    private void initializeForm() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("schedule-dialog-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // 日程名称
        nameField = new TextField();
        nameField.setPromptText("输入日程名称");
        grid.add(new Label("名称*:"), 0, 0);
        grid.add(nameField, 1, 0);

        // 描述
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("输入日程描述（可选）");
        descriptionArea.setPrefRowCount(3);
        grid.add(new Label("描述:"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        // 开始日期
        startDatePicker = new DatePicker(LocalDate.now());
        grid.add(new Label("开始日期:"), 0, 2);
        grid.add(startDatePicker, 1, 2);

        // 截止日期
        dueDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        grid.add(new Label("截止日期*:"), 0, 3);
        grid.add(dueDatePicker, 1, 3);

        // 优先级
        priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll("高", "中", "低");
        priorityCombo.setValue("中");
        grid.add(new Label("优先级:"), 0, 4);
        grid.add(priorityCombo, 1, 4);

        // 分类
        categoryField = new TextField();
        categoryField.setPromptText("输入分类");
        grid.add(new Label("分类:"), 0, 5);
        grid.add(categoryField, 1, 5);

        // 标签
        tagsField = new TextField();
        tagsField.setPromptText("用逗号分隔多个标签");
        grid.add(new Label("标签:"), 0, 6);
        grid.add(tagsField, 1, 6);

        // 提醒设置
        HBox reminderBox = new HBox(10);
        reminderCheck = new CheckBox("设置提醒");
        reminderDatePicker = new DatePicker(LocalDate.now());
        reminderDatePicker.setDisable(true);
        reminderTimeCombo = new ComboBox<>();
        reminderTimeCombo.getItems().addAll(
            "08:00", "09:00", "10:00", "11:00", "12:00",
            "13:00", "14:00", "15:00", "16:00", "17:00",
            "18:00", "19:00", "20:00", "21:00"
        );
        reminderTimeCombo.setValue("09:00");
        reminderTimeCombo.setDisable(true);

        reminderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            reminderDatePicker.setDisable(!newVal);
            reminderTimeCombo.setDisable(!newVal);
        });

        reminderBox.getChildren().addAll(reminderCheck, reminderDatePicker, reminderTimeCombo);
        grid.add(new Label("提醒:"), 0, 7);
        grid.add(reminderBox, 1, 7);

        // 颜色选择
        colorPicker = new ColorPicker(Color.web("#2196F3"));
        grid.add(new Label("颜色标记:"), 0, 8);
        grid.add(colorPicker, 1, 8);

        // 设置列扩展
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(250);
        grid.getColumnConstraints().addAll(col1, col2);
        for (Node child : grid.getChildren()) {
            if (child instanceof Label) {
                child.getStyleClass().add("schedule-dialog-label");
            }
        }
        reminderCheck.getStyleClass().add("schedule-dialog-check");

        getDialogPane().setContent(grid);

        Button saveButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });
    }

    private void loadScheduleData() {
        nameField.setText(schedule.getName());
        descriptionArea.setText(schedule.getDescription());
        startDatePicker.setValue(schedule.getStartDate());
        dueDatePicker.setValue(schedule.getDueDate());
        priorityCombo.setValue(schedule.getPriority());
        categoryField.setText(schedule.getCategory());
        tagsField.setText(schedule.getTags());

        if (schedule.getColor() != null && !schedule.getColor().isEmpty()) {
            try {
                colorPicker.setValue(Color.web(schedule.getColor()));
            } catch (Exception e) {
                // 使用默认颜色
            }
        }

        if (schedule.getReminderTime() != null) {
            reminderCheck.setSelected(true);
            reminderDatePicker.setValue(schedule.getReminderTime().toLocalDate());
            reminderTimeCombo.setValue(schedule.getReminderTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    private boolean validateForm() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            controller.showError("验证失败", "日程名称不能为空");
            return false;
        }

        if (dueDatePicker.getValue() == null) {
            controller.showError("验证失败", "请选择截止日期");
            return false;
        }

        if (startDatePicker.getValue() != null && dueDatePicker.getValue() != null) {
            if (startDatePicker.getValue().isAfter(dueDatePicker.getValue())) {
                controller.showError("验证失败", "开始日期不能晚于截止日期");
                return false;
            }
        }

        return true;
    }

    private Schedule createScheduleFromForm() {
        Schedule result = isEditMode ? schedule : new Schedule();

        result.setName(nameField.getText().trim());
        result.setDescription(descriptionArea.getText());
        result.setStartDate(startDatePicker.getValue());
        result.setDueDate(dueDatePicker.getValue());
        result.setPriority(priorityCombo.getValue());
        result.setCategory(categoryField.getText() != null ? categoryField.getText().trim() : "默认");
        result.setTags(tagsField.getText());
        result.setColor(toHexString(colorPicker.getValue()));

        if (reminderCheck.isSelected()) {
            LocalTime time = LocalTime.parse(reminderTimeCombo.getValue());
            result.setReminderTime(LocalDateTime.of(reminderDatePicker.getValue(), time));
        } else {
            result.setReminderTime(null);
        }

        return result;
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
}
