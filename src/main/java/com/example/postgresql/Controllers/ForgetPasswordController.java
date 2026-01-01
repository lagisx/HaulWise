package com.example.postgresql.Controllers;

import com.example.postgresql.API.TelegramAPI;
import com.example.postgresql.HelloApplication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class ForgetPasswordController {

    @FXML private TextField login;
    @FXML private Label statusLabel;

    @FXML private Label telegramLinkedLabel;
    @FXML private Button openTelegramButton;
    @FXML private Label commandLabel;

    private String Code;
    private Stage codeStage;
    private String currentUser;
    private Long currentChatId;
    private String currentIdentifier;

    private static final String BOT_USERNAME = "HaulwiseAuthCode";

    private static String generate(int length) {
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @FXML
    public void ForgetPass(ActionEvent event) {
        currentIdentifier = login.getText().trim();
        if (currentIdentifier.isEmpty()) {
            showStatus("Введите логин или email", "#dc2626");
            hideAllHints();
            return;
        }

        String normalized = currentIdentifier.toLowerCase();

        try (Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT login, email, telegram_chat_id FROM users WHERE LOWER(login) = ? OR LOWER(email) = ?")) {

            stmt.setString(1, normalized);
            stmt.setString(2, normalized);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                showStatus("Аккаунт не найден", "#dc2626");
                hideAllHints();
                return;
            }

            currentUser = rs.getString("login");
            currentChatId = rs.getLong("telegram_chat_id");
            if (rs.wasNull()) currentChatId = null;

            Code = generate(6);

            if (currentChatId != null) {
                if (TelegramAPI.sendCode(currentChatId, Code)) {
                    hideAllHints();
                    telegramLinkedLabel.setVisible(true);
                    telegramLinkedLabel.setManaged(true);
                    showStatus("", "");
                    FormCode();
                } else {
                    showStatus("Ошибка отправки в Telegram", "#dc2626");
                }
            } else {
                showTelegramInstruction(currentIdentifier);
                showStatus("Привяжите Telegram для получения кода", "#f59e0b");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showStatus("Ошибка базы данных", "#dc2626");
        }
    }

    private void showTelegramInstruction(String identifier) {
        if (commandLabel != null) {
            commandLabel.setText("/start " + identifier);
        }
    }

    private void hideAllHints() {
        telegramLinkedLabel.setVisible(false);
        telegramLinkedLabel.setManaged(false);
    }

    @FXML
    private void openTelegramBot() {
        if (currentIdentifier == null || currentIdentifier.isEmpty()) {
            showStatus("Введите логин или email", "#f59e0b");
            return;
        }

        String url = "https://t.me/" + BOT_USERNAME + "?start=" + currentIdentifier;
        HelloApplication.getAppHostServices().showDocument(url);

        showStatus("Открываем Telegram...", "#2563eb");
    }

    private void FormCode() {
        codeStage = new Stage();
        codeStage.setTitle("Ввод кода");
        codeStage.setResizable(false);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 40; -fx-background-color: white; -fx-background-radius: 24;");

        Label title = new Label("Введите код из Telegram");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        TextField codeField = new TextField();
        codeField.setPromptText("6 цифр");
        codeField.setMaxWidth(250);
        codeField.setStyle("-fx-font-size: 18px; -fx-padding: 14; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #94a3b8;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");

        Button confirm = new Button("Подтвердить");
        confirm.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 14 40; -fx-background-radius: 16; -fx-font-size: 16px;");
        confirm.setOnAction(e -> {
            if (codeField.getText().trim().equals(Code)) {
                codeStage.close();
                openPasswordRecover();
            } else {
                errorLabel.setText("Неверный код");
            }
        });

        Button cancel = new Button("Отмена");
        cancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-border-color: #cbd5e1; -fx-border-width: 2; -fx-background-radius: 16; -fx-padding: 12 30;");
        cancel.setOnAction(e -> codeStage.close());

        root.getChildren().addAll(title, codeField, confirm, cancel, errorLabel);

        codeStage.setScene(new Scene(root, 450, 350));
        codeStage.show();
    }

    private void openPasswordRecover() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("passwordRecover.fxml"));
            Scene scene = new Scene(loader.load());

            PasswordRecoverController controller = loader.getController();
            controller.setUserEmail(currentUser);

            Stage stage = (Stage) login.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Смена пароля");
            stage.centerOnScreen();
        } catch (IOException e) {
            showStatus("Ошибка перехода", "#dc2626");
        }
    }

    private void showStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    @FXML
    private void goBack(ActionEvent event) {
        Stage stage = (Stage) login.getScene().getWindow();
        new HelloController().goBack(stage);
    }
}