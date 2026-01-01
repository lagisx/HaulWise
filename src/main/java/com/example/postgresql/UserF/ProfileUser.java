package com.example.postgresql.UserF;

import com.example.postgresql.API.SupabaseClient;
import com.example.postgresql.Controllers.UserPanelController;
import com.example.postgresql.HelloApplication;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.google.gson.JsonNull;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ProfileUser {

    @FXML private Label LabelUser;
    @FXML private TextField login;
    @FXML private TextField email;
    @FXML private TextField phone;

    @FXML private Label statusLabel;

    // Telegram UI
    @FXML private Label telegramStatusLabel;
    @FXML private Button telegramActionButton;
    @FXML private VBox telegramBindBox;
    @FXML private Button openTelegramBtn;
    @FXML private Label telegramChatIdLabel;
    @FXML private Button copyChatIdButton;

    private String currentUser = "";
    private String initialEmail = "";
    private String initialPhone = "";
    private boolean isTelegramLinked = false;
    private long telegramChatId = 0;

    @FXML
    private void initialize() {
        clearFieldOnFocus(email);
        clearFieldOnFocus(phone);

        login.setEditable(false);
        login.setStyle("-fx-background-color: #f4f4f4; -fx-text-fill: #333333;");

        telegramBindBox.setVisible(false);
        telegramBindBox.setManaged(false);
        telegramChatIdLabel.setVisible(false);
        if (copyChatIdButton != null) copyChatIdButton.setVisible(false);
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
    }

    private void loadProfileDataFromSupabase() {
        if (currentUser.isEmpty()) return;

        SupabaseClient supabase = new SupabaseClient();
        String encodedLogin = URLEncoder.encode(currentUser, StandardCharsets.UTF_8);

        supabase.select("users", "email,phone,telegram_linked,telegram_chat_id", "login=eq." + encodedLogin)
                .thenAccept(result -> Platform.runLater(() -> {
                    if (result.isEmpty()) {
                        showStatus("Пользователь не найден", "red");
                        return;
                    }

                    JsonObject user = result.get(0).getAsJsonObject();

                    String dbEmail = getStringOrEmpty(user, "email");
                    String dbPhone = getStringOrEmpty(user, "phone");

                    email.setPromptText(dbEmail.isEmpty() ? "Не указан" : dbEmail);
                    phone.setPromptText(dbPhone.isEmpty() ? "Не указан" : dbPhone);
                    initialEmail = dbEmail;
                    initialPhone = dbPhone;

                    isTelegramLinked = getBooleanOrFalse(user, "telegram_linked");
                    telegramChatId = user.has("telegram_chat_id") && !user.get("telegram_chat_id").isJsonNull()
                            ? user.get("telegram_chat_id").getAsLong() : 0;

                    updateTelegramUI();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Ошибка связи с сервером", "red"));
                    ex.printStackTrace();
                    return null;
                });
    }

    private String getStringOrEmpty(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString().trim() : "";
    }

    private boolean getBooleanOrFalse(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() && obj.get(key).getAsBoolean();
    }

    private void updateTelegramUI() {
        if (isTelegramLinked && telegramChatId != 0) {
            telegramStatusLabel.setText("Привязан ✓");
            telegramStatusLabel.setStyle("-fx-background-color: #86efac; -fx-text-fill: #065f46; " +
                    "-fx-padding: 6 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 15px;");
            telegramActionButton.setText("Отвязать");

            telegramChatIdLabel.setText("ID: " + telegramChatId);
            telegramChatIdLabel.setVisible(true);
            if (copyChatIdButton != null) copyChatIdButton.setVisible(true);

            telegramBindBox.setVisible(false);
            telegramBindBox.setManaged(false);
        } else {
            telegramStatusLabel.setText("Не привязан ✕");
            telegramStatusLabel.setStyle("-fx-background-color: #fecaca; -fx-text-fill: #7f1d1d; " +
                    "-fx-padding: 6 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 15px;");
            telegramActionButton.setText("Привязать");

            telegramChatIdLabel.setVisible(false);
            if (copyChatIdButton != null) copyChatIdButton.setVisible(false);

            telegramBindBox.setVisible(false);
            telegramBindBox.setManaged(false);
        }
    }

    private void clearFieldOnFocus(TextField field) {
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && field.getText().isEmpty()) {
                Platform.runLater(field::clear);
            }
        });
    }

    @FXML
    private void openTelegramBot() {
        String url = "https://t.me/Haulwisebot?start=" + currentUser;
        HelloApplication.getAppHostServices().showDocument(url);
    }

    @FXML
    private void copyChatIdToClipboard() {
        if (telegramChatId != 0) {
            StringSelection stringSelection = new StringSelection(String.valueOf(telegramChatId));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            showStatus("Chat ID скопирован!", "green");
        }
    }

    private void showStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        Platform.runLater(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> statusLabel.setText(""));
        });
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

    @FXML
    private void changeParam() {
        String newEmail = email.getText().trim();
        String newPhone = phone.getText().trim();

        JsonObject updates = new JsonObject();
        boolean hasChanges = false;

        if (!newEmail.isEmpty() && !newEmail.equals(initialEmail)) {
            updates.addProperty("email", newEmail);
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
                    if (updates.has("email")) initialEmail = newEmail;
                    if (updates.has("phone")) initialPhone = newPhone;

                    email.setPromptText(newEmail.isEmpty() ? "Не указан" : newEmail);
                    phone.setPromptText(newPhone.isEmpty() ? "Не указан" : newPhone);
                    email.clear();
                    phone.clear();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Ошибка сохранения", "red"));
                    ex.printStackTrace();
                    return null;
                });
    }

    @FXML
    private void deleteUser() {
        SupabaseClient supabase = new SupabaseClient();
        String encodedLogin = URLEncoder.encode(currentUser, StandardCharsets.UTF_8);

        supabase.delete("users", "login=eq." + encodedLogin)
                .thenAccept(res -> Platform.runLater(() -> {
                    showStatus("Аккаунт удалён", "red");
                    ((Stage) LabelUser.getScene().getWindow()).close();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Ошибка удаления", "red"));
                    ex.printStackTrace();
                    return null;
                });
    }

    @FXML
    private void toggleTelegramLink() {
        if (isTelegramLinked) {
            // Отвязываем
            SupabaseClient supabase = new SupabaseClient();
            JsonObject update = new JsonObject();
            update.addProperty("telegram_linked", false);
            update.add("telegram_chat_id", JsonNull.INSTANCE);

            String encodedLogin = URLEncoder.encode(currentUser, StandardCharsets.UTF_8);

            supabase.update("users", update, "login=eq." + encodedLogin)
                    .thenAccept(res -> Platform.runLater(() -> {
                        isTelegramLinked = false;
                        telegramChatId = 0;
                        updateTelegramUI();
                        showStatus("Telegram отвязан", "orange");
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showStatus("Ошибка отвязки", "red"));
                        ex.printStackTrace();
                        return null;
                    });
        } else {
            telegramBindBox.setVisible(true);
            telegramBindBox.setManaged(true);
            showStatus("Перейдите в бот и отправьте: /start " + currentUser, "blue");
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
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}