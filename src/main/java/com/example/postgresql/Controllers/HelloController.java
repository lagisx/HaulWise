package com.example.postgresql.Controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.HelloApplication;
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

    @FXML public  TextField     username;
    @FXML private PasswordField password;
    @FXML public  TextField     passVisible;
    @FXML private Label         statusLabel;
    @FXML private TextFlow      statusFlow;

    private boolean visiblePass = false;

    

    @FXML
    private void Connect(ActionEvent event) {
        String user = username.getText().trim();
        String pass = visiblePass ? passVisible.getText().trim() : password.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showStatus("Введите логин и пароль");
            return;
        }

        AuthService auth = new AuthService();

        auth.checkBlacklist(user)
                .thenCompose(block -> {
                    if (block.isBlocked) {
                        Platform.runLater(() -> showBlockedStatus(block.reason));
                        return CompletableFuture.completedFuture(null);
                    }
                    return auth.authenticate(user, pass);
                })
                .thenAccept(result -> {
                    if (result == null) return;
                    Platform.runLater(() -> {
                        if (result.success) {
                            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
                            try {
                                if ("admin".equalsIgnoreCase(result.role))
                                    AdminPanelController.AdminPanel(stage, user, "");
                                else
                                    UserPanelController.UserPanel(stage, user, "");
                            } catch (Exception e) {
                                showStatus("Ошибка открытия панели: " + e.getMessage());
                            }
                        } else {
                            if (result.blockReason != null) showBlockedStatus(result.blockReason);
                            else showStatus(result.message);
                        }
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> showStatus("Ошибка соединения с сервером"));
                    return null;
                });
    }

    

    private void showStatus(String message) {
        statusFlow.getChildren().clear();
        Text t = new Text(message);
        t.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-fill:red;");
        statusFlow.getChildren().add(t);
        statusFlow.setTextAlignment(TextAlignment.CENTER);
        new Timeline(new KeyFrame(Duration.seconds(3), e -> statusFlow.getChildren().clear())).play();
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

    

    @FXML private void showRegs(ActionEvent event) {
        try {
            Stage stage = (Stage) username.getScene().getWindow();
            Scene regScene = new Scene(new FXMLLoader(HelloApplication.class.getResource("reg.fxml")).load());
            stage.setScene(regScene);
            stage.setTitle("Регистрация");
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (IOException e) { statusLabel.setText("Ошибка: " + e.getMessage()); }
    }

    @FXML private void showForgetPass(ActionEvent event) {
        try {
            Stage stage = (Stage) username.getScene().getWindow();
            stage.setScene(new Scene(new FXMLLoader(HelloApplication.class.getResource("forgetpass.fxml")).load()));
            stage.setTitle("Восстановление пароля");
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (IOException e) { statusLabel.setText("Ошибка: " + e.getMessage()); statusLabel.setAlignment(Pos.CENTER); }
    }

    @FXML public void goBack(Stage stage) {
        try {
            stage.setScene(new Scene(new FXMLLoader(HelloApplication.class.getResource("main.fxml")).load()));
            stage.setTitle("Авторизация");
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) { if (statusLabel != null) statusLabel.setText("Ошибка: " + e.getMessage()); }
    }

    @FXML public void goGuestPanel() throws IOException {
        GuestPanelController.GuestPanel((Stage) username.getScene().getWindow());
    }

    @FXML
    public void PassInText(ActionEvent event) {
        visiblePass = !visiblePass;
        if (visiblePass) {
            passVisible.setText(password.getText());
            passVisible.setVisible(true);  passVisible.setManaged(true);
            password.setVisible(false);    password.setManaged(false);
            passVisible.requestFocus();    passVisible.positionCaret(passVisible.getLength());
        } else {
            password.setText(passVisible.getText());
            password.setVisible(true);     password.setManaged(true);
            passVisible.setVisible(false); passVisible.setManaged(false);
            password.requestFocus();       password.positionCaret(password.getLength());
        }
    }
}
