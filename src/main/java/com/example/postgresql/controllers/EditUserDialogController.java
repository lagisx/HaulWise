package com.example.postgresql.controllers;

import com.example.postgresql.api.AuthService;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class EditUserDialogController {

    @FXML private TextField loginField, phoneField, emailField;
    @FXML private Label     statusLabel;

    private final AuthService auth = new AuthService();
    private JsonObject user;
    private Runnable   onSuccess;

    public void setUser(JsonObject user, Runnable onSuccess) {
        this.user      = user;
        this.onSuccess = onSuccess;

        loginField.setText(user.get("login").getAsString());
        phoneField.setText(user.has("phone") && !user.get("phone").isJsonNull()
                ? user.get("phone").getAsString() : "");
        emailField.setText(user.has("email") && !user.get("email").isJsonNull()
                ? user.get("email").getAsString() : "");
    }

    @FXML private void onSave() {
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (!phone.isEmpty() && !phone.matches("\\+?\\d{10,15}")) {
            showError("Неверный формат телефона! Пример: +79001234567");
            return;
        }
        if (!email.isEmpty() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Неверный email!");
            return;
        }

        JsonObject update = new JsonObject();
        if (!phone.isEmpty()) update.addProperty("phone", phone);
        if (!email.isEmpty()) update.addProperty("email", email);

        int userId = user.get("id").getAsInt();
        auth.supabase.update("users", update, "id=eq." + userId)
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        if (onSuccess != null) onSuccess.run();
                        closeDialog();
                    } else {
                        showError("Ошибка сохранения");
                    }
                }));
    }

    @FXML public void onCancel() { closeDialog(); }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill:red;");
        new Timeline(new KeyFrame(Duration.seconds(7), e -> statusLabel.setText(""))).play();
    }

    private void closeDialog() {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }
}
