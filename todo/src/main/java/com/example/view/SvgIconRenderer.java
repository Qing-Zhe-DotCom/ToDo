package com.example.view;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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

public final class SvgIconRenderer {
    public Group loadSvgGraphic(Class<?> resourceAnchor, String resourcePath, double targetSize) {
        if (resourceAnchor == null || resourcePath == null || resourcePath.isBlank()) {
            return new Group();
        }

        try (InputStream stream = resourceAnchor.getResourceAsStream(resourcePath)) {
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
        } catch (Exception exception) {
            return new Group();
        }
    }

    public Image renderSvgImage(Class<?> resourceAnchor, String resourcePath, int size) {
        Group graphic = loadSvgGraphic(resourceAnchor, resourcePath, size);
        Pane container = new Pane(graphic);
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        container.setStyle("-fx-background-color: transparent;");
        container.resize(size, size);
        container.layout();
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage(size, size);
        container.snapshot(parameters, image);
        return image;
    }

    public Image createFallbackAppIcon(int size) {
        Pane container = new Pane();
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);

        double radius = size * 0.24;
        Rectangle outer = new Rectangle(size, size);
        outer.setArcWidth(radius);
        outer.setArcHeight(radius);
        outer.setFill(Color.web("#EAF4FF"));

        Rectangle inner = new Rectangle(size * 0.15, size * 0.15, size * 0.7, size * 0.7);
        inner.setArcWidth(radius * 0.8);
        inner.setArcHeight(radius * 0.8);
        inner.setFill(Color.web("#1E88E5"));

        container.getChildren().addAll(outer, inner);
        container.resize(size, size);
        container.layout();
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage(size, size);
        container.snapshot(parameters, image);
        return image;
    }

    private void parseSvgChildren(Element parent, Group target) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            org.w3c.dom.Node child = children.item(index);
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
            text.setTextOrigin(VPos.CENTER);

            String fontFamily = element.getAttribute("font-family");
            if (fontFamily.isEmpty()) {
                fontFamily = "System";
            } else {
                fontFamily = fontFamily.replaceAll("['\"]", "");
            }

            double fontSize = parseDoubleOrDefault(element.getAttribute("font-size"), 12);
            String fontWeight = element.getAttribute("font-weight");
            FontWeight resolvedWeight = "bold".equalsIgnoreCase(fontWeight) ? FontWeight.BOLD : FontWeight.NORMAL;
            text.setFont(Font.font(fontFamily, resolvedWeight, fontSize));

            String textAnchor = element.getAttribute("text-anchor");
            if ("middle".equals(textAnchor)) {
                text.setTextAlignment(TextAlignment.CENTER);
                text.setBoundsType(TextBoundsType.VISUAL);
                double initialWidth = text.getLayoutBounds().getWidth();
                if (initialWidth > 0) {
                    text.setTranslateX(-initialWidth / 2);
                }
                text.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                    text.setTranslateX(-newBounds.getWidth() / 2);
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
        shape.setFill(fill != null ? fill : Color.TRANSPARENT);
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

    private void applyTransform(javafx.scene.Node node, String transformStr) {
        if (transformStr == null || transformStr.isEmpty()) {
            return;
        }

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
            } catch (Exception ignored) {
            }
        }
    }

    private Color parsePaint(String value) {
        if (value == null || value.isEmpty() || "none".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException exception) {
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

    private double parseDoubleOrDefault(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
