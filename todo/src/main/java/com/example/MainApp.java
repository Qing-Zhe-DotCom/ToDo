package com.example;

import com.example.controller.MainController;

import java.awt.Taskbar;
import java.awt.Taskbar.Feature;
import java.io.InputStream;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MainApp extends Application {
    
    private static final String APP_TITLE = "ToDo 日程管理";
    private static final double MIN_WIDTH = 1200;
    private static final double MIN_HEIGHT = 700;
    
    private MainController mainController;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            mainController = new MainController();
            
            Scene scene = new Scene(mainController.getRoot(), MIN_WIDTH, MIN_HEIGHT);
            
            // 加载默认主题
            scene.getStylesheets().add(getClass().getResource("/styles/light-theme.css").toExternalForm());
            
            // 将场景对象传递给控制器
            mainController.setScene(scene);
            
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            primaryStage.getIcons().setAll(
                createAppIconImage(16),
                createAppIconImage(24),
                createAppIconImage(32),
                createAppIconImage(48),
                createAppIconImage(64),
                createAppIconImage(128),
                createAppIconImage(256)
            );
            applyWindowBadge(primaryStage, 0);
            
            primaryStage.show();
            
            mainController.initialize();
            mainController.setPendingCountListener(pendingCount -> applyWindowBadge(primaryStage, pendingCount));
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("启动应用失败: " + e.getMessage());
        }
    }
    
    @Override
    public void stop() {
        // 应用关闭时的清理工作
        if (mainController != null) {
            mainController.shutdown();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }

    private void applyWindowBadge(Stage stage, int pendingCount) {
        if (stage != null) {
            if (pendingCount > 0) {
                stage.setTitle("(" + pendingCount + ") " + APP_TITLE);
            } else {
                stage.setTitle(APP_TITLE);
            }
        }
        try {
            if (!Taskbar.isTaskbarSupported()) {
                return;
            }
            Taskbar taskbar = Taskbar.getTaskbar();
            if (!taskbar.isSupported(Feature.ICON_BADGE_TEXT)) {
                return;
            }
            String badgeText;
            if (pendingCount <= 0) {
                badgeText = "";
            } else if (pendingCount > 99) {
                badgeText = "99+";
            } else {
                badgeText = String.valueOf(pendingCount);
            }
            taskbar.setIconBadge(badgeText);
        } catch (Exception ignored) {
        }
    }

    private Image createAppIconImage(int size) {
        try (InputStream stream = getClass().getResourceAsStream("/icons/macaron_todo_icon.svg")) {
            if (stream == null) {
                return createFallbackIconImage(size);
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(stream);
            Element svg = document.getDocumentElement();
            double[] viewBox = parseViewBox(svg.getAttribute("viewBox"));

            Canvas canvas = new Canvas(size, size);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, size, size);

            double viewWidth = Math.max(1, viewBox[2]);
            double viewHeight = Math.max(1, viewBox[3]);
            double scale = Math.min(size / viewWidth, size / viewHeight);
            double offsetX = (size - viewWidth * scale) / 2.0;
            double offsetY = (size - viewHeight * scale) / 2.0;

            gc.save();
            gc.translate(offsetX, offsetY);
            gc.scale(scale, scale);
            gc.translate(-viewBox[0], -viewBox[1]);
            renderSvgChildren(svg, gc);
            gc.restore();

            WritableImage image = new WritableImage(size, size);
            canvas.snapshot(new SnapshotParameters(), image);
            return image;
        } catch (Exception ex) {
            return createFallbackIconImage(size);
        }
    }

    private void renderSvgChildren(Element parent, GraphicsContext gc) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) {
                continue;
            }
            Element element = (Element) children.item(i);
            String tag = element.getTagName().toLowerCase(Locale.ROOT);
            if ("g".equals(tag)) {
                gc.save();
                applyTransform(gc, element.getAttribute("transform"));
                renderSvgChildren(element, gc);
                gc.restore();
                continue;
            }
            if ("circle".equals(tag)) {
                drawCircle(gc, element);
                continue;
            }
            if ("rect".equals(tag)) {
                drawRect(gc, element);
                continue;
            }
            if ("text".equals(tag)) {
                drawText(gc, element);
            }
        }
    }

    private void drawCircle(GraphicsContext gc, Element element) {
        double cx = parseDouble(element.getAttribute("cx"));
        double cy = parseDouble(element.getAttribute("cy"));
        double r = parseDouble(element.getAttribute("r"));
        Color fill = parseColor(element.getAttribute("fill"), Color.TRANSPARENT);
        gc.setFill(fill);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawRect(GraphicsContext gc, Element element) {
        double x = parseDouble(element.getAttribute("x"));
        double y = parseDouble(element.getAttribute("y"));
        double width = parseDouble(element.getAttribute("width"));
        double height = parseDouble(element.getAttribute("height"));
        double rx = parseDouble(element.getAttribute("rx"));
        Color fill = parseColor(element.getAttribute("fill"), Color.TRANSPARENT);
        gc.setFill(fill);
        if (rx > 0) {
            double arc = rx * 2;
            gc.fillRoundRect(x, y, width, height, arc, arc);
        } else {
            gc.fillRect(x, y, width, height);
        }
    }

    private void drawText(GraphicsContext gc, Element element) {
        double x = parseDouble(element.getAttribute("x"));
        double y = parseDouble(element.getAttribute("y"));
        double fontSize = parseDoubleOrDefault(element.getAttribute("font-size"), 12);
        String fontFamily = firstFontFamily(element.getAttribute("font-family"));
        String weight = element.getAttribute("font-weight");
        FontWeight fontWeight = "bold".equalsIgnoreCase(weight) ? FontWeight.BOLD : FontWeight.NORMAL;
        gc.setFont(Font.font(fontFamily, fontWeight, fontSize));
        gc.setFill(parseColor(element.getAttribute("fill"), Color.BLACK));
        String anchor = element.getAttribute("text-anchor");
        if ("middle".equalsIgnoreCase(anchor)) {
            gc.setTextAlign(TextAlignment.CENTER);
        } else if ("end".equalsIgnoreCase(anchor)) {
            gc.setTextAlign(TextAlignment.RIGHT);
        } else {
            gc.setTextAlign(TextAlignment.LEFT);
        }
        String text = element.getTextContent() == null ? "" : element.getTextContent().trim();
        gc.fillText(text, x, y);
    }

    private void applyTransform(GraphicsContext gc, String transform) {
        if (transform == null || transform.isBlank()) {
            return;
        }
        String value = transform.trim();
        if (value.startsWith("rotate")) {
            int start = value.indexOf('(');
            int end = value.lastIndexOf(')');
            if (start < 0 || end <= start) {
                return;
            }
            String[] params = value.substring(start + 1, end).trim().split("[,\\s]+");
            if (params.length == 0) {
                return;
            }
            double angle = parseDouble(params[0]);
            double cx = params.length > 1 ? parseDouble(params[1]) : 0;
            double cy = params.length > 2 ? parseDouble(params[2]) : 0;
            gc.translate(cx, cy);
            gc.rotate(angle);
            gc.translate(-cx, -cy);
        }
    }

    private String firstFontFamily(String value) {
        if (value == null || value.isBlank()) {
            return "System";
        }
        String family = value.split(",")[0].trim();
        if ((family.startsWith("'") && family.endsWith("'")) || (family.startsWith("\"") && family.endsWith("\""))) {
            family = family.substring(1, family.length() - 1);
        }
        return family.isBlank() ? "System" : family;
    }

    private Color parseColor(String value, Color fallback) {
        if (value == null || value.isBlank() || "none".equalsIgnoreCase(value)) {
            return fallback;
        }
        try {
            return Color.web(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double[] parseViewBox(String viewBox) {
        if (viewBox == null || viewBox.isBlank()) {
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

    private double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Image createFallbackIconImage(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double radius = size * 0.24;
        gc.setFill(Color.web("#EAF4FF"));
        gc.fillRoundRect(0, 0, size, size, radius, radius);
        gc.setFill(Color.web("#1E88E5"));
        gc.fillRoundRect(size * 0.15, size * 0.15, size * 0.7, size * 0.7, radius * 0.8, radius * 0.8);
        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(new SnapshotParameters(), image);
        return image;
    }
}
