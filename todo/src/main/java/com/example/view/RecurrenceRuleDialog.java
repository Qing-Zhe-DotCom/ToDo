package com.example.view;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.example.controller.MainController;
import com.example.model.RecurrenceRule;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

final class RecurrenceRuleDialog extends Dialog<RecurrenceRuleDialog.Result> {
    private final RecurrenceRule existingRule;
    private final LocalDateTime defaultSeed;
    private final String timezone;

    private CheckBox enabledBox;
    private ComboBox<FrequencyOption> frequencyBox;
    private TextField intervalField;
    private Map<DayOfWeek, CheckBox> dayBoxes;
    private TextField monthDayField;
    private ComboBox<EndModeOption> endModeBox;
    private DatePicker untilDatePicker;
    private ComboBox<String> untilTimeBox;
    private TextField occurrenceCountField;
    private Label validationLabel;
    private VBox weeklyBox;
    private VBox monthDayBox;
    private HBox untilRow;
    private HBox countRow;
    private HBox baseRow;

    RecurrenceRuleDialog(
        MainController controller,
        RecurrenceRule existingRule,
        LocalDateTime defaultSeed,
        String timezone
    ) {
        this.existingRule = existingRule != null ? existingRule.copy() : null;
        this.defaultSeed = defaultSeed != null ? defaultSeed : LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
        this.timezone = timezone;

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("schedule-dialog-pane");
        pane.getStylesheets().setAll(controller.getCurrentThemeStylesheets());
        setTitle("编辑重复规则");
        setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(saveButtonType, cancelButtonType);
        pane.setContent(buildContent());

        Button saveButton = (Button) pane.lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                validationLabel.setText("");
                setResult(buildResult());
            } catch (IllegalArgumentException exception) {
                validationLabel.setText(exception.getMessage());
                event.consume();
            }
        });

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return getResult();
            }
            return null;
        });
    }

    private VBox buildContent() {
        enabledBox = new CheckBox("启用重复规则");
        enabledBox.setSelected(existingRule != null && existingRule.isActive());

        frequencyBox = new ComboBox<>();
        frequencyBox.getItems().setAll(
            new FrequencyOption(RecurrenceRule.FREQ_DAILY, "每天"),
            new FrequencyOption(RecurrenceRule.FREQ_WEEKLY, "每周"),
            new FrequencyOption(RecurrenceRule.FREQ_MONTHLY, "每月"),
            new FrequencyOption(RecurrenceRule.FREQ_YEARLY, "每年")
        );
        frequencyBox.setValue(findFrequencyOption(existingRule != null ? existingRule.getFrequency() : RecurrenceRule.FREQ_DAILY));
        frequencyBox.setMaxWidth(Double.MAX_VALUE);

        intervalField = new TextField(String.valueOf(existingRule != null ? existingRule.getInterval() : 1));
        intervalField.setPromptText("1");

        baseRow = labeledRow("閲嶅棰戠巼", frequencyBox, fixedField("闂撮殧", intervalField));

        FlowPane daysPane = new FlowPane(8, 8);
        daysPane.setAlignment(Pos.CENTER_LEFT);
        dayBoxes = new LinkedHashMap<>();
        List<DayOfWeek> defaultDays = existingRule != null && !existingRule.getByDays().isEmpty()
            ? existingRule.getByDays().stream().sorted(Comparator.comparingInt(DayOfWeek::getValue)).toList()
            : List.of(defaultSeed.getDayOfWeek());
        for (DayOfWeek day : DayOfWeek.values()) {
            CheckBox box = new CheckBox(day.getDisplayName(TextStyle.SHORT, Locale.CHINA));
            box.setSelected(defaultDays.contains(day));
            dayBoxes.put(day, box);
            daysPane.getChildren().add(box);
        }
        weeklyBox = new VBox(8, sectionLabel("每周重复日"), daysPane);

        monthDayField = new TextField(String.valueOf(existingRule != null && existingRule.getByMonthDay() != null
            ? existingRule.getByMonthDay()
            : defaultSeed.getDayOfMonth()));
        monthDayField.setPromptText("1-31");
        monthDayBox = new VBox(8, sectionLabel("每月日期"), monthDayField);

        endModeBox = new ComboBox<>();
        endModeBox.getItems().setAll(
            new EndModeOption("none", "永不结束"),
            new EndModeOption("until", "截止到某天"),
            new EndModeOption("count", "重复固定次数")
        );
        endModeBox.setValue(resolveEndMode());
        endModeBox.setMaxWidth(Double.MAX_VALUE);

        untilDatePicker = new DatePicker(resolveUntilDate());
        untilDatePicker.setMaxWidth(Double.MAX_VALUE);
        untilTimeBox = new ComboBox<>();
        untilTimeBox.getItems().setAll(buildTimeOptions());
        untilTimeBox.setValue(resolveUntilTime());
        untilTimeBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(untilDatePicker, Priority.ALWAYS);
        HBox.setHgrow(untilTimeBox, Priority.ALWAYS);
        untilRow = new HBox(10, untilDatePicker, untilTimeBox);

        occurrenceCountField = new TextField(existingRule != null && existingRule.getOccurrenceCount() != null
            ? String.valueOf(existingRule.getOccurrenceCount())
            : "");
        occurrenceCountField.setPromptText("例如 10");
        countRow = new HBox(10, occurrenceCountField);

        VBox endBox = new VBox(
            8,
            sectionLabel("结束条件"),
            endModeBox,
            untilRow,
            countRow
        );

        validationLabel = new Label();
        validationLabel.getStyleClass().add("error-label");
        validationLabel.setWrapText(true);

        VBox root = new VBox(16, enabledBox, baseRow, weeklyBox, monthDayBox, endBox, validationLabel);
        root.setPadding(new Insets(18, 20, 18, 20));
        root.getStyleClass().add("schedule-dialog-root");

        enabledBox.selectedProperty().addListener((obs, oldValue, newValue) -> refreshVisibility());
        frequencyBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshVisibility());
        endModeBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshVisibility());
        refreshVisibility();
        return root;
    }

    private Result buildResult() {
        if (!enabledBox.isSelected()) {
            return new Result(null);
        }

        FrequencyOption option = Objects.requireNonNull(frequencyBox.getValue(), "请选择重复频率。");
        int interval = parsePositiveInt(intervalField.getText(), "重复间隔必须是大于 0 的整数。");

        RecurrenceRule rule = existingRule != null ? existingRule.copy() : new RecurrenceRule();
        rule.setActive(true);
        rule.setFrequency(option.value());
        rule.setInterval(interval);
        rule.setTimezone(timezone);

        if (RecurrenceRule.FREQ_WEEKLY.equals(option.value())) {
            List<DayOfWeek> days = new ArrayList<>();
            for (Map.Entry<DayOfWeek, CheckBox> entry : dayBoxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    days.add(entry.getKey());
                }
            }
            if (days.isEmpty()) {
                throw new IllegalArgumentException("每周重复至少要选择一个星期。");
            }
            rule.setByDays(days);
            rule.setByMonthDay(null);
        } else {
            rule.setByDays(List.of());
            if (RecurrenceRule.FREQ_MONTHLY.equals(option.value())) {
                rule.setByMonthDay(parseRangedInt(monthDayField.getText(), 1, 31, "每月日期必须在 1 到 31 之间。"));
            } else {
                rule.setByMonthDay(null);
            }
        }

        EndModeOption endMode = Objects.requireNonNull(endModeBox.getValue(), "请选择结束条件。");
        switch (endMode.value()) {
            case "until" -> {
                if (untilDatePicker.getValue() == null) {
                    throw new IllegalArgumentException("请选择截止日期。");
                }
                rule.setUntilAtUtc(LocalDateTime.of(untilDatePicker.getValue(), parseTime(untilTimeBox.getValue())));
                rule.setOccurrenceCount(null);
            }
            case "count" -> {
                rule.setUntilAtUtc(null);
                rule.setOccurrenceCount(parsePositiveInt(occurrenceCountField.getText(), "重复次数必须是大于 0 的整数。"));
            }
            default -> {
                rule.setUntilAtUtc(null);
                rule.setOccurrenceCount(null);
            }
        }
        return new Result(rule);
    }

    private void refreshVisibility() {
        boolean enabled = enabledBox.isSelected();
        FrequencyOption frequency = frequencyBox.getValue();
        EndModeOption endMode = endModeBox.getValue();

        setNodeVisible(baseRow, enabled);
        boolean weekly = enabled && frequency != null && RecurrenceRule.FREQ_WEEKLY.equals(frequency.value());
        boolean monthly = enabled && frequency != null && RecurrenceRule.FREQ_MONTHLY.equals(frequency.value());
        boolean until = enabled && endMode != null && "until".equals(endMode.value());
        boolean count = enabled && endMode != null && "count".equals(endMode.value());

        setNodeVisible(weeklyBox, weekly);
        setNodeVisible(monthDayBox, monthly);
        setNodeVisible(untilRow, until);
        setNodeVisible(countRow, count);
        endModeBox.setDisable(!enabled);
        frequencyBox.setDisable(!enabled);
        intervalField.setDisable(!enabled);
    }

    private NodeWrapper baseRow(boolean enabled) {
        return new NodeWrapper(frequencyBox.getParent() instanceof HBox parent ? parent : null, enabled);
    }

    private void setNodeVisible(VBox node, boolean visible) {
        node.setManaged(visible);
        node.setVisible(visible);
    }

    private void setNodeVisible(HBox node, boolean visible) {
        node.setManaged(visible);
        node.setVisible(visible);
    }

    private FrequencyOption findFrequencyOption(String value) {
        return frequencyBox == null
            ? new FrequencyOption(value, value)
            : frequencyBox.getItems().stream()
                .filter(option -> option.value().equals(value))
                .findFirst()
                .orElse(frequencyBox.getItems().getFirst());
    }

    private EndModeOption resolveEndMode() {
        if (existingRule != null && existingRule.getUntilAtUtc() != null) {
            return new EndModeOption("until", "截止到某天");
        }
        if (existingRule != null && existingRule.getOccurrenceCount() != null && existingRule.getOccurrenceCount() > 0) {
            return new EndModeOption("count", "重复固定次数");
        }
        return new EndModeOption("none", "永不结束");
    }

    private LocalDate resolveUntilDate() {
        if (existingRule != null && existingRule.getUntilAtUtc() != null) {
            return existingRule.getUntilAtUtc().toLocalDate();
        }
        return defaultSeed.toLocalDate().plusMonths(1);
    }

    private String resolveUntilTime() {
        if (existingRule != null && existingRule.getUntilAtUtc() != null) {
            return existingRule.getUntilAtUtc().toLocalTime().withSecond(0).withNano(0).toString();
        }
        return "23:59";
    }

    private List<String> buildTimeOptions() {
        List<String> values = new ArrayList<>();
        LocalTime time = LocalTime.MIDNIGHT;
        while (!time.equals(LocalTime.of(23, 59))) {
            values.add(time.toString());
            time = time.plusMinutes(30);
            if (time.isAfter(LocalTime.of(23, 30))) {
                break;
            }
        }
        values.add("23:59");
        return values;
    }

    private HBox labeledRow(String labelText, javafx.scene.Node... controls) {
        Label label = sectionLabel(labelText);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(label);
        row.getChildren().addAll(controls);
        row.getChildren().add(spacer);
        return row;
    }

    private VBox fixedField(String labelText, TextField field) {
        Label label = sectionLabel(labelText);
        VBox box = new VBox(6, label, field);
        box.setPrefWidth(120);
        field.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private int parsePositiveInt(String raw, String message) {
        try {
            int value = Integer.parseInt(raw == null ? "" : raw.strip());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private int parseRangedInt(String raw, int min, int max, String message) {
        int value = parsePositiveInt(raw, message);
        if (value < min || value > max) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalTime.of(23, 59);
        }
        return LocalTime.parse(raw);
    }

    static final class Result {
        private final RecurrenceRule rule;

        Result(RecurrenceRule rule) {
            this.rule = rule != null ? rule.copy() : null;
        }

        RecurrenceRule getRule() {
            return rule != null ? rule.copy() : null;
        }
    }

    private record FrequencyOption(String value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record EndModeOption(String value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record NodeWrapper(HBox row, boolean enabled) {
        NodeWrapper {
            if (row != null) {
                row.setManaged(enabled);
                row.setVisible(enabled);
            }
        }
    }
}
