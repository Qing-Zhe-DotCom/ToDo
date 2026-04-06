package com.example;

import java.awt.Taskbar;
import java.awt.Taskbar.Feature;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import com.example.application.ApplicationContext;
import com.example.application.FontService;
import com.example.application.LocalizationService;
import com.example.controller.MainController;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static final String APP_ICON_RESOURCE = "/icons/macaron_todo_icon.png";
    private static final double MIN_WIDTH = 1200;
    private static final double MIN_HEIGHT = 700;

    private MainController mainController;
    private Stage primaryStageRef;
    private Image baseIconImage;
    private LocalizationService localizationService;
    private FontService fontService;

    @Override
    public void start(Stage primaryStage) {
        try {
            ApplicationContext applicationContext = ApplicationContext.createDefault();
            localizationService = applicationContext.getLocalizationService();
            fontService = applicationContext.getFontService();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ui/main-shell.fxml"));
            loader.setControllerFactory(type -> {
                if (type == MainController.class) {
                    return new MainController(applicationContext);
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception exception) {
                    throw new IllegalStateException("Unable to construct controller: " + type.getName(), exception);
                }
            });

            Parent root = loader.load();
            fontService.applyTo(root, localizationService.getActiveLanguage());
            mainController = loader.getController();

            Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
            mainController.setScene(scene);

            primaryStageRef = primaryStage;
            primaryStage.setTitle(resolveAppTitle());
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            applyAppIcons(0);
            applyWindowBadge(primaryStage, 0);

            primaryStage.show();

            mainController.initializeApplication();
            mainController.setPendingCountListener(pendingCount -> applyWindowBadge(primaryStage, pendingCount));
        } catch (Exception exception) {
            exception.printStackTrace();
            System.err.println("Failed to start application: " + exception.getMessage());
        }
    }

    @Override
    public void stop() {
        if (mainController != null) {
            mainController.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void applyWindowBadge(Stage stage, int pendingCount) {
        String appTitle = resolveAppTitle();
        if (stage != null) {
            if (pendingCount > 0) {
                stage.setTitle("(" + pendingCount + ") " + appTitle);
            } else {
                stage.setTitle(appTitle);
            }
        }
        applyAppIcons(pendingCount);
        try {
            if (!Taskbar.isTaskbarSupported()) {
                return;
            }
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Feature.ICON_IMAGE)) {
                Image baseImage = loadBaseIconImage();
                if (baseImage != null && pendingCount <= 0) {
                    BufferedImage awtImage = SwingFXUtils.fromFXImage(baseImage, null);
                    taskbar.setIconImage(awtImage);
                } else {
                    BufferedImage awtImage = SwingFXUtils.fromFXImage(createAppIconImage(64, pendingCount), null);
                    taskbar.setIconImage(awtImage);
                }
            }
            if (taskbar.isSupported(Feature.ICON_BADGE_TEXT)) {
                String badgeText;
                if (pendingCount <= 0) {
                    badgeText = "";
                } else if (pendingCount > 99) {
                    badgeText = "99+";
                } else {
                    badgeText = String.valueOf(pendingCount);
                }
                taskbar.setIconBadge(badgeText);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyAppIcons(int pendingCount) {
        if (primaryStageRef == null) {
            return;
        }

        Image baseImage = loadBaseIconImage();
        if (baseImage != null && pendingCount <= 0) {
            primaryStageRef.getIcons().setAll(baseImage);
            return;
        }

        primaryStageRef.getIcons().setAll(
            createAppIconImage(16, pendingCount),
            createAppIconImage(24, pendingCount),
            createAppIconImage(32, pendingCount),
            createAppIconImage(48, pendingCount),
            createAppIconImage(64, pendingCount),
            createAppIconImage(128, pendingCount),
            createAppIconImage(256, pendingCount)
        );
    }

    private Image createAppIconImage(int size, int pendingCount) {
        Image source = loadBaseIconImage();
        if (source == null) {
            return createFallbackIconImage(size);
        }
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, size, size);
        gc.drawImage(source, 0, 0, size, size);
        if (pendingCount > 0) {
            double badgeRadius = Math.max(5, size * 0.18);
            double badgeCx = size - badgeRadius - 1;
            double badgeCy = badgeRadius + 1;
            gc.setFill(Color.web("#e53935"));
            gc.fillOval(badgeCx - badgeRadius, badgeCy - badgeRadius, badgeRadius * 2, badgeRadius * 2);
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, Math.max(8, size * 0.32)));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);
            String text = pendingCount > 99 ? "99+" : String.valueOf(pendingCount);
            gc.fillText(text, badgeCx, badgeCy);
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(params, image);
        return image;
    }

    private Image loadBaseIconImage() {
        if (baseIconImage != null) {
            return baseIconImage;
        }
        try (InputStream iconStream = getClass().getResourceAsStream(APP_ICON_RESOURCE)) {
            if (iconStream == null) {
                return null;
            }
            baseIconImage = new Image(iconStream);
            return baseIconImage;
        } catch (Exception ignored) {
            return null;
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

    private String resolveAppTitle() {
        return localizationService != null ? localizationService.text("app.title") : "ToDo";
    }
}
