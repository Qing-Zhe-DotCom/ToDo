package com.example.controller;

import java.sql.SQLException;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.function.IntConsumer;

import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;
import com.example.view.FlowchartView;
import com.example.view.HeatmapView;
import com.example.view.InfoPanelView;
import com.example.view.ScheduleDialog;
import com.example.view.ScheduleListView;
import com.example.view.TimelineView;
import com.example.view.View;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Side;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.Group;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MainController {
    
    private BorderPane root;
    private Scene scene;
    
    // 各个视图
    private ScheduleListView scheduleListView;
    private TimelineView timelineView;
    private HeatmapView heatmapView;
    private FlowchartView flowchartView;
    private InfoPanelView infoPanelView;
    
    // 当前选中的视图
    private View currentView;
    private Schedule selectedSchedule;

    private VBox sidebar;
    private VBox bottomActions;
    private Separator bottomActionsSeparator;
    private TextField searchField;
    private ToggleButton collapseToggle;
    private ToggleButton featurePanelToggle;
    private Button themeButton;
    private Pane collapseToggleIcon;
    private Pane featureToggleIcon;
    private Pane themeIcon;
    private boolean sidebarCollapsed = false;
    private boolean featurePanelExpanded = false;
    private final Map<Labeled, String[]> collapsibleLabels = new LinkedHashMap<>();
    private String importedThemeStylesheet;
    private final Map<String, String> builtinThemes = createBuiltinThemeMap();
    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);
    private static final String PREF_THEME_KEY = "todo.theme";
    private static final String PREF_IMPORTED_THEME_KEY = "todo.theme.imported.path";
    private final ScheduleDAO scheduleDAO = new ScheduleDAO();
    private IntConsumer pendingCountListener;
    
    // 主题管理
    private String currentTheme = "light";
    
    public MainController() {
        loadThemePreference();
        initializeUI();
    }
    
    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("root");

        // 创建左侧导航栏
        createSidebar();
        
        // 创建右侧信息面板
        createInfoPanel();
        
        // 初始化各个视图
        scheduleListView = new ScheduleListView(this);
        timelineView = new TimelineView(this);
        heatmapView = new HeatmapView(this);
        flowchartView = new FlowchartView(this);
        //infoPanelView = new InfoPanelView(this);
        
        // 默认显示日程列表视图
        showView(scheduleListView);
    }

    private void createSidebar() {
        sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        collapseToggle = new ToggleButton();
        collapseToggle.getStyleClass().addAll("nav-button", "sidebar-collapse-button");
        collapseToggle.setMaxWidth(Double.MAX_VALUE);
        collapseToggleIcon = createSvgIcon("/icons/macaron-logo-triangle-arrow.svg", "展开或收起侧边栏", 24);
        collapseToggle.setGraphic(collapseToggleIcon);
        collapseToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        collapseToggle.setAccessibleText("展开或收起侧边栏");
        collapseToggle.setTooltip(new Tooltip("展开或收起侧边栏"));
        collapseToggle.setOnAction(e -> {
            sidebarCollapsed = collapseToggle.isSelected();
            updateSidebarCollapseState();
        });

        searchField = new TextField();
        searchField.getStyleClass().add("search-field");
        searchField.setPromptText("搜索日程...");
        searchField.setOnAction(e -> performSearch(searchField.getText()));

        // 导航按钮
        ToggleGroup navGroup = new ToggleGroup();

        ToggleButton scheduleBtn = createNavButton("/icons/macaron-logo-calendar.svg", "日程管理", navGroup);
        scheduleBtn.setSelected(true);
        scheduleBtn.setOnAction(e -> showView(scheduleListView));

        ToggleButton timelineBtn = createNavButton("/icons/macaron-logo-timeline.svg", "日程时间轴", navGroup);
        timelineBtn.setOnAction(e -> showView(timelineView));

        ToggleButton heatmapBtn = createNavButton("/icons/macaron-logo-grid-heatmap.svg", "日程热力图", navGroup);
        heatmapBtn.setOnAction(e -> showView(heatmapView));

        ToggleButton flowchartBtn = createNavButton("/icons/macaron-logo-flowchart.svg", "日程流程图", navGroup);
        flowchartBtn.setOnAction(e -> showView(flowchartView));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label functionTitle = new Label("侧边功能栏");
        functionTitle.getStyleClass().addAll("label-hint", "sidebar-function-title");

        Button newScheduleButton = createActionButton("/icons/macaron-logo-new-schedule.svg", "新建日程", this::openNewScheduleDialog);
        themeButton = createActionButton("/icons/macaron-logo-theme.svg", "主题", this::togglePrimaryTheme);
        themeButton.setOnContextMenuRequested(e -> {
            showThemeMenu(themeButton);
            e.consume();
        });
        themeIcon = (Pane) themeButton.getGraphic();
        Button loginButton = createActionButton("/icons/macaron-logo-user.svg", "登录", this::showLoginDialog);
        Button settingsButton = createActionButton("/icons/macaron-logo-settings.svg", "设置", this::showSettingsDialog);
        Button exitButton = createActionButton("/icons/macaron-logo-logout.svg", "退出", Platform::exit);

        bottomActions = new VBox(6);
        bottomActions.getStyleClass().add("sidebar-bottom-actions");
        bottomActions.getChildren().addAll(
            functionTitle,
            newScheduleButton,
            themeButton,
            loginButton,
            settingsButton,
            exitButton
        );

        featurePanelToggle = new ToggleButton();
        featurePanelToggle.getStyleClass().addAll("nav-button", "sidebar-feature-toggle");
        featurePanelToggle.setMaxWidth(Double.MAX_VALUE);
        featureToggleIcon = createSvgIcon("/icons/macaron-logo-triangle-arrow.svg", "展开或收起侧边功能栏", 24);
        featurePanelToggle.setGraphic(featureToggleIcon);
        featurePanelToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        featurePanelToggle.setAccessibleText("展开或收起侧边功能栏");
        featurePanelToggle.setTooltip(new Tooltip("展开或收起侧边功能栏"));
        featurePanelToggle.setOnAction(e -> {
            featurePanelExpanded = featurePanelToggle.isSelected();
            updateFeaturePanelState();
        });

        bottomActionsSeparator = new Separator();

        sidebar.getChildren().addAll(
            collapseToggle,
            searchField,
            new Separator(),
            scheduleBtn,
            timelineBtn,
            heatmapBtn,
            flowchartBtn,
            spacer,
            bottomActionsSeparator,
            bottomActions,
            featurePanelToggle
        );

        updateSidebarCollapseState();
        root.setLeft(sidebar);
    }

    private ToggleButton createNavButton(String iconPath, String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("nav-button");
        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setGraphic(createSvgIcon(iconPath, text, 24));
        button.setGraphicTextGap(8);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setWrapText(false);
        button.setAccessibleText(text);
        button.setTooltip(new Tooltip(text));
        registerCollapsibleControl(button, text, "", text);
        return button;
    }

    private Button createActionButton(String iconPath, String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("nav-button", "sidebar-action-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setGraphic(createSvgIcon(iconPath, text, 24));
        button.setGraphicTextGap(8);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setWrapText(false);
        button.setAccessibleText(text);
        button.setTooltip(new Tooltip(text));
        button.setOnAction(e -> action.run());
        registerCollapsibleControl(button, text, "", text);
        return button;
    }

    private void registerCollapsibleControl(Labeled control, String expandedText, String collapsedText, String tooltipText) {
        collapsibleLabels.put(control, new String[] { expandedText, collapsedText });
        control.setTooltip(new Tooltip(tooltipText));
    }

    private void updateSidebarCollapseState() {
        if (sidebar == null) return;

        double collapsedWidth = 52;
        double expandedWidth = 256;
        sidebar.setPrefWidth(sidebarCollapsed ? collapsedWidth : expandedWidth);
        sidebar.setMinWidth(sidebarCollapsed ? collapsedWidth : expandedWidth);
        sidebar.setMaxWidth(sidebarCollapsed ? collapsedWidth : expandedWidth);
        sidebar.setPadding(sidebarCollapsed ? new Insets(6) : new Insets(10));
        sidebar.setSpacing(sidebarCollapsed ? 6 : 8);
        sidebar.setAlignment(sidebarCollapsed ? Pos.TOP_CENTER : Pos.TOP_LEFT);

        if (collapseToggle != null) {
            collapseToggle.setText("");
            collapseToggle.setAlignment(sidebarCollapsed ? Pos.CENTER : Pos.CENTER_LEFT);
            collapseToggle.setPadding(sidebarCollapsed ? new Insets(4, 0, 4, 0) : new Insets(8, 12, 8, 12));
            if (collapseToggleIcon != null) {
                collapseToggleIcon.setRotate(sidebarCollapsed ? 0 : 90);
            }
        }

        if (searchField != null) {
            searchField.setVisible(!sidebarCollapsed);
            searchField.setManaged(!sidebarCollapsed);
        }

        for (Map.Entry<Labeled, String[]> entry : collapsibleLabels.entrySet()) {
            Labeled control = entry.getKey();
            String[] labels = entry.getValue();
            control.setText(sidebarCollapsed ? labels[1] : labels[0]);
            control.setAlignment(sidebarCollapsed ? Pos.CENTER : Pos.CENTER_LEFT);
            control.setContentDisplay(sidebarCollapsed ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
            control.setPadding(sidebarCollapsed ? new Insets(4, 0, 4, 0) : new Insets(10, 12, 10, 12));
            if (sidebarCollapsed) {
                control.setMinSize(30, 30);
                control.setPrefSize(30, 30);
            } else {
                control.setMinWidth(0);
                control.setPrefHeight(40);
                control.setMaxWidth(Double.MAX_VALUE);
            }
        }

        updateFeaturePanelState();
    }

    public Pane createSvgIcon(String resourcePath, String title, double size) {
        Group iconGroup = loadSvgGraphic(resourcePath);
        Pane container = new Pane(iconGroup);
        container.getStyleClass().add("sidebar-svg-icon");
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        container.setClip(new Rectangle(size, size));
        if (title != null && !title.isEmpty()) {
            container.setAccessibleText(title);
            Tooltip.install(container, new Tooltip(title));
        }
        return container;
    }

    private Group loadSvgGraphic(String resourcePath) {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return new Group();
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(stream);
            Element svg = document.getDocumentElement();
            Group group = new Group();
            double[] viewBox = parseViewBox(svg.getAttribute("viewBox"));
            parseSvgChildren(svg, group, 0, 0);
            double scale = 24.0 / Math.max(viewBox[2], viewBox[3]);
            group.getTransforms().add(new Scale(scale, scale));
            group.getTransforms().add(new Translate(-viewBox[0], -viewBox[1]));
            return group;
        } catch (Exception e) {
            return new Group();
        }
    }

    private void parseSvgChildren(Element parent, Group target, double offsetX, double offsetY) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (!(child instanceof Element)) {
                continue;
            }
            Element element = (Element) child;
            String tag = element.getTagName();
            if ("g".equals(tag)) {
                double[] translate = parseTranslate(element.getAttribute("transform"));
                parseSvgChildren(element, target, offsetX + translate[0], offsetY + translate[1]);
                continue;
            }
            Shape shape = createShapeFromElement(element);
            if (shape == null) {
                continue;
            }
            shape.setTranslateX(offsetX);
            shape.setTranslateY(offsetY);
            target.getChildren().add(shape);
        }
    }

    private Shape createShapeFromElement(Element element) {
        String tag = element.getTagName();
        if ("circle".equals(tag)) {
            Circle circle = new Circle(
                parseDouble(element.getAttribute("cx")),
                parseDouble(element.getAttribute("cy")),
                parseDouble(element.getAttribute("r"))
            );
            applyShapeStyle(circle, element);
            return circle;
        }
        if ("rect".equals(tag)) {
            Rectangle rectangle = new Rectangle(
                parseDouble(element.getAttribute("x")),
                parseDouble(element.getAttribute("y")),
                parseDouble(element.getAttribute("width")),
                parseDouble(element.getAttribute("height"))
            );
            double rx = parseDouble(element.getAttribute("rx"));
            if (rx > 0) {
                rectangle.setArcWidth(rx * 2);
                rectangle.setArcHeight(rx * 2);
            }
            applyShapeStyle(rectangle, element);
            return rectangle;
        }
        if ("path".equals(tag)) {
            SVGPath path = new SVGPath();
            path.setContent(element.getAttribute("d"));
            applyShapeStyle(path, element);
            return path;
        }
        return null;
    }

    private void applyShapeStyle(Shape shape, Element element) {
        Color fill = parsePaint(element.getAttribute("fill"));
        Color stroke = parsePaint(element.getAttribute("stroke"));
        if (fill != null) {
            shape.setFill(fill);
        } else {
            shape.setFill(Color.TRANSPARENT);
        }
        if (stroke != null) {
            shape.setStroke(stroke);
            shape.setStrokeWidth(parseDoubleOrDefault(element.getAttribute("stroke-width"), 1));
        }
        String lineCap = element.getAttribute("stroke-linecap");
        if ("round".equalsIgnoreCase(lineCap)) {
            shape.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        String lineJoin = element.getAttribute("stroke-linejoin");
        if ("round".equalsIgnoreCase(lineJoin)) {
            shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }
    }

    private Color parsePaint(String value) {
        if (value == null || value.isEmpty() || "none".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private double[] parseViewBox(String viewBox) {
        if (viewBox == null || viewBox.isEmpty()) {
            return new double[] {0, 0, 100, 100};
        }
        String[] values = viewBox.trim().split("\\s+");
        if (values.length != 4) {
            return new double[] {0, 0, 100, 100};
        }
        return new double[] {
            parseDouble(values[0]),
            parseDouble(values[1]),
            parseDouble(values[2]),
            parseDouble(values[3])
        };
    }

    private double[] parseTranslate(String transform) {
        if (transform == null || !transform.startsWith("translate")) {
            return new double[] {0, 0};
        }
        int start = transform.indexOf('(');
        int end = transform.indexOf(')');
        if (start < 0 || end <= start) {
            return new double[] {0, 0};
        }
        String[] values = transform.substring(start + 1, end).trim().split("[,\\s]+");
        if (values.length == 1) {
            return new double[] {parseDouble(values[0]), 0};
        }
        return new double[] {parseDouble(values[0]), parseDouble(values[1])};
    }

    private double parseDouble(String value) {
        return parseDoubleOrDefault(value, 0);
    }

    private double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private void updateFeaturePanelState() {
        if (featurePanelToggle != null) {
            featurePanelToggle.setText("");
            featurePanelToggle.setAlignment(sidebarCollapsed ? Pos.CENTER : Pos.CENTER_LEFT);
            featurePanelToggle.setPadding(sidebarCollapsed ? new Insets(4, 0, 4, 0) : new Insets(8, 12, 8, 12));
            if (featureToggleIcon != null) {
                featureToggleIcon.setRotate(featurePanelExpanded ? 90 : 0);
            }
        }
        if (bottomActions != null) {
            bottomActions.setVisible(featurePanelExpanded);
            bottomActions.setManaged(featurePanelExpanded);
        }
        if (bottomActionsSeparator != null) {
            bottomActionsSeparator.setVisible(featurePanelExpanded);
            bottomActionsSeparator.setManaged(featurePanelExpanded);
        }
        updateThemeIconState();
    }

    private void createInfoPanel() {
        infoPanelView = new InfoPanelView(this);
        infoPanelView.hideImmediately();
        root.setRight(infoPanelView.getView());
    }
    
    private void showView(View view) {
        currentView = view;
        root.setCenter(view.getView());
        view.refresh();
    }
    
    public void showScheduleDetails(Schedule schedule) {
        this.selectedSchedule = schedule;
        infoPanelView.setSchedule(schedule);
        infoPanelView.showWithAnimation();
    }

    public void closeScheduleDetails() {
        infoPanelView.hideWithAnimation();
    }

    public boolean isScheduleSelected(Schedule schedule) {
        if (selectedSchedule == null || schedule == null) {
            return false;
        }
        if (selectedSchedule.getId() > 0 && schedule.getId() > 0) {
            return selectedSchedule.getId() == schedule.getId();
        }
        return selectedSchedule == schedule;
    }
    
    public Schedule getSelectedSchedule() {
        return selectedSchedule;
    }
    
    public void refreshAllViews() {
        if (currentView != null) {
            currentView.refresh();
        }
        infoPanelView.refresh();
        updatePendingCountBadge();
    }
    
    private void switchTheme(String theme) {
        importedThemeStylesheet = null;
        currentTheme = theme;
        applyThemeStylesheets(resolveBuiltinThemeStylesheets(theme));
        saveThemePreference();
        updateThemeIconState();
    }

    private void applyThemeStylesheets(List<String> stylesheets) {
        if (scene == null || stylesheets.isEmpty()) {
            return;
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().addAll(stylesheets);
        refreshAllViews();
        updateThemeIconState();
    }

    private List<String> resolveBuiltinThemeStylesheets(String theme) {
        List<String> stylesheets = new ArrayList<>();
        stylesheets.add(resolveResourceStylesheet("/styles/base.css"));

        stylesheets.add(resolveResourceStylesheet("/styles/light-theme.css"));
        if ("mint".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/mint-theme.css"));
        } else if ("ocean".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/ocean-theme.css"));
        } else if ("sunset".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/sunset-theme.css"));
        } else if ("lavender".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/lavender-theme.css"));
        } else if ("forest".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/forest-theme.css"));
        } else if ("slate".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/slate-theme.css"));
        } else if ("macaron".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/macaron-theme.css"));
        }
        return stylesheets;
    }

    private List<String> resolveImportedThemeStylesheets() {
        List<String> stylesheets = resolveBuiltinThemeStylesheets("light");
        if (importedThemeStylesheet != null && !importedThemeStylesheet.isBlank()) {
            stylesheets.add(importedThemeStylesheet);
        }
        return stylesheets;
    }

    private String resolveResourceStylesheet(String resourcePath) {
        return getClass().getResource(resourcePath).toExternalForm();
    }

    private Map<String, String> createBuiltinThemeMap() {
        Map<String, String> themes = new LinkedHashMap<>();
        themes.put("light", "浅色");
        themes.put("mint", "薄荷");
        themes.put("ocean", "海洋");
        themes.put("sunset", "落日");
        themes.put("lavender", "薰衣草");
        themes.put("forest", "森林");
        themes.put("slate", "石板");
        themes.put("macaron", "马卡龙");
        return themes;
    }

    private void showThemeMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();

        for (Map.Entry<String, String> entry : builtinThemes.entrySet()) {
            String prefix = entry.getKey().equals(currentTheme) ? "✓ " : "";
            MenuItem item = new MenuItem(prefix + "使用" + entry.getValue() + "主题");
            item.setOnAction(e -> switchTheme(entry.getKey()));
            menu.getItems().add(item);
        }

        menu.getItems().add(new SeparatorMenuItem());
        MenuItem importItem = new MenuItem("导入主题...");
        importItem.setOnAction(e -> importThemeFromFile());
        menu.getItems().add(importItem);

        menu.show(anchor, Side.RIGHT, 0, 0);
    }

    private void importThemeFromFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入主题文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSS 文件", "*.css"));
        File selected = chooser.showOpenDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (selected == null) {
            return;
        }

        importedThemeStylesheet = selected.toURI().toString();
        currentTheme = "imported";
        applyThemeStylesheets(resolveImportedThemeStylesheets());
        saveThemePreference();
        updateThemeIconState();
        showInfo("主题已导入", "已应用外部主题文件:\n" + selected.getAbsolutePath());
    }

    private void togglePrimaryTheme() {
        List<String> keys = new ArrayList<>(builtinThemes.keySet());
        int currentIndex = keys.indexOf(currentTheme);
        int nextIndex = (currentIndex + 1) % keys.size();
        if (currentIndex < 0) {
            nextIndex = 0;
        }
        switchTheme(keys.get(nextIndex));
    }

    private void loadThemePreference() {
        String savedTheme = preferences.get(PREF_THEME_KEY, "light");
        String savedImported = preferences.get(PREF_IMPORTED_THEME_KEY, "");
        importedThemeStylesheet = savedImported == null || savedImported.isBlank() ? null : savedImported;
        if (builtinThemes.containsKey(savedTheme) || "imported".equals(savedTheme)) {
            currentTheme = savedTheme;
        } else {
            currentTheme = "light";
        }
    }

    private void saveThemePreference() {
        preferences.put(PREF_THEME_KEY, currentTheme);
        if (importedThemeStylesheet == null || importedThemeStylesheet.isBlank()) {
            preferences.remove(PREF_IMPORTED_THEME_KEY);
        } else {
            preferences.put(PREF_IMPORTED_THEME_KEY, importedThemeStylesheet);
        }
    }

    private void applySavedThemeIfNeeded() {
        if (scene == null) {
            return;
        }
        if ("imported".equals(currentTheme) && importedThemeStylesheet != null && !importedThemeStylesheet.isBlank()) {
            try {
                applyThemeStylesheets(resolveImportedThemeStylesheets());
                return;
            } catch (Exception ignored) {
                currentTheme = "light";
                importedThemeStylesheet = null;
                saveThemePreference();
            }
        }
        applyThemeStylesheets(resolveBuiltinThemeStylesheets(currentTheme));
    }

    private void updateThemeIconState() {
        if (themeIcon == null) {
            return;
        }
        themeIcon.getStyleClass().removeAll("theme-icon-light", "theme-icon-dark");
        themeIcon.getStyleClass().add("theme-icon-light");
        themeIcon.setRotate(0);
        themeIcon.setOpacity(1.0);
    }
    
    private void performSearch(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            scheduleListView.searchSchedules(keyword.trim());
            showView(scheduleListView);
        }
    }
    
    public void openNewScheduleDialog() {
        ScheduleDialog dialog = new ScheduleDialog(null, this);
        Optional<Schedule> result = dialog.showAndWait();
        
        result.ifPresent(schedule -> {
            try {
                scheduleListView.addSchedule(schedule);
                refreshAllViews();
            } catch (SQLException e) {
                showError("添加日程失败", e.getMessage());
            }
        });
    }
    
    public void openEditScheduleDialog(Schedule schedule) {
        ScheduleDialog dialog = new ScheduleDialog(schedule, this);
        Optional<Schedule> result = dialog.showAndWait();
        
        result.ifPresent(updatedSchedule -> {
            try {
                scheduleListView.updateSchedule(updatedSchedule);
                refreshAllViews();
            } catch (SQLException e) {
                showError("更新日程失败", e.getMessage());
            }
        });
    }
    
    private void showLoginDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("登录");
        alert.setHeaderText("用户登录");
        alert.setContentText("登录功能开发中...");
        alert.showAndWait();
    }
    
    private void showSettingsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("设置");
        alert.setHeaderText("应用设置与关于");
        String themeDisplay = "imported".equals(currentTheme) ? "外部导入主题" : builtinThemes.getOrDefault(currentTheme, "浅色");
        alert.setContentText("主题: " + themeDisplay + "\n版本: 1.0\nToDo 日程管理应用");
        alert.showAndWait();
    }
    
    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public BorderPane getRoot() {
        return root;
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public List<String> getCurrentThemeStylesheets() {
        if (scene != null && !scene.getStylesheets().isEmpty()) {
            return new ArrayList<>(scene.getStylesheets());
        }
        if ("imported".equals(currentTheme)) {
            return resolveImportedThemeStylesheets();
        }
        return resolveBuiltinThemeStylesheets(currentTheme);
    }

    public void setPendingCountListener(IntConsumer listener) {
        pendingCountListener = listener;
        updatePendingCountBadge();
    }
    
    public void setScene(Scene scene) {
        this.scene = scene;
        applySavedThemeIfNeeded();
        setupGlobalInfoPanelInteractions();
    }

    private void setupGlobalInfoPanelInteractions() {
        if (scene == null) {
            return;
        }

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!infoPanelView.isPanelVisible()) {
                return;
            }

            Object target = event.getTarget();
            if (target instanceof Node && isDescendant((Node) target, infoPanelView.getView())) {
                return;
            }

            closeScheduleDetails();
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && infoPanelView.isPanelVisible()) {
                closeScheduleDetails();
                event.consume();
            }
        });
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
    
    public void initialize() {
        refreshAllViews();
    }
    
    public void shutdown() {
    }

    private void updatePendingCountBadge() {
        if (pendingCountListener == null) {
            return;
        }
        try {
            long pending = scheduleDAO.getAllSchedules().stream().filter(schedule -> !schedule.isCompleted()).count();
            pendingCountListener.accept((int) Math.min(Integer.MAX_VALUE, pending));
        } catch (SQLException ignored) {
            pendingCountListener.accept(0);
        }
    }
}
