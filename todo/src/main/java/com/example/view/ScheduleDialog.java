package com.example.view;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import com.example.controller.MainController;
import com.example.model.RecurrenceRule;
import com.example.model.Reminder;
import com.example.model.Schedule;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class ScheduleDialog extends Dialog<Schedule> {
    private static final String APP_ICON_RESOURCE = "/icons/macaron_todo_icon.png";

    private final MainController controller;
    private final Schedule schedule;
    private final boolean isEditMode;
    private final IosWheelDateTimePopup wheelPopup;
    private final List<Reminder> reminderDrafts = new ArrayList<>();

    private TextField nameField;
    private TextArea descriptionArea;
    private DatePicker startDatePicker;
    private DatePicker dueDatePicker;
    private ToggleGroup priorityGroup;
    private TextField categoryField;
    private TextField tagsField;
    private ToggleButton reminderToggle;
    private VBox reminderEditor;
    private VBox reminderListBox;
    private Button addReminderButton;
    private Label recurrenceSummaryLabel;
    private Button recurrenceEditButton;
    private Button recurrenceClearButton;
    private RecurrenceRule recurrenceDraft;

    private String selectedColorHex = "#2196F3";
    private HBox colorPalette;

    private Label nameErrorLabel;
    private Label dateErrorLabel;

    public ScheduleDialog(Schedule schedule, MainController controller) {
        this.controller = controller;
        this.schedule = schedule;
        this.isEditMode = schedule != null;
        this.wheelPopup = new IosWheelDateTimePopup(controller);

        initializeDialog();
        initializeForm();
        loadScheduleData();
    }

    private void initializeDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getStyleClass().add("schedule-dialog-pane");
        dialogPane.setHeaderText(null);
        dialogPane.setGraphic(null);
        dialogPane.setHeader(null);
        setTitle(isEditMode ? text("schedule.dialog.editTitle") : text("schedule.dialog.createTitle"));

        ButtonType saveButtonType = new ButtonType(text("common.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(saveButtonType, cancelButtonType);
        dialogPane.getStylesheets().setAll(controller.getCurrentThemeStylesheets());
        controller.applyDialogPreferences(dialogPane);
        setOnShown(event -> applyDialogIcon());
        setOnHidden(event -> wheelPopup.hide());

        setResultConverter(dialogButton -> dialogButton == saveButtonType ? createScheduleFromForm() : null);
    }

    private void applyDialogIcon() {
        if (getDialogPane().getScene() == null) {
            return;
        }
        if (!(getDialogPane().getScene().getWindow() instanceof Stage stage)) {
            return;
        }
        try (InputStream iconStream = getClass().getResourceAsStream(APP_ICON_RESOURCE)) {
            if (iconStream != null) {
                stage.getIcons().setAll(new Image(iconStream));
            }
        } catch (Exception ignored) {
        }
    }

    private void initializeForm() {
        VBox rootBox = new VBox(24);
        rootBox.getStyleClass().add("schedule-dialog-root");
        rootBox.setPadding(new Insets(30, 40, 30, 40));

        VBox titleBox = new VBox(2);
        nameField = new TextField();
        nameField.setPromptText(text("schedule.dialog.name.prompt"));
        nameField.getStyleClass().add("hero-title-input");
        nameErrorLabel = new Label(text("schedule.dialog.name.required"));
        nameErrorLabel.getStyleClass().add("error-label");
        nameErrorLabel.setVisible(false);
        nameErrorLabel.setManaged(false);
        titleBox.getChildren().addAll(nameField, nameErrorLabel);

        descriptionArea = new TextArea();
        descriptionArea.setPromptText(text("schedule.dialog.description.prompt"));
        descriptionArea.getStyleClass().add("hero-desc-input");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);

        HBox dateBox = new HBox(24);
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

        VBox priorityBox = new VBox(8);
        Label priorityLabel = new Label(text("schedule.dialog.priority"));
        priorityLabel.getStyleClass().add("field-label");
        HBox segmentedControl = new HBox();
        segmentedControl.getStyleClass().add("segmented-control");
        priorityGroup = new ToggleGroup();
        segmentedControl.getChildren().addAll(
            createPriorityToggle(Schedule.PRIORITY_HIGH, "segment-high"),
            createPriorityToggle(Schedule.PRIORITY_MEDIUM, "segment-mid"),
            createPriorityToggle(Schedule.PRIORITY_LOW, "segment-low")
        );
        priorityBox.getChildren().addAll(priorityLabel, segmentedControl);

        HBox metaBox = new HBox(24);
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
        tagsField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                commitTagEntryIntoField();
            }
        });
        tagsBox.getChildren().addAll(tagsLabel, tagsField);
        metaBox.getChildren().addAll(categoryBox, tagsBox);

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
            dot.setOnMouseClicked(e -> selectColor(hex, dot));
            colorPalette.getChildren().add(dot);
        }
        colorBox.getChildren().addAll(colorLabel, colorPalette);

        VBox reminderContainer = buildReminderContainer();
        VBox recurrenceContainer = buildRecurrenceContainer();

        rootBox.getChildren().addAll(
            titleBox,
            descriptionArea,
            dateBox,
            priorityBox,
            metaBox,
            colorBox,
            reminderContainer,
            recurrenceContainer
        );

        ScrollPane scrollPane = new ScrollPane(rootBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportHeight(550);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: -color-bg-panel;");
        getDialogPane().setContent(scrollPane);

        setupValidation();
        Platform.runLater(() -> nameField.requestFocus());
    }

    private ToggleButton createPriorityToggle(String value, String styleClass) {
        ToggleButton button = new ToggleButton(controller.priorityDisplayName(value));
        button.setUserData(value);
        button.getStyleClass().addAll("segment-button", styleClass);
        button.setToggleGroup(priorityGroup);
        HBox.setHgrow(button, Priority.ALWAYS);
        button.setMaxWidth(Double.MAX_VALUE);
        if (Schedule.PRIORITY_MEDIUM.equals(value)) {
            button.setSelected(true);
        }
        return button;
    }

    private VBox buildReminderContainer() {
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

        reminderListBox = new VBox(8);
        reminderListBox.getStyleClass().add("info-panel-reminder-list");

        addReminderButton = new Button(text("info.reminder.add"));
        addReminderButton.getStyleClass().addAll("button-secondary", "info-panel-secondary-action");
        addReminderButton.setOnAction(event -> addReminder());

        reminderEditor = new VBox(8, reminderListBox, addReminderButton);
        reminderEditor.getStyleClass().add("info-panel-reminder-editor");

        reminderToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            toggleStyleClass(reminderToggle, "on", Boolean.TRUE.equals(newVal));
            updateReminderEditorState();
        });

        reminderContainer.getChildren().addAll(switchBox, reminderEditor);
        return reminderContainer;
    }

    private VBox buildRecurrenceContainer() {
        VBox recurrenceContainer = new VBox(10);
        recurrenceContainer.getStyleClass().add("reminder-container");

        Label recurrenceLabel = new Label(text("info.section.recurrence"));
        recurrenceLabel.getStyleClass().add("field-label");

        recurrenceSummaryLabel = new Label(text("recurrence.none"));
        recurrenceSummaryLabel.getStyleClass().add("info-panel-recurrence-summary");
        recurrenceSummaryLabel.setWrapText(true);

        recurrenceEditButton = new Button(text("recurrence.edit"));
        recurrenceEditButton.getStyleClass().addAll("button-secondary", "info-panel-secondary-action");
        recurrenceEditButton.setOnAction(event -> editRecurrenceRule());

        recurrenceClearButton = new Button(text("recurrence.clear"));
        recurrenceClearButton.getStyleClass().addAll("button-secondary", "info-panel-secondary-action");
        recurrenceClearButton.setOnAction(event -> {
            recurrenceDraft = null;
            updateRecurrenceEditor();
        });

        HBox recurrenceActions = new HBox(8, recurrenceEditButton, recurrenceClearButton);
        recurrenceActions.setAlignment(Pos.CENTER_LEFT);
        recurrenceContainer.getChildren().addAll(recurrenceLabel, recurrenceSummaryLabel, recurrenceActions);
        return recurrenceContainer;
    }

    private void selectColor(String hex, Circle selectedDot) {
        selectedColorHex = hex;
        for (Node node : colorPalette.getChildren()) {
            node.getStyleClass().remove("color-dot-selected");
        }
        selectedDot.getStyleClass().add("color-dot-selected");
    }

    private void loadScheduleData() {
        if (!isEditMode || schedule == null) {
            recurrenceDraft = null;
            reminderDrafts.clear();
            reminderToggle.setSelected(false);
            renderReminderList();
            updateReminderEditorState();
            updateRecurrenceEditor();
            return;
        }

        nameField.setText(schedule.getName());
        descriptionArea.setText(schedule.getDescription());
        startDatePicker.setValue(schedule.getStartDate());
        dueDatePicker.setValue(schedule.getDueDate());

        String priority = schedule.getPriority();
        if (priority != null) {
            for (javafx.scene.control.Toggle toggle : priorityGroup.getToggles()) {
                if (toggle.getUserData() != null && Objects.equals(toggle.getUserData().toString(), priority)) {
                    priorityGroup.selectToggle(toggle);
                    break;
                }
            }
        }

        categoryField.setText(displayCategoryValue(schedule.getCategory()));
        tagsField.setText(schedule.getTags());
        selectCurrentColor(schedule.getColor());

        reminderDrafts.clear();
        List<Reminder> persistedReminders = schedule.getReminders();
        if (persistedReminders != null && !persistedReminders.isEmpty()) {
            reminderDrafts.addAll(normalizeReminderDrafts(persistedReminders));
        } else if (schedule.getReminderTime() != null) {
            reminderDrafts.add(new Reminder(schedule.getReminderTime()));
        }
        reminderToggle.setSelected(!reminderDrafts.isEmpty());
        renderReminderList();
        updateReminderEditorState();

        recurrenceDraft = schedule.getRecurrenceRule();
        updateRecurrenceEditor();
    }

    private void selectCurrentColor(String color) {
        if (color != null && !color.isBlank()) {
            selectedColorHex = color;
        }
        for (Node node : colorPalette.getChildren()) {
            node.getStyleClass().remove("color-dot-selected");
            if (node.getUserData() != null && node.getUserData().toString().equalsIgnoreCase(selectedColorHex)) {
                node.getStyleClass().add("color-dot-selected");
            }
        }
    }

    private void setupValidation() {
        Button saveButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().getFirst());
        saveButton.getStyleClass().add("primary-save-button");
        Button cancelButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(1));
        cancelButton.getStyleClass().add("ghost-cancel-button");

        saveButton.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> nameField.getText().trim().isEmpty() || dueDatePicker.getValue() == null,
                nameField.textProperty(),
                dueDatePicker.valueProperty()
            )
        );

        nameField.textProperty().addListener((obs, oldV, newV) -> {
            boolean empty = newV.trim().isEmpty();
            toggleStyleClass(nameField, "error-border", empty);
            nameErrorLabel.setVisible(empty);
            nameErrorLabel.setManaged(empty);
        });

        dueDatePicker.valueProperty().addListener((obs, oldV, newV) -> validateDates());
        startDatePicker.valueProperty().addListener((obs, oldV, newV) -> validateDates());

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
            toggleStyleClass(dueDatePicker, "error-border", true);
            dateErrorLabel.setVisible(true);
            dateErrorLabel.setManaged(true);
            valid = false;
        } else if (startDatePicker.getValue() != null && startDatePicker.getValue().isAfter(dueDatePicker.getValue())) {
            dateErrorLabel.setText(text("schedule.dialog.dateRange.invalid"));
            toggleStyleClass(dueDatePicker, "error-border", true);
            dateErrorLabel.setVisible(true);
            dateErrorLabel.setManaged(true);
            valid = false;
        } else {
            toggleStyleClass(dueDatePicker, "error-border", false);
            dateErrorLabel.setVisible(false);
            dateErrorLabel.setManaged(false);
        }
        return valid;
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
            List<Reminder> normalized = normalizeReminderDrafts(reminderDrafts);
            if (normalized.isEmpty()) {
                normalized = List.of(new Reminder(resolveDefaultReminderSeed(dueDatePicker.getValue(), startDatePicker.getValue())));
            }
            result.setReminders(normalized);
        } else {
            result.setReminders(List.of());
        }

        result.setRecurrenceRule(recurrenceDraft);
        return result;
    }

    private void addReminder() {
        LocalDateTime seed = resolveDefaultReminderSeed(dueDatePicker.getValue(), startDatePicker.getValue());
        openReminderPicker(addReminderButton, seed, value -> {
            reminderDrafts.add(new Reminder(value));
            reminderToggle.setSelected(true);
            renderReminderList();
            updateReminderEditorState();
        });
    }

    private void editReminder(Reminder target, Button owner) {
        if (target == null) {
            return;
        }
        openReminderPicker(owner, target.getRemindAtUtc(), value -> {
            target.setRemindAtUtc(value);
            renderReminderList();
        });
    }

    private void removeReminder(String reminderId) {
        reminderDrafts.removeIf(reminder -> Objects.equals(reminder.getId(), reminderId));
        renderReminderList();
        if (reminderDrafts.isEmpty()) {
            reminderToggle.setSelected(false);
        }
        updateReminderEditorState();
    }

    private void openReminderPicker(Button owner, LocalDateTime seed, java.util.function.Consumer<LocalDateTime> onSave) {
        wheelPopup.show(owner, text("info.reminder"), seed, value -> {
            wheelPopup.hide();
            if (value != null && onSave != null) {
                onSave.accept(value);
            }
        });
    }

    private void renderReminderList() {
        reminderListBox.getChildren().clear();
        List<Reminder> normalized = normalizeReminderDrafts(reminderDrafts);
        reminderDrafts.clear();
        reminderDrafts.addAll(normalized);

        int index = 1;
        for (Reminder reminder : reminderDrafts) {
            Button editButton = new Button(text("info.reminder.item", index, formatReminderButtonText(reminder)));
            editButton.getStyleClass().addAll("button-secondary", "info-panel-reminder-button");
            editButton.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(editButton, Priority.ALWAYS);
            editButton.setOnAction(event -> editReminder(reminder, editButton));

            Button removeButton = new Button(text("common.remove"));
            removeButton.getStyleClass().addAll("button-secondary", "info-panel-secondary-action");
            removeButton.setOnAction(event -> removeReminder(reminder.getId()));

            HBox row = new HBox(8, editButton, removeButton);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("info-panel-reminder-row");
            reminderListBox.getChildren().add(row);
            index++;
        }
    }

    private void updateReminderEditorState() {
        boolean enabled = reminderToggle.isSelected();
        reminderEditor.setVisible(enabled);
        reminderEditor.setManaged(enabled);
        addReminderButton.setDisable(!enabled);
    }

    private String formatReminderButtonText(Reminder reminder) {
        if (reminder == null || reminder.getRemindAtUtc() == null) {
            return text("common.unset");
        }
        InfoPanelView.TimeTriggerPresentation presentation =
            InfoPanelView.buildTimeTriggerPresentation(controller, reminder.getRemindAtUtc(), false);
        if (presentation.getSecondaryText().isBlank()) {
            return presentation.getPrimaryText();
        }
        return presentation.getPrimaryText() + " | " + presentation.getSecondaryText();
    }

    private void editRecurrenceRule() {
        LocalDateTime seed = resolveDefaultReminderSeed(dueDatePicker.getValue(), startDatePicker.getValue());
        RecurrenceRuleDialog dialog = new RecurrenceRuleDialog(
            controller,
            recurrenceDraft,
            seed,
            resolveTimezone()
        );
        dialog.showAndWait().ifPresent(result -> {
            recurrenceDraft = result.getRule();
            updateRecurrenceEditor();
        });
    }

    private void updateRecurrenceEditor() {
        recurrenceSummaryLabel.setText(
            recurrenceDraft != null && recurrenceDraft.isActive()
                ? controller.recurrenceSummary(recurrenceDraft)
                : text("recurrence.none")
        );
        recurrenceClearButton.setDisable(recurrenceDraft == null);
    }

    private String resolveTimezone() {
        if (schedule != null && schedule.getTimezone() != null && !schedule.getTimezone().isBlank()) {
            return schedule.getTimezone();
        }
        return TimeZone.getDefault().toZoneId().getId();
    }

    static List<Reminder> normalizeReminderDrafts(List<Reminder> reminders) {
        if (reminders == null || reminders.isEmpty()) {
            return List.of();
        }
        List<Reminder> normalized = new ArrayList<>();
        for (Reminder reminder : reminders) {
            if (reminder == null || reminder.getRemindAtUtc() == null) {
                continue;
            }
            normalized.add(reminder.copy());
        }
        normalized.sort(Comparator.comparing(Reminder::getRemindAtUtc));
        return normalized;
    }

    static LocalDateTime resolveDefaultReminderSeed(LocalDate dueDate, LocalDate startDate) {
        if (dueDate != null) {
            return dueDate.atTime(23, 59);
        }
        if (startDate != null) {
            return startDate.atTime(9, 0);
        }
        return LocalDate.now().atTime(9, 0);
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

    private void commitTagEntryIntoField() {
        String committed = commitTagEntryValue(tagsField.getText());
        tagsField.setText(committed);
        tagsField.positionCaret(committed.length());
    }

    static String commitTagEntryValue(String rawValue) {
        String current = rawValue == null ? "" : rawValue;
        int delimiter = lastTagDelimiterIndex(current);
        String head = delimiter >= 0 ? trimTrailingDelimiters(current.substring(0, delimiter)) : "";
        String candidate = delimiter >= 0 ? trimTrailingDelimiters(current.substring(delimiter + 1)) : trimTrailingDelimiters(current);
        List<String> tags = new ArrayList<>(Schedule.splitTags(head));
        for (String token : Schedule.splitTags(candidate)) {
            if (!token.isEmpty() && !tags.contains(token)) {
                tags.add(token);
            }
        }
        return tags.isEmpty() ? "" : String.join(", ", tags);
    }

    private static final char[] TAG_DELIMITERS = {',', '\uFF0C'};

    private static int lastTagDelimiterIndex(String value) {
        int index = -1;
        for (char delimiter : TAG_DELIMITERS) {
            index = Math.max(index, value.lastIndexOf(delimiter));
        }
        return index;
    }

    private static String trimTrailingDelimiters(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int length = value.length();
        while (length > 0 && isTagDelimiter(value.charAt(length - 1))) {
            length--;
        }
        return value.substring(0, length);
    }

    private static boolean isTagDelimiter(char candidate) {
        for (char delimiter : TAG_DELIMITERS) {
            if (delimiter == candidate) {
                return true;
            }
        }
        return false;
    }

    private void toggleStyleClass(Node node, String styleClass, boolean enabled) {
        if (enabled) {
            if (!node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
            return;
        }
        node.getStyleClass().remove(styleClass);
    }
}
