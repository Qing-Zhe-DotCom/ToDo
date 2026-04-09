package com.example.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import com.example.controller.MainController;
import com.example.model.Schedule;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ScheduleDialog extends Dialog<Schedule> {
    private MainController controller;
    private Schedule schedule;
    private boolean isEditMode;

    private TextField nameField;
    private TextArea descriptionArea;
    private DatePicker startDatePicker;
    private DatePicker dueDatePicker;
    private ToggleGroup priorityGroup;
    private TextField categoryField;
    private TextField tagsField;
    private ToggleButton reminderToggle;
    private DatePicker reminderDatePicker;
    private ComboBox<String> reminderTimeCombo;
    
    private String selectedColorHex = "#2196F3";
    private HBox colorPalette;
    
    private Label nameErrorLabel;
    private Label dateErrorLabel;

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
        DialogPane dialogPane = getDialogPane();
        dialogPane.getStyleClass().add("schedule-dialog-pane");
        
        // Hide the native header completely
        dialogPane.setHeaderText(null);
        dialogPane.setGraphic(null);
        dialogPane.setHeader(null);
        
        // Set window title for OS decoration
        setTitle(isEditMode ? text("schedule.dialog.editTitle") : text("schedule.dialog.createTitle"));

        ButtonType saveButtonType = new ButtonType(text("common.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(saveButtonType, cancelButtonType);
        dialogPane.getStylesheets().setAll(controller.getCurrentThemeStylesheets());
        controller.applyDialogPreferences(dialogPane);
        setOnShown(event -> applyDialogIcon());

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createScheduleFromForm();
            }
            return null;
        });
    }

    private void applyDialogIcon() {
        if (getDialogPane().getScene() == null) {
            return;
        }
        if (!(getDialogPane().getScene().getWindow() instanceof Stage)) {
            return;
        }
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.getIcons().setAll(controller.createCurrentAppIconImage(64));
    }

    private void initializeForm() {
        VBox rootBox = new VBox(24); // Increased from 20 for more breathing room
        rootBox.getStyleClass().add("schedule-dialog-root");
        rootBox.setPadding(new Insets(30, 40, 30, 40));
        
        // 1. 标题 (Hero Section)
        VBox titleBox = new VBox(2);
        nameField = new TextField();
        nameField.setPromptText(text("schedule.dialog.name.prompt"));
        nameField.getStyleClass().add("hero-title-input");
        nameErrorLabel = new Label(text("schedule.dialog.name.required"));
        nameErrorLabel.getStyleClass().add("error-label");
        nameErrorLabel.setVisible(false);
        nameErrorLabel.setManaged(false);
        titleBox.getChildren().addAll(nameField, nameErrorLabel);
        
        // 2. 描述 (Hero Section)
        descriptionArea = new TextArea();
        descriptionArea.setPromptText(text("schedule.dialog.description.prompt"));
        descriptionArea.getStyleClass().add("hero-desc-input");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        
        // 3. 时间设置 (并排)
        HBox dateBox = new HBox(24); // Increased from 20
        
        VBox startBox = new VBox(8);
        HBox.setHgrow(startBox, Priority.ALWAYS);
        Label startLabel = new Label(text("schedule.dialog.startDate"));
        startLabel.getStyleClass().add("field-label");
        startDatePicker = new DatePicker(LocalDate.now());
        startDatePicker.setMaxWidth(Double.MAX_VALUE);
        startDatePicker.getStyleClass().add("modern-input");
        DatePickerArrowSupport.install(startDatePicker, controller);
        startBox.getChildren().addAll(startLabel, startDatePicker);
        
        VBox dueBox = new VBox(8);
        HBox.setHgrow(dueBox, Priority.ALWAYS);
        Label dueLabel = new Label(text("schedule.dialog.dueDateRequired"));
        dueLabel.getStyleClass().add("field-label");
        dueDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);
        dueDatePicker.getStyleClass().add("modern-input");
        DatePickerArrowSupport.install(dueDatePicker, controller);
        dateErrorLabel = new Label(text("schedule.dialog.dueDateInvalid"));
        dateErrorLabel.getStyleClass().add("error-label");
        dateErrorLabel.setVisible(false);
        dateErrorLabel.setManaged(false);
        dueBox.getChildren().addAll(dueLabel, dueDatePicker, dateErrorLabel);
        
        dateBox.getChildren().addAll(startBox, dueBox);
        
        // 4. 优先级 (分段控制器)
        VBox priorityBox = new VBox(8);
        Label priorityLabel = new Label(text("schedule.dialog.priority"));
        priorityLabel.getStyleClass().add("field-label");
        
        HBox segmentedControl = new HBox();
        segmentedControl.getStyleClass().add("segmented-control");
        priorityGroup = new ToggleGroup();
        
        ToggleButton highBtn = new ToggleButton(controller.priorityDisplayName(Schedule.PRIORITY_HIGH));
        highBtn.setUserData(Schedule.PRIORITY_HIGH);
        highBtn.getStyleClass().addAll("segment-button", "segment-high");
        highBtn.setToggleGroup(priorityGroup);
        HBox.setHgrow(highBtn, Priority.ALWAYS);
        highBtn.setMaxWidth(Double.MAX_VALUE);
        
        ToggleButton midBtn = new ToggleButton(controller.priorityDisplayName(Schedule.PRIORITY_MEDIUM));
        midBtn.setUserData(Schedule.PRIORITY_MEDIUM);
        midBtn.getStyleClass().addAll("segment-button", "segment-mid");
        midBtn.setToggleGroup(priorityGroup);
        midBtn.setSelected(true);
        HBox.setHgrow(midBtn, Priority.ALWAYS);
        midBtn.setMaxWidth(Double.MAX_VALUE);
        
        ToggleButton lowBtn = new ToggleButton(controller.priorityDisplayName(Schedule.PRIORITY_LOW));
        lowBtn.setUserData(Schedule.PRIORITY_LOW);
        lowBtn.getStyleClass().addAll("segment-button", "segment-low");
        lowBtn.setToggleGroup(priorityGroup);
        HBox.setHgrow(lowBtn, Priority.ALWAYS);
        lowBtn.setMaxWidth(Double.MAX_VALUE);
        
        segmentedControl.getChildren().addAll(highBtn, midBtn, lowBtn);
        priorityBox.getChildren().addAll(priorityLabel, segmentedControl);
        
        // 5. 分类和标签 (并排)
        HBox metaBox = new HBox(24); // Increased from 20
        
        VBox categoryBox = new VBox(8);
        HBox.setHgrow(categoryBox, Priority.ALWAYS);
        Label categoryLabel = new Label(text("schedule.dialog.category"));
        categoryLabel.getStyleClass().add("field-label");
        categoryField = new TextField();
        categoryField.setPromptText(text("schedule.dialog.category.prompt"));
        categoryField.getStyleClass().add("modern-input");
        categoryBox.getChildren().addAll(categoryLabel, categoryField);
        
        VBox tagsBox = new VBox(8);
        HBox.setHgrow(tagsBox, Priority.ALWAYS);
        Label tagsLabel = new Label(text("schedule.dialog.tags"));
        tagsLabel.getStyleClass().add("field-label");
        tagsField = new TextField();
        tagsField.setPromptText(text("schedule.dialog.tags.prompt"));
        tagsField.getStyleClass().add("modern-input");
        tagsBox.getChildren().addAll(tagsLabel, tagsField);
        
        metaBox.getChildren().addAll(categoryBox, tagsBox);
        
        // 6. 颜色标记 (预设色板)
        VBox colorBox = new VBox(8);
        Label colorLabel = new Label(text("schedule.dialog.color"));
        colorLabel.getStyleClass().add("field-label");
        colorPalette = new HBox(12);
        colorPalette.setAlignment(Pos.CENTER_LEFT);
        
        List<String> presetColors = Arrays.asList("#2196F3", "#F44336", "#4CAF50", "#FF9800", "#FFC107", "#9C27B0", "#E91E63");
        for (String hex : presetColors) {
            Circle dot = new Circle(12, Color.web(hex));
            dot.setUserData(hex);
            dot.getStyleClass().add("color-dot");
            dot.setCursor(Cursor.HAND);
            if (hex.equals(selectedColorHex)) {
                dot.getStyleClass().add("color-dot-selected");
            }
            dot.setOnMouseClicked(e -> {
                selectedColorHex = hex;
                for (Node node : colorPalette.getChildren()) {
                    node.getStyleClass().remove("color-dot-selected");
                }
                dot.getStyleClass().add("color-dot-selected");
            });
            colorPalette.getChildren().add(dot);
        }
        colorBox.getChildren().addAll(colorLabel, colorPalette);
        
        // 7. 提醒设置 (拨动开关和展开区域)
        VBox reminderContainer = new VBox(10);
        reminderContainer.getStyleClass().add("reminder-container");
        
        HBox switchBox = new HBox(10);
        switchBox.setAlignment(Pos.CENTER_LEFT);
        Label reminderLabel = new Label(text("schedule.dialog.reminder"));
        reminderLabel.getStyleClass().add("field-label");
        reminderLabel.setStyle("-fx-padding: 0;");
        
        reminderToggle = new ToggleButton();
        reminderToggle.getStyleClass().add("modern-toggle-switch");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        switchBox.getChildren().addAll(reminderLabel, spacer, reminderToggle);
        
        HBox reminderDetails = new HBox(10);
        reminderDetails.setAlignment(Pos.CENTER_LEFT);
        reminderDetails.setVisible(false);
        reminderDetails.setManaged(false);
        
        reminderDatePicker = new DatePicker(LocalDate.now());
        reminderDatePicker.setMaxWidth(Double.MAX_VALUE);
        reminderDatePicker.getStyleClass().add("modern-input");
        DatePickerArrowSupport.install(reminderDatePicker, controller);
        HBox.setHgrow(reminderDatePicker, Priority.ALWAYS);
        
        reminderTimeCombo = new ComboBox<>();
        reminderTimeCombo.getItems().addAll(
            "08:00", "09:00", "10:00", "11:00", "12:00",
            "13:00", "14:00", "15:00", "16:00", "17:00",
            "18:00", "19:00", "20:00", "21:00"
        );
        reminderTimeCombo.setValue("09:00");
        reminderTimeCombo.setMaxWidth(Double.MAX_VALUE);
        reminderTimeCombo.getStyleClass().add("modern-input");
        HBox.setHgrow(reminderTimeCombo, Priority.ALWAYS);
        
        reminderDetails.getChildren().addAll(reminderDatePicker, reminderTimeCombo);
        
        reminderToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            reminderDetails.setVisible(newVal);
            reminderDetails.setManaged(newVal);
            if (newVal) {
                reminderToggle.getStyleClass().add("on");
            } else {
                reminderToggle.getStyleClass().remove("on");
            }
        });
        
        reminderContainer.getChildren().addAll(switchBox, reminderDetails);
        
        // 组装根容器
        rootBox.getChildren().addAll(
            titleBox,
            descriptionArea,
            dateBox,
            priorityBox,
            metaBox,
            colorBox,
            reminderContainer
        );
        
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(rootBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportHeight(550); // Set a preferred height to ensure content is scrollable and buttons are visible
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: -color-bg-panel;");
        
        getDialogPane().setContent(scrollPane);
        
        // Setup validation
        setupValidation();
        
        // Focus title
        Platform.runLater(() -> nameField.requestFocus());
    }

    private void setupValidation() {
        Button saveButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
        saveButton.getStyleClass().add("primary-save-button");
        
        Button cancelButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(1));
        cancelButton.getStyleClass().add("ghost-cancel-button");

        // Disable save button initially if new schedule and name is empty
        saveButton.disableProperty().bind(
            Bindings.createBooleanBinding(() -> 
                nameField.getText().trim().isEmpty() || dueDatePicker.getValue() == null,
                nameField.textProperty(), dueDatePicker.valueProperty()
            )
        );
        
        nameField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.trim().isEmpty()) {
                nameField.getStyleClass().add("error-border");
                nameErrorLabel.setVisible(true);
                nameErrorLabel.setManaged(true);
            } else {
                nameField.getStyleClass().remove("error-border");
                nameErrorLabel.setVisible(false);
                nameErrorLabel.setManaged(false);
            }
        });
        
        dueDatePicker.valueProperty().addListener((obs, oldV, newV) -> {
            validateDates();
        });
        
        startDatePicker.valueProperty().addListener((obs, oldV, newV) -> {
            validateDates();
        });
        
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateDates() || nameField.getText().trim().isEmpty()) {
                event.consume();
            }
        });
    }

    private boolean validateDates() {
        boolean valid = true;
        if (dueDatePicker.getValue() == null) {
            dateErrorLabel.setText(text("schedule.dialog.dueDate.required"));
            dueDatePicker.getStyleClass().add("error-border");
            dateErrorLabel.setVisible(true);
            dateErrorLabel.setManaged(true);
            valid = false;
        } else if (startDatePicker.getValue() != null && startDatePicker.getValue().isAfter(dueDatePicker.getValue())) {
            dateErrorLabel.setText(text("schedule.dialog.dateRange.invalid"));
            dueDatePicker.getStyleClass().add("error-border");
            dateErrorLabel.setVisible(true);
            dateErrorLabel.setManaged(true);
            valid = false;
        } else {
            dueDatePicker.getStyleClass().remove("error-border");
            dateErrorLabel.setVisible(false);
            dateErrorLabel.setManaged(false);
        }
        return valid;
    }

    private void loadScheduleData() {
        nameField.setText(schedule.getName());
        descriptionArea.setText(schedule.getDescription());
        startDatePicker.setValue(schedule.getStartDate());
        dueDatePicker.setValue(schedule.getDueDate());
        
        String priority = schedule.getPriority();
        if (priority != null) {
            for (javafx.scene.control.Toggle t : priorityGroup.getToggles()) {
                if (t.getUserData() != null && t.getUserData().toString().equals(priority)) {
                    priorityGroup.selectToggle(t);
                    break;
                }
            }
        }
        
        categoryField.setText(displayCategoryValue(schedule.getCategory()));
        tagsField.setText(schedule.getTags());

        if (schedule.getColor() != null && !schedule.getColor().isEmpty()) {
            selectedColorHex = schedule.getColor();
            for (Node node : colorPalette.getChildren()) {
                node.getStyleClass().remove("color-dot-selected");
                if (node.getUserData() != null && node.getUserData().toString().equalsIgnoreCase(selectedColorHex)) {
                    node.getStyleClass().add("color-dot-selected");
                }
            }
        }

        if (schedule.getReminderTime() != null) {
            reminderToggle.setSelected(true);
            reminderDatePicker.setValue(schedule.getReminderTime().toLocalDate());
            reminderTimeCombo.setValue(schedule.getReminderTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    private Schedule createScheduleFromForm() {
        Schedule result = isEditMode ? schedule : new Schedule();

        result.setName(nameField.getText().trim());
        result.setDescription(descriptionArea.getText());
        result.setStartDate(startDatePicker.getValue());
        result.setDueDate(dueDatePicker.getValue());
        
        if (priorityGroup.getSelectedToggle() != null) {
            result.setPriority(priorityGroup.getSelectedToggle().getUserData().toString());
        } else {
            result.setPriority(Schedule.PRIORITY_MEDIUM);
        }

        result.setCategory(resolveCategoryInput(categoryField.getText()));
        result.setTags(resolveTagsValue(tagsField.getText(), isEditMode));
        result.setColor(selectedColorHex);

        if (reminderToggle.isSelected()) {
            LocalTime time = LocalTime.parse(reminderTimeCombo.getValue());
            result.setReminderTime(LocalDateTime.of(reminderDatePicker.getValue(), time));
        } else {
            result.setReminderTime(null);
        }

        return result;
    }

    private String text(String key, Object... args) {
        return controller.text(key, args);
    }

    private String displayCategoryValue(String category) {
        return controller.categoryDisplayName(category);
    }

    private String resolveCategoryInput(String input) {
        return resolveCategoryValue(input, isEditMode, text("category.default"));
    }

    static String resolveCategoryValue(String input, boolean isEditMode) {
        return resolveCategoryValue(input, isEditMode, Schedule.DEFAULT_CATEGORY);
    }

    static String resolveCategoryValue(String input, boolean isEditMode, String localizedDefaultLabel) {
        String normalized = input != null ? input.trim() : "";
        if (normalized.equals(localizedDefaultLabel)) {
            normalized = "";
        }
        if (!normalized.isEmpty()) {
            return Schedule.normalizeCategory(normalized);
        }
        return isEditMode ? "" : Schedule.DEFAULT_CATEGORY;
    }

    static String resolveTagsValue(String input, boolean isEditMode) {
        String normalized = input != null ? input.trim() : "";
        if (!normalized.isEmpty()) {
            return Schedule.normalizeTags(normalized);
        }
        return isEditMode ? "" : "";
    }
}
