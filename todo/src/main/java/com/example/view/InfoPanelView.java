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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class InfoPanelView implements ScheduleCompletionParticipant {
    private static final String EMPTY_TITLE_TEXT = "请选择日程";
    private static final String EMPTY_TIME_TEXT = "未设置时间";
    private static final List<String> STATUS_CLASSES = List.of(
        "info-panel-status-completed",
        "info-panel-status-overdue",
        "info-panel-status-upcoming",
        "info-panel-status-ongoing"
    );
    private static final DateTimeFormatter SUMMARY_FORMATTER = DateTimeFormatter.ofPattern("M月d日 HH:mm");
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy年");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final MainController controller;
    private final PauseTransition titleDelay = new PauseTransition(Duration.millis(400));
    private final PauseTransition categoryDelay = new PauseTransition(Duration.millis(400));
    private final PauseTransition tagsDelay = new PauseTransition(Duration.millis(400));
    private final PauseTransition notesDelay = new PauseTransition(Duration.millis(400));

    private VBox root;
    private Schedule currentSchedule;
    private Schedule persistedSchedule;
    private ParallelTransition panelTransition;
    private boolean panelVisible;
    private boolean suspend;

    private Button completeButton;
    private Button closeButton;
    private Button deleteButton;
    private Label statusLabel;
    private Label summaryPrimary;
    private Label summarySecondary;
    private TextField titleField;
    private CheckBox dueToggle;
    private DatePicker dueDate;
    private TextField dueTime;
    private CheckBox startToggle;
    private DatePicker startDate;
    private TextField startTime;
    private CheckBox reminderToggle;
    private DatePicker reminderDate;
    private TextField reminderTime;
    private ComboBox<String> priorityBox;
    private TextField categoryField;
    private TextField tagsField;
    private FlowPane chipPane;
    private TextArea notesArea;
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

        completeButton = new Button("完成");
        completeButton.getStyleClass().add("info-panel-complete-toggle");
        completeButton.setGraphic(controller.createSvgIcon("/icons/macaron_info-complete_icon.svg", null, 16));
        completeButton.setGraphicTextGap(8);
        completeButton.setContentDisplay(ContentDisplay.LEFT);
        completeButton.setOnAction(event -> toggleComplete());

        statusLabel = new Label();
        statusLabel.getStyleClass().add("info-panel-status-pill");
        closeButton = iconButton("/icons/macaron_info-close_icon.svg", "关闭详情", controller::closeScheduleDetails);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, completeButton, statusLabel, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("info-panel-header");

        titleField = new TextField();
        titleField.getStyleClass().add("info-panel-title-input");
        titleField.setPromptText("输入日程标题");
        summaryPrimary = new Label(EMPTY_TIME_TEXT);
        summaryPrimary.getStyleClass().add("info-panel-date-primary");
        summarySecondary = new Label();
        summarySecondary.getStyleClass().add("info-panel-date-secondary");

        dueToggle = rowToggle("截止时间");
        dueDate = rowDatePicker();
        dueTime = rowTimeField();
        startToggle = rowToggle("开始时间");
        startDate = rowDatePicker();
        startTime = rowTimeField();
        reminderToggle = rowToggle("提醒");
        reminderDate = rowDatePicker();
        reminderTime = rowTimeField();

        priorityBox = new ComboBox<>();
        priorityBox.getItems().setAll(Schedule.PRIORITY_HIGH, Schedule.PRIORITY_MEDIUM, Schedule.PRIORITY_LOW);
        priorityBox.getStyleClass().add("info-panel-combo");
        priorityBox.setMaxWidth(Double.MAX_VALUE);

        categoryField = textInput("未分类");
        tagsField = textInput("用逗号拆解标签");
        chipPane = new FlowPane();
        chipPane.setHgap(8);
        chipPane.setVgap(8);
        chipPane.getStyleClass().add("info-panel-chip-pane");
        chipPane.prefWrapLengthProperty().bind(root.widthProperty().subtract(56));

        notesArea = new TextArea();
        notesArea.getStyleClass().addAll("info-panel-notes-input", "info-panel-borderless-area");
        notesArea.setPrefRowCount(6);
        notesArea.setWrapText(true);
        categoryEditor = inlineEditor(categoryField);
        tagsEditor = inlineEditor(tagsField, chipPane);
        notesEditor = inlineEditor(notesArea);
        notesArea.setPromptText("补充备注、描述和上下文");

        deleteButton = new Button("删除日程");
        deleteButton.getStyleClass().add("info-panel-delete-button");
        deleteButton.setGraphic(controller.createSvgIcon("/icons/macaron_info-delete_icon.svg", null, 16));
        deleteButton.setGraphicTextGap(8);
        deleteButton.setContentDisplay(ContentDisplay.LEFT);
        deleteButton.setOnAction(event -> deleteSchedule());

        VBox content = new VBox(
            14,
            titleField,
            summaryPrimary,
            summarySecondary,
            section("时间", inlineRow(dueToggle, dueDate, dueTime), inlineRow(startToggle, startDate, startTime), inlineRow(reminderToggle, reminderDate, reminderTime)),
            section("优先级", priorityBox),
            section("任务", categoryField),
            section("标签", tagsField, chipPane),
            section("备注", notesArea),
            deleteButton
        );
        content.getStyleClass().add("info-panel-content");
        content.getChildren().setAll(
            titleField,
            summaryPrimary,
            summarySecondary,
            section("\u65f6\u95f4", inlineRow(dueToggle, dueDate, dueTime), inlineRow(startToggle, startDate, startTime), inlineRow(reminderToggle, reminderDate, reminderTime)),
            section("\u4f18\u5148\u7ea7", priorityBox),
            section("\u4efb\u52a1", categoryEditor),
            section("\u6807\u7b7e", tagsEditor),
            section("\u5907\u6ce8", notesEditor),
            deleteButton
        );

        ScrollPane scrollPane = new ScrollPane(content);
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
        tagsField.textProperty().addListener((obs, oldValue, newValue) -> { if (!suspend) updateChips(categoryField.getText(), newValue); });
        categoryField.textProperty().addListener((obs, oldValue, newValue) -> { if (!suspend) updateChips(newValue, tagsField.getText()); });
        notesArea.textProperty().addListener((obs, oldValue, newValue) -> { if (!suspend) { notesDelay.stop(); notesDelay.setOnFinished(e -> saveNotes()); notesDelay.playFromStart(); }});
        notesArea.focusedProperty().addListener((obs, oldValue, focused) -> { if (!focused && !suspend) saveNotes(); });

        priorityBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!suspend && newValue != null) save("保存优先级失败", draft -> draft.setPriority(newValue), false);
        });

        dueToggle.selectedProperty().addListener((obs, oldValue, selected) -> { if (!suspend) handleDueToggle(selected); });
        startToggle.selectedProperty().addListener((obs, oldValue, selected) -> { if (!suspend) handleStartToggle(selected); });
        reminderToggle.selectedProperty().addListener((obs, oldValue, selected) -> { if (!suspend) handleReminderToggle(selected); });

        dueDate.valueProperty().addListener((obs, oldValue, newValue) -> { if (!suspend && dueToggle.isSelected()) saveDue(); });
        startDate.valueProperty().addListener((obs, oldValue, newValue) -> { if (!suspend && startToggle.isSelected()) saveStart(); });
        reminderDate.valueProperty().addListener((obs, oldValue, newValue) -> { if (!suspend && reminderToggle.isSelected()) saveReminder(); });

        bindTimeField(dueTime, this::saveDue);
        bindTimeField(startTime, this::saveStart);
        bindTimeField(reminderTime, this::saveReminder);
    }

    private void bindTextField(TextField field, PauseTransition delay, Runnable saveAction) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (suspend) return;
            delay.stop();
            delay.setOnFinished(event -> saveAction.run());
            delay.playFromStart();
        });
        field.setOnAction(event -> saveAction.run());
        field.focusedProperty().addListener((obs, oldValue, focused) -> { if (!focused && !suspend) saveAction.run(); });
    }

    private void bindTimeField(TextField field, Runnable saveAction) {
        field.setOnAction(event -> saveAction.run());
        field.focusedProperty().addListener((obs, oldValue, focused) -> { if (!focused && !suspend) saveAction.run(); });
    }

    private void saveTitle() {
        String value = titleField.getText() == null ? "" : titleField.getText().strip();
        if (value.isEmpty()) {
            controller.showError("保存标题失败", "日程标题不能为空。");
            restorePersisted();
            return;
        }
        if (persistedSchedule != null && value.equals(persistedSchedule.getName())) return;
        save("保存标题失败", draft -> draft.setName(value), false);
    }

    private void saveCategory() {
        String value = categoryField.getText();
        if (persistedSchedule != null && Schedule.normalizeCategory(value).equals(persistedSchedule.getCategory())) return;
        save("保存任务失败", draft -> draft.setCategory(value), false);
    }

    private void saveTags() {
        String value = Schedule.normalizeTags(tagsField.getText());
        if (persistedSchedule != null && value.equals(persistedSchedule.getTags())) return;
        save("保存标签失败", draft -> draft.setTags(value), true);
    }

    private void saveNotes() {
        String value = notesArea.getText() == null ? "" : notesArea.getText();
        if (persistedSchedule != null && value.equals(persistedSchedule.getDescription())) return;
        save("保存备注失败", draft -> draft.setDescription(value), false);
    }

    private void handleDueToggle(boolean selected) {
        if (selected) {
            LocalDateTime value = currentSchedule != null && currentSchedule.getDueAt() != null ? currentSchedule.getDueAt() : LocalDate.now().atTime(23, 59);
            suspend = true;
            dueDate.setValue(value.toLocalDate());
            dueTime.setText(value.toLocalTime().format(TIME_FORMATTER));
            suspend = false;
            saveDue();
            return;
        }
        save("保存截止时间失败", draft -> draft.setDueAt(null), true);
    }

    private void handleStartToggle(boolean selected) {
        if (selected) {
            LocalDateTime value = currentSchedule != null && currentSchedule.getStartAt() != null ? currentSchedule.getStartAt() : LocalDate.now().atTime(9, 0);
            suspend = true;
            startDate.setValue(value.toLocalDate());
            startTime.setText(value.toLocalTime().format(TIME_FORMATTER));
            suspend = false;
            saveStart();
            return;
        }
        save("保存开始时间失败", draft -> draft.setStartAt(null), true);
    }

    private void handleReminderToggle(boolean selected) {
        if (selected) {
            LocalDateTime value = currentSchedule != null && currentSchedule.getReminderTime() != null
                ? currentSchedule.getReminderTime()
                : (currentSchedule != null && currentSchedule.getDueAt() != null ? currentSchedule.getDueAt() : LocalDate.now().atTime(9, 0));
            suspend = true;
            reminderDate.setValue(value.toLocalDate());
            reminderTime.setText(value.toLocalTime().format(TIME_FORMATTER));
            suspend = false;
            saveReminder();
            return;
        }
        save("保存提醒失败", draft -> draft.setReminderTime(null), true);
    }

    private void saveDue() {
        save("保存截止时间失败", draft -> draft.setDueAt(resolveDateTime(dueToggle, dueDate, dueTime, LocalTime.of(23, 59))), true);
    }

    private void saveStart() {
        save("保存开始时间失败", draft -> draft.setStartAt(resolveDateTime(startToggle, startDate, startTime, LocalTime.MIDNIGHT)), true);
    }

    private void saveReminder() {
        save("保存提醒失败", draft -> draft.setReminderTime(resolveDateTime(reminderToggle, reminderDate, reminderTime, LocalTime.of(9, 0))), true);
    }

    private void save(String errorTitle, Change change, boolean rerender) {
        if (currentSchedule == null || persistedSchedule == null) return;
        Schedule draft = copyOf(currentSchedule);
        try {
            change.apply(draft);
            if (!controller.saveSchedule(draft)) throw new SQLException("日程变更未被持久化。");
            currentSchedule = draft;
            persistedSchedule = copyOf(draft);
            if (rerender) renderForm(); else updateDerivedState();
            controller.refreshDataViews();
        } catch (IllegalArgumentException exception) {
            controller.showError(errorTitle, exception.getMessage());
            restorePersisted();
        } catch (SQLException exception) {
            controller.showError(errorTitle, exception.getMessage());
            restorePersisted();
        }
    }

    private void restorePersisted() {
        if (persistedSchedule == null) return;
        currentSchedule = copyOf(persistedSchedule);
        renderForm();
    }

    private void renderForm() {
        if (currentSchedule == null) {
            applyEmptyState();
            return;
        }
        titleDelay.stop();
        categoryDelay.stop();
        tagsDelay.stop();
        notesDelay.stop();
        suspend = true;
        titleField.setText(currentSchedule.getName());
        priorityBox.setValue(currentSchedule.getPriority());
        categoryField.setText(currentSchedule.getCategory());
        tagsField.setText(currentSchedule.getTags());
        notesArea.setText(currentSchedule.getDescription());
        dueToggle.setSelected(currentSchedule.getDueAt() != null);
        dueDate.setValue(currentSchedule.getDueAt() != null ? currentSchedule.getDueAt().toLocalDate() : null);
        dueTime.setText(currentSchedule.getDueAt() != null ? currentSchedule.getDueAt().toLocalTime().format(TIME_FORMATTER) : "");
        startToggle.setSelected(currentSchedule.getStartAt() != null);
        startDate.setValue(currentSchedule.getStartAt() != null ? currentSchedule.getStartAt().toLocalDate() : null);
        startTime.setText(currentSchedule.getStartAt() != null ? currentSchedule.getStartAt().toLocalTime().format(TIME_FORMATTER) : "");
        reminderToggle.setSelected(currentSchedule.getReminderTime() != null);
        reminderDate.setValue(currentSchedule.getReminderTime() != null ? currentSchedule.getReminderTime().toLocalDate() : null);
        reminderTime.setText(currentSchedule.getReminderTime() != null ? currentSchedule.getReminderTime().toLocalTime().format(TIME_FORMATTER) : "");
        suspend = false;
        updateEditorsEnabled();
        updateDerivedState();
    }

    private void applyEmptyState() {
        suspend = true;
        titleField.setText("");
        priorityBox.setValue(Schedule.DEFAULT_PRIORITY);
        categoryField.setText(Schedule.DEFAULT_CATEGORY);
        tagsField.setText("");
        notesArea.setText("");
        dueToggle.setSelected(false);
        dueDate.setValue(null);
        dueTime.setText("");
        startToggle.setSelected(false);
        startDate.setValue(null);
        startTime.setText("");
        reminderToggle.setSelected(false);
        reminderDate.setValue(null);
        reminderTime.setText("");
        suspend = false;
        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll(STATUS_CLASSES);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        summaryPrimary.setText(EMPTY_TIME_TEXT);
        summarySecondary.setText("");
        chipPane.getChildren().clear();
        completeButton.setText("完成");
        completeButton.setDisable(true);
        deleteButton.setDisable(true);
        setDisabled(true);
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
        completeButton.setText(currentSchedule.isCompleted() ? "标记未完成" : "完成");
        completeButton.setDisable(false);
        deleteButton.setDisable(false);
        DatePresentation presentation = buildDatePresentation(currentSchedule.getStartAt(), currentSchedule.getDueAt());
        summaryPrimary.setText(presentation.getPrimaryText());
        summarySecondary.setText(presentation.getSecondaryText());
        summarySecondary.setVisible(!presentation.getSecondaryText().isBlank());
        summarySecondary.setManaged(!presentation.getSecondaryText().isBlank());
        updateChips(currentSchedule.getCategory(), currentSchedule.getTags());
    }

    private void updateChips(String category, String tags) {
        chipPane.getChildren().clear();
        if (shouldShowCategoryChip(category)) chipPane.getChildren().add(chip(Schedule.normalizeCategory(category), "info-panel-chip-category"));
        for (String tag : splitTagChips(tags)) chipPane.getChildren().add(chip(tag, "info-panel-chip-tag"));
        chipPane.setVisible(!chipPane.getChildren().isEmpty());
        chipPane.setManaged(!chipPane.getChildren().isEmpty());
    }

    private void updateEditorsEnabled() {
        setDisabled(false);
        dueDate.setDisable(!dueToggle.isSelected());
        dueTime.setDisable(!dueToggle.isSelected());
        startDate.setDisable(!startToggle.isSelected());
        startTime.setDisable(!startToggle.isSelected());
        reminderDate.setDisable(!reminderToggle.isSelected());
        reminderTime.setDisable(!reminderToggle.isSelected());
    }

    private void setDisabled(boolean disabled) {
        titleField.setDisable(disabled);
        dueToggle.setDisable(disabled);
        startToggle.setDisable(disabled);
        reminderToggle.setDisable(disabled);
        priorityBox.setDisable(disabled);
        categoryField.setDisable(disabled);
        tagsField.setDisable(disabled);
        notesArea.setDisable(disabled);
    }

    private void toggleComplete() {
        if (currentSchedule != null) controller.updateScheduleCompletion(currentSchedule, !currentSchedule.isCompleted());
    }

    private void deleteSchedule() {
        if (currentSchedule == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除日程");
        alert.setContentText("确定要删除日程 \"" + currentSchedule.getName() + "\" 吗？");
        alert.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;
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

    private LocalDateTime resolveDateTime(CheckBox toggle, DatePicker picker, TextField field, LocalTime fallback) {
        if (!toggle.isSelected()) return null;
        LocalDate date = picker.getValue();
        if (date == null) throw new IllegalArgumentException("请先选择日期。");
        String text = field.getText() == null ? "" : field.getText().strip();
        LocalTime time = text.isEmpty() ? fallback : parseTime(text);
        return LocalDateTime.of(date, time);
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (Exception exception) {
            throw new IllegalArgumentException("时间请按 HH:mm 格式输入。");
        }
    }

    private Button iconButton(String iconPath, String tooltipText, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("info-panel-toolbar-button");
        button.setGraphic(controller.createSvgIcon(iconPath, tooltipText, 16));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> action.run());
        return button;
    }

    private CheckBox rowToggle(String text) {
        CheckBox box = new CheckBox(text);
        box.getStyleClass().add("info-panel-editor-toggle");
        return box;
    }

    private DatePicker rowDatePicker() {
        DatePicker picker = new DatePicker();
        picker.getStyleClass().add("info-panel-date-picker");
        picker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(picker, Priority.ALWAYS);
        DatePickerArrowSupport.install(picker, controller);
        return picker;
    }

    private TextField rowTimeField() {
        TextField field = new TextField();
        field.getStyleClass().add("info-panel-time-input");
        field.setPromptText("HH:mm");
        field.setPrefColumnCount(5);
        return field;
    }

    private TextField textInput(String prompt) {
        TextField field = new TextField();
        field.getStyleClass().addAll("info-panel-text-input", "info-panel-borderless-field");
        field.setPromptText(prompt);
        return field;
    }

    private HBox inlineRow(Node a, Node b, Node c) {
        HBox row = new HBox(8, a, b, c);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("info-panel-inline-row");
        HBox.setHgrow(b, Priority.ALWAYS);
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
            boolean hovered = interactive && wrapper.isHover() && !focusTarget.isFocused();
            boolean active = interactive && focusTarget.isFocused();
            toggleStyleClass(wrapper, "info-panel-inline-editor-hover", hovered);
            toggleStyleClass(wrapper, "info-panel-inline-editor-active", active);
        };
        wrapper.hoverProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        focusTarget.focusedProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        focusTarget.disableProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
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

    private void requestEditorFocus(Node editor) {
        editor.requestFocus();
        if (editor instanceof TextField field) {
            field.positionCaret(field.getText().length());
        } else if (editor instanceof TextArea area) {
            area.positionCaret(area.getText().length());
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
    private interface Change { void apply(Schedule schedule); }

    static final class DatePresentation {
        private final String primaryText;
        private final String secondaryText;
        DatePresentation(String primaryText, String secondaryText) {
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
        }
        String getPrimaryText() { return primaryText; }
        String getSecondaryText() { return secondaryText; }
    }
}
