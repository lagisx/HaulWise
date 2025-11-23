package com.example.postgresql.UserF;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.Controllers.UserPanelController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.regex.Pattern;

public class ProfileUser {

    @FXML private Label LabelUser;
    @FXML private TextField login;
    @FXML private TextField email;
    @FXML private TextField phone;
    @FXML private PasswordField passwordVIEW;
    @FXML private Label statusLabel;

    private String currentUser;
    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        // Автоочистка полей при фокусе
        clearFieldOnFocus(login);
        clearFieldOnFocus(email);
        clearFieldOnFocus(phone);
    }

    private void clearFieldOnFocus(TextField field) {
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused && field.getText().isEmpty()) {
                Platform.runLater(field::clear);
            }
        });
    }

    public void setUserData(String username, String currentEmail, String currentPhone) {
        this.currentUser = username;
        LabelUser.setText(username);

        login.setPromptText(username);
        email.setPromptText(currentEmail.isEmpty() ? "Не указан" : currentEmail);
        phone.setPromptText(currentPhone.isEmpty() ? "Не указан" : currentPhone);
    }

    @FXML
    private void changeParam() {
        String newLogin = login.getText().trim();
        String newEmail = email.getText().trim();
        String newPhone = phone.getText().trim();
        String newPassword = passwordVIEW.getText();

        if (newLogin.isEmpty() && newEmail.isEmpty() && newPhone.isEmpty() && newPassword.isEmpty()) {
            showStatus("Заполните хотя бы одно поле", "red");
            return;
        }

        if (!newPhone.isEmpty() && !newPhone.matches("\\+?\\d{10,15}")) {
            showStatus("Некорректный номер телефона", "red");
            return;
        }

        if (!newEmail.isEmpty() && !isValidEmail(newEmail)) {
            showStatus("Некорректный email", "red");
            return;
        }

        if (!newPassword.isEmpty() && (newPassword.length() < 8 || newPassword.length() > 32)) {
            showStatus("Пароль: 8–32 символа", "red");
            return;
        }

        authService.updateUserProfile(currentUser, newLogin.isEmpty() ? null : newLogin,
                        newEmail.isEmpty() ? null : newEmail,
                        newPhone.isEmpty() ? null : newPhone,
                        newPassword.isEmpty() ? null : newPassword)
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        showStatus("Профиль успешно обновлён", "green");

                        // Обновляем данные в текущем окне
                        if (!newLogin.isEmpty()) {
                            currentUser = newLogin;
                            LabelUser.setText(newLogin);
                        }

                        // Перезагружаем UserPanel с новым именем
                        reloadUserPanel();
                    } else {
                        showStatus("Ошибка обновления профиля", "red");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Ошибка сервера: " + ex.getMessage(), "red"));
                    return null;
                });
    }

    private void reloadUserPanel() {
        try {
            Stage stage = (Stage) LabelUser.getScene().getWindow();
            UserPanelController.UserPanel(stage, currentUser, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBackUserPanel() {
        reloadUserPanel();
    }

    @FXML
    private void deleteUser() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление аккаунта");
        alert.setHeaderText("Вы уверены?");
        alert.setContentText("Это действие нельзя отменить!");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                authService.deleteUser(currentUser)
                        .thenAccept(deleted -> Platform.runLater(() -> {
                            if (deleted) {
                                showStatus("Аккаунт удалён", "green");
                                Platform.runLater(() -> {
                                    try {
                                        new com.example.postgresql.Controllers.HelloController()
                                                .goBack((Stage) LabelUser.getScene().getWindow());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            } else {
                                showStatus("Не удалось удалить аккаунт", "red");
                            }
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> showStatus("Ошибка: " + ex.getMessage(), "red"));
                            return null;
                        });
            }
        });
    }

    private void showStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private boolean isValidEmail(String email) {
        return Pattern.matches("^[\\w-\\.]+@[\\w-]+\\.[a-zA-Z]{2,}$", email);
    }

    // Статический метод для открытия профиля
    public static void profilePanel(Stage stage, String username, String email, String phone) {
        try {
            FXMLLoader loader = new FXMLLoader(ProfileUser.class.getResource("ProfileUser.fxml"));
            Scene scene = new Scene(loader.load());
            ProfileUser controller = loader.getController();
            controller.setUserData(username, email, phone);

            stage.setScene(scene);
            stage.setTitle("Профиль • " + username);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}