package com.example.controller;

import java.sql.SQLException;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.IntConsumer;

import com.example.application.ApplicationContext;
import com.example.application.MainViewModel;
import com.example.application.NavigationService;
import com.example.application.ScheduleService;
import com.example.application.ThemeService;
import com.example.model.Schedule;
import com.example.view.FlowchartView;
import com.example.view.HeatmapView;
import com.example.view.InfoPanelView;
import com.example.view.ScheduleCardStyleSupport;
import com.example.view.ScheduleCompletionParticipant;
import com.example.view.ScheduleDialog;
import com.example.view.ScheduleListView;
import com.example.view.TimelineView;
import com.example.view.View;

import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Side;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
import javafx.scene.transform.Rotate;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.geometry.VPos;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MainController {
    private final ApplicationContext applicationContext;
    private final MainViewModel mainViewModel;
    private final ScheduleService scheduleService;
    private final NavigationService navigationService;
    private final ThemeService themeService;

    @FXML
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

    private VBox sidebar;
    private VBox bottomActions;
    private Separator bottomActionsSeparator;
    private Label functionTitle;
    private TextField searchField;
    private ToggleButton collapseToggle;
    private ToggleButton featurePanelToggle;
    private Button themeButton;
    private Pane collapseToggleIcon;
    private Pane featureToggleIcon;
    private Pane themeIcon;
    private boolean sidebarCollapsed = false;
    private boolean featurePanelExpanded = false;
    private boolean uiInitialized = false;
    private final Map<Labeled, String[]> collapsibleLabels = new LinkedHashMap<>();
    private String importedThemeStylesheet;
    private final Map<String, String> builtinThemes;
    private final List<String> scheduleCardStyles;
    private static final String DEFAULT_SCHEDULE_CARD_STYLE = ScheduleCardStyleSupport.getDefaultStyleName();
    private final ExecutorService scheduleCompletionExecutor;
    private final ScheduleCompletionCoordinator scheduleCompletionCoordinator;
    private IntConsumer pendingCountListener;
    private int lastKnownPendingCount = -1;
    
    // 主题管理
    private String currentTheme = "light";
    private String currentScheduleCardStyle = DEFAULT_SCHEDULE_CARD_STYLE;
    
    public MainController() {
        this(ApplicationContext.createDefault());
    }

    public MainController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.mainViewModel = applicationContext.getMainViewModel();
        this.scheduleService = applicationContext.getScheduleService();
        this.navigationService = mainViewModel.getNavigationService();
        this.themeService = mainViewModel.getThemeService();
        this.builtinThemes = new LinkedHashMap<>(themeService.getBuiltinThemes());
        this.scheduleCardStyles = List.copyOf(themeService.getScheduleCardStyles());
        syncThemeState();
        scheduleCompletionExecutor = Executors.newSingleThreadExecutor(createCompletionThreadFactory());
        scheduleCompletionCoordinator = new ScheduleCompletionCoordinator(
            scheduleService::updateScheduleStatus,
            new ScheduleCompletionCoordinator.MutationApplier() {
                @Override
                public void applyOptimistic(ScheduleCompletionMutation mutation) {
                    applyCompletionMutationLocally(mutation);
                }

                @Override
                public void confirm(ScheduleCompletionMutation mutation) {
                    confirmCompletionMutationLocally(mutation);
                }

                @Override
                public void revert(ScheduleCompletionMutation mutation) {
                    revertCompletionMutationLocally(mutation);
                }
            },
            this::reportCompletionPersistenceFailure,
            scheduleCompletionExecutor,
            Platform::runLater
        );
    }

    @FXML
    private void initialize() {
        initializeUI();
    }
    
    private void initializeUI() {
        if (uiInitialized) {
            return;
        }
        if (root == null) {
            root = new BorderPane();
        }
        if (!root.getStyleClass().contains("root")) {
            root.getStyleClass().add("root");
        }

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
        uiInitialized = true;
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

        functionTitle = new Label("侧边功能栏");
        functionTitle.getStyleClass().addAll("label-hint", "sidebar-function-title");

        Button newScheduleButton = createActionButton("/icons/macaron-logo-new-schedule.svg", "新建日程", this::openNewScheduleDialog);
        Button loginButton = createActionButton("/icons/macaron-logo-user.svg", "登录", this::showLoginDialog);
        Button settingsButton = createActionButton("/icons/macaron-logo-settings.svg", "设置", this::showSettingsDialog);
        Button exitButton = createActionButton("/icons/macaron-logo-logout.svg", "退出", Platform::exit);

        bottomActions = new VBox(6);
        bottomActions.getStyleClass().add("sidebar-bottom-actions");
        bottomActions.getChildren().addAll(
            functionTitle,
            newScheduleButton,
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
        sidebar.setFillWidth(!sidebarCollapsed);

        if (collapseToggle != null) {
            if (sidebarCollapsed) {
                collapseToggle.setText("");
                collapseToggle.setStyle("-fx-padding: 8 0 8 0; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-pref-width: 40; -fx-pref-height: 40; -fx-max-width: 40; -fx-max-height: 40;");
                collapseToggle.setAlignment(Pos.CENTER);
                collapseToggle.setPadding(new Insets(8, 0, 8, 0));
            } else {
                collapseToggle.setText("侧边栏");
                collapseToggle.setStyle("-fx-padding: 10 12 10 12; -fx-alignment: center-left; -fx-min-height: 40; -fx-pref-height: 40; -fx-max-height: 40;");
                collapseToggle.setAlignment(Pos.CENTER_LEFT);
                collapseToggle.setPadding(new Insets(10, 12, 10, 12));
            }
            if (collapseToggleIcon != null) {
                collapseToggleIcon.setRotate(sidebarCollapsed ? 0 : 90);
            }
        }
        
        for (Map.Entry<Labeled, String[]> entry : collapsibleLabels.entrySet()) {
            Labeled control = entry.getKey();
            String[] texts = entry.getValue();
            if (sidebarCollapsed) {
                control.setText("");
                control.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                control.setStyle("-fx-padding: 8 0 8 0; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-pref-width: 40; -fx-pref-height: 40; -fx-max-width: 40; -fx-max-height: 40;");
                control.setAlignment(Pos.CENTER);
                control.setPadding(new Insets(8, 0, 8, 0)); // Center padding
                control.setMinSize(40, 40); // Make it a square for centering
                control.setPrefSize(40, 40);
                control.setMaxSize(40, 40);
            } else {
                control.setText(texts[0]);
                control.setContentDisplay(ContentDisplay.LEFT);
                control.setStyle("-fx-padding: 10 12 10 12; -fx-alignment: center-left; -fx-min-height: 40; -fx-pref-height: 40; -fx-max-height: 40;");
                control.setAlignment(Pos.CENTER_LEFT);
                control.setPadding(new Insets(10, 12, 10, 12)); // Original nav-button padding
                control.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                control.setPrefSize(Region.USE_COMPUTED_SIZE, 40);
                control.setMaxSize(Double.MAX_VALUE, 40);
            }
        }

        if (bottomActions != null) {
            bottomActions.setAlignment(sidebarCollapsed ? Pos.TOP_CENTER : Pos.TOP_LEFT);
            bottomActions.setFillWidth(!sidebarCollapsed);
        }

        if (functionTitle != null) {
            functionTitle.setVisible(!sidebarCollapsed);
            functionTitle.setManaged(!sidebarCollapsed);
        }
        
        if (searchField != null) {
            searchField.setVisible(!sidebarCollapsed);
            searchField.setManaged(!sidebarCollapsed);
        }

        updateFeaturePanelState();
    }

    public Pane createSvgIcon(String resourcePath, String title, double size) {
        Group iconGroup = loadSvgGraphic(resourcePath, size);
        Pane container = new Pane(iconGroup);
        container.getStyleClass().add("sidebar-svg-icon");
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        
        // 使用圆角矩形作为裁剪区域，如果不设置圆角则默认为直角
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(size);
        clip.setArcHeight(size);
        container.setClip(clip);
        
        if (title != null && !title.isEmpty()) {
            container.setAccessibleText(title);
            Tooltip.install(container, new Tooltip(title));
        }
        return container;
    }

    private Group loadSvgGraphic(String resourcePath, double targetSize) {
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
            parseSvgChildren(svg, group);
            double scale = targetSize / Math.max(viewBox[2], viewBox[3]);
            group.getTransforms().add(new Scale(scale, scale));
            group.getTransforms().add(new Translate(-viewBox[0], -viewBox[1]));
            return group;
        } catch (Exception e) {
            return new Group();
        }
    }

    private void parseSvgChildren(Element parent, Group target) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (!(child instanceof Element)) {
                continue;
            }
            Element element = (Element) child;
            String tag = element.getTagName();
            if ("g".equals(tag)) {
                Group group = new Group();
                applyTransform(group, element.getAttribute("transform"));
                target.getChildren().add(group);
                parseSvgChildren(element, group);
                continue;
            }
            Shape shape = createShapeFromElement(element);
            if (shape == null) {
                continue;
            }
            applyTransform(shape, element.getAttribute("transform"));
            target.getChildren().add(shape);
        }
    }

    private void applyTransform(Node node, String transformStr) {
        if (transformStr == null || transformStr.isEmpty()) return;
        
        Matcher matcher = Pattern.compile("(\\w+)\\s*\\(([^)]+)\\)").matcher(transformStr);
        while (matcher.find()) {
            String type = matcher.group(1);
            String[] args = matcher.group(2).split("[,\\s]+");
            try {
                if ("translate".equals(type)) {
                    double tx = parseDouble(args[0]);
                    double ty = args.length > 1 ? parseDouble(args[1]) : 0;
                    node.getTransforms().add(new Translate(tx, ty));
                } else if ("rotate".equals(type)) {
                    double angle = parseDouble(args[0]);
                    if (args.length >= 3) {
                        double cx = parseDouble(args[1]);
                        double cy = parseDouble(args[2]);
                        node.getTransforms().add(new Rotate(angle, cx, cy));
                    } else {
                        node.getTransforms().add(new Rotate(angle));
                    }
                } else if ("scale".equals(type)) {
                    double sx = parseDouble(args[0]);
                    double sy = args.length > 1 ? parseDouble(args[1]) : sx;
                    node.getTransforms().add(new Scale(sx, sy));
                }
            } catch (Exception e) {
                // ignore parsing errors for a single transform
            }
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
        if ("text".equals(tag)) {
            Text text = new Text(element.getTextContent());
            text.setX(parseDouble(element.getAttribute("x")));
            text.setY(parseDouble(element.getAttribute("y")));
            
            String fontFamily = element.getAttribute("font-family");
            if (fontFamily.isEmpty()) fontFamily = "System";
            else fontFamily = fontFamily.replaceAll("['\"]", "");
            
            double fontSize = parseDoubleOrDefault(element.getAttribute("font-size"), 12);
            String fontWeight = element.getAttribute("font-weight");
            FontWeight fw = "bold".equalsIgnoreCase(fontWeight) ? FontWeight.BOLD : FontWeight.NORMAL;
            text.setFont(Font.font(fontFamily, fw, fontSize));
            
            String textAnchor = element.getAttribute("text-anchor");
            if ("middle".equals(textAnchor)) {
                text.setTextAlignment(TextAlignment.CENTER);
                text.setBoundsType(TextBoundsType.VISUAL);
                double initWidth = text.getLayoutBounds().getWidth();
                if (initWidth > 0) {
                    text.setTranslateX(-initWidth / 2);
                }
                text.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
                    text.setTranslateX(-newB.getWidth() / 2);
                });
            }
            applyShapeStyle(text, element);
            return text;
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
            if (sidebarCollapsed) {
                featurePanelToggle.setText("");
                featurePanelToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                featurePanelToggle.setStyle("-fx-padding: 8 0 8 0; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-pref-width: 40; -fx-pref-height: 40; -fx-max-width: 40; -fx-max-height: 40;");
                featurePanelToggle.setAlignment(Pos.CENTER);
                featurePanelToggle.setPadding(new Insets(8, 0, 8, 0));
                featurePanelToggle.setMinSize(40, 40);
                featurePanelToggle.setPrefSize(40, 40);
                featurePanelToggle.setMaxSize(40, 40);
            } else {
                featurePanelToggle.setText("更多功能");
                featurePanelToggle.setContentDisplay(ContentDisplay.LEFT);
                featurePanelToggle.setStyle("-fx-padding: 10 12 10 12; -fx-alignment: center-left; -fx-min-height: 40; -fx-pref-height: 40; -fx-max-height: 40;");
                featurePanelToggle.setAlignment(Pos.CENTER_LEFT);
                featurePanelToggle.setPadding(new Insets(10, 12, 10, 12));
                featurePanelToggle.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                featurePanelToggle.setPrefSize(Region.USE_COMPUTED_SIZE, 40);
                featurePanelToggle.setMaxSize(Double.MAX_VALUE, 40);
            }
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
        navigationService.setCurrentScreen(resolveScreen(view));
        root.setCenter(view.getView());
        view.refresh();
    }
    
    public void showScheduleDetails(Schedule schedule) {
        navigationService.setSelectedSchedule(schedule);
        infoPanelView.setSchedule(schedule);
        infoPanelView.showWithAnimation();
    }

    public void closeScheduleDetails() {
        infoPanelView.hideWithAnimation();
    }

    public boolean isScheduleSelected(Schedule schedule) {
        Schedule selectedSchedule = navigationService.getSelectedSchedule();
        if (selectedSchedule == null || schedule == null) {
            return false;
        }
        if (selectedSchedule.getId() > 0 && schedule.getId() > 0) {
            return selectedSchedule.getId() == schedule.getId();
        }
        return selectedSchedule == schedule;
    }
    
    public Schedule getSelectedSchedule() {
        return navigationService.getSelectedSchedule();
    }

    public ScheduleCompletionCoordinator.PendingCompletion prepareScheduleCompletion(
        Schedule schedule,
        boolean targetCompleted
    ) {
        return scheduleCompletionCoordinator.prepare(schedule, targetCompleted);
    }

    public boolean updateScheduleCompletion(Schedule schedule, boolean targetCompleted) {
        if (schedule == null) {
            return false;
        }
        return scheduleCompletionCoordinator.submitImmediate(schedule, targetCompleted);
        /*    boolean updated = scheduleDAO.updateScheduleStatus(schedule.getId(), targetCompleted);
            if (!updated) {
                showError("更新状态失败", "未能保存日程状态变更。");
                return false;
            }

            schedule.setCompleted(targetCompleted);
            if (selectedSchedule != null && isScheduleSelected(schedule)) {
                selectedSchedule.setCompleted(targetCompleted);
            }

            refreshAllViews();
            return true;
        } catch (SQLException e) {
            showError("更新状态失败", e.getMessage());
            return false;
        }*/
    }
    
    public void refreshAllViews() {
        if (currentView != null) {
            currentView.refresh();
        }
        infoPanelView.refresh();
        updatePendingCountBadge();
    }

    public int createSchedule(Schedule schedule) throws SQLException {
        return scheduleService.addSchedule(schedule);
    }

    public boolean saveSchedule(Schedule schedule) throws SQLException {
        return scheduleService.updateSchedule(schedule);
    }

    public boolean removeSchedule(int scheduleId) throws SQLException {
        return scheduleService.deleteSchedule(scheduleId);
    }

    public Schedule findScheduleById(int scheduleId) throws SQLException {
        return scheduleService.getScheduleById(scheduleId);
    }

    public List<Schedule> loadAllSchedules() throws SQLException {
        return scheduleService.getAllSchedules();
    }

    public List<Schedule> searchSchedules(String keyword) throws SQLException {
        return scheduleService.searchSchedules(keyword);
    }

    public List<Schedule> applyPendingCompletionMutations(List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return schedules;
        }
        for (ScheduleCompletionMutation mutation : scheduleCompletionCoordinator.snapshotCommittedMutations()) {
            for (Schedule schedule : schedules) {
                mutation.applyTo(schedule);
            }
        }
        return schedules;
    }

    private void applyCompletionMutationLocally(ScheduleCompletionMutation mutation) {
        mutation.applyTo(navigationService.getSelectedSchedule());
        for (ScheduleCompletionParticipant participant : collectCompletionParticipants()) {
            participant.applyCompletionMutation(mutation);
        }
        adjustPendingCountOptimistically(mutation.pendingCountDelta());
    }

    private void confirmCompletionMutationLocally(ScheduleCompletionMutation mutation) {
        mutation.applyTo(navigationService.getSelectedSchedule());
        for (ScheduleCompletionParticipant participant : collectCompletionParticipants()) {
            participant.confirmCompletionMutation(mutation);
        }
    }

    private void revertCompletionMutationLocally(ScheduleCompletionMutation mutation) {
        mutation.revertOn(navigationService.getSelectedSchedule());
        for (ScheduleCompletionParticipant participant : collectCompletionParticipants()) {
            participant.revertCompletionMutation(mutation);
        }
        adjustPendingCountOptimistically(-mutation.pendingCountDelta());
    }

    private List<ScheduleCompletionParticipant> collectCompletionParticipants() {
        List<ScheduleCompletionParticipant> participants = new ArrayList<>();
        if (scheduleListView instanceof ScheduleCompletionParticipant) {
            participants.add(scheduleListView);
        }
        if (timelineView instanceof ScheduleCompletionParticipant) {
            participants.add(timelineView);
        }
        if (heatmapView instanceof ScheduleCompletionParticipant) {
            participants.add(heatmapView);
        }
        if (infoPanelView instanceof ScheduleCompletionParticipant) {
            participants.add(infoPanelView);
        }
        return participants;
    }

    private void adjustPendingCountOptimistically(int delta) {
        if (pendingCountListener == null || delta == 0) {
            return;
        }
        if (lastKnownPendingCount < 0) {
            updatePendingCountBadge();
            return;
        }
        lastKnownPendingCount = Math.max(0, lastKnownPendingCount + delta);
        pendingCountListener.accept(lastKnownPendingCount);
    }

    /* private void reportCompletionPersistenceFailure(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()
            ? cause.getMessage()
            : "鏈兘淇濆瓨鏃ョ▼鐘舵€佸彉鏇淬€?";
        showError("鏇存柊鐘舵€佸け璐?, message);
    }

    }*/

    private void reportCompletionPersistenceFailure(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()
            ? cause.getMessage()
            : "Failed to save schedule completion state.";
        showError("Schedule update failed", message);
    }

    private ThreadFactory createCompletionThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "todo-schedule-completion");
            thread.setDaemon(true);
            return thread;
        };
    }

    private void syncThemeState() {
        currentTheme = themeService.getCurrentTheme();
        importedThemeStylesheet = themeService.getImportedThemeStylesheet();
        currentScheduleCardStyle = themeService.getCurrentScheduleCardStyle();
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

    private boolean importThemeFromFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入主题文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSS 文件", "*.css"));
        File selected = chooser.showOpenDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (selected == null) {
            return false;
        }

        importedThemeStylesheet = selected.toURI().toString();
        currentTheme = "imported";
        applyThemeStylesheets(resolveImportedThemeStylesheets());
        saveThemePreference();
        updateThemeIconState();
        showInfo("主题已导入", "已应用外部主题文件:\n" + selected.getAbsolutePath());
        return true;
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
        syncThemeState();
    }

    private void loadScheduleCardStylePreference() {
        syncThemeState();
    }

    private void saveThemePreference() {
        if ("imported".equals(currentTheme) && importedThemeStylesheet != null && !importedThemeStylesheet.isBlank()) {
            themeService.importTheme(Path.of(URI.create(importedThemeStylesheet)));
        } else {
            themeService.selectBuiltinTheme(currentTheme);
        }
        syncThemeState();
    }

    private void saveScheduleCardStylePreference() {
        themeService.setScheduleCardStyle(currentScheduleCardStyle);
        syncThemeState();
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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("设置");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().setAll(getCurrentThemeStylesheets());
        dialog.getDialogPane().getStyleClass().add("settings-dialog-pane");
        dialog.getDialogPane().setPrefWidth(940);
        dialog.getDialogPane().setPrefHeight(600);

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("settings-shell");
        shell.setPrefSize(940, 600);

        VBox navBar = new VBox(8);
        navBar.getStyleClass().addAll("sidebar");
        navBar.setPrefWidth(196);
        Label navTitle = new Label("设置");
        navTitle.getStyleClass().add("label-title");
        Label navSubTitle = new Label("选择要设置的功能");
        navSubTitle.getStyleClass().add("label-hint");

        ToggleGroup categoryGroup = new ToggleGroup();
        ToggleButton detailTab = new ToggleButton("详情");
        detailTab.setGraphic(createSvgIcon("/icons/macaron_detail-v2_icon.svg", "详情", 20));
        detailTab.setGraphicTextGap(8);
        
        ToggleButton themeTab = new ToggleButton("主题");
        themeTab.setGraphic(createSvgIcon("/icons/macaron_theme-v1_icon.svg", "主题", 20));
        themeTab.setGraphicTextGap(8);
        
        ToggleButton styleTab = new ToggleButton("样式");
        styleTab.setGraphic(createSvgIcon("/icons/macaron_style-v1_icon.svg", "样式", 20));
        styleTab.setGraphicTextGap(8);
        for (ToggleButton tab : List.of(detailTab, themeTab, styleTab)) {
            tab.getStyleClass().add("nav-button");
            tab.setMaxWidth(Double.MAX_VALUE);
            tab.setContentDisplay(ContentDisplay.LEFT);
            tab.setTextOverrun(OverrunStyle.CLIP);
            tab.setWrapText(false);
            tab.setToggleGroup(categoryGroup);
        }
        themeTab.setSelected(true);
        navBar.getChildren().addAll(navTitle, navSubTitle, detailTab, themeTab, styleTab);

        StackPane contentHost = new StackPane();
        contentHost.getStyleClass().add("settings-content-host");

        VBox detailPage = new VBox(18);
        detailPage.getStyleClass().add("settings-page");
        VBox aboutCard = createSettingsCard("应用详情", "关于当前应用与设置入口说明");
        Label aboutText = new Label("ToDo 日程管理应用\n版本: 1.0\n当前设置中心只包含：详情、主题、样式。");
        aboutText.getStyleClass().add("settings-info-text");
        aboutText.setWrapText(true);
        aboutCard.getChildren().add(aboutText);
        VBox currentCard = createSettingsCard("当前配置", "用于快速确认正在生效的视觉配置");
        Label themeValue = new Label("imported".equals(currentTheme) ? "外部导入主题" : builtinThemes.getOrDefault(currentTheme, "浅色"));
        themeValue.getStyleClass().add("settings-inline-value");
        Label styleValue = new Label(currentScheduleCardStyle);
        styleValue.getStyleClass().add("settings-inline-value");
        currentCard.getChildren().addAll(
            createSettingRow("当前主题", "当前应用使用的主题", themeValue),
            createSettingRow("日程卡片样式", "当前全局日程卡片视觉样式", styleValue)
        );
        detailPage.getChildren().addAll(aboutCard, currentCard);

        VBox themePage = new VBox(18);
        themePage.getStyleClass().add("settings-page");
        VBox themeCard = createSettingsCard("主题配色", "点击色卡立即预览，保存后作为默认主题");
        HBox swatchRow = new HBox(10);
        swatchRow.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup themeGroup = new ToggleGroup();
        String selectedThemeKey = builtinThemes.containsKey(currentTheme) ? currentTheme : "light";
        Map<String, String> themeColorMap = new LinkedHashMap<>();
        themeColorMap.put("light", "#cfd8dc");
        themeColorMap.put("mint", "#98d8c8");
        themeColorMap.put("ocean", "#64b5f6");
        themeColorMap.put("sunset", "#ffab91");
        themeColorMap.put("lavender", "#ce93d8");
        themeColorMap.put("forest", "#81c784");
        themeColorMap.put("slate", "#90a4ae");
        themeColorMap.put("macaron", "#f8bbd0");
        for (Map.Entry<String, String> entry : themeColorMap.entrySet()) {
            ToggleButton swatch = new ToggleButton();
            swatch.setToggleGroup(themeGroup);
            swatch.getStyleClass().add("settings-theme-swatch");
            swatch.setStyle("-fx-background-color: " + entry.getValue() + ";");
            swatch.setTooltip(new Tooltip("使用" + builtinThemes.get(entry.getKey()) + "主题"));
            if (entry.getKey().equals(selectedThemeKey)) {
                swatch.setSelected(true);
            }
            swatch.setOnAction(e -> switchTheme(entry.getKey()));
            swatchRow.getChildren().add(swatch);
        }
        Button importThemeButton = new Button("导入外部主题");
        importThemeButton.getStyleClass().add("button-secondary");
        importThemeButton.setOnAction(e -> importThemeFromFile());
        themeCard.getChildren().addAll(
            createSettingRow("主题色板", "可视化快速选择主题，不再使用传统下拉菜单", swatchRow),
            createSettingRow("外部主题", "支持导入 CSS 文件扩展主题风格", importThemeButton)
        );
        themePage.getChildren().add(themeCard);

        VBox styleCard = createSettingsCard("日程卡片样式", "统一调整列表、时间轴、热力图等模块中的日程卡片风格");
        ToggleGroup styleGroup = new ToggleGroup();
        HBox styleChipRow = new HBox(8);
        styleChipRow.setAlignment(Pos.CENTER_LEFT);
        String[] selectedCardStyle = new String[] { currentScheduleCardStyle };
        for (String styleName : scheduleCardStyles) {
            ToggleButton styleChip = new ToggleButton(styleName);
            styleChip.getStyleClass().add("settings-style-chip");
            styleChip.setToggleGroup(styleGroup);
            if (styleName.equals(currentScheduleCardStyle)) {
                styleChip.setSelected(true);
            }
            styleChip.setOnAction(e -> selectedCardStyle[0] = styleName);
            styleChipRow.getChildren().add(styleChip);
        }
        styleCard.getChildren().add(createSettingRow("卡片风格", "样式变化会同时作用于日程管理、时间轴与热力图中的日程卡片", styleChipRow));
        VBox stylePage = new VBox(18);
        stylePage.getStyleClass().add("settings-page");
        stylePage.getChildren().add(styleCard);

        Map<ToggleButton, VBox> pages = new LinkedHashMap<>();
        pages.put(detailTab, detailPage);
        pages.put(themeTab, themePage);
        pages.put(styleTab, stylePage);

        Runnable updateNavActive = () -> {
            for (ToggleButton tab : pages.keySet()) {
                tab.getStyleClass().remove("active");
                if (tab.isSelected()) {
                    tab.getStyleClass().add("active");
                }
            }
        };

        Runnable switchPage = () -> {
            for (Map.Entry<ToggleButton, VBox> entry : pages.entrySet()) {
                if (entry.getKey().isSelected()) {
                    updateNavActive.run();
                    switchSettingsPage(contentHost, entry.getValue());
                    return;
                }
            }
            updateNavActive.run();
            switchSettingsPage(contentHost, themePage);
        };

        detailTab.setOnAction(e -> switchPage.run());
        themeTab.setOnAction(e -> switchPage.run());
        styleTab.setOnAction(e -> switchPage.run());
        switchPage.run();

        shell.setLeft(navBar);
        shell.setCenter(contentHost);
        dialog.getDialogPane().setContent(shell);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        if (selectedCardStyle[0] != null && scheduleCardStyles.contains(selectedCardStyle[0]) && !selectedCardStyle[0].equals(currentScheduleCardStyle)) {
            currentScheduleCardStyle = selectedCardStyle[0];
            saveScheduleCardStylePreference();
        }
        refreshAllViews();
    }

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
            new ParallelTransition(fadeIn, moveIn).play();
        });
        out.play();
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

    public String getCurrentScheduleCardStyle() {
        return currentScheduleCardStyle;
    }

    public String getCurrentTimelineCardStyle() {
        return currentScheduleCardStyle;
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
    
    public void initializeApplication() {
        refreshAllViews();
    }
    
    public void shutdown() {
        scheduleCompletionExecutor.shutdownNow();
    }

    private void updatePendingCountBadge() {
        if (pendingCountListener == null) {
            return;
        }
        try {
            lastKnownPendingCount = scheduleService.getPendingCount();
            pendingCountListener.accept(lastKnownPendingCount);
        } catch (SQLException ignored) {
            lastKnownPendingCount = 0;
            pendingCountListener.accept(0);
        }
    }

    private NavigationService.Screen resolveScreen(View view) {
        if (view instanceof TimelineView) {
            return NavigationService.Screen.TIMELINE;
        }
        if (view instanceof HeatmapView) {
            return NavigationService.Screen.HEATMAP;
        }
        if (view instanceof FlowchartView) {
            return NavigationService.Screen.FLOWCHART;
        }
        return NavigationService.Screen.LIST;
    }
}
