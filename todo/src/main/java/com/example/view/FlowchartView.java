package com.example.view;

import com.example.controller.MainController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class FlowchartView implements View {

    private MainController controller;
    private VBox root;

    public FlowchartView(MainController controller) {
        this.controller = controller;
        initializeUI();
    }

    private void initializeUI() {
        root = new VBox(20);
        root.getStyleClass().add("main-content");
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        // 标题
        Label titleLabel = new Label("日程流程图");
        titleLabel.getStyleClass().add("label-title");

        // 开发中提示
        Label developingLabel = new Label("日程流程图功能正在开发中...");
        developingLabel.getStyleClass().add("developing-label");

        // 说明文字
        Label descriptionLabel = new Label("该功能将展示日程之间的依赖关系和执行流程\n敬请期待后续版本更新");
        descriptionLabel.getStyleClass().add("label-subtitle");
        descriptionLabel.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, developingLabel, descriptionLabel);
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void refresh() {
        // 预留接口，后续实现
    }
}
