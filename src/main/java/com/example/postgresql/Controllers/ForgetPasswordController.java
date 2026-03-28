package com.example.postgresql.Controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.API.OtpStore;
import com.example.postgresql.API.ResendEmailService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.security.SecureRandom;

public class ForgetPasswordController {

    
    @FXML private TextField  loginField;
    @FXML private Button     sendEmailButton;

    
    @FXML private VBox       otpBox;
    @FXML private TextField  otpField;
    @FXML private Button     verifyOtpButton;

    
    @FXML private VBox        newPasswordBox;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button      changePasswordButton;

    @FXML private Label statusLabel;

    private final AuthService        auth   = new AuthService();
    private final ResendEmailService email  = ResendEmailService.getInstance();
    private final SecureRandom       rng    = new SecureRandom();

    private String resolvedEmail = null;
    private String accessToken   = null;

    @FXML
    private void initialize() {
        hide(otpBox);
        hide(newPasswordBox);
    }

    

    @FXML
    private void ForgetPass(ActionEvent event) {
        String identifier = loginField.getText().trim();
        if (identifier.isEmpty()) { showStatus("Введите логин или email", "#dc2626"); return; }

        sendEmailButton.setDisable(true);
        showStatus("Ищем аккаунт...", "#2563eb");

        auth.findUserForPasswordReset(identifier.toLowerCase())
            .thenAccept(info -> Platform.runLater(() -> {
                if (info == null || info.email == null || info.email.isEmpty()) {
                    sendEmailButton.setDisable(false);
                    showStatus("❌ Аккаунт не найден", "#dc2626");
                    return;
                }

                resolvedEmail = info.email;
                String code = generateCode();
                OtpStore.save(resolvedEmail, code);

                showStatus("Отправляем код...", "#2563eb");

                email.sendOtpCode(resolvedEmail, code)
                     .thenAccept(sent -> Platform.runLater(() -> {
                         sendEmailButton.setDisable(false);
                         if (sent) {
                             showStatus("✅ Код отправлен на " + maskEmail(resolvedEmail), "#16a34a");
                             loginField.setDisable(true);
                             sendEmailButton.setText("🔄 Отправить повторно");
                             show(otpBox);
                             otpField.requestFocus();
                         } else {
                             OtpStore.remove(resolvedEmail);
                             showStatus("❌ Ошибка отправки. Проверьте API ключ Resend.", "#dc2626");
                         }
                     }));
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    sendEmailButton.setDisable(false);
                    showStatus("❌ Ошибка сети: " + ex.getMessage(), "#dc2626");
                });
                return null;
            });
    }

    

    @FXML
    private void verifyOtp(ActionEvent event) {
        String code = otpField.getText().trim();
        if (code.isEmpty()) { showStatus("Введите код из письма", "#dc2626"); return; }
        if (resolvedEmail == null) { showStatus("Сначала отправьте код", "#dc2626"); return; }

        verifyOtpButton.setDisable(true);

        if (!OtpStore.verify(resolvedEmail, code)) {
            verifyOtpButton.setDisable(false);
            showStatus("❌ Неверный или истёкший код", "#dc2626");
            otpField.clear();
            return;
        }

        
        showStatus("Код верный! Получаем доступ...", "#2563eb");

        
        auth.getServiceTokenForEmail(resolvedEmail)
            .thenAccept(token -> Platform.runLater(() -> {
                verifyOtpButton.setDisable(false);
                if (token != null) {
                    accessToken = token;
                    showStatus("✅ Введите новый пароль", "#16a34a");
                    hide(otpBox);
                    sendEmailButton.setVisible(false);
                    sendEmailButton.setManaged(false);
                    show(newPasswordBox);
                    newPasswordField.requestFocus();
                } else {
                    showStatus("❌ Ошибка получения доступа. Попробуйте ещё раз.", "#dc2626");
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    verifyOtpButton.setDisable(false);
                    showStatus("❌ Ошибка: " + ex.getMessage(), "#dc2626");
                });
                return null;
            });
    }

    

    @FXML
    private void changePassword(ActionEvent event) {
        String newPass     = newPasswordField.getText().trim();
        String confirmPass = confirmPasswordField.getText().trim();

        if (newPass.isEmpty())             { showStatus("Введите новый пароль", "#dc2626"); return; }
        if (newPass.length() < 6)          { showStatus("Минимум 6 символов", "#dc2626"); return; }
        if (!newPass.equals(confirmPass))  { showStatus("Пароли не совпадают", "#dc2626"); return; }
        if (accessToken == null)           { showStatus("Сначала подтвердите код", "#dc2626"); return; }

        changePasswordButton.setDisable(true);
        showStatus("Сохраняем пароль...", "#2563eb");

        auth.updatePasswordWithToken(accessToken, newPass)
            .thenAccept(ok -> Platform.runLater(() -> {
                if (ok) {
                    showStatus("✅ Пароль успешно изменён! Войдите с новым паролем.", "#16a34a");
                    changePasswordButton.setText("✅ Готово");
                    changePasswordButton.setDisable(true);
                    newPasswordField.setDisable(true);
                    confirmPasswordField.setDisable(true);
                } else {
                    changePasswordButton.setDisable(false);
                    showStatus("❌ Ошибка смены пароля. Попробуйте позже.", "#dc2626");
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    changePasswordButton.setDisable(false);
                    showStatus("❌ Ошибка: " + ex.getMessage(), "#dc2626");
                });
                return null;
            });
    }

    

    private String generateCode() {
        return String.format("%06d", rng.nextInt(1_000_000));
    }

    private String maskEmail(String e) {
        int at = e.indexOf('@');
        if (at <= 1) return e;
        return e.charAt(0) + "***" + e.substring(at);
    }

    private void showStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;-fx-font-size:14px;");
    }

    private void show(VBox box) { if (box != null) { box.setVisible(true); box.setManaged(true); } }
    private void hide(VBox box) { if (box != null) { box.setVisible(false); box.setManaged(false); } }

    @FXML
    private void goBack(ActionEvent event) {
        new HelloController().goBack((Stage) loginField.getScene().getWindow());
    }
}
