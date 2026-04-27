module com.tradematrix {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.gson;
    
    opens com.tradematrix to javafx.fxml, com.google.gson, javafx.base;
    exports com.tradematrix;
}
