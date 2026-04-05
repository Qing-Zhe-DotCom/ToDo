module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.sql;
    requires java.prefs;
    requires java.xml;
    requires java.desktop;
    
    exports com.example;
    exports com.example.controller;
    exports com.example.model;
    exports com.example.view;
    exports com.example.application;

    opens com.example.controller to javafx.fxml;
}
