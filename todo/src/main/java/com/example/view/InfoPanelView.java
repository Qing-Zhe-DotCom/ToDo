package com.example.view;

import com.example.controller.MainController;
import com.example.controller.ScheduleCompletionMutation;
import com.example.model.Schedule;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class InfoPanelView implements ScheduleCompletionParticipant {
    private static final double TIME_TOGGLE_SLOT_WIDTH = 96.0;
    private static final double TIME_TRIGGER_WIDTH = 148.0;
    private static final double TIME_TRIGGER_HEIGHT = 54.0;
    private static final String TITLE_PROMPT = "输入日程标题";
    private static final String EMPTY_TITLE_TEXT = "请选择日程";
    private static final String EMPTY_TIME_TEXT = "未设置时间";
    private static final String UNSET_TRIGGER_TEXT = "未设置";
    private static final List<String> STATUS_CLASSES = List.of(
        "info-panel-status-completed",
        "info-panel-status-overdue",
        "info-panel-status-upcoming",
        "info-panel-status-ongoing"
    );
    private static final DateTimeFormatter SUMMARY_FORMATTER = DateTimeFormatter.ofPattern("M月d日 HH:mm");
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy年");

    private final MainController controller;
    private final IosWheelDateTimePopup wheelPopup = new IosWheelDateTimePopup();
    private final PauseTransition titleDelay = new PauseTransition(Duration.millis(400));
    private final PauseTransition categoryDelay = new PauseTransition(Duration.millis(400));
    private final PauseTransition tagsDelay = new PauseTransition(Duration.millis(400));
    private final PauseTransition notesDelay = new PauseTransition(Duration.millis(400));

    private VBox root;
    private ScrollPane scrollPane;
    private Schedule currentSchedule;
    private Schedule persistedSchedule;
    private ParallelTransition panelTransition;
    private boolean panelVisible;
    private boolean suspend;

    private ScheduleStatusControl completeControl;
    private Button closeButton;
    private Button deleteButton;
    private Label statusLabel;
    private FlowPane chipPane;
    private Label summaryPrimary;
    private Label summarySecondary;
    private TextField titleField;
    private CheckBox dueToggle;
    private Button dueTrigger;
    private Label dueTriggerTitle;
    private Label dueTriggerSubtitle;
    private CheckBox startToggle;
    private Button startTrigger;
    private Label startTriggerTitle;
    private Label startTriggerSubtitle;
    private CheckBox reminderToggle;
    private Button reminderTrigger;
    private Label reminderTriggerTitle;
    private Label reminderTriggerSubtitle;
    private ComboBox<String> priorityBox;
    private TextField categoryField;
    private TextField tagsField;
    private TextArea notesArea;
    private VBox priorityEditor;
    private VBox categoryEditor;
    private VBox tagsEditor;
    private VBox notesEditor;

    public InfoPanelView(MainController controller) {
        this.controller = controller;
        buildUi();
        wireListeners();
        applyEmptyState();
    }

    public Node getView() {
        return root;
    }

    public void focusTitleEditor() {
        if (titleField == null || currentSchedule == null) {
            return;
        }
        Platform.runLater(() -> {
            titleField.requestFocus();
            titleField.positionCaret(titleField.getText().length());
        });
    }

    public void showWithAnimation() {
        root.setManaged(true);
        root.setVisible(true);
        stopTransition();
        FadeTransition fade = new FadeTransition(Duration.millis(300), root);
        fade.setFromValue(root.getOpacity());
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), root);
        slide.setFromX(root.getTranslateX());
        slide.setToX(0);
        panelTransition = new ParallelTransition(fade, slide);
        panelTransition.setOnFinished(event -> panelVisible = true);
        panelTransition.play();
    }

    public void hideWithAnimation() {
        if (!panelVisible && !root.isVisible()) {
            return;
        }
        closeWheelPopup();
        stopTransition();
        FadeTransition fade = new FadeTransition(Duration.millis(300), root);
        fade.setFromValue(root.getOpacity());
        fade.setToValue(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), root);
        slide.setFromX(root.getTranslateX());
        slide.setToX(24);
        panelTransition = new ParallelTransition(fade, slide);
        panelTransition.setOnFinished(event -> hideImmediately());
        panelTransition.play();
    }

    public void hideImmediately() {
        closeWheelPopup();
        stopTransition();
        panelVisible = false;
        root.setOpacity(0);
        root.setTranslateX(24);
        root.setVisible(false);
        root.setManaged(false);
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    public void setSchedule(Schedule schedule) {
        closeWheelPopup();
        if (schedule == null) {
            currentSchedule = null;
            persistedSchedule = null;
            applyEmptyState();
            return;
        }
        currentSchedule = copyOf(schedule);
        persistedSchedule = copyOf(schedule);
        renderForm();
    }

    public void refresh() {
        closeWheelPopup();
        if (currentSchedule == null) {
            return;
        }
        try {
            Schedule updated = controller.findScheduleById(currentSchedule.getId());
            if (updated == null) {
                currentSchedule = null;
                persistedSchedule = null;
                applyEmptyState();
                return;
            }
            currentSchedule = copyOf(updated);
            persistedSchedule = copyOf(updated);
            renderForm();
        } catch (SQLException exception) {
            controller.showError("刷新失败", exception.getMessage());
        }
    }

    @Override
    public void applyCompletionMutation(ScheduleCompletionMutation mutation) {
        if (currentSchedule == null || mutation == null || !mutation.matches(currentSchedule)) {
            return;
        }
        mutation.applyTo(currentSchedule);
        updateDerivedState();
    }

    @Override
    public void confirmCompletionMutation(ScheduleCompletionMutation mutation) {
        if (currentSchedule == null || persistedSchedule == null || mutation == null || !mutation.matches(currentSchedule)) {
            return;
        }
        mutation.applyTo(persistedSchedule);
        updateDerivedState();
    }

    @Override
    public void revertCompletionMutation(ScheduleCompletionMutation mutation) {
        if (currentSchedule == null || mutation == null || !mutation.matches(currentSchedule)) {
            return;
        }
        mutation.revertOn(currentSchedule);
        updateDerivedState();
    }

    static DatePresentation buildDatePresentation(LocalDateTime startAt, LocalDateTime dueAt) {
        LocalDateTime start = startAt != null ? startAt : dueAt;
        LocalDateTime end = dueAt != null ? dueAt : startAt;
        if (start == null || end == null) {
            return new DatePresentation(EMPTY_TIME_TEXT, "");
        }
        if (start.isAfter(end)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }
        if (startAt == null && dueAt != null) {
            return new DatePresentation("截止 " + SUMMARY_FORMATTER.format(end), YEAR_FORMATTER.format(end));
        }
        if (startAt != null && dueAt == null) {
            return new DatePresentation("开始于 " + SUMMARY_FORMATTER.format(start), YEAR_FORMATTER.format(start));
        }
        String primary = SUMMARY_FORMATTER.format(start) + " - " + SUMMARY_FORMATTER.format(end);
        String secondary = start.getYear() == end.getYear()
            ? YEAR_FORMATTER.format(start)
            : YEAR_FORMATTER.format(start) + " - " + YEAR_FORMATTER.format(end);
        return new DatePresentation(primary, secondary);
    }

    static TimeTriggerPresentation buildTimeTriggerPresentation(LocalDateTime value) {
        if (value == null) {
            return new TimeTriggerPresentation(UNSET_TRIGGER_TEXT, "", true);
        }
        return new TimeTriggerPresentation(SUMMARY_FORMATTER.format(value), YEAR_FORMATTER.format(value), false);
    }

    static LocalDateTime defaultDueValue(LocalDate date) {
        return date.atTime(23, 59);
    }

    static LocalDateTime defaultStartValue(LocalDate date) {
        return date.atTime(9, 0);
    }

    static LocalDateTime defaultReminderValue(LocalDate date, LocalDateTime dueAt) {
        return dueAt != null ? dueAt : defaultStartValue(date);
    }

    static List<String> splitTagChips(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Collections.emptyList();
        }
        return Schedule.splitTags(rawTags);
    }

    static boolean shouldShowCategoryChip(String category) {
        return category != null && !category.isBlank() && !Schedule.isDefaultCategory(category);
    }

    private void buildUi() {
        root = new VBox();
        root.getStyleClass().add("info-panel");
        root.setPrefWidth(360);
        root.setMinWidth(340);
        root.setMaxWidth(420);

        completeControl = new ScheduleStatusControl(
            false,
            ScheduleStatusControl.SizePreset.DETAIL,
            "schedule-status-role-detail",
            targetCompleted -> currentSchedule != null && controller.updateScheduleCompletion(currentSchedule, targetCompleted)
        );
        completeControl.getStyleClass().add("info-panel-complete-control");

        deleteButton = actionIconButton(
            "/icons/macaron_info-delete_icon.svg",
            "删除日程",
            this::deleteSchedule,
            "info-panel-icon-button-danger",
            "info-panel-delete-button"
        );
        closeButton = iconButton("/icons/macaron_info-close_icon.svg", "关闭详情", controller::closeScheduleDetails);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionGroup = new HBox(10, completeControl, deleteButton);
        actionGroup.setAlignment(Pos.CENTER_LEFT);
        actionGroup.getStyleClass().add("info-panel-action-group");
        HBox header = new HBox(10, actionGroup, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().addAll("info-panel-header", "info-panel-action-row");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("info-panel-status-pill");

        chipPane = new FlowPane();
        chipPane.setHgap(8);
        chipPane.setVgap(8);
        chipPane.getStyleClass().addAll("info-panel-chip-pane", "info-panel-top-chip-pane");
        chipPane.prefWrapLengthProperty().bind(root.widthProperty().subtract(56));
        chipPane.setVisible(false);
        chipPane.setManaged(false);

        titleField = new TextField();
        titleField.getStyleClass().add("info-panel-title-input");
        titleField.setPromptText(TITLE_PROMPT);

        summaryPrimary = summaryLabel("info-panel-date-primary");
        summaryPrimary.setText(EMPTY_TIME_TEXT);
        summarySecondary = summaryLabel("info-panel-date-secondary");

        dueToggle = rowToggle("截止时间");
        dueTrigger = timeTrigger();
        dueTriggerTitle = triggerLabel("info-panel-time-trigger-title");
        dueTriggerSubtitle = triggerLabel("info-panel-time-trigger-subtitle");
        attachTriggerGraphic(dueTrigger, dueTriggerTitle, dueTriggerSubtitle);

        startToggle = rowToggle("开始时间");
        startTrigger = timeTrigger();
        startTriggerTitle = triggerLabel("info-panel-time-trigger-title");
        startTriggerSubtitle = triggerLabel("info-panel-time-trigger-subtitle");
        attachTriggerGraphic(startTrigger, startTriggerTitle, startTriggerSubtitle);

        reminderToggle = rowToggle("提醒");
        reminderTrigger = timeTrigger();
        reminderTriggerTitle = triggerLabel("info-panel-time-trigger-title");
        reminderTriggerSubtitle = triggerLabel("info-panel-time-trigger-subtitle");
        attachTriggerGraphic(reminderTrigger, reminderTriggerTitle, reminderTriggerSubtitle);

        priorityBox = new ComboBox<>();
        priorityBox.getItems().setAll(Schedule.PRIORITY_HIGH, Schedule.PRIORITY_MEDIUM, Schedule.PRIORITY_LOW);
        priorityBox.getStyleClass().addAll("info-panel-combo", "info-panel-borderless-combo");
        priorityBox.setMaxWidth(Double.MAX_VALUE);

        categoryField = textInput("未分类");
        tagsField = textInput("用逗号拆解标签");

        notesArea = new TextArea();
        notesArea.getStyleClass().addAll("info-panel-notes-input", "info-panel-borderless-area");
        notesArea.setPrefRowCount(6);
        notesArea.setWrapText(true);
        notesArea.setPromptText("补充备注、描述和上下文");

        priorityEditor = inlineEditor(priorityBox);
        categoryEditor = inlineEditor(categoryField);
        tagsEditor = inlineEditor(tagsField);
        notesEditor = inlineEditor(notesArea);

        VBox content = new VBox();
        content.getStyleClass().add("info-panel-content");
        content.getChildren().addAll(
            chipPane,
            titleField,
            summaryPrimary,
            summarySecondary,
            section("时间", timeRow(dueToggle, dueTrigger), timeRow(startToggle, startTrigger), timeRow(reminderToggle, reminderTrigger)),
            section("优先级", priorityEditor),
            section("任务", categoryEditor),
            section("标签", tagsEditor),
            section("备注", notesEditor)
        );

        scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("info-panel-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, scrollPane);
    }

    private void wireListeners() {
        bindTextField(titleField, titleDelay, this::saveTitle);
        bindTextField(categoryField, categoryDelay, this::saveCategory);
        bindTextField(tagsField, tagsDelay, this::saveTags);
        categoryField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!suspend) {
                updateChips(newValue, tagsField.getText());
            }
        });
        tagsField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!suspend) {
                updateChips(categoryField.getText(), newValue);
            }
        });
        notesArea.textProperty().addListener((obs, oldValue, newValue) -> {
            if (suspend) {
                return;
            }
            notesDelay.stop();
            notesDelay.setOnFinished(event -> saveNotes());
            notesDelay.playFromStart();
        });
        notesArea.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused && !suspend) {
                saveNotes();
            }
        });
        priorityBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!suspend && newValue != null) {
                save("保存优先级失败", draft -> draft.setPriority(newValue), false);
            }
        });

        dueToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (!suspend) {
                handleDueToggle(selected);
            }
        });
        startToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (!suspend) {
                handleStartToggle(selected);
            }
        });
        reminderToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (!suspend) {
                handleReminderToggle(selected);
            }
        });

        dueTrigger.setOnAction(event -> openDuePicker());
        startTrigger.setOnAction(event -> openStartPicker());
        reminderTrigger.setOnAction(event -> openReminderPicker());
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> closeWheelPopup());
    }

    private void bindTextField(TextField field, PauseTransition delay, Runnable saveAction) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (suspend) {
                return;
            }
            delay.stop();
            delay.setOnFinished(event -> saveAction.run());
            delay.playFromStart();
        });
        field.setOnAction(event -> saveAction.run());
        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused && !suspend) {
                saveAction.run();
            }
        });
    }

    private void saveTitle() {
        String value = titleField.getText() == null ? "" : titleField.getText().strip();
        if (value.isEmpty()) {
            controller.showError("保存标题失败", "日程标题不能为空。");
            restorePersisted();
            return;
        }
        if (persistedSchedule != null && value.equals(persistedSchedule.getName())) {
            return;
        }
        save("保存标题失败", draft -> draft.setName(value), false);
    }

    private void saveCategory() {
        String value = categoryField.getText();
        if (persistedSchedule != null && Schedule.normalizeCategory(value).equals(persistedSchedule.getCategory())) {
            return;
        }
        save("保存任务失败", draft -> draft.setCategory(value), false);
    }

    private void saveTags() {
        String value = Schedule.normalizeTags(tagsField.getText());
        if (persistedSchedule != null && value.equals(persistedSchedule.getTags())) {
            return;
        }
        save("保存标签失败", draft -> draft.setTags(value), true);
    }

    private void saveNotes() {
        String value = notesArea.getText() == null ? "" : notesArea.getText();
        if (persistedSchedule != null && value.equals(persistedSchedule.getDescription())) {
            return;
        }
        save("保存备注失败", draft -> draft.setDescription(value), false);
    }

    private void handleDueToggle(boolean selected) {
        closeWheelPopup();
        saveDue(selected ? (currentSchedule != null && currentSchedule.getDueAt() != null
            ? currentSchedule.getDueAt()
            : defaultDueValue(LocalDate.now())) : null);
    }

    private void handleStartToggle(boolean selected) {
        closeWheelPopup();
        saveStart(selected ? (currentSchedule != null && currentSchedule.getStartAt() != null
            ? currentSchedule.getStartAt()
            : defaultStartValue(LocalDate.now())) : null);
    }

    private void handleReminderToggle(boolean selected) {
        closeWheelPopup();
        saveReminder(selected ? (currentSchedule != null && currentSchedule.getReminderTime() != null
            ? currentSchedule.getReminderTime()
            : defaultReminderValue(LocalDate.now(), currentSchedule != null ? currentSchedule.getDueAt() : null)) : null);
    }

    private void openDuePicker() {
        if (currentSchedule != null && dueToggle.isSelected()) {
            openTimePicker("截止时间", dueTrigger, currentSchedule.getDueAt(), this::saveDue);
        }
    }

    private void openStartPicker() {
        if (currentSchedule != null && startToggle.isSelected()) {
            openTimePicker("开始时间", startTrigger, currentSchedule.getStartAt(), this::saveStart);
        }
    }

    private void openReminderPicker() {
        if (currentSchedule != null && reminderToggle.isSelected()) {
            openTimePicker("提醒", reminderTrigger, currentSchedule.getReminderTime(), this::saveReminder);
        }
    }

    private void openTimePicker(String title, Button owner, LocalDateTime currentValue, java.util.function.Consumer<LocalDateTime> saveAction) {
        LocalDateTime seed = currentValue != null ? currentValue : defaultStartValue(LocalDate.now());
        wheelPopup.show(owner, title, seed, value -> {
            closeWheelPopup();
            saveAction.accept(value);
        });
    }

    private void saveDue(LocalDateTime value) {
        if (persistedSchedule != null && Objects.equals(value, persistedSchedule.getDueAt())) {
            updateTimeTriggers();
            return;
        }
        save("保存截止时间失败", draft -> draft.setDueAt(value), true);
    }

    private void saveStart(LocalDateTime value) {
        if (persistedSchedule != null && Objects.equals(value, persistedSchedule.getStartAt())) {
            updateTimeTriggers();
            return;
        }
        save("保存开始时间失败", draft -> draft.setStartAt(value), true);
    }

    private void saveReminder(LocalDateTime value) {
        if (persistedSchedule != null && Objects.equals(value, persistedSchedule.getReminderTime())) {
            updateTimeTriggers();
            return;
        }
        save("保存提醒失败", draft -> draft.setReminderTime(value), true);
    }

    private void save(String errorTitle, Change change, boolean rerender) {
        if (currentSchedule == null || persistedSchedule == null) {
            return;
        }
        Schedule draft = copyOf(currentSchedule);
        try {
            change.apply(draft);
            if (!controller.saveSchedule(draft)) {
                throw new SQLException("日程变更未被持久化。");
            }
            currentSchedule = draft;
            persistedSchedule = copyOf(draft);
            if (rerender) {
                renderForm();
            } else {
                updateDerivedState();
            }
            controller.refreshDataViews();
        } catch (IllegalArgumentException | SQLException exception) {
            controller.showError(errorTitle, exception.getMessage());
            restorePersisted();
        }
    }

    private void restorePersisted() {
        closeWheelPopup();
        if (persistedSchedule == null) {
            return;
        }
        currentSchedule = copyOf(persistedSchedule);
        renderForm();
    }

    private void renderForm() {
        if (currentSchedule == null) {
            applyEmptyState();
            return;
        }
        closeWheelPopup();
        titleDelay.stop();
        categoryDelay.stop();
        tagsDelay.stop();
        notesDelay.stop();
        suspend = true;
        titleField.setPromptText(TITLE_PROMPT);
        titleField.setText(currentSchedule.getName());
        priorityBox.setValue(currentSchedule.getPriority());
        categoryField.setText(currentSchedule.getCategory());
        tagsField.setText(currentSchedule.getTags());
        notesArea.setText(currentSchedule.getDescription());
        dueToggle.setSelected(currentSchedule.getDueAt() != null);
        startToggle.setSelected(currentSchedule.getStartAt() != null);
        reminderToggle.setSelected(currentSchedule.getReminderTime() != null);
        suspend = false;
        updateEditorsEnabled();
        updateDerivedState();
    }

    private void applyEmptyState() {
        closeWheelPopup();
        suspend = true;
        titleField.setText("");
        priorityBox.setValue(Schedule.DEFAULT_PRIORITY);
        categoryField.setText(Schedule.DEFAULT_CATEGORY);
        tagsField.setText("");
        notesArea.setText("");
        dueToggle.setSelected(false);
        startToggle.setSelected(false);
        reminderToggle.setSelected(false);
        suspend = false;

        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll(STATUS_CLASSES);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        chipPane.getChildren().clear();
        chipPane.setVisible(false);
        chipPane.setManaged(false);

        summaryPrimary.setText(EMPTY_TIME_TEXT);
        summarySecondary.setText("");
        summarySecondary.setVisible(false);
        summarySecondary.setManaged(false);

        completeControl.syncCompleted(false);
        setDisabled(true);
        updateTimeTriggers();
        titleField.setPromptText(EMPTY_TITLE_TEXT);
    }

    private void updateDerivedState() {
        if (currentSchedule == null) {
            applyEmptyState();
            return;
        }
        statusLabel.getStyleClass().removeAll(STATUS_CLASSES);
        if (currentSchedule.isCompleted()) {
            statusLabel.setText("已完成");
            statusLabel.getStyleClass().add("info-panel-status-completed");
        } else if (currentSchedule.isOverdue()) {
            statusLabel.setText("已过期");
            statusLabel.getStyleClass().add("info-panel-status-overdue");
        } else if (currentSchedule.isUpcoming()) {
            statusLabel.setText("即将到期");
            statusLabel.getStyleClass().add("info-panel-status-upcoming");
        } else {
            statusLabel.setText("进行中");
            statusLabel.getStyleClass().add("info-panel-status-ongoing");
        }
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        completeControl.syncCompleted(currentSchedule.isCompleted());
        setDisabled(false);

        DatePresentation presentation = buildDatePresentation(currentSchedule.getStartAt(), currentSchedule.getDueAt());
        summaryPrimary.setText(presentation.getPrimaryText());
        summarySecondary.setText(presentation.getSecondaryText());
        summarySecondary.setVisible(!presentation.getSecondaryText().isBlank());
        summarySecondary.setManaged(!presentation.getSecondaryText().isBlank());

        updateTimeTriggers();
        updateChips(currentSchedule.getCategory(), currentSchedule.getTags());
    }

    private void updateChips(String category, String tags) {
        chipPane.getChildren().clear();
        if (statusLabel.isManaged() && !statusLabel.getText().isBlank()) {
            chipPane.getChildren().add(statusLabel);
        }
        if (shouldShowCategoryChip(category)) {
            chipPane.getChildren().add(chip(Schedule.normalizeCategory(category), "info-panel-chip-category"));
        }
        for (String tag : splitTagChips(tags)) {
            chipPane.getChildren().add(chip(tag, "info-panel-chip-tag"));
        }
        boolean hasContent = !chipPane.getChildren().isEmpty();
        chipPane.setVisible(hasContent);
        chipPane.setManaged(hasContent);
    }

    private void updateEditorsEnabled() {
        boolean disabled = currentSchedule == null;
        setDisabled(disabled);
        dueTrigger.setDisable(disabled || !dueToggle.isSelected());
        startTrigger.setDisable(disabled || !startToggle.isSelected());
        reminderTrigger.setDisable(disabled || !reminderToggle.isSelected());
    }

    private void updateTimeTriggers() {
        configureTimeTrigger(dueTrigger, dueTriggerTitle, dueTriggerSubtitle, currentSchedule != null && dueToggle.isSelected() ? currentSchedule.getDueAt() : null);
        configureTimeTrigger(startTrigger, startTriggerTitle, startTriggerSubtitle, currentSchedule != null && startToggle.isSelected() ? currentSchedule.getStartAt() : null);
        configureTimeTrigger(reminderTrigger, reminderTriggerTitle, reminderTriggerSubtitle, currentSchedule != null && reminderToggle.isSelected() ? currentSchedule.getReminderTime() : null);
        updateEditorsEnabled();
    }

    private void configureTimeTrigger(Button trigger, Label titleLabel, Label subtitleLabel, LocalDateTime value) {
        TimeTriggerPresentation presentation = buildTimeTriggerPresentation(value);
        titleLabel.setText(presentation.getPrimaryText());
        subtitleLabel.setText(presentation.getSecondaryText());
        subtitleLabel.setVisible(!presentation.getSecondaryText().isBlank());
        subtitleLabel.setManaged(!presentation.getSecondaryText().isBlank());
        toggleStyleClass(trigger, "info-panel-time-trigger-unset", presentation.isUnset());
    }

    private void setDisabled(boolean disabled) {
        completeControl.setDisable(disabled);
        deleteButton.setDisable(disabled);
        titleField.setDisable(disabled);
        dueToggle.setDisable(disabled);
        startToggle.setDisable(disabled);
        reminderToggle.setDisable(disabled);
        priorityBox.setDisable(disabled);
        categoryField.setDisable(disabled);
        tagsField.setDisable(disabled);
        notesArea.setDisable(disabled);
    }

    private void deleteSchedule() {
        closeWheelPopup();
        if (currentSchedule == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除日程");
        alert.setContentText("确定要删除日程 \"" + currentSchedule.getName() + "\" 吗？");
        alert.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }
            try {
                controller.removeSchedule(currentSchedule.getId());
                currentSchedule = null;
                persistedSchedule = null;
                applyEmptyState();
                controller.closeScheduleDetails();
                controller.refreshAllViews();
            } catch (SQLException exception) {
                controller.showError("删除失败", exception.getMessage());
            }
        });
    }

    private void closeWheelPopup() {
        wheelPopup.hide();
    }

    private Label summaryLabel(String styleClass) {
        Label label = new Label();
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Button iconButton(String iconPath, String tooltipText, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("info-panel-toolbar-button");
        button.setGraphic(controller.createSvgIcon(iconPath, tooltipText, 16));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button actionIconButton(String iconPath, String tooltipText, Runnable action, String... styleClasses) {
        Button button = new Button();
        button.getStyleClass().add("info-panel-icon-button");
        button.getStyleClass().addAll(styleClasses);
        button.setGraphic(controller.createSvgIcon(iconPath, tooltipText, 16));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> action.run());
        return button;
    }

    private CheckBox rowToggle(String text) {
        CheckBox box = new CheckBox(text);
        box.getStyleClass().add("info-panel-editor-toggle");
        box.setMinWidth(Region.USE_PREF_SIZE);
        box.setTextOverrun(OverrunStyle.CLIP);
        return box;
    }

    private Button timeTrigger() {
        Button button = new Button();
        button.getStyleClass().add("info-panel-time-trigger");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMinWidth(TIME_TRIGGER_WIDTH);
        button.setPrefWidth(TIME_TRIGGER_WIDTH);
        button.setMaxWidth(TIME_TRIGGER_WIDTH);
        button.setMinHeight(TIME_TRIGGER_HEIGHT);
        button.setPrefHeight(TIME_TRIGGER_HEIGHT);
        button.setMaxHeight(TIME_TRIGGER_HEIGHT);
        return button;
    }

    private void attachTriggerGraphic(Button trigger, Label titleLabel, Label subtitleLabel) {
        VBox textBox = new VBox(2, titleLabel, subtitleLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setFillWidth(false);
        textBox.setMouseTransparent(true);
        trigger.setGraphic(textBox);
    }

    private Label triggerLabel(String styleClass) {
        Label label = new Label();
        label.getStyleClass().add(styleClass);
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.CLIP);
        return label;
    }

    private TextField textInput(String prompt) {
        TextField field = new TextField();
        field.getStyleClass().addAll("info-panel-text-input", "info-panel-borderless-field");
        field.setPromptText(prompt);
        return field;
    }

    private GridPane timeRow(CheckBox toggle, Button trigger) {
        HBox toggleSlot = new HBox(toggle);
        toggleSlot.setAlignment(Pos.CENTER_LEFT);
        toggleSlot.setMinWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleSlot.setPrefWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleSlot.setMaxWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleSlot.getStyleClass().add("info-panel-time-toggle-slot");

        ColumnConstraints toggleColumn = new ColumnConstraints();
        toggleColumn.setMinWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleColumn.setPrefWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleColumn.setMaxWidth(TIME_TOGGLE_SLOT_WIDTH);

        ColumnConstraints triggerColumn = new ColumnConstraints();
        triggerColumn.setMinWidth(TIME_TRIGGER_WIDTH);
        triggerColumn.setPrefWidth(TIME_TRIGGER_WIDTH);
        triggerColumn.setMaxWidth(TIME_TRIGGER_WIDTH);

        GridPane row = new GridPane();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setHgap(6);
        row.setVgap(0);
        row.getStyleClass().add("info-panel-time-row");
        row.getColumnConstraints().addAll(toggleColumn, triggerColumn);
        row.add(toggleSlot, 0, 0);
        row.add(trigger, 1, 0);
        return row;
    }

    private VBox inlineEditor(Node editor, Node... supportingNodes) {
        VBox wrapper = new VBox(8);
        wrapper.getStyleClass().add("info-panel-inline-editor");
        wrapper.getChildren().add(editor);
        wrapper.getChildren().addAll(supportingNodes);
        installInlineEditorState(wrapper, editor);
        return wrapper;
    }

    private void installInlineEditorState(VBox wrapper, Node focusTarget) {
        Runnable refreshState = () -> {
            boolean interactive = !focusTarget.isDisabled();
            boolean active = interactive && isEditorActive(focusTarget);
            boolean hovered = interactive && wrapper.isHover() && !active;
            toggleStyleClass(wrapper, "info-panel-inline-editor-hover", hovered);
            toggleStyleClass(wrapper, "info-panel-inline-editor-active", active);
        };
        wrapper.hoverProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        focusTarget.focusedProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        focusTarget.disableProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        if (focusTarget instanceof ComboBoxBase<?> comboBoxBase) {
            comboBoxBase.showingProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        }
        wrapper.setOnMouseClicked(event -> {
            if (focusTarget.isDisabled()) {
                return;
            }
            if (event.getTarget() instanceof Node target && isDescendant(target, focusTarget)) {
                return;
            }
            requestEditorFocus(focusTarget);
        });
        refreshState.run();
    }

    private boolean isEditorActive(Node editor) {
        if (editor instanceof ComboBoxBase<?> comboBoxBase) {
            return comboBoxBase.isFocused() || comboBoxBase.isShowing();
        }
        return editor.isFocused();
    }

    private void requestEditorFocus(Node editor) {
        editor.requestFocus();
        if (editor instanceof TextField field) {
            field.positionCaret(field.getText().length());
        } else if (editor instanceof TextArea area) {
            area.positionCaret(area.getText().length());
        } else if (editor instanceof ComboBoxBase<?> comboBoxBase && !comboBoxBase.isShowing()) {
            comboBoxBase.show();
        }
    }

    private boolean isDescendant(Node node, Node possibleAncestor) {
        Node current = node;
        while (current != null) {
            if (current == possibleAncestor) {
                return true;
            }
            current = current.getParent();
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

    private VBox section(String title, Node... nodes) {
        Label label = new Label(title);
        label.getStyleClass().add("info-panel-section-title");
        VBox box = new VBox(10);
        box.getStyleClass().add("info-panel-editor-section");
        box.getChildren().add(label);
        box.getChildren().addAll(nodes);
        return box;
    }

    private Label chip(String text, String styleClass) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("info-panel-chip", styleClass);
        return chip;
    }

    private Schedule copyOf(Schedule source) {
        Schedule copy = new Schedule();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        copy.setStartAt(source.getStartAt());
        copy.setDueAt(source.getDueAt());
        copy.setCompleted(source.isCompleted());
        copy.setPriority(source.getPriority());
        copy.setCategory(source.getCategory());
        copy.setTags(source.getTags());
        copy.setReminderTime(source.getReminderTime());
        copy.setColor(source.getColor());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private void stopTransition() {
        if (panelTransition != null) {
            panelTransition.stop();
            panelTransition = null;
        }
    }

    @FunctionalInterface
    private interface Change {
        void apply(Schedule schedule);
    }

    static final class DatePresentation {
        private final String primaryText;
        private final String secondaryText;

        DatePresentation(String primaryText, String secondaryText) {
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
        }

        String getPrimaryText() {
            return primaryText;
        }

        String getSecondaryText() {
            return secondaryText;
        }
    }

    static final class TimeTriggerPresentation {
        private final String primaryText;
        private final String secondaryText;
        private final boolean unset;

        TimeTriggerPresentation(String primaryText, String secondaryText, boolean unset) {
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.unset = unset;
        }

        String getPrimaryText() {
            return primaryText;
        }

        String getSecondaryText() {
            return secondaryText;
        }

        boolean isUnset() {
            return unset;
        }
    }
}
