module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    
    exports com.example;
    exports com.example.controller;
    exports com.example.model;
    exports com.example.view;
    exports com.example.databaseutil;
}
