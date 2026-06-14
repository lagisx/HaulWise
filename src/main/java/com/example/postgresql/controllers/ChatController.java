package com.example.postgresql.controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.API.Bitrix24Client;
import com.example.postgresql.API.CompanyService;
import com.example.postgresql.API.DealService;
import com.example.postgresql.API.RealtimeClient;
import com.example.postgresql.UserF.Cargo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML
    private VBox messagesContainer;
    @FXML
    private TextField messageInput;
    @FXML
    private Button sendButton;
    @FXML
    private Label chatTitleLabel;
    @FXML
    private Label partnerPhoneLabel;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private HBox dealButtonsBox;
    @FXML
    private Button completeDealBtn;
    @FXML
    private Button cancelDealBtn2;
    @FXML
    private HBox cargoInfoBox;
    @FXML
    private Label cargoInfoLabel;
    @FXML
    private Label dealStatusLabel;
    @FXML
    private HBox cargoSwitcherBox;
    @FXML
    private HBox cargoTabsContainer;

    private final AuthService authService = new AuthService();
    private final RealtimeClient realtimeClient = new RealtimeClient();
    private final DealService dealService = new DealService();
    private final CompanyService companyService = new CompanyService();

    private String myLogin;
    private String partnerLogin;
    private Cargo cargo;
    private String ownerLogin;
    private int activeDealId = -1;
    private int bitrixTaskId = -1;
    private int bitrixDealId = -1;
    private final AtomicBoolean dealCreationInProgress = new AtomicBoolean(false);
    private final List<Cargo> allCargosList = new ArrayList<>();


    public void init(String myLogin, String partnerLogin) {
        this.myLogin = myLogin;
        this.partnerLogin = partnerLogin;
        this.ownerLogin = partnerLogin;
        chatTitleLabel.setText("Чат с " + partnerLogin);
        hideDealButtonsAll();
        loadHistory();
        connectRealtime();
        loadPartnerPhone();
        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());
    }


    public void initWithCargo(String myLogin, String partnerLogin,
                              Cargo cargo, String ownerLogin) {
        this.myLogin = myLogin;
        this.partnerLogin = partnerLogin;
        this.cargo = cargo;
        this.ownerLogin = ownerLogin;
        activeDealId = -1;
        bitrixTaskId = -1;
        bitrixDealId = -1;
        dealCreationInProgress.set(false);

        chatTitleLabel.setText("Чат с " + partnerLogin);
        updateCargoInfoLabel();
        loadHistory();
        connectRealtime();
        loadPartnerPhone();
        loadActiveDeal();
        Platform.runLater(this::buildCargoSwitcher);
        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());
    }


    public void initWithCargoList(String myLogin, String partnerLogin,
                                  Cargo cargo, String ownerLogin, List<Cargo> allCargos) {
        allCargosList.clear();
        if (allCargos != null) allCargosList.addAll(allCargos);
        initWithCargo(myLogin, partnerLogin, cargo, ownerLogin);
    }


    private void updateCargoInfoLabel() {
        if (cargo == null) return;
        cargoInfoBox.setVisible(true);
        cargoInfoBox.setManaged(true);
        cargoInfoLabel.setText(cargo.getProduct() + " | "
                + cargo.getFromCity() + " → " + cargo.getToCity()
                + " | " + (int) cargo.getPriceCard() + " ₽");
        dealButtonsBox.setVisible(true);
        dealButtonsBox.setManaged(true);
    }


    private void buildCargoSwitcher() {
        if (cargoSwitcherBox == null) return;
        if (allCargosList.size() <= 1) {
            cargoSwitcherBox.setVisible(false);
            cargoSwitcherBox.setManaged(false);
            return;
        }
        cargoSwitcherBox.setVisible(true);
        cargoSwitcherBox.setManaged(true);
        cargoTabsContainer.getChildren().clear();
        for (Cargo c : allCargosList) {
            Button tab = new Button(c.getFromCity() + "→" + c.getToCity() + " | " + c.getProduct());
            boolean active = cargo != null && c.getId() == cargo.getId();
            tab.setStyle(active
                    ? "-fx-background-color:#1e40af;-fx-text-fill:white;-fx-font-size:10;-fx-padding:4 10;-fx-background-radius:12;-fx-cursor:hand;"
                    : "-fx-background-color:#e2e8f0;-fx-text-fill:#374151;-fx-font-size:10;-fx-padding:4 10;-fx-background-radius:12;-fx-cursor:hand;");
            tab.setOnAction(ev -> switchCargo(c));
            cargoTabsContainer.getChildren().add(tab);
        }
    }

    private void switchCargo(Cargo newCargo) {
        if (newCargo == null || (cargo != null && newCargo.getId() == cargo.getId())) return;
        cargo = newCargo;
        activeDealId = -1;
        bitrixTaskId = -1;
        bitrixDealId = -1;
        dealCreationInProgress.set(false);
        dealButtonsBox.getChildren().removeIf(n -> "resumeBtn".equals(n.getId()));
        updateCargoInfoLabel();
        buildCargoSwitcher();
        loadActiveDeal();
    }


    private void loadActiveDeal() {
        if (cargo == null) return;
        final int cargoId = cargo.getId();
        dealService.getDealByCargoId(ownerLogin, myLogin, cargoId)
                .thenAccept(deal -> Platform.runLater(() -> {
                    if (deal == null) {
                        createNewDeal();
                        return;
                    }
                    activeDealId = deal.get("id").getAsInt();
                    bitrixTaskId = deal.has("bitrix_task_id") && !deal.get("bitrix_task_id").isJsonNull()
                            ? deal.get("bitrix_task_id").getAsInt() : -1;
                    bitrixDealId = deal.has("bitrix_deal_id") && !deal.get("bitrix_deal_id").isJsonNull()
                            ? deal.get("bitrix_deal_id").getAsInt() : -1;
                    String status = deal.has("status") && !deal.get("status").isJsonNull()
                            ? deal.get("status").getAsString() : "active";

                    switch (status) {
                        case "completed" -> {
                            setDealStatus("✅ Сделка завершена", "#16a34a", true);
                            hideDealButtons();
                        }
                        case "cancelled" -> {
                            setDealStatus("✕ Сделка отменена", "#ef4444", false);
                            hideDealButtons();
                            showResumeButton();
                        }
                        default -> {
                            setDealStatus("● Сделка активна", "#16a34a", false);
                            showDealButtons();
                        }
                    }
                })).exceptionally(ex -> null);
    }

    private void createNewDeal() {
        if (cargo == null) return;
        String deadline = java.time.LocalDate.now().plusDays(7)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T23:59:59+03:00";

        companyService.getWebhookForUser(myLogin).thenAccept(myWebhook -> {
            companyService.getWebhookForUser(ownerLogin).thenAccept(ownerWebhook -> {
                java.util.concurrent.CompletableFuture<Integer> taskFuture;
                java.util.concurrent.CompletableFuture<Integer> dealFuture;

                if (myWebhook != null && !myWebhook.isEmpty()) {
                    taskFuture = Bitrix24Client.getInstance()
                            .createTaskForCarrier(myWebhook, cargo, myLogin, deadline);
                } else {
                    taskFuture = java.util.concurrent.CompletableFuture.completedFuture(-1);
                }

                if (ownerWebhook != null && !ownerWebhook.isEmpty()) {
                    int dealId = cargo != null ? cargo.getId() : 0;
                    dealFuture = java.util.concurrent.CompletableFuture.completedFuture(dealId);
                } else {
                    dealFuture = java.util.concurrent.CompletableFuture.completedFuture(-1);
                }

                taskFuture.thenCombine(dealFuture, (taskId, bid) -> {
                            bitrixTaskId = taskId;
                            bitrixDealId = bid;
                            return null;
                        }).thenCompose(v -> dealService.createDeal(
                                cargo.getId(), ownerLogin, myLogin, bitrixTaskId, bitrixDealId))
                        .thenAccept(id -> Platform.runLater(() -> {
                            activeDealId = id;
                            dealStatusLabel.setText("● Сделка активна");
                            dealStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #16a34a;");
                            System.out.println("[Deal] Создана сделка ID=" + id
                                    + " taskId=" + bitrixTaskId);
                        })).exceptionally(ex -> {
                            System.err.println("[Deal] Ошибка создания сделки: " + ex.getMessage());
                            return null;
                        });
            });
        });
    }


    @FXML
    private void onCompleteDeal() {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
                "Завершить сделку? Груз считается доставленным.", ButtonType.YES, ButtonType.NO);
        dlg.setTitle("Завершение сделки");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            completeDealBtn.setDisable(true);
            cancelDealBtn2.setDisable(true);

            companyService.getWebhookForUser(myLogin)
                    .thenCompose(myWebhook -> {
                        java.util.concurrent.CompletableFuture<Boolean> taskF =
                                (myWebhook != null && !myWebhook.isEmpty() && bitrixTaskId > 0)
                                        ? Bitrix24Client.getInstance().completeTask(myWebhook, bitrixTaskId)
                                        : java.util.concurrent.CompletableFuture.completedFuture(true);
                        return taskF.thenCompose(ok ->
                                activeDealId > 0
                                        ? dealService.updateDealStatus(activeDealId, "completed")
                                        : java.util.concurrent.CompletableFuture.completedFuture(true));
                    })
                    .thenAccept(ok -> Platform.runLater(() -> {
                        setDealStatus("✅ Сделка завершена", "#16a34a", true);
                        hideDealButtons();
                        addSystemMessage("✅ Сделка завершена. Груз доставлен.");
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            completeDealBtn.setDisable(false);
                            cancelDealBtn2.setDisable(false);
                        });
                        return null;
                    });
        });
    }


    @FXML
    private void onCancelDeal() {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
                "Отменить сделку? Задача будет отложена в Битрикс24.", ButtonType.YES, ButtonType.NO);
        dlg.setTitle("Отмена сделки");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            completeDealBtn.setDisable(true);
            cancelDealBtn2.setDisable(true);

            companyService.getWebhookForUser(myLogin)
                    .thenCompose(myWebhook -> {
                        java.util.concurrent.CompletableFuture<Boolean> taskF =
                                (myWebhook != null && !myWebhook.isEmpty() && bitrixTaskId > 0)
                                        ? Bitrix24Client.getInstance().cancelTask(myWebhook, bitrixTaskId)
                                        : java.util.concurrent.CompletableFuture.completedFuture(true);
                        return taskF.thenCompose(ok ->
                                activeDealId > 0
                                        ? dealService.updateDealStatus(activeDealId, "cancelled")
                                        : java.util.concurrent.CompletableFuture.completedFuture(true));
                    })
                    .thenAccept(ok -> Platform.runLater(() -> {
                        setDealStatus("✕ Сделка отменена", "#ef4444", false);
                        hideDealButtons();
                        showResumeButton();
                        addSystemMessage("✕ Сделка отменена.");
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            completeDealBtn.setDisable(false);
                            cancelDealBtn2.setDisable(false);
                        });
                        return null;
                    });
        });
    }


    private void showResumeButton() {
        if (dealButtonsBox == null) return;
        dealButtonsBox.setVisible(true);
        dealButtonsBox.setManaged(true);
        dealButtonsBox.getChildren().removeIf(n -> "resumeBtn".equals(n.getId()));
        Button btn = new Button("🔄 Возобновить сделку");
        btn.setId("resumeBtn");
        btn.setStyle("-fx-background-color:#1e40af;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-font-size:11px;-fx-padding:6 14;-fx-background-radius:8;-fx-cursor:hand;");
        btn.setOnAction(e -> resumeDeal(btn));
        dealButtonsBox.getChildren().add(btn);
    }

    private void resumeDeal(Button resumeBtn) {
        resumeBtn.setDisable(true);
        final String deadline = java.time.LocalDate.now().plusDays(7)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T23:59:59+03:00";
        final Cargo snap = this.cargo;
        final String snapMe = this.myLogin;

        companyService.getWebhookForUser(snapMe)
                .thenCompose(myWebhook -> {
                    java.util.concurrent.CompletableFuture<Integer> taskF =
                            (myWebhook != null && !myWebhook.isEmpty())
                                    ? Bitrix24Client.getInstance().createTaskForCarrier(myWebhook, snap, snapMe, deadline)
                                    : java.util.concurrent.CompletableFuture.completedFuture(-1);
                    return taskF.thenCompose(taskId -> {
                        bitrixTaskId = taskId;
                        java.util.concurrent.CompletableFuture<Boolean> statusF =
                                dealService.updateDealStatus(activeDealId, "active");
                        java.util.concurrent.CompletableFuture<Boolean> taskUpd = (taskId > 0)
                                ? dealService.updateTaskId(activeDealId, taskId)
                                : java.util.concurrent.CompletableFuture.completedFuture(true);
                        return statusF.thenCombine(taskUpd, (a, b) -> taskId);
                    });
                })
                .thenAccept(taskId -> Platform.runLater(() -> {
                    setDealStatus("● Сделка активна", "#16a34a", false);
                    dealButtonsBox.getChildren().removeIf(n -> "resumeBtn".equals(n.getId()));
                    showDealButtons();
                    addSystemMessage("🔄 Сделка возобновлена.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> resumeBtn.setDisable(false));
                    return null;
                });
    }


    private void setDealStatus(String text, String color, boolean bold) {
        if (dealStatusLabel == null) return;
        dealStatusLabel.setText(text);
        dealStatusLabel.setStyle("-fx-font-size:11;-fx-text-fill:" + color
                + (bold ? ";-fx-font-weight:bold;" : ";"));
    }

    private void hideDealButtons() {
        if (completeDealBtn != null) {
            completeDealBtn.setVisible(false);
            completeDealBtn.setManaged(false);
            completeDealBtn.setDisable(false);
        }
        if (cancelDealBtn2 != null) {
            cancelDealBtn2.setVisible(false);
            cancelDealBtn2.setManaged(false);
            cancelDealBtn2.setDisable(false);
        }
    }

    private void showDealButtons() {
        if (dealButtonsBox != null) {
            dealButtonsBox.setVisible(true);
            dealButtonsBox.setManaged(true);
        }
        if (completeDealBtn != null) {
            completeDealBtn.setVisible(true);
            completeDealBtn.setManaged(true);
            completeDealBtn.setDisable(false);
        }
        if (cancelDealBtn2 != null) {
            cancelDealBtn2.setVisible(true);
            cancelDealBtn2.setManaged(true);
            cancelDealBtn2.setDisable(false);
        }
        dealButtonsBox.getChildren().removeIf(n -> "resumeBtn".equals(n.getId()));
    }

    private void hideDealButtonsAll() {
        if (dealButtonsBox != null) {
            dealButtonsBox.setVisible(false);
            dealButtonsBox.setManaged(false);
        }
        if (cargoInfoBox != null) {
            cargoInfoBox.setVisible(false);
            cargoInfoBox.setManaged(false);
        }
        if (cargoSwitcherBox != null) {
            cargoSwitcherBox.setVisible(false);
            cargoSwitcherBox.setManaged(false);
        }
    }


    private void loadPartnerPhone() {
        authService.getUserProfile(partnerLogin).thenAccept(arr -> Platform.runLater(() -> {
            if (arr != null && !arr.isEmpty()) {
                JsonObject u = arr.get(0).getAsJsonObject();
                String phone = has(u, "phone");
                partnerPhoneLabel.setText(phone.isEmpty() ? "Телефон не указан" : "📞 " + phone);
                if (phone.isEmpty())
                    partnerPhoneLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");
            }
        }));
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
            String sender = has(record, "sender_login");
            String receiver = has(record, "receiver_login");
            boolean isOurs = (sender.equals(myLogin) && receiver.equals(partnerLogin))
                    || (sender.equals(partnerLogin) && receiver.equals(myLogin));
            if (isOurs) Platform.runLater(() -> {
                addBubble(record);
                scrollToBottom();
            });
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
        String sender = has(msg, "sender_login");
        String content = has(msg, "content");
        String time = has(msg, "created_at");
        if (time.length() >= 16) time = time.substring(11, 16);
        boolean isMine = sender.equals(myLogin);
        Text text = new Text(content);
        text.setWrappingWidth(320);
        text.setStyle("-fx-font-size:13px;-fx-fill:" + (isMine ? "white" : "#1e293b") + ";");
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size:10px;-fx-text-fill:" + (isMine ? "#93c5fd" : "#94a3b8") + ";");
        VBox bubble = new VBox(3, text, timeLabel);
        bubble.setPadding(new Insets(8, 12, 6, 12));
        bubble.setMaxWidth(340);
        bubble.setStyle(isMine
                ? "-fx-background-color:#1e40af;-fx-background-radius:16 4 16 16;"
                : "-fx-background-color:white;-fx-background-radius:4 16 16 16;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),4,0,0,1);");
        HBox row = new HBox(bubble);
        row.setPadding(new Insets(3, 14, 3, 14));
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messagesContainer.getChildren().add(row);
    }

    private void addSystemMessage(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;-fx-font-style:italic;");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(6, 14, 6, 14));
        messagesContainer.getChildren().add(row);
        scrollToBottom();
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