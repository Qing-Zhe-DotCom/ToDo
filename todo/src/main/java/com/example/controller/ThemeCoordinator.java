package com.example.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.example.application.ClassicThemePalette;
import com.example.application.ExperimentalFeaturesService;
import com.example.application.FontService;
import com.example.application.GlassBackdropCoordinator;
import com.example.application.IconKey;
import com.example.application.IconPack;
import com.example.application.IconService;
import com.example.application.LocalizationService;
import com.example.application.ThemeAppearance;
import com.example.application.ThemeFamily;
import com.example.application.ThemeService;
import com.example.config.UserPreferencesStore;
import com.example.view.ScheduleCardStyleSupport;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThemeCoordinator {

    private final ThemeService themeService;
    private final IconService iconService;
    private final ExperimentalFeaturesService experimentalFeaturesService;
    private final UserPreferencesStore preferencesStore;
    private final FontService fontService;
    private final LocalizationService localizationService;
    private final LocalizationFacade loc;
    private final List<ThemeFamily> availableThemeFamilies;
    private final List<ClassicThemePalette> classicThemePalettes;
    private final List<IconPack> availableIconPacks;
    private static final String DEFAULT_SCHEDULE_CARD_STYLE = ScheduleCardStyleSupport.getDefaultStyleId();

    // 主题管理
    private ThemeFamily currentThemeFamily = ThemeFamily.CLASSIC;
    private ThemeAppearance currentThemeAppearance = ThemeAppearance.LIGHT;
    private ClassicThemePalette currentClassicPalette = ClassicThemePalette.LIGHT;
    private String currentScheduleCardStyle = DEFAULT_SCHEDULE_CARD_STYLE;
    private IconPack currentIconPack = IconPack.CLASSIC;
    private boolean currentThemeIconBinding = true;

    // Scene/root references
    private Scene scene;
    private StackPane root;
    private Pane macaronBackgroundLayer;

    // Callback for icon refresh (view icon refresh handled by MainController)
    private Runnable onSidebarIconsChanged;

    // UI References set from MainController
    private ToggleButton collapseToggle;
    private Pane collapseToggleIcon;
    private ToggleButton featurePanelToggle;
    private Pane featureToggleIcon;
    private ToggleButton scheduleNavButton;
    private ToggleButton timelineNavButton;
    private ToggleButton heatmapNavButton;
    private Button settingsActionButton;
    private ToggleButton appearanceToggle;
    private Button exitActionButton;
    private Button clearSearchTextButton;
    private Button clearSearchHistoryButton;
    private Pane themeIcon;

    private boolean sidebarCollapsed;
    private boolean featurePanelExpanded;

    public ThemeCoordinator(
        ThemeService themeService,
        IconService iconService,
        ExperimentalFeaturesService experimentalFeaturesService,
        UserPreferencesStore preferencesStore,
        FontService fontService,
        LocalizationService localizationService
    ) {
        this.themeService = themeService;
        this.iconService = iconService;
        this.experimentalFeaturesService = experimentalFeaturesService;
        this.preferencesStore = preferencesStore;
        this.fontService = fontService;
        this.localizationService = localizationService;
        this.loc = new LocalizationFacade(localizationService);
        this.availableThemeFamilies = List.copyOf(themeService.getThemeFamilies());
        this.classicThemePalettes = List.copyOf(themeService.getClassicPalettes());
        this.availableIconPacks = List.copyOf(iconService.getAvailableIconPacks());
        syncThemeState();
        syncIconState();
        iconService.addChangeListener(this::refreshIconography);
    }

    // --- Setters for UI component references ---

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void setRoot(StackPane root) {
        this.root = root;
    }

    public void setMacaronBackgroundLayer(Pane macaronBackgroundLayer) {
        this.macaronBackgroundLayer = macaronBackgroundLayer;
    }

    public void setOnSidebarIconsChanged(Runnable onSidebarIconsChanged) {
        this.onSidebarIconsChanged = onSidebarIconsChanged;
    }

    public void setSidebarCollapsed(boolean sidebarCollapsed) {
        this.sidebarCollapsed = sidebarCollapsed;
    }

    public void setFeaturePanelExpanded(boolean featurePanelExpanded) {
        this.featurePanelExpanded = featurePanelExpanded;
    }

    public void setCollapseToggle(ToggleButton collapseToggle) {
        this.collapseToggle = collapseToggle;
    }

    public void setCollapseToggleIcon(Pane collapseToggleIcon) {
        this.collapseToggleIcon = collapseToggleIcon;
    }

    public void setFeaturePanelToggle(ToggleButton featurePanelToggle) {
        this.featurePanelToggle = featurePanelToggle;
    }

    public void setFeatureToggleIcon(Pane featureToggleIcon) {
        this.featureToggleIcon = featureToggleIcon;
    }

    public void setScheduleNavButton(ToggleButton scheduleNavButton) {
        this.scheduleNavButton = scheduleNavButton;
    }

    public void setTimelineNavButton(ToggleButton timelineNavButton) {
        this.timelineNavButton = timelineNavButton;
    }

    public void setHeatmapNavButton(ToggleButton heatmapNavButton) {
        this.heatmapNavButton = heatmapNavButton;
    }

    public void setSettingsActionButton(Button settingsActionButton) {
        this.settingsActionButton = settingsActionButton;
    }

    public void setAppearanceToggle(ToggleButton appearanceToggle) {
        this.appearanceToggle = appearanceToggle;
    }

    public void setExitActionButton(Button exitActionButton) {
        this.exitActionButton = exitActionButton;
    }

    public void setClearSearchTextButton(Button clearSearchTextButton) {
        this.clearSearchTextButton = clearSearchTextButton;
    }

    public void setClearSearchHistoryButton(Button clearSearchHistoryButton) {
        this.clearSearchHistoryButton = clearSearchHistoryButton;
    }

    public void setThemeIcon(Pane themeIcon) {
        this.themeIcon = themeIcon;
    }

    // --- Public getters for theme state ---

    public List<ThemeFamily> getAvailableThemeFamilies() {
        return availableThemeFamilies;
    }

    public List<ClassicThemePalette> getClassicThemePalettes() {
        return classicThemePalettes;
    }

    public List<IconPack> getAvailableIconPacks() {
        return availableIconPacks;
    }

    public ThemeFamily getCurrentThemeFamily() {
        return currentThemeFamily;
    }

    public ThemeAppearance getCurrentThemeAppearance() {
        return currentThemeAppearance;
    }

    public ClassicThemePalette getCurrentClassicPalette() {
        return currentClassicPalette;
    }

    public String getCurrentScheduleCardStyle() {
        return currentScheduleCardStyle;
    }

    public IconPack getCurrentIconPack() {
        return currentIconPack;
    }

    public boolean isCurrentThemeIconBinding() {
        return currentThemeIconBinding;
    }

    public String getCurrentTheme() {
        return currentThemeFamily.getId();
    }

    public String getCurrentTimelineCardStyle() {
        return currentScheduleCardStyle;
    }

    public List<String> getCurrentThemeStylesheets() {
        return themeService.resolveStylesheets(getClass(), currentThemeFamily, currentThemeAppearance, currentClassicPalette);
    }

    public String text(String key, Object... args) {
        return loc.text(key, args);
    }

    public String themeFamilyDisplayName(ThemeFamily family) {
        return loc.themeFamilyDisplayName(family);
    }

    public String classicPaletteDisplayName(ClassicThemePalette palette) {
        return loc.classicPaletteDisplayName(palette);
    }

    public String currentThemeDisplayName(ThemeFamily family, ClassicThemePalette palette) {
        return loc.currentThemeDisplayName(family, palette);
    }

    // --- Public SVG icon creation methods (used by views via MainController delegation) ---

    public Pane createSvgIcon(String resourcePath, String title, double size) {
        Group iconGroup = loadSvgGraphic(resourcePath, size);
        Pane container = new Pane(iconGroup);
        container.getStyleClass().add("sidebar-svg-icon");
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);

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

    public Pane createSvgIcon(IconKey iconKey, String title, double size) {
        return createSvgIcon(iconService.resolveResourcePath(iconKey), title, size);
    }

    // --- Theme state sync ---

    private void syncThemeState() {
        currentThemeFamily = themeService.getCurrentThemeFamily();
        currentThemeAppearance = themeService.getCurrentAppearance();
        currentClassicPalette = themeService.getCurrentClassicPalette();
        currentScheduleCardStyle = themeService.getCurrentScheduleCardStyle();
        iconService.syncThemeAppearance(currentThemeAppearance);
    }

    public void syncIconState() {
        currentIconPack = iconService.getCurrentIconPack();
        currentThemeIconBinding = iconService.isThemeBindingEnabled();
    }

    // --- Theme switching ---

    public void switchTheme(ThemeFamily family) {
        previewThemeSelection(family, currentThemeAppearance, currentClassicPalette, null);
        saveThemePreference();
        updateThemeIconState();
    }

    private void applyThemeStylesheets(List<String> stylesheets) {
        if (scene == null || stylesheets.isEmpty()) {
            return;
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().addAll(stylesheets);
        if (onSidebarIconsChanged != null) {
            onSidebarIconsChanged.run();
        }
        updateThemeIconState();
        updateMacaronPresentation();
        updateAppearanceTogglePresentation();
    }

    public void previewThemeSelection(
        ThemeFamily family,
        ThemeAppearance appearance,
        ClassicThemePalette classicPalette,
        DialogPane dialogPane
    ) {
        currentThemeFamily = family != null ? family : ThemeFamily.CLASSIC;
        currentThemeAppearance = appearance != null ? appearance : ThemeAppearance.LIGHT;
        currentClassicPalette = classicPalette != null ? classicPalette : ClassicThemePalette.LIGHT;
        currentScheduleCardStyle = currentThemeFamily.getBoundScheduleCardStyle();
        iconService.syncThemeAppearance(currentThemeAppearance);

        List<String> stylesheets = themeService.resolveStylesheets(
            getClass(),
            currentThemeFamily,
            currentThemeAppearance,
            currentClassicPalette
        );
        applyThemeStylesheets(stylesheets);
        if (dialogPane != null) {
            dialogPane.getStylesheets().setAll(stylesheets);
            fontService.applyTo(dialogPane, localizationService.getActiveLanguage());
            updateDialogGlass(dialogPane);
        }
    }

    public void showThemeMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();

        for (ThemeFamily family : getSelectableThemeFamilies()) {
            String prefix = family == currentThemeFamily ? text("common.selected.prefix") : "";
            MenuItem item = new MenuItem(prefix + text("theme.menu.use", themeFamilyDisplayName(family)));
            item.setOnAction(e -> switchTheme(family));
            menu.getItems().add(item);
        }

        menu.show(anchor, Side.RIGHT, 0, 0);
    }

    public void togglePrimaryTheme() {
        List<ThemeFamily> selectableThemeFamilies = getSelectableThemeFamilies();
        int currentIndex = selectableThemeFamilies.indexOf(currentThemeFamily);
        int nextIndex = (currentIndex + 1) % selectableThemeFamilies.size();
        if (currentIndex < 0) {
            nextIndex = 0;
        }
        switchTheme(selectableThemeFamilies.get(nextIndex));
    }

    public void toggleThemeAppearance() {
        ThemeAppearance next = currentThemeAppearance == ThemeAppearance.DARK ? ThemeAppearance.LIGHT : ThemeAppearance.DARK;
        previewThemeSelection(currentThemeFamily, next, currentClassicPalette, null);
        saveThemePreference();
        updateAppearanceTogglePresentation();
    }

    public void updateAppearanceTogglePresentation() {
        if (appearanceToggle == null) {
            return;
        }

        boolean dark = currentThemeAppearance == ThemeAppearance.DARK;
        if (appearanceToggle.isSelected() != dark) {
            appearanceToggle.setSelected(dark);
        }

        String label = text("sidebar.appearance.darkMode");
        String tooltipText = dark ? text("sidebar.appearance.switchToLight") : text("sidebar.appearance.switchToDark");
        IconKey iconKey = dark ? IconKey.THEME_DARK : IconKey.THEME_LIGHT;

        appearanceToggle.setText(sidebarCollapsed ? "" : label);
        appearanceToggle.setContentDisplay(sidebarCollapsed ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
        appearanceToggle.setGraphic(createSvgIcon(iconKey, label, 24));
        appearanceToggle.setAccessibleText(label);
        appearanceToggle.setTooltip(new Tooltip(tooltipText));
    }

    public void loadThemePreference() {
        syncThemeState();
    }

    public void saveThemePreference() {
        themeService.selectTheme(currentThemeFamily, currentThemeAppearance, currentClassicPalette);
        syncThemeState();
        iconService.syncThemeFamily(currentThemeFamily);
        syncIconState();
    }

    public void applySavedThemeIfNeeded() {
        if (scene == null) {
            return;
        }
        syncThemeState();
        applyThemeStylesheets(getCurrentThemeStylesheets());
    }

    // --- Glass/backdrop effects ---

    public void updateMacaronPresentation() {
        boolean macaronActive = currentThemeFamily == ThemeFamily.MACARON;
        if (macaronBackgroundLayer != null) {
            macaronBackgroundLayer.setVisible(macaronActive);
            macaronBackgroundLayer.setOpacity(macaronActive ? 1.0 : 0.0);
            macaronBackgroundLayer.setMouseTransparent(true);
        }

        if (root != null) {
            if (macaronActive) {
                if (!root.getStyleClass().contains("theme-family-macaron")) {
                    root.getStyleClass().add("theme-family-macaron");
                }
            } else {
                root.getStyleClass().remove("theme-family-macaron");
            }
        }

        if (scene != null) {
            GlassBackdropCoordinator coordinator = GlassBackdropCoordinator.install(scene);
            coordinator.setActive(macaronActive);
            coordinator.setAppearance(currentThemeAppearance);
            if (macaronActive) {
                coordinator.requestBurstRefresh(Duration.millis(500));
            }
        }
    }

    public List<ThemeFamily> getSelectableThemeFamilies() {
        return filterThemeFamilies(availableThemeFamilies, experimentalFeaturesService.isLabsEnabled());
    }

    static List<ThemeFamily> filterThemeFamilies(List<ThemeFamily> families, boolean labsEnabled) {
        List<ThemeFamily> filtered = new ArrayList<>();
        for (ThemeFamily family : families) {
            if (labsEnabled || !family.isLabsOnly()) {
                filtered.add(family);
            }
        }
        return filtered;
    }

    public void updateSceneGlass(Scene targetScene) {
        if (targetScene == null) {
            return;
        }
        GlassBackdropCoordinator coordinator = GlassBackdropCoordinator.install(targetScene);
        coordinator.setAppearance(currentThemeAppearance);
        coordinator.setActive(currentThemeFamily == ThemeFamily.MACARON);
        if (currentThemeFamily == ThemeFamily.MACARON) {
            coordinator.requestBurstRefresh(Duration.millis(420));
        }
    }

    public void requestGlassRefresh() {
        requestGlassRefresh(scene);
    }

    public void requestGlassRefresh(Scene targetScene) {
        if (targetScene == null || currentThemeFamily != ThemeFamily.MACARON) {
            return;
        }
        GlassBackdropCoordinator coordinator = GlassBackdropCoordinator.install(targetScene);
        coordinator.setAppearance(currentThemeAppearance);
        coordinator.requestBurstRefresh(Duration.millis(260));
    }

    public void updateDialogGlass(DialogPane pane) {
        if (pane == null) {
            return;
        }
        Object marker = pane.getProperties().putIfAbsent("todo.macaron.glass.bound", Boolean.TRUE);
        if (marker != null) {
            updateSceneGlass(pane.getScene());
            return;
        }
        pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                updateSceneGlass(newScene);
            }
        });
        if (pane.getScene() != null) {
            updateSceneGlass(pane.getScene());
        }
    }

    // --- Icon sync ---

    public void updateThemeIconState() {
        if (themeIcon == null) {
            return;
        }
        themeIcon.getStyleClass().removeAll("theme-icon-light", "theme-icon-dark");
        themeIcon.getStyleClass().add("theme-icon-light");
        themeIcon.setRotate(0);
        themeIcon.setOpacity(1.0);
    }

    public void refreshIconography() {
        syncIconState();
        updateSidebarIcons();
        if (onSidebarIconsChanged != null) {
            onSidebarIconsChanged.run();
        }
    }

    public void updateSidebarIcons() {
        if (collapseToggle != null) {
            collapseToggle.setGraphic(createSvgIcon(IconKey.ARROW_RIGHT, text("sidebar.collapse.toggle"), 24));
            collapseToggleIcon = (Pane) collapseToggle.getGraphic();
            collapseToggleIcon.setRotate(sidebarCollapsed ? 0 : 90);
        }
        if (featurePanelToggle != null) {
            featurePanelToggle.setGraphic(createSvgIcon(IconKey.ARROW_RIGHT, text("sidebar.feature.toggle"), 24));
            featureToggleIcon = (Pane) featurePanelToggle.getGraphic();
            featureToggleIcon.setRotate(featurePanelExpanded ? 90 : 0);
        }
        if (scheduleNavButton != null) {
            scheduleNavButton.setGraphic(createSvgIcon(IconKey.CALENDAR, text("nav.schedule"), 24));
        }
        if (timelineNavButton != null) {
            timelineNavButton.setGraphic(createSvgIcon(IconKey.TIMELINE, text("nav.timeline"), 24));
        }
        if (heatmapNavButton != null) {
            heatmapNavButton.setGraphic(createSvgIcon(IconKey.GRID_HEATMAP, text("nav.heatmap"), 24));
        }
        if (settingsActionButton != null) {
            settingsActionButton.setGraphic(createSvgIcon(IconKey.SETTINGS, text("sidebar.settings"), 24));
        }
        if (appearanceToggle != null) {
            updateAppearanceTogglePresentation();
        }
        if (exitActionButton != null) {
            exitActionButton.setGraphic(createSvgIcon(IconKey.LOGOUT, text("sidebar.exit"), 24));
        }
        if (clearSearchTextButton != null) {
            String clearText = text("sidebar.search.clearText");
            clearSearchTextButton.setGraphic(createSvgIcon(IconKey.CLOSE, clearText, 16));
            clearSearchTextButton.setAccessibleText(clearText);
            clearSearchTextButton.setTooltip(new Tooltip(clearText));
        }
        if (clearSearchHistoryButton != null) {
            String clearHistory = text("sidebar.search.clearHistory");
            clearSearchHistoryButton.setGraphic(createSvgIcon(IconKey.DELETE, clearHistory, 16));
            clearSearchHistoryButton.setAccessibleText(clearHistory);
            clearSearchHistoryButton.setTooltip(new Tooltip(clearHistory));
        }
    }

    // --- Macaron background layer creation ---

    public Pane createMacaronBackgroundLayer() {
        Pane backgroundLayer = new Pane();
        backgroundLayer.getStyleClass().add("macaron-background-layer");
        backgroundLayer.setManaged(false);
        backgroundLayer.setMouseTransparent(true);
        backgroundLayer.prefWidthProperty().bind(root.widthProperty());
        backgroundLayer.prefHeightProperty().bind(root.heightProperty());

        Region wash = createMacaronBackdropRegion("macaron-background-wash");
        wash.prefWidthProperty().bind(root.widthProperty());
        wash.prefHeightProperty().bind(root.heightProperty());

        Region blueOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-blue");
        blueOrb.setPrefSize(520, 360);
        blueOrb.layoutXProperty().bind(root.widthProperty().multiply(-0.06));
        blueOrb.layoutYProperty().bind(root.heightProperty().multiply(-0.08));

        Region pinkOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-pink");
        pinkOrb.setPrefSize(430, 320);
        pinkOrb.layoutXProperty().bind(root.widthProperty().multiply(0.56));
        pinkOrb.layoutYProperty().bind(root.heightProperty().multiply(0.05));

        Region mintOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-mint");
        mintOrb.setPrefSize(360, 280);
        mintOrb.layoutXProperty().bind(root.widthProperty().multiply(0.18));
        mintOrb.layoutYProperty().bind(root.heightProperty().multiply(0.52));

        Region purpleOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-purple");
        purpleOrb.setPrefSize(300, 240);
        purpleOrb.layoutXProperty().bind(root.widthProperty().multiply(0.72));
        purpleOrb.layoutYProperty().bind(root.heightProperty().multiply(0.48));

        Region creamOrb = createMacaronBackdropRegion("macaron-blob", "macaron-blob-cream");
        creamOrb.setPrefSize(260, 220);
        creamOrb.layoutXProperty().bind(root.widthProperty().multiply(0.42));
        creamOrb.layoutYProperty().bind(root.heightProperty().multiply(-0.04));

        Region ribbonLeft = createMacaronBackdropRegion("macaron-ribbon", "macaron-ribbon-left");
        ribbonLeft.setPrefSize(760, 220);
        ribbonLeft.layoutXProperty().bind(root.widthProperty().multiply(-0.18));
        ribbonLeft.layoutYProperty().bind(root.heightProperty().multiply(0.34));

        Region ribbonRight = createMacaronBackdropRegion("macaron-ribbon", "macaron-ribbon-right");
        ribbonRight.setPrefSize(620, 200);
        ribbonRight.layoutXProperty().bind(root.widthProperty().multiply(0.44));
        ribbonRight.layoutYProperty().bind(root.heightProperty().multiply(0.68));

        backgroundLayer.getChildren().addAll(
            wash,
            blueOrb,
            pinkOrb,
            mintOrb,
            purpleOrb,
            creamOrb,
            ribbonLeft,
            ribbonRight
        );
        return backgroundLayer;
    }

    private Region createMacaronBackdropRegion(String... styleClasses) {
        Region region = new Region();
        region.setManaged(false);
        region.setMouseTransparent(true);
        region.getStyleClass().addAll(styleClasses);
        return region;
    }

    // --- SVG loading helpers ---

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
        if ("line".equals(tag)) {
            Line line = new Line(
                parseDouble(element.getAttribute("x1")),
                parseDouble(element.getAttribute("y1")),
                parseDouble(element.getAttribute("x2")),
                parseDouble(element.getAttribute("y2"))
            );
            applyShapeStyle(line, element);
            return line;
        }
        if ("polyline".equals(tag)) {
            String[] coords = element.getAttribute("points").trim().split("[,\\s]+");
            Polyline polyline = new Polyline();
            for (int i = 0; i + 1 < coords.length; i += 2) {
                polyline.getPoints().addAll(parseDouble(coords[i]), parseDouble(coords[i + 1]));
            }
            applyShapeStyle(polyline, element);
            return polyline;
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

    private double parseDouble(String value) {
        return parseDoubleOrDefault(value, 0);
    }

    double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
