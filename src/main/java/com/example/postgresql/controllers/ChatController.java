package com.example.postgresql.controllers;

import com.example.postgresql.api.AuthService;
import com.example.postgresql.api.RealtimeClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

public class ChatController {

    @FXML private VBox      messagesContainer;
    @FXML private TextField messageInput;
    @FXML private Button    sendButton;
    @FXML private Label     chatTitleLabel;
    @FXML private Label     partnerPhoneLabel;
    @FXML private ScrollPane scrollPane;

    private final AuthService    authService    = new AuthService();
    private final RealtimeClient realtimeClient = new RealtimeClient();

    private String myLogin;
    private String partnerLogin;

    public void init(String myLogin, String partnerLogin) {
        this.myLogin      = myLogin;
        this.partnerLogin = partnerLogin;
        chatTitleLabel.setText("Чат с " + partnerLogin);

        loadHistory();
        connectRealtime();
        loadPartnerPhone();

        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());
    }

    
    private void loadPartnerPhone() {
        authService.getUserProfile(partnerLogin).thenAccept(arr -> {
            Platform.runLater(() -> {
                if (arr != null && arr.size() > 0) {
                    JsonObject u = arr.get(0).getAsJsonObject();
                    String phone = has(u, "phone");
                    if (!phone.isEmpty()) {
                        partnerPhoneLabel.setText("📞 " + phone);
                    } else {
                        partnerPhoneLabel.setText("Телефон не указан");
                        partnerPhoneLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
                    }
                }
            });
        });
    }

    
    public void disconnect() {
        realtimeClient.disconnect();
    }

    private void loadHistory() {
        authService.getChatHistory(myLogin, partnerLogin)
            .thenAccept(array -> Platform.runLater(() -> {
                messagesContainer.getChildren().clear();
                for (JsonElement el : array) addBubble(el.getAsJsonObject());
                scrollToBottom();
            }));
    }

    private void connectRealtime() {
        realtimeClient.connect(record -> {
            String sender   = has(record, "sender_login");
            String receiver = has(record, "receiver_login");
            boolean isOurs  = (sender.equals(myLogin)      && receiver.equals(partnerLogin))
                           || (sender.equals(partnerLogin) && receiver.equals(myLogin));
            if (isOurs) Platform.runLater(() -> { addBubble(record); scrollToBottom(); });
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        messageInput.clear();
        messageInput.setDisable(true);

        authService.sendMessage(myLogin, partnerLogin, text)
            .thenAccept(ok -> Platform.runLater(() -> {
                messageInput.setDisable(false);
                messageInput.requestFocus();
                if (!ok) showError("Не удалось отправить сообщение");
                
            }));
    }

    private void addBubble(JsonObject msg) {
        String sender  = has(msg, "sender_login");
        String content = has(msg, "content");
        String time    = has(msg, "created_at");
        if (time.length() >= 16) time = time.substring(11, 16);

        boolean isMine = sender.equals(myLogin);

        Text text = new Text(content);
        text.setWrappingWidth(320);
        text.setStyle("-fx-font-size: 13px; -fx-fill: " + (isMine ? "white" : "#1e293b") + ";");

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isMine ? "#93c5fd" : "#94a3b8") + ";");

        VBox bubble = new VBox(3, text, timeLabel);
        bubble.setPadding(new Insets(8, 12, 6, 12));
        bubble.setMaxWidth(340);
        bubble.setStyle(isMine
            ? "-fx-background-color: #1e40af; -fx-background-radius: 16 4 16 16;"
            : "-fx-background-color: white; -fx-background-radius: 4 16 16 16;" +
              "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),4,0,0,1);");

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(3, 14, 3, 14));
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        messagesContainer.getChildren().add(row);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private String has(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(msg);
        a.show();
    }
}
