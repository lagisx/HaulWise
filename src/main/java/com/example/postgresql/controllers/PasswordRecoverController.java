package com.example.postgresql.controllers;

import com.example.postgresql.API.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PasswordRecoverController {

    @FXML private Label statusLabel;

    private final AuthService auth = new AuthService();

    @FXML
    private void initialize() {
        setStatus("Ссылка для сброса пароля отправлена на вашу почту.\nПерейдите по ссылке из письма.", "#16a34a");
    }

    private void setStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;-fx-font-size:14px;");
        statusLabel.setAlignment(Pos.CENTER);
    }

    @FXML
    private void goBack(ActionEvent event) {
        new HelloController().goBack((Stage) statusLabel.getScene().getWindow());
    }
}
