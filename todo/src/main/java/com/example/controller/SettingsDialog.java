package com.example.controller;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntConsumer;

import com.example.application.AppFontWeight;
import com.example.application.AppLanguage;
import com.example.application.CustomOptionsService;
import com.example.application.ExperimentalFeaturesService;
import com.example.application.FontService;
import com.example.application.HeatmapColorScheme;
import com.example.application.IconKey;
import com.example.application.IconPack;
import com.example.application.ClassicThemePalette;
import com.example.application.IconService;
import com.example.application.LocalizationService;
import com.example.application.ShortcutSpec;
import com.example.application.ThemeAppearance;
import com.example.application.ThemeFamily;
import com.example.application.ThemeService;
import com.example.application.WheelModifier;
import com.example.config.AppProperties;
import com.example.config.UserPreferencesStore;
import com.example.model.ScheduleItem;
import com.example.view.LabeledTextAutoFit;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class SettingsDialog {

    private static final String PREF_TIMELINE_ZOOM_WHEEL_MODIFIER_KEY = "todo.shortcut.timeline.zoom.wheelModifier";
    private static final String PREF_TIMELINE_ZOOM_IN_KEY = "todo.shortcut.timeline.zoom.in";
    private static final String PREF_TIMELINE_ZOOM_OUT_KEY = "todo.shortcut.timeline.zoom.out";

    private final LocalizationFacade loc;
    private final ThemeCoordinator themeCoordinator;
    private final ThemeService themeService;
    private final IconService iconService;
    private final FontService fontService;
    private final ScheduleHandler scheduleHandler;
    private final CustomOptionsService customOptionsService;
    private final ExperimentalFeaturesService experimentalFeaturesService;
    private final UserPreferencesStore preferencesStore;
    private final LocalizationService localizationService;
    private final AppProperties appProperties;

    private final Runnable onRefreshAllViews;
    private final Runnable onRefreshDataViews;
    private final Runnable onApplyCurrentFont;
    private final Runnable onTimelineShortcutsChanged;

    private final WheelModifier initialTimelineZoomWheelModifier;
    private final ShortcutSpec initialTimelineZoomInShortcut;
    private final ShortcutSpec initialTimelineZoomOutShortcut;

    public SettingsDialog(
        LocalizationFacade loc,
        ThemeCoordinator themeCoordinator,
        ThemeService themeService,
        IconService iconService,
        FontService fontService,
        ScheduleHandler scheduleHandler,
        CustomOptionsService customOptionsService,
        ExperimentalFeaturesService experimentalFeaturesService,
        UserPreferencesStore preferencesStore,
        LocalizationService localizationService,
        AppProperties appProperties,
        Runnable onRefreshAllViews,
        Runnable onRefreshDataViews,
        Runnable onApplyCurrentFont,
        Runnable onTimelineShortcutsChanged,
        WheelModifier initialTimelineZoomWheelModifier,
        ShortcutSpec initialTimelineZoomInShortcut,
        ShortcutSpec initialTimelineZoomOutShortcut
    ) {
        this.loc = loc;
        this.themeCoordinator = themeCoordinator;
        this.themeService = themeService;
        this.iconService = iconService;
        this.fontService = fontService;
        this.scheduleHandler = scheduleHandler;
        this.customOptionsService = customOptionsService;
        this.experimentalFeaturesService = experimentalFeaturesService;
        this.preferencesStore = preferencesStore;
        this.localizationService = localizationService;
        this.appProperties = appProperties;
        this.onRefreshAllViews = onRefreshAllViews;
        this.onRefreshDataViews = onRefreshDataViews;
        this.onApplyCurrentFont = onApplyCurrentFont;
        this.onTimelineShortcutsChanged = onTimelineShortcutsChanged;
        this.initialTimelineZoomWheelModifier = initialTimelineZoomWheelModifier;
        this.initialTimelineZoomInShortcut = initialTimelineZoomInShortcut;
        this.initialTimelineZoomOutShortcut = initialTimelineZoomOutShortcut;
    }

    public Optional<ButtonType> show() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setResizable(true);
        dialog.setTitle(loc.text("settings.title"));
        dialog.setHeaderText(null);
        ButtonType saveButtonType = new ButtonType(loc.text("common.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(loc.text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        dialog.getDialogPane().getStylesheets().setAll(themeCoordinator.getCurrentThemeStylesheets());
        dialog.getDialogPane().getStyleClass().add("settings-dialog-pane");
        applyDialogPreferences(dialog.getDialogPane());
        dialog.getDialogPane().setPrefWidth(940);
        dialog.getDialogPane().setPrefHeight(720);
        dialog.getDialogPane().setMinWidth(760);
        dialog.getDialogPane().setMinHeight(580);

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("settings-shell");
        shell.setPrefWidth(940);
        shell.setPrefHeight(640);
        shell.setMinHeight(480);
        shell.setMaxHeight(Double.MAX_VALUE);

        VBox navBar = new VBox(8);
        navBar.getStyleClass().addAll("sidebar", "settings-nav");
        navBar.setPrefWidth(220);
        Label navTitle = new Label(loc.text("settings.title"));
        navTitle.getStyleClass().addAll("label-title", "settings-nav-title");
        LabeledTextAutoFit.install(navTitle, LabeledTextAutoFit.titleSpec());
        Label navSubTitle = new Label(loc.text("settings.subtitle"));
        navSubTitle.getStyleClass().addAll("label-hint", "settings-nav-subtitle");

        ToggleGroup categoryGroup = new ToggleGroup();
        ToggleButton generalTab = new ToggleButton(loc.text("settings.tab.details"));
        generalTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.DETAIL, loc.text("settings.tab.details"), 20));
        generalTab.setGraphicTextGap(8);

        ToggleButton personalizationTab = new ToggleButton(loc.text("settings.tab.personalization"));
        personalizationTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.STYLE, loc.text("settings.tab.personalization"), 20));
        personalizationTab.setGraphicTextGap(8);

        ToggleButton customTab = new ToggleButton(loc.text("settings.tab.custom"));
        customTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.TAG, loc.text("settings.tab.custom"), 20));
        customTab.setGraphicTextGap(8);

        ToggleButton shortcutsTab = new ToggleButton(loc.text("settings.tab.shortcuts"));
        shortcutsTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.TIMELINE, loc.text("settings.tab.shortcuts"), 20));
        shortcutsTab.setGraphicTextGap(8);

        ToggleButton labsTab = new ToggleButton(loc.text("settings.tab.labs"));
        labsTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.ANIM, loc.text("settings.tab.labs"), 20));
        labsTab.setGraphicTextGap(8);

        ToggleButton dataTab = new ToggleButton(loc.text("settings.tab.data"));
        dataTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.FOLDER, loc.text("settings.tab.data"), 20));
        dataTab.setGraphicTextGap(8);

        for (ToggleButton tab : List.of(generalTab, personalizationTab, customTab, shortcutsTab, labsTab, dataTab)) {
            tab.getStyleClass().add("nav-button");
            tab.setMaxWidth(Double.MAX_VALUE);
            tab.setContentDisplay(ContentDisplay.LEFT);
            tab.setTextOverrun(OverrunStyle.CLIP);
            tab.setWrapText(false);
            tab.setToggleGroup(categoryGroup);
            LabeledTextAutoFit.install(tab, LabeledTextAutoFit.buttonSpec());
        }
        navBar.getChildren().addAll(
            navTitle,
            navSubTitle,
            generalTab,
            personalizationTab,
            customTab,
            shortcutsTab,
            labsTab,
            dataTab
        );

        StackPane contentHost = new StackPane();
        contentHost.getStyleClass().add("settings-content-host");
        contentHost.setMinHeight(0);
        contentHost.setPrefHeight(0);
        contentHost.setMaxHeight(Double.MAX_VALUE);

        String displayAppVersion = "v" + appProperties.getAppVersion();
        ThemeFamily originalThemeFamily = themeCoordinator.getCurrentThemeFamily();
        ThemeAppearance originalThemeAppearance = themeCoordinator.getCurrentThemeAppearance();
        ClassicThemePalette originalClassicPalette = themeCoordinator.getCurrentClassicPalette();
        IconPack originalIconPack = themeCoordinator.getCurrentIconPack();
        boolean originalThemeIconBinding = themeCoordinator.isCurrentThemeIconBinding();
        boolean originalLabsEnabled = experimentalFeaturesService.isLabsEnabled();
        WheelModifier originalTimelineZoomWheelModifier = initialTimelineZoomWheelModifier;
        ShortcutSpec originalTimelineZoomInShortcut = initialTimelineZoomInShortcut;
        ShortcutSpec originalTimelineZoomOutShortcut = initialTimelineZoomOutShortcut;
        List<String> originalCustomTasks = customOptionsService != null ? customOptionsService.getTasks() : List.of();
        List<String> originalCustomTags = customOptionsService != null ? customOptionsService.getTags() : List.of();
        BooleanProperty customOptionsValid = new SimpleBooleanProperty(true);
        List<CustomOptionRow> customTaskRows = new ArrayList<>();
        List<CustomOptionRow> customTagRows = new ArrayList<>();
        boolean originalTimeTextInputEnabled = customOptionsService != null && customOptionsService.isTimeTextInputEnabled();
        boolean originalTagCommaSplitEnabled = customOptionsService != null && customOptionsService.isTagCommaSplitEnabled();
        int originalHeatmapThreshold1 = customOptionsService != null ? customOptionsService.getHeatmapThreshold1() : CustomOptionsService.DEFAULT_HEATMAP_THRESHOLD_1;
        int originalHeatmapThreshold2 = customOptionsService != null ? customOptionsService.getHeatmapThreshold2() : CustomOptionsService.DEFAULT_HEATMAP_THRESHOLD_2;
        int originalHeatmapThreshold3 = customOptionsService != null ? customOptionsService.getHeatmapThreshold3() : CustomOptionsService.DEFAULT_HEATMAP_THRESHOLD_3;
        boolean originalHeatmapColorBinding = customOptionsService == null || customOptionsService.isHeatmapColorBindingEnabled();
        HeatmapColorScheme originalHeatmapColorScheme = customOptionsService != null
            ? customOptionsService.getHeatmapColorScheme()
            : HeatmapColorScheme.GREEN;

        VBox generalPage = new VBox(18);
        generalPage.getStyleClass().add("settings-page");
        generalPage.setFillWidth(true);
        generalPage.setMinHeight(0);
        VBox aboutCard = createSettingsCard(loc.text("settings.about.title"), loc.text("settings.about.subtitle"));
        Label aboutText = new Label(loc.text("settings.about.body", displayAppVersion));
        aboutText.getStyleClass().add("settings-info-text");
        aboutText.setWrapText(true);
        aboutCard.getChildren().add(aboutText);
        VBox currentCard = createSettingsCard(loc.text("settings.current.title"), loc.text("settings.current.subtitle"));
        Label themeValue = new Label(loc.currentThemeDisplayName(themeCoordinator.getCurrentThemeFamily(), themeCoordinator.getCurrentClassicPalette()));
        themeValue.getStyleClass().add("settings-inline-value");
        Label languageValue = new Label(localizationService.languageLabel(localizationService.getPreferredLanguage()));
        languageValue.getStyleClass().add("settings-inline-value");
        Label fontValue = new Label(localizationService.fontWeightLabel(fontService.getCurrentFontWeight()));
        fontValue.getStyleClass().add("settings-inline-value");
        Label iconValue = new Label(loc.currentIconDisplayName(themeCoordinator.getCurrentIconPack(), themeCoordinator.isCurrentThemeIconBinding()));
        iconValue.getStyleClass().add("settings-inline-value");
        currentCard.getChildren().addAll(
            createSettingRow(loc.text("settings.current.theme.label"), loc.text("settings.current.theme.description"), themeValue),
            createSettingRow(loc.text("settings.current.icon.label"), loc.text("settings.current.icon.description"), iconValue),
            createSettingRow(loc.text("settings.current.language.label"), loc.text("settings.current.language.description"), languageValue),
            createSettingRow(loc.text("settings.current.font.label"), loc.text("settings.current.font.description"), fontValue)
        );
        ThemeFamily[] selectedThemeFamily = new ThemeFamily[] { originalThemeFamily };
        ClassicThemePalette[] selectedClassicPalette = new ClassicThemePalette[] { originalClassicPalette };
        IconPack[] selectedIconPack = new IconPack[] { originalIconPack };
        AppLanguage originalPreferredLanguage = localizationService.getPreferredLanguage();
        AppFontWeight originalFontWeight = fontService.getCurrentFontWeight();
        AppLanguage[] selectedLanguage = new AppLanguage[] { originalPreferredLanguage };
        AppFontWeight[] selectedFontWeight = new AppFontWeight[] { originalFontWeight };
        boolean[] selectedLabsEnabled = new boolean[] { originalLabsEnabled };
        boolean[] selectedThemeIconBinding = new boolean[] { originalThemeIconBinding };
        WheelModifier[] selectedTimelineZoomWheelModifier = new WheelModifier[] { originalTimelineZoomWheelModifier };
        ShortcutSpec[] selectedTimelineZoomInShortcut = new ShortcutSpec[] { originalTimelineZoomInShortcut };
        ShortcutSpec[] selectedTimelineZoomOutShortcut = new ShortcutSpec[] { originalTimelineZoomOutShortcut };

        VBox languageFontCard = createSettingsCard(loc.text("settings.preferences.title"), loc.text("settings.preferences.subtitle"));
        ComboBox<AppLanguage> languageComboBox = new ComboBox<>();
        languageComboBox.getItems().setAll(AppLanguage.supportedValues());
        languageComboBox.setValue(originalPreferredLanguage);
        languageComboBox.setMaxWidth(200);
        languageComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AppLanguage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : localizationService.languageLabel(item));
            }
        });
        languageComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AppLanguage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : localizationService.languageLabel(item));
            }
        });
        languageComboBox.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            selectedLanguage[0] = newValue != null ? newValue : originalPreferredLanguage;
            languageValue.setText(localizationService.languageLabel(selectedLanguage[0]));
        });

        ToggleGroup fontWeightGroup = new ToggleGroup();
        HBox fontChipRow = new HBox(8);
        fontChipRow.setAlignment(Pos.CENTER_LEFT);
        for (AppFontWeight fontWeight : AppFontWeight.supportedValues()) {
            ToggleButton chip = new ToggleButton(localizationService.fontWeightLabel(fontWeight));
            chip.getStyleClass().add("settings-style-chip");
            chip.setToggleGroup(fontWeightGroup);
            chip.setUserData(fontWeight);
            if (fontWeight == originalFontWeight) {
                chip.setSelected(true);
            }
            chip.setOnAction(event -> {
                selectedFontWeight[0] = fontWeight;
                fontValue.setText(localizationService.fontWeightLabel(fontWeight));
            });
            fontChipRow.getChildren().add(chip);
        }

        languageFontCard.getChildren().addAll(
            createSettingRow(loc.text("settings.preferences.language.label"), loc.text("settings.preferences.language.description"), languageComboBox),
            createSettingRow(loc.text("settings.preferences.font.label"), loc.text("settings.preferences.font.description"), fontChipRow)
        );

        VBox shortcutsCard = createSettingsCard(loc.text("settings.shortcuts.title"), loc.text("settings.shortcuts.subtitle"));
        ComboBox<WheelModifier> wheelModifierCombo = new ComboBox<>();
        wheelModifierCombo.getItems().setAll(WheelModifier.supportedValues());
        wheelModifierCombo.setValue(selectedTimelineZoomWheelModifier[0]);
        wheelModifierCombo.setMaxWidth(200);
        wheelModifierCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(WheelModifier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : wheelModifierDisplayName(item));
            }
        });
        wheelModifierCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(WheelModifier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : wheelModifierDisplayName(item));
            }
        });
        wheelModifierCombo.valueProperty().addListener((obs, oldValue, newValue) ->
            selectedTimelineZoomWheelModifier[0] = newValue != null ? newValue : originalTimelineZoomWheelModifier
        );

        Label zoomInValueLabel = new Label(selectedTimelineZoomInShortcut[0].toDisplayString());
        zoomInValueLabel.getStyleClass().add("settings-inline-value");
        Button zoomInSetButton = new Button(loc.text("settings.shortcuts.action.set"));
        zoomInSetButton.getStyleClass().add("button-secondary");
        zoomInSetButton.setCursor(Cursor.HAND);
        zoomInSetButton.setOnAction(event -> {
            ShortcutSpec captured = captureShortcutSpec();
            if (captured != null) {
                selectedTimelineZoomInShortcut[0] = captured;
                zoomInValueLabel.setText(captured.toDisplayString());
            }
        });
        HBox zoomInControl = new HBox(10, zoomInValueLabel, zoomInSetButton);
        zoomInControl.setAlignment(Pos.CENTER_RIGHT);

        Label zoomOutValueLabel = new Label(selectedTimelineZoomOutShortcut[0].toDisplayString());
        zoomOutValueLabel.getStyleClass().add("settings-inline-value");
        Button zoomOutSetButton = new Button(loc.text("settings.shortcuts.action.set"));
        zoomOutSetButton.getStyleClass().add("button-secondary");
        zoomOutSetButton.setCursor(Cursor.HAND);
        zoomOutSetButton.setOnAction(event -> {
            ShortcutSpec captured = captureShortcutSpec();
            if (captured != null) {
                selectedTimelineZoomOutShortcut[0] = captured;
                zoomOutValueLabel.setText(captured.toDisplayString());
            }
        });
        HBox zoomOutControl = new HBox(10, zoomOutValueLabel, zoomOutSetButton);
        zoomOutControl.setAlignment(Pos.CENTER_RIGHT);

        shortcutsCard.getChildren().addAll(
            createSettingRow(
                loc.text("settings.shortcuts.timelineZoomWheel.label"),
                loc.text("settings.shortcuts.timelineZoomWheel.description"),
                wheelModifierCombo
            ),
            createSettingRow(
                loc.text("settings.shortcuts.timelineZoomIn.label"),
                loc.text("settings.shortcuts.timelineZoomIn.description"),
                zoomInControl
            ),
            createSettingRow(
                loc.text("settings.shortcuts.timelineZoomOut.label"),
                loc.text("settings.shortcuts.timelineZoomOut.description"),
                zoomOutControl
            )
        );

        VBox labsCard = createSettingsCard(loc.text("settings.labs.title"), loc.text("settings.labs.subtitle"));
        ToggleButton labsToggle = new ToggleButton();
        labsToggle.getStyleClass().add("modern-toggle-switch");
        labsToggle.setCursor(Cursor.HAND);
        labsToggle.setSelected(originalLabsEnabled);
        if (originalLabsEnabled) {
            labsToggle.getStyleClass().add("on");
        }
        Label labsFootnote = new Label(loc.text("settings.labs.note"));
        labsFootnote.getStyleClass().add("settings-row-desc");
        labsFootnote.setWrapText(true);
        labsCard.getChildren().addAll(
            createSettingRow(loc.text("settings.labs.toggle.label"), loc.text("settings.labs.toggle.description"), labsToggle),
            labsFootnote
        );
        generalPage.getChildren().addAll(aboutCard, currentCard, languageFontCard);

        VBox personalizationPage = new VBox(18);
        personalizationPage.getStyleClass().add("settings-page");
        personalizationPage.setFillWidth(true);
        personalizationPage.setMinHeight(0);
        VBox themeCard = createSettingsCard(loc.text("settings.theme.title"), loc.text("settings.theme.subtitle"));
        ToggleGroup themeFamilyGroup = new ToggleGroup();
        FlowPane familyChipFlow = new FlowPane();
        familyChipFlow.getStyleClass().add("settings-chip-flow");
        familyChipFlow.setHgap(8);
        familyChipFlow.setVgap(10);
        familyChipFlow.setAlignment(Pos.CENTER_LEFT);
        familyChipFlow.setMaxWidth(Double.MAX_VALUE);
        Map<ThemeFamily, ToggleButton> familyChips = new LinkedHashMap<>();
        for (ThemeFamily family : themeCoordinator.getAvailableThemeFamilies()) {
            ToggleButton familyChip = new ToggleButton(loc.themeFamilyDisplayName(family));
            familyChip.getStyleClass().add("settings-style-chip");
            familyChip.setToggleGroup(themeFamilyGroup);
            familyChip.setWrapText(true);
            familyChip.setTextOverrun(OverrunStyle.CLIP);
            familyChip.setUserData(family);
            if (family == selectedThemeFamily[0]) {
                familyChip.setSelected(true);
            }
            familyChips.put(family, familyChip);
            familyChipFlow.getChildren().add(familyChip);
        }

        FlowPane paletteFlow = new FlowPane();
        paletteFlow.getStyleClass().add("settings-chip-flow");
        paletteFlow.setHgap(10);
        paletteFlow.setVgap(10);
        paletteFlow.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup paletteGroup = new ToggleGroup();
        for (ClassicThemePalette palette : themeCoordinator.getClassicThemePalettes()) {
            ToggleButton swatch = new ToggleButton();
            swatch.setToggleGroup(paletteGroup);
            swatch.getStyleClass().add("settings-theme-swatch");
            swatch.setStyle("-fx-background-color: " + palette.getPreviewColor() + ";");
            swatch.setTooltip(new Tooltip(loc.classicPaletteDisplayName(palette)));
            swatch.setUserData(palette);
            if (palette == selectedClassicPalette[0]) {
                swatch.setSelected(true);
            }
            paletteFlow.getChildren().add(swatch);
        }
        VBox paletteRow = createStackedSettingRow(
            loc.text("settings.theme.palette.label"),
            loc.text("settings.theme.palette.description"),
            paletteFlow
        );

        VBox iconCard = createSettingsCard(loc.text("settings.icon.title"), loc.text("settings.icon.subtitle"));
        Label iconSummaryValue = new Label(loc.currentIconDisplayName(selectedIconPack[0], selectedThemeIconBinding[0]));
        iconSummaryValue.getStyleClass().add("settings-inline-value");
        ToggleButton iconBindingToggle = new ToggleButton();
        iconBindingToggle.getStyleClass().add("modern-toggle-switch");
        iconBindingToggle.setCursor(Cursor.HAND);
        iconBindingToggle.setSelected(originalThemeIconBinding);
        if (originalThemeIconBinding) {
            iconBindingToggle.getStyleClass().add("on");
        }

        ToggleGroup iconPackGroup = new ToggleGroup();
        FlowPane iconPackFlow = new FlowPane();
        iconPackFlow.getStyleClass().add("settings-chip-flow");
        iconPackFlow.setHgap(8);
        iconPackFlow.setVgap(10);
        iconPackFlow.setAlignment(Pos.CENTER_LEFT);
        iconPackFlow.setMaxWidth(Double.MAX_VALUE);
        Map<IconPack, ToggleButton> iconPackChips = new LinkedHashMap<>();
        for (IconPack iconPack : themeCoordinator.getAvailableIconPacks()) {
            ToggleButton chip = new ToggleButton(loc.iconPackDisplayName(iconPack));
            chip.getStyleClass().add("settings-style-chip");
            chip.setToggleGroup(iconPackGroup);
            chip.setWrapText(true);
            chip.setTextOverrun(OverrunStyle.CLIP);
            chip.setUserData(iconPack);
            if (iconPack == selectedIconPack[0]) {
                chip.setSelected(true);
            }
            iconPackChips.put(iconPack, chip);
            iconPackFlow.getChildren().add(chip);
        }

        Runnable refreshSettingsDialogIcons = () -> {
            generalTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.DETAIL, loc.text("settings.tab.details"), 20));
            personalizationTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.STYLE, loc.text("settings.tab.personalization"), 20));
            customTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.TAG, loc.text("settings.tab.custom"), 20));
            shortcutsTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.TIMELINE, loc.text("settings.tab.shortcuts"), 20));
            labsTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.ANIM, loc.text("settings.tab.labs"), 20));
            dataTab.setGraphic(themeCoordinator.createSvgIcon(IconKey.FOLDER, loc.text("settings.tab.data"), 20));
        };
        Runnable updateThemeSummary = () ->
            themeValue.setText(loc.currentThemeDisplayName(selectedThemeFamily[0], selectedClassicPalette[0]));
        Runnable updateIconSummary = () -> {
            String iconDisplay = loc.currentIconDisplayName(selectedIconPack[0], selectedThemeIconBinding[0]);
            iconValue.setText(iconDisplay);
            iconSummaryValue.setText(iconDisplay);
        };
        Runnable updatePaletteVisibility = () -> {
            boolean visible = selectedThemeFamily[0] != null && selectedThemeFamily[0].supportsClassicPalette();
            paletteRow.setManaged(visible);
            paletteRow.setVisible(visible);
        };
        Runnable updateIconPackInteractivity = () -> {
            boolean manualSelectionEnabled = !selectedThemeIconBinding[0];
            for (ToggleButton chip : iconPackChips.values()) {
                chip.setDisable(!manualSelectionEnabled);
            }
        };
        Runnable updateLabsThemeVisibility = () -> {
            for (Map.Entry<ThemeFamily, ToggleButton> entry : familyChips.entrySet()) {
                boolean visible = selectedLabsEnabled[0] || !entry.getKey().isLabsOnly();
                entry.getValue().setManaged(visible);
                entry.getValue().setVisible(visible);
            }
        };
        Runnable previewThemeAndIcons = () -> {
            themeCoordinator.previewThemeSelection(selectedThemeFamily[0], originalThemeAppearance, selectedClassicPalette[0], dialog.getDialogPane());
            iconService.previewSelection(selectedThemeFamily[0], selectedThemeIconBinding[0], selectedIconPack[0]);
            themeCoordinator.syncIconState();
            selectedIconPack[0] = themeCoordinator.getCurrentIconPack();
            ToggleButton selectedPackChip = iconPackChips.get(selectedIconPack[0]);
            if (selectedPackChip != null) {
                selectedPackChip.setSelected(true);
            }
            updateThemeSummary.run();
            updateIconSummary.run();
            updatePaletteVisibility.run();
            updateIconPackInteractivity.run();
            refreshSettingsDialogIcons.run();
        };

        for (javafx.scene.Node node : familyChipFlow.getChildren()) {
            if (node instanceof ToggleButton chip) {
                chip.setOnAction(event -> {
                    selectedThemeFamily[0] = (ThemeFamily) ((ToggleButton) event.getSource()).getUserData();
                    previewThemeAndIcons.run();
                });
            }
        }
        for (javafx.scene.Node node : paletteFlow.getChildren()) {
            if (node instanceof ToggleButton swatch) {
                swatch.setOnAction(event -> {
                    selectedClassicPalette[0] = (ClassicThemePalette) ((ToggleButton) event.getSource()).getUserData();
                    previewThemeAndIcons.run();
                });
            }
        }
        for (javafx.scene.Node node : iconPackFlow.getChildren()) {
            if (node instanceof ToggleButton chip) {
                chip.setOnAction(event -> {
                    if (selectedThemeIconBinding[0]) {
                        return;
                    }
                    selectedIconPack[0] = (IconPack) ((ToggleButton) event.getSource()).getUserData();
                    previewThemeAndIcons.run();
                });
            }
        }

        iconBindingToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            boolean bindingEnabled = Boolean.TRUE.equals(newValue);
            selectedThemeIconBinding[0] = bindingEnabled;
            if (bindingEnabled) {
                iconBindingToggle.getStyleClass().add("on");
                selectedIconPack[0] = IconPack.boundToThemeFamily(selectedThemeFamily[0]);
                ToggleButton selectedPackChip = iconPackChips.get(selectedIconPack[0]);
                if (selectedPackChip != null) {
                    selectedPackChip.setSelected(true);
                }
            } else {
                iconBindingToggle.getStyleClass().remove("on");
            }
            previewThemeAndIcons.run();
        });

        labsToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            boolean labsEnabled = Boolean.TRUE.equals(newValue);
            selectedLabsEnabled[0] = labsEnabled;
            if (labsEnabled) {
                labsToggle.getStyleClass().add("on");
            } else {
                labsToggle.getStyleClass().remove("on");
            }

            if (!labsEnabled && selectedThemeFamily[0] != null && selectedThemeFamily[0].isLabsOnly()) {
                selectedThemeFamily[0] = ThemeFamily.CLASSIC;
                selectedClassicPalette[0] = ClassicThemePalette.LIGHT;
                ToggleButton classicChip = familyChips.get(ThemeFamily.CLASSIC);
                if (classicChip != null) {
                    classicChip.setSelected(true);
                }
                for (Node node : paletteFlow.getChildren()) {
                    if (node instanceof ToggleButton swatch
                        && swatch.getUserData() == ClassicThemePalette.LIGHT) {
                        swatch.setSelected(true);
                        break;
                    }
                }
                updateLabsThemeVisibility.run();
                previewThemeAndIcons.run();
                showInfo(
                    loc.text("settings.labs.disabled.title"),
                    loc.text("settings.labs.disabled.message")
                );
                return;
            }

            updateLabsThemeVisibility.run();
            updateThemeSummary.run();
        });

        updateLabsThemeVisibility.run();
        updatePaletteVisibility.run();
        updateIconSummary.run();
        updateIconPackInteractivity.run();
        refreshSettingsDialogIcons.run();
        themeCard.getChildren().addAll(
            createStackedSettingRow(loc.text("settings.theme.family.label"), loc.text("settings.theme.family.description"), familyChipFlow),
            paletteRow
        );
        iconCard.getChildren().addAll(
            createSettingRow(loc.text("settings.icon.summary.label"), loc.text("settings.icon.summary.description"), iconSummaryValue),
            createSettingRow(loc.text("settings.icon.binding.label"), loc.text("settings.icon.binding.description"), iconBindingToggle),
            createStackedSettingRow(loc.text("settings.icon.pack.label"), loc.text("settings.icon.pack.description"), iconPackFlow)
        );

        // Heatmap Color Scheme Card
        VBox heatmapColorCard = createSettingsCard(loc.text("settings.heatmap.color.title"), loc.text("settings.heatmap.color.subtitle"));
        boolean[] selectedHeatmapColorBinding = new boolean[] { originalHeatmapColorBinding };
        HeatmapColorScheme[] selectedHeatmapScheme = new HeatmapColorScheme[] { originalHeatmapColorScheme };

        ToggleButton heatmapColorBindingToggle = new ToggleButton();
        heatmapColorBindingToggle.getStyleClass().add("modern-toggle-switch");
        heatmapColorBindingToggle.setCursor(Cursor.HAND);
        heatmapColorBindingToggle.setSelected(originalHeatmapColorBinding);
        if (originalHeatmapColorBinding) {
            heatmapColorBindingToggle.getStyleClass().add("on");
        }

        ToggleGroup heatmapSchemeGroup = new ToggleGroup();
        FlowPane heatmapSchemeFlow = new FlowPane();
        heatmapSchemeFlow.getStyleClass().add("settings-chip-flow");
        heatmapSchemeFlow.setHgap(10);
        heatmapSchemeFlow.setVgap(10);
        heatmapSchemeFlow.setAlignment(Pos.CENTER_LEFT);
        heatmapSchemeFlow.setMaxWidth(Double.MAX_VALUE);
        Map<HeatmapColorScheme, ToggleButton> heatmapSchemeChips = new LinkedHashMap<>();

        HeatmapColorScheme effectiveScheme = originalHeatmapColorBinding && themeService != null
            ? themeService.getCurrentThemeFamily().getDefaultHeatmapColorScheme()
            : originalHeatmapColorScheme;

        for (HeatmapColorScheme scheme : HeatmapColorScheme.supportedValues()) {
            HBox chipContent = new HBox(4);
            chipContent.setAlignment(Pos.CENTER);
            boolean dark = themeService != null && themeService.getCurrentAppearance() == ThemeAppearance.DARK;
            for (int ci = 0; ci < 5; ci++) {
                Rectangle colorRect = new Rectangle(12, 12);
                colorRect.setArcWidth(3);
                colorRect.setArcHeight(3);
                colorRect.setFill(scheme.getColor(ci, dark));
                chipContent.getChildren().add(colorRect);
            }
            Label chipLabel = new Label(loc.text(scheme.getLabelKey()));
            chipLabel.setStyle("-fx-padding: 0 0 0 4;");
            chipContent.getChildren().add(chipLabel);

            ToggleButton chip = new ToggleButton();
            chip.setGraphic(chipContent);
            chip.getStyleClass().add("settings-style-chip");
            chip.setToggleGroup(heatmapSchemeGroup);
            chip.setUserData(scheme);
            if (scheme == effectiveScheme) {
                chip.setSelected(true);
            }
            heatmapSchemeChips.put(scheme, chip);
            heatmapSchemeFlow.getChildren().add(chip);
        }

        Runnable updateHeatmapSchemeInteractivity = () -> {
            boolean manualEnabled = !selectedHeatmapColorBinding[0];
            for (ToggleButton chip : heatmapSchemeChips.values()) {
                chip.setDisable(!manualEnabled);
            }
            if (selectedHeatmapColorBinding[0] && themeService != null) {
                HeatmapColorScheme boundScheme = selectedThemeFamily[0] != null
                    ? selectedThemeFamily[0].getDefaultHeatmapColorScheme()
                    : HeatmapColorScheme.GREEN;
                selectedHeatmapScheme[0] = boundScheme;
                ToggleButton boundChip = heatmapSchemeChips.get(boundScheme);
                if (boundChip != null) {
                    boundChip.setSelected(true);
                }
            }
        };

        heatmapColorBindingToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            selectedHeatmapColorBinding[0] = enabled;
            if (enabled) {
                heatmapColorBindingToggle.getStyleClass().add("on");
            } else {
                heatmapColorBindingToggle.getStyleClass().remove("on");
            }
            updateHeatmapSchemeInteractivity.run();
        });

        for (javafx.scene.Node node : heatmapSchemeFlow.getChildren()) {
            if (node instanceof ToggleButton chip) {
                chip.setOnAction(event -> {
                    if (selectedHeatmapColorBinding[0]) {
                        return;
                    }
                    selectedHeatmapScheme[0] = (HeatmapColorScheme) ((ToggleButton) event.getSource()).getUserData();
                });
            }
        }

        updateHeatmapSchemeInteractivity.run();

        heatmapColorCard.getChildren().addAll(
            createSettingRow(loc.text("settings.heatmap.color.binding.label"), loc.text("settings.heatmap.color.binding.description"), heatmapColorBindingToggle),
            createStackedSettingRow(loc.text("settings.heatmap.color.scheme.label"), loc.text("settings.heatmap.color.scheme.description"), heatmapSchemeFlow)
        );

        VBox customCard = createSettingsCard(loc.text("settings.custom.title"), loc.text("settings.custom.subtitle"));
        boolean[] selectedTimeTextInputEnabled = new boolean[] { originalTimeTextInputEnabled };
        ToggleButton timeTextInputToggle = new ToggleButton();
        timeTextInputToggle.getStyleClass().add("modern-toggle-switch");
        timeTextInputToggle.setCursor(Cursor.HAND);
        timeTextInputToggle.setSelected(originalTimeTextInputEnabled);
        if (originalTimeTextInputEnabled) {
            timeTextInputToggle.getStyleClass().add("on");
        }
        timeTextInputToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            selectedTimeTextInputEnabled[0] = enabled;
            if (enabled) {
                timeTextInputToggle.getStyleClass().add("on");
            } else {
                timeTextInputToggle.getStyleClass().remove("on");
            }
        });
        customCard.getChildren().add(
            createSettingRow(
                loc.text("settings.custom.timeInput.label"),
                loc.text("settings.custom.timeInput.description"),
                timeTextInputToggle
            )
        );

        personalizationPage.getChildren().addAll(themeCard, iconCard, heatmapColorCard);

        VBox customPage = new VBox(18);
        customPage.getStyleClass().add("settings-page");
        customPage.setFillWidth(true);
        customPage.setMinHeight(0);
        VBox dataPage = new VBox(18);
        dataPage.getStyleClass().add("settings-page");
        dataPage.setFillWidth(true);
        dataPage.setMinHeight(0);

        VBox customOptionsCard = createSettingsCard(loc.text("settings.customOptions.title"), loc.text("settings.customOptions.subtitle"));
        customOptionsCard.getStyleClass().add("settings-custom-options-card");

        Label tasksTitle = new Label(loc.text("settings.customOptions.tasks.title"));
        tasksTitle.getStyleClass().add("settings-row-title");
        Label tasksError = new Label();
        tasksError.getStyleClass().addAll("settings-row-desc", "settings-custom-options-error");
        tasksError.setWrapText(true);
        tasksError.setVisible(false);
        tasksError.setManaged(false);
        VBox tasksList = new VBox(8);
        tasksList.getStyleClass().add("settings-custom-options-list");
        TextField addTaskField = new TextField();
        addTaskField.getStyleClass().add("settings-custom-options-input");
        addTaskField.setPromptText(loc.text("settings.customOptions.tasks.prompt"));
        Button addTaskButton = new Button(loc.text("common.add"));
        addTaskButton.getStyleClass().addAll("button-secondary", "settings-custom-options-add");
        HBox addTaskRow = new HBox(10, addTaskField, addTaskButton);
        addTaskRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(addTaskField, Priority.ALWAYS);

        Label tagsTitle = new Label(loc.text("settings.customOptions.tags.title"));
        tagsTitle.getStyleClass().add("settings-row-title");
        Label tagsError = new Label();
        tagsError.getStyleClass().addAll("settings-row-desc", "settings-custom-options-error");
        tagsError.setWrapText(true);
        tagsError.setVisible(false);
        tagsError.setManaged(false);
        VBox tagsList = new VBox(8);
        tagsList.getStyleClass().add("settings-custom-options-list");
        TextField addTagField = new TextField();
        addTagField.getStyleClass().add("settings-custom-options-input");
        addTagField.setPromptText(loc.text("settings.customOptions.tags.prompt"));
        Button addTagButton = new Button(loc.text("common.add"));
        addTagButton.getStyleClass().addAll("button-secondary", "settings-custom-options-add");
        HBox addTagRow = new HBox(10, addTagField, addTagButton);
        addTagRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(addTagField, Priority.ALWAYS);

        boolean[] selectedTagCommaSplitEnabled = new boolean[] { originalTagCommaSplitEnabled };
        ToggleButton tagCommaSplitToggle = new ToggleButton();
        tagCommaSplitToggle.getStyleClass().add("modern-toggle-switch");
        tagCommaSplitToggle.setCursor(Cursor.HAND);
        tagCommaSplitToggle.setSelected(originalTagCommaSplitEnabled);
        if (originalTagCommaSplitEnabled) {
            tagCommaSplitToggle.getStyleClass().add("on");
        }
        tagCommaSplitToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            selectedTagCommaSplitEnabled[0] = enabled;
            if (enabled) {
                tagCommaSplitToggle.getStyleClass().add("on");
            } else {
                tagCommaSplitToggle.getStyleClass().remove("on");
            }
        });

        Runnable validateCustomOptions = () -> {
            ValidationResult tasksValidation = validateCustomOptionRows(customTaskRows, CustomOptionsService.MAX_TASKS);
            tasksError.setText(tasksValidation.message != null ? tasksValidation.message : "");
            tasksError.setVisible(!tasksValidation.ok);
            tasksError.setManaged(!tasksValidation.ok);

            ValidationResult tagsValidation = validateCustomOptionRows(customTagRows, CustomOptionsService.MAX_TAGS);
            tagsError.setText(tagsValidation.message != null ? tagsValidation.message : "");
            tagsError.setVisible(!tagsValidation.ok);
            tagsError.setManaged(!tagsValidation.ok);

            customOptionsValid.set(tasksValidation.ok && tagsValidation.ok);
        };

        Runnable populateCustomOptions = () -> {
            tasksList.getChildren().clear();
            customTaskRows.clear();
            for (String task : originalCustomTasks) {
                CustomOptionRow row = createCustomOptionRow(task, validateCustomOptions, customTaskRows, tasksList);
                customTaskRows.add(row);
                tasksList.getChildren().add(row.row());
            }

            tagsList.getChildren().clear();
            customTagRows.clear();
            for (String tag : originalCustomTags) {
                CustomOptionRow row = createCustomOptionRow(tag, validateCustomOptions, customTagRows, tagsList);
                customTagRows.add(row);
                tagsList.getChildren().add(row.row());
            }

            validateCustomOptions.run();
        };

        addTaskButton.setOnAction(event -> {
            String normalized = normalizeCustomOptionInput(addTaskField.getText());
            if (normalized == null) {
                return;
            }
            CustomOptionRow row = createCustomOptionRow(null, validateCustomOptions, customTaskRows, tasksList);
            row.field().setText(normalized);
            customTaskRows.add(row);
            tasksList.getChildren().add(row.row());
            addTaskField.setText("");
            validateCustomOptions.run();
            row.field().requestFocus();
            row.field().positionCaret(row.field().getText().length());
        });
        addTagField.setOnAction(event -> addTagButton.fire());
        addTagButton.setOnAction(event -> {
            String normalized = normalizeCustomOptionInput(addTagField.getText());
            if (normalized == null) {
                return;
            }
            CustomOptionRow row = createCustomOptionRow(null, validateCustomOptions, customTagRows, tagsList);
            row.field().setText(normalized);
            customTagRows.add(row);
            tagsList.getChildren().add(row.row());
            addTagField.setText("");
            validateCustomOptions.run();
            row.field().requestFocus();
            row.field().positionCaret(row.field().getText().length());
        });

        populateCustomOptions.run();
        customOptionsCard.getChildren().addAll(
            tasksTitle,
            tasksError,
            tasksList,
            addTaskRow,
            new Separator(),
            tagsTitle,
            tagsError,
            tagsList,
            addTagRow,
            createSettingRow(
                loc.text("settings.customOptions.tags.commaSplit.label"),
                loc.text("settings.customOptions.tags.commaSplit.description"),
                tagCommaSplitToggle
            )
        );

        VBox trashCard = createSettingsCard(loc.text("settings.data.title"), loc.text("settings.data.subtitle"));
        Label trashSummary = new Label();
        trashSummary.getStyleClass().add("settings-row-desc");
        trashSummary.setWrapText(true);
        VBox trashItemsBox = new VBox(10);
        trashItemsBox.getStyleClass().add("settings-trash-list");
        populateTrashSettingsList(trashItemsBox, trashSummary);
        trashCard.getChildren().addAll(trashSummary, trashItemsBox);
        VBox shortcutsPage = new VBox(18);
        shortcutsPage.getStyleClass().add("settings-page");
        shortcutsPage.setFillWidth(true);
        shortcutsPage.setMinHeight(0);
        shortcutsPage.getChildren().add(shortcutsCard);

        VBox labsPage = new VBox(18);
        labsPage.getStyleClass().add("settings-page");
        labsPage.setFillWidth(true);
        labsPage.setMinHeight(0);
        labsPage.getChildren().add(labsCard);

        VBox heatmapThresholdsCard = createSettingsCard(
            loc.text("settings.heatmap.thresholds.title"),
            loc.text("settings.heatmap.thresholds.subtitle")
        );
        int[] selectedHeatmapThreshold1 = new int[] { originalHeatmapThreshold1 };
        int[] selectedHeatmapThreshold2 = new int[] { originalHeatmapThreshold2 };
        int[] selectedHeatmapThreshold3 = new int[] { originalHeatmapThreshold3 };

        HBox threshold1Input = createModernNumberInput(originalHeatmapThreshold1, 1, 999, v -> selectedHeatmapThreshold1[0] = v);
        HBox threshold2Input = createModernNumberInput(originalHeatmapThreshold2, 1, 999, v -> selectedHeatmapThreshold2[0] = v);
        HBox threshold3Input = createModernNumberInput(originalHeatmapThreshold3, 1, 999, v -> selectedHeatmapThreshold3[0] = v);

        heatmapThresholdsCard.getChildren().addAll(
            createSettingRow(loc.text("settings.heatmap.thresholds.level1.label"), "", threshold1Input),
            createSettingRow(loc.text("settings.heatmap.thresholds.level2.label"), "", threshold2Input),
            createSettingRow(loc.text("settings.heatmap.thresholds.level3.label"), "", threshold3Input)
        );

        customPage.getChildren().addAll(customCard, customOptionsCard, heatmapThresholdsCard);
        dataPage.getChildren().add(trashCard);

        ScrollPane generalPageScroll = createSettingsScrollPane(generalPage);
        ScrollPane personalizationPageScroll = createSettingsScrollPane(personalizationPage);
        ScrollPane customPageScroll = createSettingsScrollPane(customPage);
        ScrollPane shortcutsPageScroll = createSettingsScrollPane(shortcutsPage);
        ScrollPane labsPageScroll = createSettingsScrollPane(labsPage);
        ScrollPane dataPageScroll = createSettingsScrollPane(dataPage);

        Map<ToggleButton, Node> pages = new LinkedHashMap<>();
        pages.put(generalTab, generalPageScroll);
        pages.put(personalizationTab, personalizationPageScroll);
        pages.put(customTab, customPageScroll);
        pages.put(shortcutsTab, shortcutsPageScroll);
        pages.put(labsTab, labsPageScroll);
        pages.put(dataTab, dataPageScroll);

        Runnable updateNavActive = () -> {
            for (ToggleButton tab : pages.keySet()) {
                tab.getStyleClass().remove("active");
                if (tab.isSelected()) {
                    tab.getStyleClass().add("active");
                }
            }
        };

        ToggleButton[] lastSelectedTab = new ToggleButton[] { generalTab };
        categoryGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                ToggleButton fallbackTab = lastSelectedTab[0] != null ? lastSelectedTab[0] : generalTab;
                Platform.runLater(() -> fallbackTab.setSelected(true));
                return;
            }

            ToggleButton selectedTab = (ToggleButton) newToggle;
            lastSelectedTab[0] = selectedTab;
            if (selectedTab == dataTab) {
                populateTrashSettingsList(trashItemsBox, trashSummary);
            }
            updateNavActive.run();
            switchSettingsPage(contentHost, resolveSettingsPage(selectedTab, pages, generalPageScroll));
        });
        generalTab.setSelected(true);

        shell.setLeft(navBar);
        shell.setCenter(contentHost);
        dialog.getDialogPane().setContent(shell);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        if (saveButton != null) {
            saveButton.getStyleClass().add("primary-save-button");
            saveButton.setDefaultButton(true);
            saveButton.setCursor(Cursor.HAND);
            saveButton.disableProperty().bind(customOptionsValid.not());
        }
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("ghost-cancel-button");
            cancelButton.setCancelButton(true);
            cancelButton.setCursor(Cursor.HAND);
        }
        dialog.setOnShown(event -> {
            Node buttonBar = dialog.getDialogPane().lookup(".button-bar");
            if (buttonBar != null) {
                buttonBar.toFront();
                buttonBar.setMouseTransparent(false);
            }
            if (saveButton != null) {
                saveButton.toFront();
                saveButton.setMouseTransparent(false);
            }
            if (cancelButton != null) {
                cancelButton.toFront();
                cancelButton.setMouseTransparent(false);
            }
            themeCoordinator.requestGlassRefresh(dialog.getDialogPane().getScene());
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveButtonType) {
            themeCoordinator.previewThemeSelection(originalThemeFamily, originalThemeAppearance, originalClassicPalette, null);
            iconService.previewSelection(originalThemeFamily, originalThemeIconBinding, originalIconPack);
            themeCoordinator.syncIconState();
            return result;
        }

        if (selectedLabsEnabled[0] != originalLabsEnabled) {
            experimentalFeaturesService.setLabsEnabled(selectedLabsEnabled[0]);
        }
        if (selectedThemeFamily[0] != originalThemeFamily || selectedClassicPalette[0] != originalClassicPalette) {
            themeCoordinator.saveThemePreference();
        }
        if (selectedThemeFamily[0] != originalThemeFamily
            || selectedIconPack[0] != originalIconPack
            || selectedThemeIconBinding[0] != originalThemeIconBinding) {
            iconService.commitSelection(selectedThemeFamily[0], selectedThemeIconBinding[0], selectedIconPack[0]);
            themeCoordinator.syncIconState();
        }
        if (selectedLanguage[0] != null && selectedLanguage[0] != originalPreferredLanguage) {
            localizationService.saveLanguagePreference(selectedLanguage[0]);
        }
        if (selectedFontWeight[0] != null && selectedFontWeight[0] != originalFontWeight) {
            fontService.selectFontWeight(selectedFontWeight[0]);
            onApplyCurrentFont.run();
        }
        commitCustomOptionsFromSettingsDialog(originalCustomTasks, originalCustomTags, customTaskRows, customTagRows);
        if (selectedTimeTextInputEnabled[0] != originalTimeTextInputEnabled && customOptionsService != null) {
            customOptionsService.setTimeTextInputEnabled(selectedTimeTextInputEnabled[0]);
        }
        if (selectedTagCommaSplitEnabled[0] != originalTagCommaSplitEnabled && customOptionsService != null) {
            customOptionsService.setTagCommaSplitEnabled(selectedTagCommaSplitEnabled[0]);
        }
        if (customOptionsService != null) {
            if (selectedHeatmapThreshold1[0] != originalHeatmapThreshold1) {
                customOptionsService.setHeatmapThreshold1(selectedHeatmapThreshold1[0]);
            }
            if (selectedHeatmapThreshold2[0] != originalHeatmapThreshold2) {
                customOptionsService.setHeatmapThreshold2(selectedHeatmapThreshold2[0]);
            }
            if (selectedHeatmapThreshold3[0] != originalHeatmapThreshold3) {
                customOptionsService.setHeatmapThreshold3(selectedHeatmapThreshold3[0]);
            }
            if (selectedHeatmapColorBinding[0] != originalHeatmapColorBinding) {
                customOptionsService.setHeatmapColorBindingEnabled(selectedHeatmapColorBinding[0]);
            }
            if (selectedHeatmapScheme[0] != originalHeatmapColorScheme) {
                customOptionsService.setHeatmapColorScheme(selectedHeatmapScheme[0]);
            }
        }
        if (selectedTimelineZoomWheelModifier[0] != originalTimelineZoomWheelModifier
             || !selectedTimelineZoomInShortcut[0].equals(originalTimelineZoomInShortcut)
             || !selectedTimelineZoomOutShortcut[0].equals(originalTimelineZoomOutShortcut)) {
             saveTimelineShortcutPreferences(
                 selectedTimelineZoomWheelModifier[0],
                 selectedTimelineZoomInShortcut[0],
                 selectedTimelineZoomOutShortcut[0]
             );
        }
        onRefreshAllViews.run();
        if (selectedLanguage[0] != null && selectedLanguage[0] != originalPreferredLanguage) {
            showRestartLanguageNotice(selectedLanguage[0]);
        }

        return result;
    }

    // ============ UI HELPERS ============

    private VBox createSettingsCard(String title, String subtitle) {
        VBox card = new VBox(12);
        card.getStyleClass().add("settings-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-card-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("settings-card-subtitle");
        subtitleLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, subtitleLabel);
        return card;
    }

    private ScrollPane createSettingsScrollPane(VBox page) {
        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.getStyleClass().add("settings-page-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setMinHeight(0);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setFocusTraversable(false);
        return scrollPane;
    }

    static <K, V> V resolveSettingsPage(K selectedKey, Map<K, V> pages, V generalPage) {
        V page = pages.get(selectedKey);
        return page != null ? page : generalPage;
    }

    private HBox createModernNumberInput(int initialValue, int min, int max, java.util.function.IntConsumer onChange) {
        HBox container = new HBox(0);
        container.getStyleClass().add("modern-number-input");
        container.setAlignment(Pos.CENTER);

        TextField field = new TextField(String.valueOf(initialValue));
        field.getStyleClass().add("number-input-field");
        field.setPrefWidth(44);
        field.setMaxWidth(44);
        field.setAlignment(Pos.CENTER);

        Button decrementButton = new Button("−");
        decrementButton.getStyleClass().add("number-input-button");
        decrementButton.setCursor(Cursor.HAND);

        Button incrementButton = new Button("+");
        incrementButton.getStyleClass().add("number-input-button");
        incrementButton.setCursor(Cursor.HAND);

        Runnable commitField = () -> {
            try {
                int value = Integer.parseInt(field.getText().trim());
                int clamped = Math.max(min, Math.min(max, value));
                field.setText(String.valueOf(clamped));
                onChange.accept(clamped);
            } catch (NumberFormatException ex) {
                field.setText(String.valueOf(initialValue));
                onChange.accept(initialValue);
            }
        };

        field.setOnAction(e -> commitField.run());
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitField.run();
            }
        });

        decrementButton.setOnAction(e -> {
            try {
                int value = Integer.parseInt(field.getText().trim());
                int next = Math.max(min, value - 1);
                field.setText(String.valueOf(next));
                onChange.accept(next);
            } catch (NumberFormatException ex) {
                field.setText(String.valueOf(min));
                onChange.accept(min);
            }
        });

        incrementButton.setOnAction(e -> {
            try {
                int value = Integer.parseInt(field.getText().trim());
                int next = Math.min(max, value + 1);
                field.setText(String.valueOf(next));
                onChange.accept(next);
            } catch (NumberFormatException ex) {
                field.setText(String.valueOf(min));
                onChange.accept(min);
            }
        });

        container.getChildren().addAll(decrementButton, field, incrementButton);
        return container;
    }

    private HBox createSettingRow(String title, String description, Node control) {
        HBox row = new HBox(12);
        row.getStyleClass().add("settings-row");
        VBox textBox = new VBox(4);
        Label rowTitle = new Label(title);
        rowTitle.getStyleClass().add("settings-row-title");
        Label rowDesc = new Label(description);
        rowDesc.getStyleClass().add("settings-row-desc");
        rowDesc.setWrapText(true);
        textBox.getChildren().addAll(rowTitle, rowDesc);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(textBox, spacer, control);
        return row;
    }

    private VBox createStackedSettingRow(String title, String description, Node control) {
        VBox row = new VBox(10);
        row.getStyleClass().addAll("settings-row", "settings-row-stacked");
        VBox textBox = new VBox(4);
        Label rowTitle = new Label(title);
        rowTitle.getStyleClass().add("settings-row-title");
        Label rowDesc = new Label(description);
        rowDesc.getStyleClass().add("settings-row-desc");
        rowDesc.setWrapText(true);
        textBox.getChildren().addAll(rowTitle, rowDesc);
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        row.getChildren().addAll(textBox, control);
        return row;
    }

    // ============ CUSTOM OPTIONS HELPERS ============

    private CustomOptionRow createCustomOptionRow(
        String originalValue,
        Runnable validationRefresh,
        List<CustomOptionRow> rows,
        VBox host
    ) {
        TextField field = new TextField();
        field.getStyleClass().addAll("settings-custom-options-input", "settings-custom-options-row-input");
        if (originalValue != null) {
            field.setText(originalValue);
        }
        Button removeButton = new Button(loc.text("common.remove"));
        removeButton.getStyleClass().addAll("button-secondary", "settings-custom-options-remove");

        HBox row = new HBox(10, field, removeButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("settings-custom-options-row");
        HBox.setHgrow(field, Priority.ALWAYS);

        CustomOptionRow[] holder = new CustomOptionRow[1];
        holder[0] = new CustomOptionRow(row, field, originalValue);
        removeButton.setOnAction(event -> {
            CustomOptionRow current = holder[0];
            if (current == null) {
                return;
            }
            host.getChildren().remove(current.row());
            rows.remove(current);
            if (validationRefresh != null) {
                validationRefresh.run();
            }
        });

        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (validationRefresh != null) {
                validationRefresh.run();
            }
        });

        return holder[0];
    }

    private ValidationResult validateCustomOptionRows(List<CustomOptionRow> rows, int maxAllowed) {
        if (rows == null || rows.isEmpty()) {
            return new ValidationResult(true, null);
        }

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        int count = 0;
        for (CustomOptionRow row : rows) {
            if (row == null || row.field() == null) {
                continue;
            }
            String normalized = normalizeCustomOptionInput(row.field().getText());
            if (normalized == null) {
                return new ValidationResult(false, loc.text("settings.customOptions.validation.empty"));
            }
            String key = customOptionKey(normalized);
            if (!keys.add(key)) {
                return new ValidationResult(false, loc.text("settings.customOptions.validation.duplicate"));
            }
            if (++count > maxAllowed) {
                return new ValidationResult(false, loc.text("settings.customOptions.validation.limit", maxAllowed));
            }
        }

        return new ValidationResult(true, null);
    }

    private void commitCustomOptionsFromSettingsDialog(
        List<String> originalTasks,
        List<String> originalTags,
        List<CustomOptionRow> taskRows,
        List<CustomOptionRow> tagRows
    ) {
        if (customOptionsService == null) {
            return;
        }

        ValidationResult taskValidation = validateCustomOptionRows(taskRows, CustomOptionsService.MAX_TASKS);
        ValidationResult tagValidation = validateCustomOptionRows(tagRows, CustomOptionsService.MAX_TAGS);
        if (!taskValidation.ok || !tagValidation.ok) {
            String message = !taskValidation.ok ? taskValidation.message : tagValidation.message;
            showError(loc.text("error.customOptions.save.title"), message != null ? message : loc.text("error.customOptions.save.message"));
            return;
        }

        List<String> updatedTasks = collectCustomOptionValues(taskRows);
        List<String> updatedTags = collectCustomOptionValues(tagRows);

        boolean tasksChanged = !Objects.equals(updatedTasks, originalTasks);
        boolean tagsChanged = !Objects.equals(updatedTags, originalTags);
        if (!tasksChanged && !tagsChanged) {
            return;
        }

        CustomOptionsMutation mutation = buildCustomOptionsMutation(originalTasks, originalTags, taskRows, tagRows);
        try {
            if (mutation.requiresScheduleSync()) {
                syncActiveSchedulesForCustomOptions(
                    mutation.taskRenamesByKey,
                    mutation.deletedTaskKeys,
                    mutation.tagRenamesByKey,
                    mutation.deletedTagKeys
                );
            }
            if (tasksChanged) {
                customOptionsService.replaceTasks(updatedTasks);
            }
            if (tagsChanged) {
                customOptionsService.replaceTags(updatedTags);
            }
        } catch (Exception exception) {
            showError(loc.text("error.customOptions.save.title"), exception.getMessage());
        }
    }

    private static List<String> collectCustomOptionValues(List<CustomOptionRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (CustomOptionRow row : rows) {
            if (row == null || row.field() == null) {
                continue;
            }
            String normalized = normalizeCustomOptionInput(row.field().getText());
            if (normalized == null) {
                continue;
            }
            String key = customOptionKey(normalized);
            if (!keys.add(key)) {
                continue;
            }
            values.add(normalized);
        }
        return values;
    }

    private CustomOptionsMutation buildCustomOptionsMutation(
        List<String> originalTasks,
        List<String> originalTags,
        List<CustomOptionRow> taskRows,
        List<CustomOptionRow> tagRows
    ) {
        LinkedHashSet<String> deletedTaskKeys = new LinkedHashSet<>();
        if (originalTasks != null) {
            for (String value : originalTasks) {
                String normalized = normalizeCustomOptionInput(value);
                if (normalized != null) {
                    deletedTaskKeys.add(customOptionKey(normalized));
                }
            }
        }
        LinkedHashSet<String> deletedTagKeys = new LinkedHashSet<>();
        if (originalTags != null) {
            for (String value : originalTags) {
                String normalized = normalizeCustomOptionInput(value);
                if (normalized != null) {
                    deletedTagKeys.add(customOptionKey(normalized));
                }
            }
        }

        LinkedHashMap<String, String> taskRenamesByKey = new LinkedHashMap<>();
        if (taskRows != null) {
            for (CustomOptionRow row : taskRows) {
                if (row == null || row.originalValue() == null || row.field() == null) {
                    continue;
                }
                String originalNormalized = normalizeCustomOptionInput(row.originalValue());
                if (originalNormalized == null) {
                    continue;
                }
                String originalKey = customOptionKey(originalNormalized);
                deletedTaskKeys.remove(originalKey);

                String updated = normalizeCustomOptionInput(row.field().getText());
                if (updated == null) {
                    continue;
                }
                if (!originalNormalized.equals(updated)) {
                    taskRenamesByKey.put(originalKey, updated);
                }
            }
        }

        LinkedHashMap<String, String> tagRenamesByKey = new LinkedHashMap<>();
        if (tagRows != null) {
            for (CustomOptionRow row : tagRows) {
                if (row == null || row.originalValue() == null || row.field() == null) {
                    continue;
                }
                String originalNormalized = normalizeCustomOptionInput(row.originalValue());
                if (originalNormalized == null) {
                    continue;
                }
                String originalKey = customOptionKey(originalNormalized);
                deletedTagKeys.remove(originalKey);

                String updated = normalizeCustomOptionInput(row.field().getText());
                if (updated == null) {
                    continue;
                }
                if (!originalNormalized.equals(updated)) {
                    tagRenamesByKey.put(originalKey, updated);
                }
            }
        }

        return new CustomOptionsMutation(taskRenamesByKey, deletedTaskKeys, tagRenamesByKey, deletedTagKeys);
    }

    private void syncActiveSchedulesForCustomOptions(
        Map<String, String> taskRenamesByKey,
        LinkedHashSet<String> deletedTaskKeys,
        Map<String, String> tagRenamesByKey,
        LinkedHashSet<String> deletedTagKeys
    ) throws SQLException {
        boolean hasTaskEdits = taskRenamesByKey != null && !taskRenamesByKey.isEmpty();
        boolean hasTaskDeletes = deletedTaskKeys != null && !deletedTaskKeys.isEmpty();
        boolean hasTagEdits = tagRenamesByKey != null && !tagRenamesByKey.isEmpty();
        boolean hasTagDeletes = deletedTagKeys != null && !deletedTagKeys.isEmpty();
        if (!hasTaskEdits && !hasTaskDeletes && !hasTagEdits && !hasTagDeletes) {
            return;
        }

        List<ScheduleItem> schedules = scheduleHandler.loadAllSchedules();
        for (ScheduleItem item : schedules) {
            if (item == null) {
                continue;
            }
            boolean changed = false;

            if (hasTaskEdits || hasTaskDeletes) {
                String category = item.getCategory();
                if (!ScheduleItem.isDefaultCategory(category)) {
                    String normalizedCategory = normalizeCustomOptionInput(category);
                    if (normalizedCategory != null) {
                        String key = customOptionKey(normalizedCategory);
                        String renamed = hasTaskEdits ? taskRenamesByKey.get(key) : null;
                        if (renamed != null) {
                            item.setCategory(renamed);
                            changed = true;
                        } else if (hasTaskDeletes && deletedTaskKeys.contains(key)) {
                            item.setCategory(ScheduleItem.DEFAULT_CATEGORY);
                            changed = true;
                        }
                    }
                }
            }

            if (hasTagEdits || hasTagDeletes) {
                List<String> existingTags = ScheduleItem.splitTags(item.getTags());
                if (!existingTags.isEmpty()) {
                    LinkedHashSet<String> updated = new LinkedHashSet<>();
                    for (String tag : existingTags) {
                        String normalizedTag = normalizeCustomOptionInput(tag);
                        if (normalizedTag == null) {
                            continue;
                        }
                        String key = customOptionKey(normalizedTag);
                        if (hasTagDeletes && deletedTagKeys.contains(key)) {
                            changed = true;
                            continue;
                        }
                        String renamed = hasTagEdits ? tagRenamesByKey.get(key) : null;
                        if (renamed != null) {
                            updated.add(renamed);
                            if (!renamed.equals(normalizedTag)) {
                                changed = true;
                            }
                        } else {
                            updated.add(normalizedTag);
                        }
                    }
                    if (changed) {
                        item.setTagNames(new ArrayList<>(updated));
                    }
                }
            }

            if (changed) {
                if (!scheduleHandler.saveSchedule(item)) {
                    throw new SQLException(loc.text("error.schedulePersist.message"));
                }
            }
        }
    }

    private static String normalizeCustomOptionInput(String raw) {
        if (raw == null) {
            return null;
        }
        String sanitized = raw.replace('\r', ' ').replace('\n', ' ').strip();
        return sanitized.isEmpty() ? null : sanitized;
    }

    private static String customOptionKey(String normalized) {
        return normalized.toLowerCase(Locale.ROOT);
    }

    // ============ TRASH MANAGEMENT ============

    private void populateTrashSettingsList(VBox host, Label summaryLabel) {
        host.getChildren().clear();
        try {
            List<ScheduleItem> deletedSchedules = scheduleHandler.loadDeletedSchedules();
            summaryLabel.setText(loc.text("trash.summary", deletedSchedules.size()));
            if (deletedSchedules.isEmpty()) {
                Label emptyLabel = new Label(loc.text("trash.empty"));
                emptyLabel.getStyleClass().add("settings-row-desc");
                host.getChildren().add(emptyLabel);
                return;
            }

            for (ScheduleItem schedule : deletedSchedules) {
                host.getChildren().add(createTrashRow(schedule, host, summaryLabel));
            }
        } catch (SQLException exception) {
            summaryLabel.setText(loc.text("trash.load.failed"));
            Label errorLabel = new Label(exception.getMessage());
            errorLabel.getStyleClass().add("settings-row-desc");
            errorLabel.setWrapText(true);
            host.getChildren().add(errorLabel);
        }
    }

    private Node createTrashRow(ScheduleItem item, VBox host, Label summaryLabel) {
        VBox textBox = new VBox(4);
        Label titleLabel = new Label(item.getName());
        titleLabel.getStyleClass().add("settings-row-title");
        Label metaLabel = new Label(buildTrashMetaText(item));
        metaLabel.getStyleClass().add("settings-row-desc");
        metaLabel.setWrapText(true);
        textBox.getChildren().addAll(titleLabel, metaLabel);

        Button restoreButton = new Button(loc.text("trash.restore"));
        restoreButton.getStyleClass().add("button-secondary");
        restoreButton.setOnAction(e -> {
            try {
                if (scheduleHandler.restoreDeletedSchedule(item.getId())) {
                    populateTrashSettingsList(host, summaryLabel);
                    onRefreshDataViews.run();
                }
            } catch (SQLException exception) {
                showError(loc.text("trash.restore.failed.title"), exception.getMessage());
            }
        });

        Button purgeButton = new Button(loc.text("trash.purge"));
        purgeButton.getStyleClass().add("button-secondary");
        purgeButton.setOnAction(e -> {
            try {
                if (!scheduleHandler.permanentlyDeleteSchedule(item.getId())) {
                    showInfo(loc.text("trash.purge.unavailable.title"), loc.text("trash.purge.unavailable.message"));
                    return;
                }
                populateTrashSettingsList(host, summaryLabel);
                onRefreshDataViews.run();
            } catch (SQLException exception) {
                showError(loc.text("trash.purge.failed.title"), exception.getMessage());
            }
        });

        HBox actions = new HBox(8, restoreButton, purgeButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, textBox, spacer, actions);
        row.getStyleClass().add("settings-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String buildTrashMetaText(ScheduleItem item) {
        LocalDateTime dueAt = item.getDueAt();
        LocalDateTime startAt = item.getStartAt();
        LocalDateTime deletedAt = item.getDeletedAt();
        String timeText;
        if (startAt != null && dueAt != null) {
            timeText = loc.format("format.trash.dateTime", startAt) + " -> " + loc.format("format.trash.dateTime", dueAt);
        } else if (dueAt != null) {
            timeText = loc.text("trash.meta.due", loc.format("format.trash.dateTime", dueAt));
        } else if (startAt != null) {
            timeText = loc.text("trash.meta.start", loc.format("format.trash.dateTime", startAt));
        } else {
            timeText = loc.text("trash.meta.unset");
        }
        String deletedText = deletedAt != null ? loc.format("format.trash.dateTime", deletedAt) : loc.text("trash.meta.none");
        return loc.text("trash.meta.template", timeText, deletedText);
    }

    // ============ SHORTCUT HELPERS ============

    private String wheelModifierDisplayName(WheelModifier modifier) {
        WheelModifier resolved = modifier != null ? modifier : WheelModifier.CTRL;
        return switch (resolved) {
            case CTRL -> loc.text("shortcut.modifier.ctrl");
            case ALT -> loc.text("shortcut.modifier.alt");
            case SHIFT -> loc.text("shortcut.modifier.shift");
            case META -> loc.text("shortcut.modifier.meta");
            case NONE -> loc.text("shortcut.modifier.none");
        };
    }

    private ShortcutSpec captureShortcutSpec() {
        Dialog<ShortcutSpec> dialog = new Dialog<>();
        dialog.setTitle(loc.text("settings.shortcuts.capture.title"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType(loc.text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().getStylesheets().setAll(themeCoordinator.getCurrentThemeStylesheets());
        dialog.getDialogPane().getStyleClass().add("settings-dialog-pane");
        applyDialogPreferences(dialog.getDialogPane());

        Label hint = new Label(loc.text("settings.shortcuts.capture.hint"));
        hint.getStyleClass().add("settings-row-desc");
        hint.setWrapText(true);
        dialog.getDialogPane().setContent(hint);

        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.setResult(null);
                dialog.close();
                event.consume();
                return;
            }

            ShortcutSpec spec = ShortcutSpec.fromKeyEvent(event);
            if (spec == null) {
                if (event.getCode() != null && event.getCode().isModifierKey()) {
                    hint.setText(loc.text("settings.shortcuts.capture.invalidModifier"));
                } else {
                    hint.setText(loc.text("settings.shortcuts.capture.invalid"));
                }
                event.consume();
                return;
            }

            dialog.setResult(spec);
            dialog.close();
            event.consume();
        });

        dialog.setResultConverter(button -> null);
        dialog.setOnShown(event -> Platform.runLater(() -> dialog.getDialogPane().requestFocus()));

        Optional<ShortcutSpec> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void saveTimelineShortcutPreferences(WheelModifier modifier, ShortcutSpec zoomIn, ShortcutSpec zoomOut) {
        WheelModifier resolvedModifier = modifier != null ? modifier : WheelModifier.CTRL;
        ShortcutSpec resolvedZoomIn = zoomIn != null ? zoomIn : ShortcutSpec.of(true, false, false, false, KeyCode.EQUALS);
        ShortcutSpec resolvedZoomOut = zoomOut != null ? zoomOut : ShortcutSpec.of(true, false, false, false, KeyCode.MINUS);

        preferencesStore.put(PREF_TIMELINE_ZOOM_WHEEL_MODIFIER_KEY, resolvedModifier.getId());
        preferencesStore.put(PREF_TIMELINE_ZOOM_IN_KEY, resolvedZoomIn.toPreferenceString());
        preferencesStore.put(PREF_TIMELINE_ZOOM_OUT_KEY, resolvedZoomOut.toPreferenceString());

        if (onTimelineShortcutsChanged != null) {
            onTimelineShortcutsChanged.run();
        }
    }

    // ============ DIALOG HELPERS ============

    private void applyDialogPreferences(DialogPane pane) {
        if (pane == null) {
            return;
        }
        pane.getStylesheets().setAll(themeCoordinator.getCurrentThemeStylesheets());
        fontService.applyTo(pane, localizationService.getActiveLanguage());
        themeCoordinator.updateDialogGlass(pane);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        applyDialogPreferences(alert.getDialogPane());
        alert.setTitle(loc.text("alert.error.title"));
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        applyDialogPreferences(alert.getDialogPane());
        alert.setTitle(loc.text("alert.info.title"));
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showRestartLanguageNotice(AppLanguage language) {
        showInfo(
            loc.text("settings.language.restart.title"),
            loc.text("settings.language.restart.message", localizationService.languageLabel(language))
        );
    }

    private void switchSettingsPage(StackPane host, Node page) {
        if (host.getChildren().isEmpty()) {
            page.setOpacity(0);
            page.setTranslateX(8);
            host.getChildren().setAll(page);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), page);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            TranslateTransition moveIn = new TranslateTransition(Duration.millis(180), page);
            moveIn.setFromX(8);
            moveIn.setToX(0);
            new ParallelTransition(fadeIn, moveIn).play();
            themeCoordinator.requestGlassRefresh(host.getScene());
            return;
        }
        Node current = host.getChildren().get(0);
        if (current == page) {
            return;
        }
        FadeTransition fadeOut = new FadeTransition(Duration.millis(130), current);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        TranslateTransition moveOut = new TranslateTransition(Duration.millis(130), current);
        moveOut.setFromX(0);
        moveOut.setToX(-8);
        ParallelTransition out = new ParallelTransition(fadeOut, moveOut);
        out.setOnFinished(evt -> {
            page.setOpacity(0);
            page.setTranslateX(8);
            host.getChildren().setAll(page);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), page);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            TranslateTransition moveIn = new TranslateTransition(Duration.millis(180), page);
            moveIn.setFromX(8);
            moveIn.setToX(0);
            ParallelTransition in = new ParallelTransition(fadeIn, moveIn);
            in.setOnFinished(event -> themeCoordinator.requestGlassRefresh(host.getScene()));
            in.play();
        });
        out.play();
        themeCoordinator.requestGlassRefresh(host.getScene());
    }

    // ============ INNER TYPES ============

    private record CustomOptionRow(HBox row, TextField field, String originalValue) {
    }

    private static final class ValidationResult {
        private final boolean ok;
        private final String message;

        private ValidationResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }

    private static final class CustomOptionsMutation {
        private final LinkedHashMap<String, String> taskRenamesByKey;
        private final LinkedHashSet<String> deletedTaskKeys;
        private final LinkedHashMap<String, String> tagRenamesByKey;
        private final LinkedHashSet<String> deletedTagKeys;

        private CustomOptionsMutation(
            LinkedHashMap<String, String> taskRenamesByKey,
            LinkedHashSet<String> deletedTaskKeys,
            LinkedHashMap<String, String> tagRenamesByKey,
            LinkedHashSet<String> deletedTagKeys
        ) {
            this.taskRenamesByKey = taskRenamesByKey != null ? taskRenamesByKey : new LinkedHashMap<>();
            this.deletedTaskKeys = deletedTaskKeys != null ? deletedTaskKeys : new LinkedHashSet<>();
            this.tagRenamesByKey = tagRenamesByKey != null ? tagRenamesByKey : new LinkedHashMap<>();
            this.deletedTagKeys = deletedTagKeys != null ? deletedTagKeys : new LinkedHashSet<>();
        }

        private boolean requiresScheduleSync() {
            return !taskRenamesByKey.isEmpty()
                || !deletedTaskKeys.isEmpty()
                || !tagRenamesByKey.isEmpty()
                || !deletedTagKeys.isEmpty();
        }
    }
}
