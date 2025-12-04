module com.example.testing2 {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // External libraries
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.bootstrapfx.core;
    requires io.github.cdimascio.dotenv.java;
    requires java.desktop;

    // Packages opened to JavaFX FXML for injection
    opens com.example.testing2 to javafx.fxml;
    opens com.example.testing2.controllers to javafx.fxml;

    // Packages exported for other modules (if any)
    exports com.example.testing2;
    exports com.example.testing2.controllers;
    exports com.example.testing2.utils; // DBHelper
}
