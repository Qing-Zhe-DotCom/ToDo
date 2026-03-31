module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    
    //requires mysql.connector.java;
    
    opens com.example to javafx.fxml;
    opens com.example.databaseutil to java.sql;
    opens com.example.model to java.sql;
    
    exports com.example;
}