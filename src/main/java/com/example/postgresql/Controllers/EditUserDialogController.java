package com.example.postgresql.Controllers;

import com.example.postgresql.API.AuthService;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.concurrent.CompletableFuture;

public class EditUserDialogController {

    @FXML private TextField loginField, phoneField, emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final AuthService authService = new AuthService();
    private JsonObject user;
    private Runnable onSuccess;

    public void setUser(JsonObject user, Runnable onSuccess) {
        this.user = user;
        this.onSuccess = onSuccess;

        loginField.setText(user.get("login").getAsString());
        phoneField.setText(user.has("phone") && !user.get("phone").isJsonNull() ? user.get("phone").getAsString() : "");
        emailField.setText(user.has("email") && !user.get("email").isJsonNull() ? user.get("email").getAsString() : "");
    }

    @FXML private void onSave() {
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (!phone.matches("\\+7\\d{10}")) {
            showError("Неверный формат телефона! Должен быть: +7XXXXXXXXXX");
            return;
        }
        if (!email.isEmpty() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Неверный email!");
            return;
        }

        JsonObject update = new JsonObject();
        update.addProperty("phone", phone);
        update.addProperty("email", email);
        if (!password.isEmpty()) {
            update.addProperty("password", password);
        }

        statusLabel.setText("Сохранение...");
        statusLabel.setStyle("-fx-text-fill: blue;");

        int userId = user.get("id").getAsInt();

        authService.supabase.update("users", update, "id=eq." + userId)
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        statusLabel.setText("Сохранено!");
                        statusLabel.setStyle("-fx-text-fill: green;");
                        if (onSuccess != null) onSuccess.run();
                        closeDialog();
                    } else {
                        showError("Ошибка сохранения");
                    }
                }));
    }

    @FXML
    public void onCancel() {
        closeDialog();
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: red;");
        new Timeline(new KeyFrame(Duration.seconds(7), e -> statusLabel.setText(""))).play();
    }

    private void closeDialog() {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }
}