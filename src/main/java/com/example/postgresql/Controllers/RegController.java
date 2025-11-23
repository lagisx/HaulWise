package com.example.postgresql.Controllers;

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

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class RegController {

    @FXML public TextField login;
    @FXML private PasswordField password;
    @FXML private Label statusLabel;
    @FXML public TextField email;
    @FXML public TextField phone;
    @FXML public TextField passVisible;  // текстовое поле для отображения пароля
    @FXML private Hyperlink agreement;

    private boolean visiblePass = false;

    private final AuthService authService = new AuthService();

    @FXML
    private void AgreementPanelOpen(ActionEvent event) {
        new AgreementPanelController().AgreementPanel(event);
    }

    @FXML
    private void Register() {
        String user = login.getText().trim().toLowerCase();
        String pass = visiblePass ? passVisible.getText().trim() : password.getText().trim();
        String mail = email.getText().trim();
        String phonenum = phone.getText().trim();

        if (user.isEmpty() || pass.isEmpty() || phonenum.isEmpty()) {
            setStatus("Заполните все обязательные поля");
            return;
        }

        if (pass.length() < 8 || pass.length() > 15) {
            setStatus("Пароль должен быть от 8 до 15 символов");
            return;
        }

        if (!phonenum.matches("\\+?\\d{10,15}")) {
            setStatus("Некорректный номер телефона");
            return;
        }

        if (!mail.isEmpty() && !isValidEmail(mail)) {
            setStatus("Некорректный адрес email");
            return;
        }

        authService.registerUser(user, pass, mail.isEmpty() ? "" : mail, phonenum)
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        setStatus("Регистрация успешна!");
                        new Timeline(new KeyFrame(Duration.millis(1200), e -> back())).play();
                    } else {
                        setStatus("Логин уже занят или ошибка сервера");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> setStatus("Ошибка сети: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void goBack(ActionEvent event) {
        back();
    }

    private void back() {
        Stage stage = (Stage) (login != null ? login.getScene().getWindow() :
                password.getScene().getWindow());
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
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return Pattern.compile(regex).matcher(email).matches();
    }
}