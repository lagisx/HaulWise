package com.example.postgresql.controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.HelloApplication;
import com.example.postgresql.UserF.Cargo;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashSet;


public class ChatPanelManager {

    private final AuthService authService;
    private final VBox chatListContainer;
    private final VBox chatContentPane;
    private final VBox chatPlaceholder;
    private final TabPane tabPane;
    private final int chatTabIndex;

    private String currentUser;
    private String activeChatPartner = null;
    private ChatController activeChatController = null;
    private final LinkedHashSet<String> openedChats = new LinkedHashSet<>();
    private final java.util.Map<String, Cargo> cargoByPartner = new java.util.HashMap<>();
    private final java.util.Map<String, String> ownerByPartner = new java.util.HashMap<>();
    private final java.util.Map<String, List<Cargo>> cargoListByPartner = new java.util.HashMap<>();

    public ChatPanelManager(AuthService authService,
                            VBox chatListContainer,
                            VBox chatContentPane,
                            VBox chatPlaceholder,
                            TabPane tabPane,
                            int chatTabIndex) {
        this.authService = authService;
        this.chatListContainer = chatListContainer;
        this.chatContentPane = chatContentPane;
        this.chatPlaceholder = chatPlaceholder;
        this.tabPane = tabPane;
        this.chatTabIndex = chatTabIndex;
    }

    public void setCurrentUser(String user) {
        this.currentUser = user;
    }


    public void loadChatList() {
        if (chatListContainer == null) return;
        renderChatList();
        authService.getMyConversations(currentUser)
                .thenAccept(array -> Platform.runLater(() -> {
                    if (array != null) {
                        for (JsonElement el : array) {
                            JsonObject msg = el.getAsJsonObject();
                            String sender = getStrOrEmpty(msg, "sender_login");
                            String receiver = getStrOrEmpty(msg, "receiver_login");
                            if (!sender.isEmpty() && !sender.equals(currentUser)) openedChats.add(sender);
                            if (!receiver.isEmpty() && !receiver.equals(currentUser)) openedChats.add(receiver);
                        }
                    }
                    renderChatList();
                }));
    }


    private void renderChatList() {
        if (chatListContainer == null) return;
        chatListContainer.getChildren().clear();
        if (openedChats.isEmpty()) {
            Label empty = new Label("Нет чатов.\nНажмите «Написать» на карточке груза.");
            empty.setStyle("-fx-padding: 20 16; -fx-font-size: 13; -fx-text-fill: #94a3b8; -fx-wrap-text: true;");
            empty.setMaxWidth(Double.MAX_VALUE);
            chatListContainer.getChildren().add(empty);
            return;
        }
        for (String partner : openedChats) {
            chatListContainer.getChildren().add(buildChatRow(partner));
        }
    }

    private HBox buildChatRow(String partner) {
        boolean isActive = partner.equals(activeChatPartner);

        Label avatar = new Label(partner.substring(0, 1).toUpperCase());
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: " + (isActive ? "#1e40af" : "#dbeafe") + ";" +
                "-fx-text-fill: " + (isActive ? "white" : "#1e40af") + ";" +
                "-fx-font-weight: bold; -fx-font-size: 17; -fx-background-radius: 20;");

        Label name = new Label(partner);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: " +
                (isActive ? "#1e40af" : "#0f172a") + ";");
        HBox.setHgrow(name, Priority.ALWAYS);

        Button deleteButton = new Button("✕");
        deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8;" +
                "-fx-font-size: 13; -fx-padding: 2 6; -fx-cursor: hand;");
        deleteButton.setOnAction(e -> {
            openedChats.remove(partner);
            if (partner.equals(activeChatPartner)) clearChatView();
            renderChatList();
        });
        deleteButton.setVisible(false);

        HBox row = new HBox(10, avatar, name, deleteButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: " + (isActive ? "#eff6ff" : "transparent") + ";" +
                "-fx-cursor: hand; -fx-border-color: transparent transparent #f1f5f9 transparent;");

        row.setOnMouseEntered(e -> {
            deleteButton.setVisible(true);
            if (!partner.equals(activeChatPartner))
                row.setStyle("-fx-background-color: #f8fafc; -fx-cursor: hand;" +
                        "-fx-border-color: transparent transparent #f1f5f9 transparent;");
        });
        row.setOnMouseExited(e -> {
            deleteButton.setVisible(false);
            if (!partner.equals(activeChatPartner))
                row.setStyle("-fx-background-color: transparent; -fx-cursor: hand;" +
                        "-fx-border-color: transparent transparent #f1f5f9 transparent;");
        });
        row.setOnMouseClicked(e -> openChatInline(partner));
        return row;
    }


    public void openChatInline(String partnerLogin) {
        Cargo savedCargo = cargoByPartner.get(partnerLogin);
        String savedOwner = ownerByPartner.get(partnerLogin);
        List<Cargo> savedList = cargoListByPartner.get(partnerLogin);
        if (savedCargo != null) {
            openChatInlineWithCargoList(partnerLogin, savedCargo, savedOwner, savedList);
        } else {
            openChatInlineWithCargo(partnerLogin, null, null);
        }
    }

    public void openChatInlineWithCargo(String partnerLogin, Cargo cargo, String ownerLogin) {
        openChatInlineWithCargoList(partnerLogin, cargo, ownerLogin, null);
    }

    public void openChatInlineWithCargoList(String partnerLogin, Cargo cargo,
                                            String ownerLogin, List<Cargo> allCargos) {
        openedChats.add(partnerLogin);
        activeChatPartner = partnerLogin;
        if (cargo != null) {
            cargoByPartner.put(partnerLogin, cargo);
            ownerByPartner.put(partnerLogin, ownerLogin);
        }
        if (allCargos != null && !allCargos.isEmpty()) {
            cargoListByPartner.put(partnerLogin, allCargos);
        }
        Platform.runLater(this::renderChatList);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("Chat.fxml"));
            Parent chatRoot = loader.load();
            activeChatController = loader.getController();
            if (cargo != null) {
                if (allCargos != null && allCargos.size() > 1) {
                    activeChatController.initWithCargoList(currentUser, partnerLogin,
                            cargo, ownerLogin, allCargos);
                } else {
                    activeChatController.initWithCargo(currentUser, partnerLogin, cargo, ownerLogin);
                }
            } else {
                activeChatController.init(currentUser, partnerLogin);
            }
            chatContentPane.getChildren().clear();
            VBox.setVgrow(chatRoot, Priority.ALWAYS);
            if (chatRoot instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
                VBox.setVgrow(region, Priority.ALWAYS);
                HBox.setHgrow(region, Priority.ALWAYS);
            }
            chatContentPane.getChildren().add(chatRoot);
            tabPane.getSelectionModel().select(chatTabIndex);
        } catch (Exception e) {
            System.err.println("[ChatPanelManager] Не удалось открыть чат: " + e.getMessage());
        }
    }

    private void clearChatView() {
        activeChatPartner = null;
        activeChatController = null;
        chatContentPane.getChildren().clear();
        if (chatPlaceholder != null)
            chatContentPane.getChildren().add(chatPlaceholder);
    }


    private String getStrOrEmpty(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : "";
    }
}