package com.example;

import com.example.controller.MainController;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
            
            // 设置应用图标（如果有的话）
            // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app-icon.png")));
            
            primaryStage.show();
            
            // 初始化控制器
            mainController.initialize();
            
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
}
