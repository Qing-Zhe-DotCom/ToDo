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

        Label titleLabel = new Label(controller.text("view.flowchart.title"));
        titleLabel.getStyleClass().add("label-title");

        Label developingLabel = new Label(controller.text("view.flowchart.developing"));
        developingLabel.getStyleClass().add("developing-label");

        Label descriptionLabel = new Label(controller.text("view.flowchart.description"));
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
