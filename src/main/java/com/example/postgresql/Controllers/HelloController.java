package com.example.postgresql.Controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.HelloApplication;
import com.example.postgresql.support.SupportTechController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class HelloController {

    /// TODO СДЕЛАТЬ ФИЛЬТРЫ (ПОИСК ПО МАРШРУТУ
    /// TODO Сделать дату в виде DatePicker
    /// TODO Сделать масштабируемость под окно (полножкранный и обычный)


    public static final String DB_URL = "jdbc:postgresql://aws-1-eu-west-1.pooler.supabase.com:5432/postgres?sslmode=require";
    public static final String DB_USER = "postgres.mkdwltdoayuhuikzycod";
    public static final String DB_PASSWORD = "lagisx";
    public static final String DataBase_Name = "postgres";

    @FXML public TextField username;
    @FXML private PasswordField password;

    @FXML private Label statusLabel;
    @FXML private TextFlow statusFlow;

    @FXML TextField passVisible;
    private boolean visiblePass = false;

    @FXML
    private void Connect(ActionEvent event) {
        String user = username.getText().trim();
        String pass = visiblePass ? passVisible.getText().trim() : password.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showStatus("Введите логин и пароль");
            return;
        }

        AuthService authService = new AuthService();
        authService.checkBlacklist(user)
                .thenCompose(blockStatus -> {
                    if (blockStatus.isBlocked) {
                        Platform.runLater(() -> showBlockedStatus(blockStatus.reason));
                        return CompletableFuture.completedFuture(null);
                    }
                    return authService.authenticate(user, pass);
                })
                .thenAccept(authResult -> {
                    if (authResult == null) return;

                    Platform.runLater(() -> {
                        if (authResult.success) {
                            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
                            try {
                                if ("admin".equalsIgnoreCase(authResult.role)) {
                                    AdminPanelController.AdminPanel(stage, user, pass);
                                } else {
                                    UserPanelController.UserPanel(stage, user, pass);
                                }
                            } catch (IOException e) {
                                showStatus("Ошибка при открытии панели");
                                e.printStackTrace();
                            }
                        } else {
                            showStatus(authResult.message);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Ошибка при подключении к базе!"));
                    ex.printStackTrace();
                    return null;
                });
    }
    private void showStatus(String message) {
        statusFlow.getChildren().clear();
        Text text = new Text(message);
        text.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-fill: red;");
        statusFlow.getChildren().add(text);
        statusFlow.setTextAlignment(TextAlignment.CENTER);
    }

    private void showBlockedStatus(String reason) {
        statusFlow.getChildren().clear();
        statusFlow.setTextAlignment(TextAlignment.CENTER);
        Text t1 = new Text("Данный пользователь заблокирован:\n" + reason + "\nЧтобы узнать подробнее обратитесь в");
        t1.setStyle("-fx-fill: red; -fx-font-size: 14; -fx-font-weight: bold");

        Hyperlink supportLink = new Hyperlink("тех.поддержку");
        supportLink.setStyle("-fx-font-size: 14; -fx-font-weight: bold");
        supportLink.setOnAction(this::openSupport);

        statusFlow.getChildren().addAll(t1, supportLink);
    }

    @FXML private void openSupport(ActionEvent event) {
        SupportTechController sup = new SupportTechController();
        sup.SupportTechPanel(event);
    }

    @FXML private void showRegs(ActionEvent event) {
        try {
            FXMLLoader regfxml = new FXMLLoader(HelloApplication.class.getResource("reg.fxml"));
            Scene scene1 = new Scene(regfxml.load());

            Stage currentStage = (Stage) username.getScene().getWindow();
            currentStage.centerOnScreen();
            currentStage.setTitle("Регистрация");
            currentStage.setScene(scene1);

        } catch (IOException e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void showForgetPass(ActionEvent event) {
        try {
            FXMLLoader forgetpassFXML = new FXMLLoader(HelloApplication.class.getResource("forgetpass.fxml"));
            Scene scene1 = new Scene(forgetpassFXML.load());

            Stage currentStage = (Stage) username.getScene().getWindow();
            currentStage.setTitle("Восстановление пароля");
            currentStage.centerOnScreen();
            currentStage.setScene(scene1);
        } catch (IOException e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
            statusLabel.setAlignment(Pos.CENTER);
        }
    }

    @FXML public void goBack(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("main.fxml"));
            Scene scene = new Scene(loader.load());

            stage.setScene(scene);
            stage.show();
            stage.centerOnScreen();
            stage.setTitle("Авторизация");

        } catch (IOException e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML public void goGuestPanel() throws IOException {
        Stage currentStage = (Stage) username.getScene().getWindow();
        GuestPanelController.GuestPanel(currentStage);
    }

    @FXML
    public void PassInText(ActionEvent event){
        visiblePass = !visiblePass;

        if (visiblePass) {
            passVisible.setText(password.getText());
            passVisible.setVisible(true);
            passVisible.setManaged(true);
            password.setVisible(false);
            password.setManaged(false);

            passVisible.requestFocus();
            passVisible.positionCaret(passVisible.getText().length());
        } else {
            password.setText(passVisible.getText());
            password.setVisible(true);
            password.setManaged(true);

            passVisible.setVisible(false);
            passVisible.setManaged(false);

            password.requestFocus();
            password.positionCaret(password.getText().length());
        }
    }
}
