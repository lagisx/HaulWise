package com.example.postgresql.UserF;

import com.example.postgresql.API.OtpStore;
import com.example.postgresql.API.SupabaseClient;
import com.example.postgresql.Controllers.UserPanelController;
import com.example.postgresql.HelloApplication;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.example.postgresql.API.ResendEmailService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class ProfileUser {

    @FXML private Label    LabelUser;
    @FXML private TextField login;
    @FXML private TextField email;
    @FXML private TextField phone;
    @FXML private Label    statusLabel;

    
    @FXML private VBox  emailStatusCard;        
    @FXML private Label emailStatusIcon;        
    @FXML private Label emailStatusTitle;       
    @FXML private Label emailStatusDesc;        
    @FXML private Button resendConfirmBtn;      
    @FXML private Label  emailStatusBadge;
    @FXML private VBox   emailOtpBox;
    @FXML private TextField emailOtpField;
    @FXML private Button verifyEmailBtn;

    
    @FXML private VBox          passwordStep1Box;
    @FXML private Button        sendPasswordCodeBtn;
    @FXML private VBox          passwordStep2Box;
    @FXML private TextField     passwordOtpField;
    @FXML private Button        verifyPasswordCodeBtn;
    @FXML private VBox          passwordStep3Box;
    @FXML private PasswordField profileNewPassword;
    @FXML private PasswordField profileConfirmPassword;
    @FXML private Button        saveNewPasswordBtn;
    @FXML private Label         passwordStatusLabel;

    private String currentUser  = "";
    private String initialEmail = "";
    private String initialPhone = "";
    private boolean emailConfirmed = false;
    private String passwordAccessToken = null;

    private final com.example.postgresql.API.AuthService authService = new com.example.postgresql.API.AuthService();

    @FXML
    private void initialize() {
        clearFieldOnFocus(email);
        clearFieldOnFocus(phone);
        login.setEditable(false);
        login.setStyle("-fx-background-color: #f4f4f4; -fx-text-fill: #333333;");
    }

    public void setUserData(String username, String currentEmail, String currentPhone) {
        this.currentUser  = username != null ? username : "";
        this.initialEmail = currentEmail != null ? currentEmail : "";
        this.initialPhone = currentPhone != null ? currentPhone : "";

        LabelUser.setText(this.currentUser);
        login.setText(this.currentUser);
        email.setPromptText(this.initialEmail.isEmpty() ? "Не указан" : this.initialEmail);
        phone.setPromptText(this.initialPhone.isEmpty() ? "Не указан" : this.initialPhone);

        loadProfileDataFromSupabase();
    }

    private void loadProfileDataFromSupabase() {
        if (currentUser.isEmpty()) return;
        SupabaseClient supabase = new SupabaseClient();
        String encodedLogin = URLEncoder.encode(currentUser, StandardCharsets.UTF_8);

        supabase.select("users", "email,phone,email_confirmed", "login=eq." + encodedLogin)
                .thenAccept(result -> Platform.runLater(() -> {
                    if (result.isEmpty()) { showStatus("Пользователь не найден", "red"); return; }
                    JsonObject user = result.get(0).getAsJsonObject();

                    String dbEmail = getStr(user, "email");
                    String dbPhone = getStr(user, "phone");
                    email.setPromptText(dbEmail.isEmpty() ? "Не указан" : dbEmail);
                    phone.setPromptText(dbPhone.isEmpty() ? "Не указан" : dbPhone);
                    initialEmail = dbEmail;
                    initialPhone = dbPhone;

                    
                    if (user.has("email_confirmed") && !user.get("email_confirmed").isJsonNull()) {
                        emailConfirmed = user.get("email_confirmed").getAsBoolean();
                    } else {
                        emailConfirmed = false;
                    }
                    updateEmailStatusCard(dbEmail);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Ошибка связи с сервером", "red"));
                    return null;
                });
    }

    
    private void updateEmailStatusCard(String currentEmail) {
        if (emailStatusCard == null) return;

        if (emailConfirmed) {
            
            emailStatusCard.setStyle(
                    "-fx-background-color: #f0fdf4; -fx-background-radius: 14; " +
                            "-fx-border-color: #86efac; -fx-border-radius: 14; -fx-border-width: 1.5; -fx-padding: 18 22;"
            );
            if (emailStatusIcon  != null) emailStatusIcon.setText("✅");
            if (emailStatusTitle != null) {
                emailStatusTitle.setText("Email подтверждён");
                emailStatusTitle.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #15803d;");
            }
            if (emailStatusDesc != null) {
                emailStatusDesc.setText(currentEmail.isEmpty() ? "Адрес подтверждён" : currentEmail);
                emailStatusDesc.setStyle("-fx-font-size: 13; -fx-text-fill: #4ade80;");
            }
            if (emailStatusBadge != null) {
                emailStatusBadge.setText("● Подтверждён");
                emailStatusBadge.setStyle(
                        "-fx-background-color: #dcfce7; -fx-text-fill: #166534; " +
                                "-fx-background-radius: 20; -fx-padding: 4 14; -fx-font-size: 12; -fx-font-weight: bold;"
                );
            }
            if (resendConfirmBtn != null) {
                resendConfirmBtn.setVisible(false);
                resendConfirmBtn.setManaged(false);
            }
        } else {
            
            emailStatusCard.setStyle(
                    "-fx-background-color: #fffbeb; -fx-background-radius: 14; " +
                            "-fx-border-color: #fcd34d; -fx-border-radius: 14; -fx-border-width: 1.5; -fx-padding: 18 22;"
            );
            if (emailStatusIcon  != null) emailStatusIcon.setText("⚠️");
            if (emailStatusTitle != null) {
                emailStatusTitle.setText("Email не подтверждён");
                emailStatusTitle.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #b45309;");
            }
            if (emailStatusDesc != null) {
                emailStatusDesc.setText("Подтвердите почту, чтобы восстанавливать пароль и получать уведомления.");
                emailStatusDesc.setStyle("-fx-font-size: 13; -fx-text-fill: #92400e; -fx-wrap-text: true;");
            }
            if (emailStatusBadge != null) {
                emailStatusBadge.setText("● Не подтверждён");
                emailStatusBadge.setStyle(
                        "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; " +
                                "-fx-background-radius: 20; -fx-padding: 4 14; -fx-font-size: 12; -fx-font-weight: bold;"
                );
            }
            if (resendConfirmBtn != null) {
                resendConfirmBtn.setVisible(true);
                resendConfirmBtn.setManaged(true);
            }
        }
    }

    
    @FXML
    private void onResendConfirmation() {
        if (initialEmail.isEmpty()) {
            showStatus("Email не указан — сначала сохраните email", "orange");
            return;
        }

        resendConfirmBtn.setDisable(true);
        resendConfirmBtn.setText("⏳ Отправка...");
        showStatus("", "");

        String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        OtpStore.save(initialEmail, otpCode);

        ResendEmailService.getInstance().sendOtpCode(initialEmail, otpCode)
                .thenAccept(ok -> Platform.runLater(() -> {
                    resendConfirmBtn.setDisable(false);
                    if (ok) {
                        resendConfirmBtn.setText("📧 Отправить повторно");
                        showStatus("Код отправлен на " + maskEmail(initialEmail), "#16a34a");
                        if (emailOtpBox != null) {
                            emailOtpBox.setVisible(true);
                            emailOtpBox.setManaged(true);
                        }
                    } else {
                        OtpStore.remove(initialEmail);
                        resendConfirmBtn.setText("📧 Отправить код");
                        showStatus("Ошибка отправки. Попробуйте позже.", "red");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        OtpStore.remove(initialEmail);
                        resendConfirmBtn.setDisable(false);
                        resendConfirmBtn.setText("📧 Отправить код");
                        showStatus("Ошибка: " + ex.getMessage(), "red");
                    });
                    return null;
                });
    }

    @FXML
    private void onVerifyEmailCode() {
        String code = emailOtpField.getText().trim();
        if (code.isEmpty()) {
            showStatus("Введите код из письма", "red");
            return;
        }

        verifyEmailBtn.setDisable(true);
        showStatus("Проверяем код...", "#2563eb");

        boolean verified = OtpStore.verify(initialEmail, code);
        verifyEmailBtn.setDisable(false);

        if (verified) {
            SupabaseClient supabase = new SupabaseClient();
            String encodedLogin = URLEncoder.encode(currentUser, StandardCharsets.UTF_8);
            com.google.gson.JsonObject update = new com.google.gson.JsonObject();
            update.addProperty("email_confirmed", true);
            supabase.update("users", update, "login=eq." + encodedLogin)
                    .thenAccept(r -> Platform.runLater(() -> {
                        emailConfirmed = true;
                        updateEmailStatusCard(initialEmail);
                        showStatus("Email успешно подтверждён!", "#16a34a");
                        if (emailOtpBox != null) {
                            emailOtpBox.setVisible(false);
                            emailOtpBox.setManaged(false);
                        }
                    }));
        } else {
            showStatus("Неверный или истёкший код.", "red");
        }
    }

    
    
    

    
    @FXML
    private void onSendPasswordCode() {
        if (initialEmail.isEmpty()) {
            showPasswordStatus("Email не указан — сначала сохраните email", "#dc2626");
            return;
        }

        sendPasswordCodeBtn.setDisable(true);
        sendPasswordCodeBtn.setText("⏳ Отправка...");
        showPasswordStatus("", "");

        
        String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        OtpStore.save(initialEmail, otpCode);

        ResendEmailService.getInstance().sendOtpCode(initialEmail, otpCode)
                .thenAccept(ok -> Platform.runLater(() -> {
                    sendPasswordCodeBtn.setDisable(false);
                    if (ok) {
                        sendPasswordCodeBtn.setText("📧 Отправить повторно");
                        showPasswordStatus("Код отправлен на " + maskEmail(initialEmail), "#16a34a");
                        
                        if (passwordStep2Box != null) {
                            passwordStep2Box.setVisible(true);
                            passwordStep2Box.setManaged(true);
                        }
                    } else {
                        OtpStore.remove(initialEmail);
                        sendPasswordCodeBtn.setText("📧 Отправить код");
                        showPasswordStatus("Ошибка отправки. Проверьте API ключ Resend.", "#dc2626");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        OtpStore.remove(initialEmail);
                        sendPasswordCodeBtn.setDisable(false);
                        sendPasswordCodeBtn.setText("Отправить код");
                        showPasswordStatus("Ошибка: " + ex.getMessage(), "#dc2626");
                    });
                    return null;
                });
    }

    
    @FXML
    private void onVerifyPasswordCode() {
        String code = passwordOtpField.getText().trim();
        if (code.isEmpty()) {
            showPasswordStatus("Введите код из письма", "#dc2626");
            return;
        }

        verifyPasswordCodeBtn.setDisable(true);
        showPasswordStatus("Проверяем код...", "#2563eb");

        
        boolean verified = OtpStore.verify(initialEmail, code);
        verifyPasswordCodeBtn.setDisable(false);
        if (verified) {
            
            authService.getServiceTokenForEmail(initialEmail)
                    .thenAccept(token -> Platform.runLater(() -> {
                        if (token != null) {
                            passwordAccessToken = token;
                            showPasswordStatus("Код подтверждён! Введите новый пароль.", "#16a34a");
                            if (passwordStep1Box != null) { passwordStep1Box.setVisible(false); passwordStep1Box.setManaged(false); }
                            if (passwordStep2Box != null) { passwordStep2Box.setVisible(false); passwordStep2Box.setManaged(false); }
                            if (passwordStep3Box != null) { passwordStep3Box.setVisible(true); passwordStep3Box.setManaged(true); }
                        } else {
                            showPasswordStatus("Ошибка получения доступа. Попробуйте снова.", "#dc2626");
                        }
                    }));
            return;
        } else {
            showPasswordStatus("Неверный или истёкший код.", "#dc2626");
        }
    }

    
    @FXML
    private void onSaveNewPassword() {
        String newPass = profileNewPassword.getText().trim();
        String confirmPass = profileConfirmPassword.getText().trim();

        if (newPass.isEmpty()) {
            showPasswordStatus("Введите новый пароль", "#dc2626");
            return;
        }
        if (newPass.length() < 6) {
            showPasswordStatus("Пароль должен быть не менее 6 символов", "#dc2626");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showPasswordStatus("Пароли не совпадают", "#dc2626");
            return;
        }
        if (passwordAccessToken == null) {
            showPasswordStatus("Сначала подтвердите код", "#dc2626");
            return;
        }

        saveNewPasswordBtn.setDisable(true);
        showPasswordStatus("Сохраняем...", "#2563eb");

        authService.updatePasswordWithToken(passwordAccessToken, newPass)
                .thenAccept(ok -> Platform.runLater(() -> {
                    saveNewPasswordBtn.setDisable(false);
                    if (ok) {
                        showPasswordStatus("Пароль успешно изменён!", "#16a34a");
                        saveNewPasswordBtn.setText("Готово");
                        saveNewPasswordBtn.setDisable(true);
                        profileNewPassword.setDisable(true);
                        profileConfirmPassword.setDisable(true);
                        passwordAccessToken = null;
                    } else {
                        showPasswordStatus("Ошибка смены пароля", "#dc2626");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        saveNewPasswordBtn.setDisable(false);
                        showPasswordStatus("Ошибка: " + ex.getMessage(), "#dc2626");
                    });
                    return null;
                });
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    private void showPasswordStatus(String text, String color) {
        if (passwordStatusLabel == null) return;
        passwordStatusLabel.setText(text);
        if (text.isEmpty()) {
            passwordStatusLabel.setStyle("");
        } else {
            passwordStatusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12;");
        }
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString().trim() : "";
    }

    private void clearFieldOnFocus(TextField field) {
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && field.getText().isEmpty()) Platform.runLater(field::clear);
        });
    }

    @FXML
    private void changeParam() {
        String newEmail = email.getText().trim();
        String newPhone = phone.getText().trim();

        JsonObject updates = new JsonObject();
        boolean hasChanges = false;

        if (!newEmail.isEmpty() && !newEmail.equals(initialEmail)) {
            updates.addProperty("email", newEmail);
            
            updates.addProperty("email_confirmed", false);
            hasChanges = true;
        }
        if (!newPhone.isEmpty() && !newPhone.equals(initialPhone)) {
            updates.addProperty("phone", newPhone);
            hasChanges = true;
        }

        if (!hasChanges) { showStatus("Нет изменений", "orange"); return; }

        SupabaseClient supabase = new SupabaseClient();
        String encodedLogin = URLEncoder.encode(currentUser, StandardCharsets.UTF_8);

        supabase.update("users", updates, "login=eq." + encodedLogin)
                .thenAccept(res -> Platform.runLater(() -> {
                    showStatus("Данные сохранены", "green");
                    if (updates.has("email")) {
                        initialEmail = newEmail;
                        email.setPromptText(newEmail);
                        email.clear();
                        
                        emailConfirmed = false;
                        updateEmailStatusCard(newEmail);
                    }
                    if (updates.has("phone")) {
                        initialPhone = newPhone;
                        phone.setPromptText(newPhone);
                        phone.clear();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Ошибка сохранения", "red"));
                    return null;
                });
    }

    @FXML
    private void deleteUser() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить аккаунт? Это действие нельзя отменить.", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Подтверждение");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                SupabaseClient supabase = new SupabaseClient();
                String encodedLogin = URLEncoder.encode(currentUser, StandardCharsets.UTF_8);
                supabase.delete("users", "login=eq." + encodedLogin)
                        .thenAccept(res -> Platform.runLater(() -> {
                            showStatus("Аккаунт удалён", "red");
                            ((Stage) LabelUser.getScene().getWindow()).close();
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> showStatus("Ошибка удаления", "red"));
                            return null;
                        });
            }
        });
    }

    private void showStatus(String text, String color) {
        statusLabel.setText(text);
        if (text.isEmpty()) {
            statusLabel.setStyle("");
        } else {
            statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void goBackUserPanel() {
        try {
            UserPanelController.UserPanel((Stage) LabelUser.getScene().getWindow(), currentUser, null);
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Ошибка перехода", "red");
        }
    }

    public static void profilePanel(Stage stage, String username, String email, String phone) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("ProfileUser.fxml"));
            Scene scene = new Scene(loader.load());
            ProfileUser controller = loader.getController();
            controller.setUserData(username, email, phone);

            stage.setScene(scene);
            stage.setTitle("Профиль • " + username);
            stage.setMaximized(true);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}