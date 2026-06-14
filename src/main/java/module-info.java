module com.example.postgresql {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires org.postgresql.jdbc;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires javafx.graphics;
    requires java.desktop;
    requires java.compiler;
    requires javafx.base;
    requires jdk.compiler;
    requires com.google.gson;
    requires okhttp3;
    requires java.net.http;

    opens com.example.postgresql to javafx.fxml;
    exports com.example.postgresql;
    exports com.example.postgresql.API;
    opens com.example.postgresql.API to javafx.fxml;
    exports com.example.postgresql.UserF;
    opens com.example.postgresql.UserF to javafx.fxml;
    exports com.example.postgresql.controllers;
    opens com.example.postgresql.controllers to javafx.fxml;
    exports com.example.postgresql.controllers.CardControllers;
    opens com.example.postgresql.controllers.CardControllers to javafx.fxml;
    exports com.example.postgresql.support;
    opens com.example.postgresql.support to javafx.fxml;
    exports com.example.postgresql.utils;
    opens com.example.postgresql.utils to javafx.fxml;
}
