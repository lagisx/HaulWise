package com.example.postgresql.controllers;

import com.example.postgresql.API.CompanyService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class CreateCompanyController {

    @FXML
    private TextField companyNameField;
    @FXML
    private TextField webhookField;
    @FXML
    private Button createBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private Label statusLabel;

    private String currentUser;
    private Runnable onSuccessCallback;

    private final CompanyService companyService = new CompanyService();

    public void setData(String login, Runnable onSuccess) {
        this.currentUser = login;
        this.onSuccessCallback = onSuccess;
    }

    @FXML
    private void onCreate() {
        String name = companyNameField.getText().trim();
        String webhook = webhookField.getText().trim();

        if (name.isEmpty()) {
            showStatus("Введите название компании", "orange");
            return;
        }

        createBtn.setDisable(true);
        showStatus("Создаём компанию...", "#2563eb");

        companyService.createCompany(name, webhook, currentUser)
                .thenCompose(companyId -> {
                    if (companyId <= 0) {
                        return java.util.concurrent.CompletableFuture.completedFuture(false);
                    }
                    return companyService.linkUserToCompany(currentUser, companyId, "owner");
                })
                .thenAccept(ok -> Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    if (ok) {
                        showStatus("✅ Компания создана!", "#16a34a");
                        if (onSuccessCallback != null) onSuccessCallback.run();
                        new javafx.animation.Timeline(
                                new javafx.animation.KeyFrame(
                                        javafx.util.Duration.seconds(1),
                                        e -> ((Stage) createBtn.getScene().getWindow()).close()
                                )
                        ).play();
                    } else {
                        showStatus("Не удалось создать компанию. Попробуйте позже.", "red");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        createBtn.setDisable(false);
                        showStatus("Ошибка: " + ex.getMessage(), "red");
                    });
                    return null;
                });
    }

    @FXML
    private void onCancel() {
        ((Stage) cancelBtn.getScene().getWindow()).close();
    }

    private void showStatus(String text, String color) {
        if (statusLabel == null) return;
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }
}
