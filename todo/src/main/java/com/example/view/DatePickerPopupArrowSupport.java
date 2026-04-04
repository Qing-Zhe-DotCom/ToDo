package com.example.view;

import com.example.controller.MainController;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

final class DatePickerPopupArrowSupport {
    private static final String LEFT_ICON_RESOURCE = "/icons/macaron_arrow-left_icon.svg";
    private static final String RIGHT_ICON_RESOURCE = "/icons/macaron_arrow-right_icon.svg";
    private static final String INSTALL_KEY = "calendar-arrow-support-installed";
    private static final String CUSTOMIZED_KEY = "calendar-arrow-customized";

    private DatePickerPopupArrowSupport() {
    }

    static void install(DatePicker datePicker, MainController controller) {
        if (datePicker == null || controller == null) {
            return;
        }
        if (Boolean.TRUE.equals(datePicker.getProperties().get(INSTALL_KEY))) {
            return;
        }
        datePicker.getProperties().put(INSTALL_KEY, Boolean.TRUE);
        datePicker.addEventHandler(ComboBoxBase.ON_SHOWN, event -> Platform.runLater(() -> {
            applyCustomArrows(controller);
            Platform.runLater(() -> applyCustomArrows(controller));
        }));
    }

    private static void applyCustomArrows(MainController controller) {
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
            customizeButtons(root.lookupAll(".left-button"), controller, LEFT_ICON_RESOURCE, ".left-arrow");
            customizeButtons(root.lookupAll(".right-button"), controller, RIGHT_ICON_RESOURCE, ".right-arrow");
        }
    }

    private static void customizeButtons(Iterable<Node> nodes, MainController controller, String iconPath, String arrowSelector) {
        for (Node node : nodes) {
            if (node instanceof Button) {
                applyCalendarArrowButton((Button) node, controller, iconPath, arrowSelector);
            }
        }
    }

    private static void applyCalendarArrowButton(Button button, MainController controller, String iconPath, String arrowSelector) {
        if (Boolean.TRUE.equals(button.getProperties().get(CUSTOMIZED_KEY))) {
            return;
        }
        button.getProperties().put(CUSTOMIZED_KEY, Boolean.TRUE);
        button.getStyleClass().add("calendar-arrow-button");
        button.setGraphic(controller.createSvgIcon(iconPath, null, 18));
        button.setText("");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0; -fx-effect: null; -fx-border-color: transparent;");
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
