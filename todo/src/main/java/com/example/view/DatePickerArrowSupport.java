package com.example.view;

import com.example.application.IconKey;
import com.example.controller.MainController;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

final class DatePickerArrowSupport {
    private static final String INSTALL_FLAG = "calendar-arrow-support-installed";
    private static final String CUSTOMIZED_FLAG = "calendar-arrow-customized";

    private DatePickerArrowSupport() {
    }

    static void install(DatePicker datePicker, MainController controller) {
        if (datePicker == null || controller == null) {
            return;
        }
        if (Boolean.TRUE.equals(datePicker.getProperties().get(INSTALL_FLAG))) {
            return;
        }

        datePicker.getProperties().put(INSTALL_FLAG, Boolean.TRUE);
        EventHandler<Event> existingOnShown = datePicker.getOnShown();
        datePicker.setOnShown(event -> {
            if (existingOnShown != null) {
                existingOnShown.handle(event);
            }
            Platform.runLater(() -> {
                applyCustomDatePickerArrows(controller);
                Platform.runLater(() -> applyCustomDatePickerArrows(controller));
            });
        });
    }

    private static void applyCustomDatePickerArrows(MainController controller) {
        for (Window window : Window.getWindows()) {
            if (!(window instanceof PopupWindow)) {
                continue;
            }
            if (window.getScene() == null || !(window.getScene().getRoot() instanceof Parent)) {
                continue;
            }

            Parent root = (Parent) window.getScene().getRoot();
            if (root.lookup(".month-year-pane") == null) {
                continue;
            }

            for (Node node : root.lookupAll(".left-button")) {
                if (node instanceof Button) {
                    applyCalendarArrowButton((Button) node, controller, IconKey.ARROW_LEFT, ".left-arrow");
                }
            }
            for (Node node : root.lookupAll(".right-button")) {
                if (node instanceof Button) {
                    applyCalendarArrowButton((Button) node, controller, IconKey.ARROW_RIGHT, ".right-arrow");
                }
            }
        }
    }

    private static void applyCalendarArrowButton(
        Button button,
        MainController controller,
        IconKey iconKey,
        String arrowSelector
    ) {
        if (Boolean.TRUE.equals(button.getProperties().get(CUSTOMIZED_FLAG))) {
            return;
        }

        button.getProperties().put(CUSTOMIZED_FLAG, Boolean.TRUE);
        button.getStyleClass().add("calendar-arrow-button");
        button.setGraphic(controller.createSvgIcon(iconKey, null, 18));
        button.setText("");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setStyle(
            "-fx-background-color: transparent; -fx-background-insets: 0; "
                + "-fx-padding: 0; -fx-effect: null; -fx-border-color: transparent;"
        );
        button.setMinSize(26, 26);
        button.setPrefSize(26, 26);
        button.setMaxSize(26, 26);

        Node arrowNode = button.lookup(arrowSelector);
        if (arrowNode != null) {
            arrowNode.setVisible(false);
            arrowNode.setManaged(false);
        }
    }
}
