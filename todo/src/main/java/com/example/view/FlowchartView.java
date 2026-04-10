package com.example.view;

import com.example.controller.MainController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class FlowchartView implements View {

    private MainController controller;
    private VBox root;

    public FlowchartView(MainController controller) {
        this.controller = controller;
        initializeUI();
    }

    private void initializeUI() {
        root = new VBox(16);
        root.getStyleClass().add("main-content");
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(controller.text("view.flowchart.title"));
        titleLabel.getStyleClass().add("label-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label clockLabel = controller.createHeaderClockLabel();
        header.getChildren().addAll(titleLabel, spacer, clockLabel);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        VBox.setVgrow(content, Priority.ALWAYS);

        Label developingLabel = new Label(controller.text("view.flowchart.developing"));
        developingLabel.getStyleClass().add("developing-label");

        Label descriptionLabel = new Label(controller.text("view.flowchart.description"));
        descriptionLabel.getStyleClass().add("label-subtitle");
        descriptionLabel.setAlignment(Pos.CENTER);

        content.getChildren().addAll(developingLabel, descriptionLabel);
        root.getChildren().addAll(header, content);
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
