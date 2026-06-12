package com.example.postgresql.UserF;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.API.OtpStore;
import com.example.postgresql.API.SupabaseClient;
import com.example.postgresql.controllers.UserPanelController;
import com.example.postgresql.controllers.CompanyPanelController;
import com.example.postgresql.controllers.CreateCompanyController;
import com.example.postgresql.controllers.InviteCardController;
import com.example.postgresql.API.CompanyService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.example.postgresql.HelloApplication;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.example.postgresql.API.ResendEmailService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class ProfileUser {

    @FXML
    private Label LabelUser;
    @FXML
    private TextField login;
    @FXML
    private TextField email;
    @FXML
    private TextField phone;
    @FXML
    private Label statusLabel;


    @FXML
    private VBox emailStatusCard;
    @FXML
    private Label emailStatusIcon;
    @FXML
    private Label emailStatusTitle;
    @FXML
    private Label emailStatusDesc;
    @FXML
    private Button resendConfirmBtn;
    @FXML
    private Label emailStatusBadge;
    @FXML
    private VBox emailOtpBox;
    @FXML
    private TextField emailOtpField;
    @FXML
    private Button verifyEmailBtn;


    @FXML
    private VBox passwordStep1Box;
    @FXML
    private Button sendPasswordCodeBtn;
    @FXML
    private VBox passwordStep2Box;
    @FXML
    private TextField passwordOtpField;
    @FXML
    private Button verifyPasswordCodeBtn;
    @FXML
    private VBox passwordStep3Box;
    @FXML
    private PasswordField profileNewPassword;
    @FXML
    private PasswordField profileConfirmPassword;
    @FXML
    private Button saveNewPasswordBtn;
    @FXML
    private Label passwordStatusLabel;

    private String currentUser = "";
    private String initialEmail = "";
    private String initialPhone = "";
    private boolean emailConfirmed = false;

    @FXML
    private VBox profileCompanyBox;
    @FXML
    private VBox profileNoCompanyBox;
    @FXML
    private VBox profileCompanyInfoBox;
    @FXML
    private Label profileCompanyName;
    @FXML
    private Label profileCompanyRole;
    @FXML
    private VBox profileWebhookBox;
    @FXML
    private TextField profileWebhookField;
    @FXML
    private TextField profileInviteField;
    @FXML
    private Label profileInviteStatus;
    @FXML
    private VBox profileMembersBox;
    @FXML
    private HBox profileDeleteCompanyBox;
    @FXML
    private HBox profileLeaveCompanyBox;
    @FXML
    private Label profileCompanyStatus;

    private int profileCompanyId = -1;
    private String profileMyRole = "";

    @FXML
    private VBox profileInvitesSection;
    @FXML
    private VBox profileInvitesContainer;
    @FXML
    private VBox profileInviteBox;

    private final AuthService authService = new AuthService();
    private final CompanyService companyService = new CompanyService();

    @FXML
    private void initialize() {
        clearFieldOnFocus(email);
        clearFieldOnFocus(phone);
        login.setEditable(false);
        login.setStyle("-fx-background-color: #f4f4f4; -fx-text-fill: #333333;");
    }

    public void setUserData(String username, String currentEmail, String currentPhone) {
        this.currentUser = username != null ? username : "";
        this.initialEmail = currentEmail != null ? currentEmail : "";
        this.initialPhone = currentPhone != null ? currentPhone : "";

        LabelUser.setText(this.currentUser);
        login.setText(this.currentUser);
        email.setPromptText(this.initialEmail.isEmpty() ? "Не указан" : this.initialEmail);
        phone.setPromptText(this.initialPhone.isEmpty() ? "Не указан" : this.initialPhone);

        loadProfileDataFromSupabase();
        loadProfileCompany();
        loadPendingInvites();
    }

    private void loadProfileDataFromSupabase() {
        if (currentUser.isEmpty()) return;
        SupabaseClient supabase = new SupabaseClient();
        String encodedLogin = URLEncoder.encode(currentUser, StandardCharsets.UTF_8);

        supabase.select("users", "email,phone,email_confirmed", "login=eq." + encodedLogin)
                .thenAccept(result -> Platform.runLater(() -> {
                    if (result.isEmpty()) {
                        showStatus("Пользователь не найден", "red");
                        return;
                    }
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
                    Platform.runLater(() -> showStatus("Нет подключения к серверу. Проверьте интернет.", "red"));
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
            if (emailStatusIcon != null) emailStatusIcon.setText("✅");
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
            if (emailStatusIcon != null) emailStatusIcon.setText("⚠️");
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
                        showStatus("Не удалось отправить письмо. Попробуйте позже.", "red");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        OtpStore.remove(initialEmail);
                        resendConfirmBtn.setDisable(false);
                        resendConfirmBtn.setText("📧 Отправить код");
                        showStatus("Нет подключения к серверу. Попробуйте позже.", "red");
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

        if (!emailConfirmed) {
            showPasswordStatus("❌ Сначала подтвердите email — без этого смена пароля недоступна", "#dc2626");
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
                        showPasswordStatus("Не удалось отправить письмо. Попробуйте позже.", "#dc2626");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        OtpStore.remove(initialEmail);
                        sendPasswordCodeBtn.setDisable(false);
                        sendPasswordCodeBtn.setText("Отправить код");
                        showPasswordStatus("Нет подключения к серверу. Попробуйте позже.", "#dc2626");
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
            showPasswordStatus("Код подтверждён! Введите новый пароль.", "#16a34a");
            if (passwordStep1Box != null) {
                passwordStep1Box.setVisible(false);
                passwordStep1Box.setManaged(false);
            }
            if (passwordStep2Box != null) {
                passwordStep2Box.setVisible(false);
                passwordStep2Box.setManaged(false);
            }
            if (passwordStep3Box != null) {
                passwordStep3Box.setVisible(true);
                passwordStep3Box.setManaged(true);
            }
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
        saveNewPasswordBtn.setDisable(true);
        showPasswordStatus("Сохраняем...", "#2563eb");

        authService.adminChangePassword(initialEmail, newPass)
                .thenAccept(ok -> Platform.runLater(() -> {
                    saveNewPasswordBtn.setDisable(false);
                    if (ok) {
                        showPasswordStatus("Пароль успешно изменён!", "#16a34a");
                        saveNewPasswordBtn.setText("Готово");
                        saveNewPasswordBtn.setDisable(true);
                        profileNewPassword.setDisable(true);
                        profileConfirmPassword.setDisable(true);
                    } else {
                        showPasswordStatus("Не удалось сменить пароль. Попробуйте позже.", "#dc2626");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        saveNewPasswordBtn.setDisable(false);
                        showPasswordStatus("Нет подключения к серверу. Попробуйте позже.", "#dc2626");
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

        if (!hasChanges) {
            showStatus("Нет изменений", "orange");
            return;
        }

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
                    Platform.runLater(() -> showStatus("Не удалось сохранить данные. Попробуйте позже.", "red"));
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

                supabase.selectWithRaw("users", "auth_uuid", "login=eq." + encodedLogin)
                        .thenCompose(rows -> {
                            String authUuid = null;
                            if (!rows.isEmpty()) {
                                com.google.gson.JsonObject u = rows.get(0).getAsJsonObject();
                                if (u.has("auth_uuid") && !u.get("auth_uuid").isJsonNull()) {
                                    authUuid = u.get("auth_uuid").getAsString();
                                }
                            }

                            final String finalAuthUuid = authUuid;

                            return supabase.delete("users", "login=eq." + encodedLogin)
                                    .thenCompose(res -> {
                                        if (finalAuthUuid != null) {
                                            return supabase.adminDeleteAuthUser(finalAuthUuid);
                                        }
                                        return java.util.concurrent.CompletableFuture.completedFuture(true);
                                    });
                        })
                        .thenAccept(res -> Platform.runLater(() -> {
                            showStatus("Аккаунт удалён", "red");
                            ((Stage) LabelUser.getScene().getWindow()).close();
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> showStatus("Не удалось удалить аккаунт. Попробуйте позже.", "red"));
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

    private void loadProfileCompany() {
        if (currentUser.isEmpty()) return;
        companyService.getUserCompany(currentUser)
                .thenAccept(company -> javafx.application.Platform.runLater(() -> {
                    if (company == null) {
                        showProfileNoCompany();
                    } else {
                        showProfileCompany(company);
                    }
                }))
                .exceptionally(ex -> null);
    }

    private void showProfileNoCompany() {
        if (profileNoCompanyBox == null) return;
        profileNoCompanyBox.setVisible(true);
        profileNoCompanyBox.setManaged(true);
        if (profileCompanyInfoBox != null) {
            profileCompanyInfoBox.setVisible(false);
            profileCompanyInfoBox.setManaged(false);
        }
    }

    private void showProfileCompany(com.google.gson.JsonObject company) {
        if (profileCompanyInfoBox == null) return;
        profileCompanyId = company.get("id").getAsInt();
        profileMyRole = getStr(company, "my_role");

        profileNoCompanyBox.setVisible(false);
        profileNoCompanyBox.setManaged(false);
        profileCompanyInfoBox.setVisible(true);
        profileCompanyInfoBox.setManaged(true);

        profileCompanyName.setText(getStr(company, "name"));
        profileCompanyRole.setText("owner".equals(profileMyRole) ? "👑 Владелец" : "👤 Сотрудник");

        boolean isOwner = "owner".equals(profileMyRole);
        profileWebhookBox.setVisible(isOwner);
        profileWebhookBox.setManaged(isOwner);
        profileDeleteCompanyBox.setVisible(isOwner);
        profileDeleteCompanyBox.setManaged(isOwner);
        if (profileLeaveCompanyBox != null) {
            profileLeaveCompanyBox.setVisible(!isOwner);
            profileLeaveCompanyBox.setManaged(!isOwner);
        }
        if (profileInviteBox != null) {
            profileInviteBox.setVisible(isOwner);
            profileInviteBox.setManaged(isOwner);
        }

        if (isOwner) {
            profileWebhookField.setText(getStr(company, "bitrix24_webhook"));
        }

        loadProfileMembers();
    }

    private void loadProfileMembers() {
        if (profileMembersBox == null || profileCompanyId < 0) return;
        profileMembersBox.getChildren().clear();
        companyService.getCompanyMembers(profileCompanyId)
                .thenAccept(members -> javafx.application.Platform.runLater(() -> {
                    if (members == null || members.isEmpty()) {
                        Label empty = new Label("Сотрудников пока нет");
                        empty.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");
                        profileMembersBox.getChildren().add(empty);
                        return;
                    }
                    for (JsonElement el : members) {
                        com.google.gson.JsonObject m = el.getAsJsonObject();
                        String login = getStr(m, "login");
                        String role = getStr(m, "company_role");
                        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8);
                        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        row.setStyle("-fx-padding: 6 10; -fx-background-color: #f8fafc; -fx-background-radius: 6;");
                        Label icon = new Label("owner".equals(role) ? "👑" : "👤");
                        Label name = new Label(login);
                        name.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");
                        javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
                        javafx.scene.layout.HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
                        row.getChildren().addAll(icon, name, sp);
                        if ("owner".equals(profileMyRole) && !"owner".equals(role)) {
                            javafx.scene.control.Button kick = new javafx.scene.control.Button("✕");
                            kick.setStyle("-fx-background-color: #fff1f2; -fx-text-fill: #ef4444; -fx-font-size: 10; -fx-padding: 2 6; -fx-background-radius: 4; -fx-cursor: hand;");
                            kick.setOnAction(e -> {
                                companyService.leaveCompany(login)
                                        .thenAccept(ok -> javafx.application.Platform.runLater(() -> {
                                            if (ok) loadProfileMembers();
                                        }));
                            });
                            row.getChildren().add(kick);
                        }
                        profileMembersBox.getChildren().add(row);
                    }
                }))
                .exceptionally(ex -> null);
    }

    @FXML
    private void onProfileCreateCompany() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("CreateCompany.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            CreateCompanyController ctrl = loader.getController();
            ctrl.setData(currentUser, () -> javafx.application.Platform.runLater(this::loadProfileCompany));
            Stage stage = new Stage();
            stage.setTitle("Создать компанию");
            stage.setScene(scene);
            stage.setMinWidth(500);
            stage.setMinHeight(400);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            showProfileCompanyStatus("Ошибка: " + e.getMessage(), "red");
        }
    }

    @FXML
    private void onProfileSaveWebhook() {
        if (profileCompanyId < 0) return;
        String webhook = profileWebhookField.getText().trim();
        if (webhook.isEmpty()) {
            showProfileCompanyStatus("Введите URL вебхука", "orange");
            return;
        }
        companyService.updateWebhook(profileCompanyId, webhook)
                .thenAccept(ok -> javafx.application.Platform.runLater(() ->
                        showProfileCompanyStatus(ok ? "✅ Вебхук сохранён" : "Ошибка сохранения", ok ? "#16a34a" : "red")
                )).exceptionally(ex -> null);
    }

    @FXML
    private void onProfileInvite() {
        if (profileCompanyId < 0 || !"owner".equals(profileMyRole)) return;
        String login = profileInviteField.getText().trim();
        if (login.isEmpty() || login.equals(currentUser)) {
            setProfileInviteStatus("Введите логин другого пользователя", "orange");
            return;
        }
        companyService.sendInvite(profileCompanyId, currentUser, login)
                .thenAccept(ok -> javafx.application.Platform.runLater(() -> {
                    if (ok) {
                        setProfileInviteStatus("✅ Приглашение отправлено " + login, "#16a34a");
                        profileInviteField.clear();
                    } else {
                        setProfileInviteStatus("Не найден, уже в компании или уже приглашён", "red");
                    }
                })).exceptionally(ex -> null);
    }

    @FXML
    private void onProfileDeleteCompany() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить компанию? Все сотрудники будут отвязаны.", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                companyService.deleteCompany(profileCompanyId)
                        .thenAccept(ok -> javafx.application.Platform.runLater(() -> {
                            if (ok) {
                                profileCompanyId = -1;
                                showProfileNoCompany();
                            }
                        })).exceptionally(ex -> null);
            }
        });
    }

    private void loadPendingInvites() {
        if (currentUser.isEmpty()) return;
        companyService.getPendingInvites(currentUser)
                .thenAccept(invites -> Platform.runLater(() -> {
                    if (profileInvitesSection == null || profileInvitesContainer == null) return;
                    if (invites == null || invites.isEmpty()) {
                        profileInvitesSection.setVisible(false);
                        profileInvitesSection.setManaged(false);
                        return;
                    }
                    profileInvitesSection.setVisible(true);
                    profileInvitesSection.setManaged(true);
                    profileInvitesContainer.getChildren().clear();

                    for (com.google.gson.JsonElement el : invites) {
                        com.google.gson.JsonObject inv = el.getAsJsonObject();
                        int inviteId = inv.get("id").getAsInt();
                        int companyId = inv.get("company_id").getAsInt();
                        String from = inv.has("from_login") && !inv.get("from_login").isJsonNull()
                                ? inv.get("from_login").getAsString() : "?";

                        companyService.getCompanyName(companyId)
                                .thenAccept(name -> Platform.runLater(() -> {
                                    try {
                                        FXMLLoader loader = new FXMLLoader(
                                                HelloApplication.class.getResource("InviteCard.fxml"));
                                        javafx.scene.layout.VBox card = loader.load();
                                        InviteCardController ctrl = loader.getController();
                                        ctrl.setData(
                                                inviteId, companyId, name, from, currentUser,
                                                () -> {
                                                    profileInvitesContainer.getChildren().remove(card);
                                                    if (profileInvitesContainer.getChildren().isEmpty()) {
                                                        profileInvitesSection.setVisible(false);
                                                        profileInvitesSection.setManaged(false);
                                                    }
                                                    loadProfileCompany();
                                                },
                                                () -> {
                                                    profileInvitesContainer.getChildren().remove(card);
                                                    if (profileInvitesContainer.getChildren().isEmpty()) {
                                                        profileInvitesSection.setVisible(false);
                                                        profileInvitesSection.setManaged(false);
                                                    }
                                                }
                                        );
                                        profileInvitesContainer.getChildren().add(card);
                                    } catch (Exception e) {
                                        System.err.println("[InviteCard] Ошибка загрузки FXML: " + e.getMessage());
                                    }
                                }));
                    }
                })).exceptionally(ex -> null);
    }

    @FXML
    private void onProfileLeaveCompany() {
        Alert confirm = new Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Покинуть компанию? Вы потеряете доступ к её инструментам.",
                javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        confirm.setTitle("Подтверждение");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.YES) {
                companyService.leaveCompany(currentUser)
                        .thenAccept(ok -> javafx.application.Platform.runLater(() -> {
                            if (ok) {
                                profileCompanyId = -1;
                                profileMyRole = "";
                                showProfileNoCompany();
                                showProfileCompanyStatus("Вы покинули компанию", "#64748b");
                            } else {
                                showProfileCompanyStatus("Не удалось покинуть компанию", "red");
                            }
                        })).exceptionally(ex -> null);
            }
        });
    }

    private void showProfileCompanyStatus(String text, String color) {
        if (profileCompanyStatus == null) return;
        profileCompanyStatus.setText(text);
        profileCompanyStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private void setProfileInviteStatus(String text, String color) {
        if (profileInviteStatus == null) return;
        profileInviteStatus.setText(text);
        profileInviteStatus.setStyle("-fx-text-fill: " + color + ";");
    }

    @FXML
    private void goBackUserPanel() {
        try {
            UserPanelController.userPanel((Stage) LabelUser.getScene().getWindow(), currentUser, null);
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
            stage.setTitle("Профиль " + username);
            stage.setMaximized(true);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}