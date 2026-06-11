package com.example.postgresql.controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.HelloApplication;
import com.example.postgresql.API.AuthService.UserAuthResult;
import com.example.postgresql.support.SupportTechController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

import java.util.concurrent.CompletableFuture;

public class HelloController {

    @FXML
    public TextField username;
    @FXML
    private PasswordField password;
    @FXML
    public TextField passVisible;
    @FXML
    private Label statusLabel;
    @FXML
    private TextFlow statusFlow;

    private boolean visiblePass = false;


    @FXML
    private void connect(ActionEvent event) {
        String user = username.getText().trim();
        String pass = visiblePass ? passVisible.getText().trim() : password.getText().trim();

        if (user.equals("__reset__")) {
            showAdminResetDialog();
            username.clear();
            return;
        }
        if (user.isEmpty() || pass.isEmpty()) {
            showStatus("Введите логин и пароль");
            return;
        }

        performLogin(user, pass, event);
    }

    private void performLogin(String user, String pass, ActionEvent event) {
        AuthService auth = new AuthService();
        auth.checkBlacklist(user)
                .thenCompose(block -> {
                    if (block.isBlocked) {
                        Platform.runLater(() -> showBlockedStatus(block.reason));
                        return CompletableFuture.completedFuture(null);
                    }
                    return auth.authenticate(user, pass);
                })
                .thenAccept(result -> Platform.runLater(() -> handleAuthResult(result, user, event)))
                .exceptionally(e -> {
                    e.printStackTrace();
                    Platform.runLater(() -> showStatus("Не удалось подключиться к серверу. Проверьте интернет-соединение."));
                    return null;
                });
    }

    private void handleAuthResult(UserAuthResult result, String user, ActionEvent event) {
        if (result == null) return;
        if (result.success) {
            openPanel(result, user, event);
        } else {
            if (result.blockReason != null) showBlockedStatus(result.blockReason);
            else showStatus(result.message);
        }
    }

    private void openPanel(UserAuthResult result, String user, ActionEvent event) {
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        try {
            if ("admin".equalsIgnoreCase(result.role)) {
                AdminPanelController.AdminPanel(stage, user, "");
            } else {
                UserPanelController.userPanel(stage, user, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String msg = cause.getMessage() != null ? cause.getMessage() : e.getClass().getSimpleName();
            showStatus("Ошибка открытия панели: " + msg);

            Throwable t = e;
            while (t != null) {
                System.err.println("[openPanel] причина: " + t.getClass().getName() + ": " + t.getMessage());
                t = t.getCause();
            }
        }
    }

    private void showAdminResetDialog() {
        javafx.scene.control.Dialog<String[]> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Сброс пароля (Admin)");
        dialog.setHeaderText("Введите логин и новый пароль");

        javafx.scene.control.TextField loginF = new javafx.scene.control.TextField();
        loginF.setPromptText("Логин пользователя");
        javafx.scene.control.PasswordField passF = new javafx.scene.control.PasswordField();
        passF.setPromptText("Новый пароль");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(8, new Label("Логин:"), loginF, new Label("Новый пароль:"), passF);
        box.setPadding(new javafx.geometry.Insets(10));
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? new String[]{loginF.getText().trim(), passF.getText().trim()} : null);

        dialog.showAndWait().ifPresent(arr -> {
            String login = arr[0];
            String newPass = arr[1];
            if (login.isEmpty() || newPass.isEmpty()) {
                showStatus("Заполните все поля");
                return;
            }
            if (newPass.length() < 6) {
                showStatus("Пароль минимум 6 символов");
                return;
            }

            showStatus("Сбрасываем пароль...");
            new AuthService().adminChangePassword_byLogin(login, newPass)
                    .thenAccept(ok -> Platform.runLater(() -> {
                        if (ok) showStatus("✅ Пароль для " + login + " успешно сброшен!");
                        else showStatus("❌ Ошибка сброса. Проверьте логин.");
                    }));
        });
    }

    private void showStatus(String message) {
        statusFlow.getChildren().clear();
        Text t = new Text(message);
        t.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-fill:red;");
        statusFlow.getChildren().add(t);
        statusFlow.setTextAlignment(TextAlignment.CENTER);
        new Timeline(new KeyFrame(Duration.seconds(3), _ -> statusFlow.getChildren().clear())).play();
    }

    private void showBlockedStatus(String reason) {
        statusFlow.getChildren().clear();
        statusFlow.setTextAlignment(TextAlignment.CENTER);
        Text t1 = new Text("Данный пользователь заблокирован:\n" + reason + "\nЧтобы узнать подробнее обратитесь в");
        t1.setStyle("-fx-fill:red;-fx-font-size:14;-fx-font-weight:bold");
        Hyperlink link = new Hyperlink("тех.поддержку");
        link.setStyle("-fx-font-size:14;-fx-font-weight:bold");
        link.setOnAction(e -> openSupportWithAutoFill(reason));
        statusFlow.getChildren().addAll(t1, link);
    }

    private void openSupportWithAutoFill(String reason) {
        String login = username.getText().trim();
        new SupportTechController().SupportTechPanelWithPrefill(
                login, "Прошу разблокировать аккаунт",
                "Здравствуйте!\n\nМой логин: " + login +
                        "\nАккаунт заблокирован по причине: " + reason +
                        "\n\nПрошу рассмотреть возможность разблокировки.\nГотов предоставить пояснения.\n\nСпасибо.");
    }


    @FXML
    private void showRegs(ActionEvent event) {
        try {
            Stage stage = (Stage) username.getScene().getWindow();
            Scene regScene = new Scene(new FXMLLoader(HelloApplication.class.getResource("reg.fxml")).load());
            stage.setScene(regScene);
            stage.setTitle("Регистрация");
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (IOException _) {
            statusLabel.setText("Ошибка регистрации" );
        }
    }

    @FXML
    private void showForgetPass(ActionEvent event) {
        try {
            Stage stage = (Stage) username.getScene().getWindow();
            stage.setScene(new Scene(new FXMLLoader(HelloApplication.class.getResource("forgetpass.fxml")).load()));
            stage.setTitle("Восстановление пароля");
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (IOException e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
            statusLabel.setAlignment(Pos.CENTER);
        }
    }

    @FXML
    public void goBack(Stage stage) {
        try {
            stage.setScene(new Scene(new FXMLLoader(HelloApplication.class.getResource("main.fxml")).load()));
            stage.setTitle("Авторизация");
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            if (statusLabel != null) statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    public void goGuestPanel() throws IOException {
        GuestPanelController.GuestPanel((Stage) username.getScene().getWindow());
    }

    @FXML
    public void passInText(ActionEvent event) {
        visiblePass = !visiblePass;
        if (visiblePass) {
            passVisible.setText(password.getText());
            passVisible.setVisible(true);
            passVisible.setManaged(true);
            password.setVisible(false);
            password.setManaged(false);
            passVisible.requestFocus();
            passVisible.positionCaret(passVisible.getLength());
        } else {
            password.setText(passVisible.getText());
            password.setVisible(true);
            password.setManaged(true);
            passVisible.setVisible(false);
            passVisible.setManaged(false);
            password.requestFocus();
            password.positionCaret(password.getLength());
        }
    }
}
