package com.example.postgresql.controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.support.AgreementPanelController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.regex.Pattern;

public class RegController {

    @FXML
    public TextField login;
    @FXML
    private PasswordField password;
    @FXML
    public TextField email;
    @FXML
    public TextField phone;
    @FXML
    public TextField passVisible;
    @FXML
    private Label statusLabel;
    @FXML
    private Hyperlink agreement;

    private boolean visiblePass = false;
    private final AuthService auth = new AuthService();

    @FXML
    private void AgreementPanelOpen(ActionEvent e) {
        new AgreementPanelController().AgreementPanel(e);
    }

    @FXML
    private void Register() {
        String user = login.getText().trim().toLowerCase();
        String pass = visiblePass ? passVisible.getText().trim() : password.getText().trim();
        String mail = email.getText().trim();
        String tel = phone.getText().trim();
        if (user.isEmpty() || pass.isEmpty() || tel.isEmpty() || mail.isEmpty()) {
            setStatus("Заполните все обязательные поля (включая email)");
            return;
        }
        if (!isValidEmail(mail)) {
            setStatus("Некорректный адрес email");
            return;
        }
        if (pass.length() < 6 || pass.length() > 15) {
            setStatus("Пароль должен быть от 6 до 15 символов");
            return;
        }
        if (!tel.matches("\\+?\\d{10,15}")) {
            setStatus("Некорректный номер телефона");
            return;
        }

        setStatus("Регистрация...");
        auth.registerUser(user, pass, mail, tel)
                .thenAccept(error -> Platform.runLater(() -> {
                    if (error == null) {
                        setStatus("Регистрация успешна! Войдите в аккаунт.");
                        new Timeline(new KeyFrame(Duration.millis(1800), ev -> back())).play();
                    } else {
                        setStatus(error);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> setStatus("Не удалось подключиться к серверу. Проверьте интернет-соединение."));
                    return null;
                });
    }

    @FXML
    private void goBack(ActionEvent event) {
        back();
    }

    private void back() {
        Stage stage = (Stage) login.getScene().getWindow();
        new HelloController().goBack(stage);
        stage.centerOnScreen();
    }

    @FXML
    private void PassInText(ActionEvent event) {
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

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private static boolean isValidEmail(String email) {
        return Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$").matcher(email).matches();
    }
}
