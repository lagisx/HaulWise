package com.example.postgresql.controllers;

import com.example.postgresql.API.CompanyService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class InviteCardController {

    @FXML
    private Label companyNameLabel;
    @FXML
    private Label fromLoginLabel;
    @FXML
    private Button acceptBtn;
    @FXML
    private Button declineBtn;
    @FXML
    private Label statusLabel;

    private int inviteId;
    private int companyId;
    private String toLogin;
    private Runnable onAccepted;
    private Runnable onDeclined;

    private final CompanyService companyService = new CompanyService();

    public void setData(int inviteId, int companyId, String companyName,
                        String fromLogin, String toLogin,
                        Runnable onAccepted, Runnable onDeclined) {
        this.inviteId = inviteId;
        this.companyId = companyId;
        this.toLogin = toLogin;
        this.onAccepted = onAccepted;
        this.onDeclined = onDeclined;

        companyNameLabel.setText("🏢 " + companyName);
        fromLoginLabel.setText("от: " + fromLogin);
    }

    @FXML
    private void onAccept() {
        setButtonsDisabled(true);
        showStatus("Принимаем...", "#2563eb");

        companyService.acceptInvite(inviteId, toLogin, companyId)
                .thenAccept(ok -> Platform.runLater(() -> {
                    if (ok) {
                        showStatus("✅ Вы вступили в компанию!", "#16a34a");
                        if (onAccepted != null) onAccepted.run();
                    } else {
                        showStatus("Не удалось принять приглашение", "red");
                        setButtonsDisabled(false);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showStatus("Ошибка: " + ex.getMessage(), "red");
                        setButtonsDisabled(false);
                    });
                    return null;
                });
    }

    @FXML
    private void onDecline() {
        setButtonsDisabled(true);
        showStatus("Отклоняем...", "#64748b");

        companyService.declineInvite(inviteId)
                .thenAccept(ok -> Platform.runLater(() -> {
                    if (ok) {
                        showStatus("Приглашение отклонено", "#64748b");
                        if (onDeclined != null) onDeclined.run();
                    } else {
                        showStatus("Не удалось отклонить", "red");
                        setButtonsDisabled(false);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showStatus("Ошибка: " + ex.getMessage(), "red");
                        setButtonsDisabled(false);
                    });
                    return null;
                });
    }

    private void setButtonsDisabled(boolean disabled) {
        acceptBtn.setDisable(disabled);
        declineBtn.setDisable(disabled);
    }

    private void showStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}
