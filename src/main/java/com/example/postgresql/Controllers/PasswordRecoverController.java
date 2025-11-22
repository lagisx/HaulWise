package com.example.postgresql.Controllers;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PasswordRecoverController {
    @FXML
    private TextField password;
    @FXML
    private Label statusLabel;

    public String login;

    public void setUserEmail(String email) {
        this.login = email;
    }
    @FXML
    private void Recover(ActionEvent event) {
        String newPass = password.getText().trim();

        if (newPass.isEmpty()) {
            statusLabel.setText("Ошибка! Пустое поле");
            statusLabel.setAlignment(Pos.CENTER);
            return;
        }

        if (newPass.length() < 8) {
            statusLabel.setText("Пароль должен быть от 8 символов");
            statusLabel.setAlignment(Pos.CENTER);
            return;
        } else if (newPass.length() > 15) {
            statusLabel.setText("Пароль должен быть не более 15 символов");
            return;
        } else {
            boolean success = changePassword(login, newPass);
            if (success) {
                Stage stage = (Stage) password.getScene().getWindow();
                HelloController helloController = new HelloController();
                helloController.goBack(stage);
            } else {
                statusLabel.setText("Ошибка! Пользователь не найден");
                statusLabel.setAlignment(Pos.CENTER);
            }
        }
    }

    public static boolean changePassword(String login, String newPassword) {
        Label statusLabel = new Label();
        String lowerLonig = login.toLowerCase().trim();
        if (newPassword.length() < 8) {
            statusLabel.setText("Пароль должен быть от 8 символов");
            return false;
        } else {
            String updateTab = "UPDATE users SET password = ? WHERE login = ?";

            try (Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD)) {
                try (PreparedStatement updateTable = conn.prepareStatement(updateTab)){
                    updateTable.setString(1, newPassword);
                    updateTable.setString(2, lowerLonig);
                    updateTable.executeUpdate();
                }
                return true;

            } catch (SQLException e) {
                statusLabel.setText("Ошибка! " + e.getMessage());
                statusLabel.setAlignment(Pos.CENTER);
                return false;
            }
        }
    }
    @FXML
    private void goBack(ActionEvent event) {
        Stage stage = (Stage) password.getScene().getWindow();
        HelloController helloController = new HelloController();
        helloController.goBack(stage);
    }

}

