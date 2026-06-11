package com.example.postgresql.controllers;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.example.postgresql.HelloApplication;
import com.example.postgresql.API.AuthService;
import com.example.postgresql.API.Bitrix24Client;
import com.example.postgresql.API.CompanyService;
import com.example.postgresql.UserF.Cargo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.example.postgresql.controllers.CardControllers.UserCargoCardController;
import com.example.postgresql.UserF.ProfileUser;
import com.example.postgresql.utils.CargoImageLoader;
import com.example.postgresql.utils.MapManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class UserPanelController {

    @FXML
    private VBox cargoContainer;
    @FXML
    private VBox userCargoContainer;
    @FXML
    private VBox favoritesContainer;
    @FXML
    private TabPane tabPane;
    @FXML
    private Button btnAddCargo;
    @FXML
    private Label LabelUser;
    @FXML
    private TextField fromFilter, toFilter;
    @FXML
    private TextField minWeightFilter, maxWeightFilter;
    @FXML
    private TextField minPriceFilter, maxPriceFilter;
    @FXML
    private Button applyFilterButton;
    @FXML
    private VBox chatListContainer;
    @FXML
    private VBox chatContentPane;
    @FXML
    private VBox chatPlaceholder;

    private JsonArray allCargos;
    private static String currentUser;
    private final AuthService authService = new AuthService();
    private final CompanyService companyService = new CompanyService();

    private ChatPanelManager chatManager;

    public static void setCurrentUser(String u) {
        currentUser = u;
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    @FXML
    private void initialize() {
        chatManager = new ChatPanelManager(
                authService, chatListContainer, chatContentPane, chatPlaceholder, tabPane, 3
        );

        tabPane.getTabs().forEach(t -> t.setClosable(false));
        if (applyFilterButton != null)
            applyFilterButton.setOnAction(e -> applyFilters());

        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (newIdx.intValue() == 2 && currentUser != null) {
                loadFavorites();
            }
        });
    }

    public void setUser(String login) {
        currentUser = login;
        LabelUser.setText(login);
        chatManager.setCurrentUser(login);
        loadAllCargos();
        loadUserCargos();
        loadFavorites();
        chatManager.loadChatList();
    }


    @FXML
    private void profileClick() {
        authService.getUserProfile(currentUser).thenAccept(arr -> {
            String email = "", phone = "";
            if (arr != null && arr.size() > 0) {
                JsonObject u = arr.get(0).getAsJsonObject();
                if (u.has("email") && !u.get("email").isJsonNull()) email = u.get("email").getAsString();
                if (u.has("phone") && !u.get("phone").isJsonNull()) phone = u.get("phone").getAsString();
            }
            final String e = email, p = phone;
            Platform.runLater(() ->
                    ProfileUser.profilePanel((Stage) LabelUser.getScene().getWindow(), currentUser, e, p)
            );
        }).exceptionally(ex -> {
            Platform.runLater(() -> showError("Ошибка профиля: " + ex.getMessage()));
            return null;
        });
    }


    public void openChatInline(String partnerLogin) {
        chatManager.openChatInline(partnerLogin);
    }

    public void openChatWith(String partnerLogin) {
        chatManager.openChatInline(partnerLogin);
    }


    private void loadFavorites() {
        if (favoritesContainer == null) return;
        favoritesContainer.getChildren().clear();
        showLoading(favoritesContainer);
        authService.getFavoriteCargos(currentUser)
                .thenAccept(array -> Platform.runLater(() -> {
                    favoritesContainer.getChildren().clear();
                    if (array == null || array.isEmpty()) {
                        favoritesContainer.getChildren().add(createInfoLabel("У вас нет избранных грузов"));
                        return;
                    }
                    HBox header = new HBox(12);
                    header.setAlignment(Pos.CENTER_LEFT);
                    header.setStyle("-fx-padding: 14 14 8 14;");
                    Label cnt = new Label("★  Избранное: " + array.size());
                    cnt.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #0f172a;");
                    Button refreshBtn = new Button("🔄 Обновить");
                    refreshBtn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1e40af;" +
                            "-fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5 14;" +
                            "-fx-background-radius: 8; -fx-border-color: #bfdbfe;" +
                            "-fx-border-radius: 8; -fx-cursor: hand;");
                    refreshBtn.setOnAction(ev -> loadFavorites());
                    header.getChildren().addAll(cnt, refreshBtn);
                    favoritesContainer.getChildren().add(header);
                    for (JsonElement el : array)
                        addCargoCard(el.getAsJsonObject(), favoritesContainer, false, true);
                }))
                .exceptionally(ex -> handleError(favoritesContainer, ex));
    }


    private void loadAllCargos() {
        cargoContainer.getChildren().clear();
        showLoading(cargoContainer);
        authService.getAllCargos()
                .thenAccept(array -> Platform.runLater(() -> {
                    allCargos = array;
                    displayAllCargos(array);
                }))
                .exceptionally(ex -> handleError(cargoContainer, ex));
    }

    private void displayAllCargos(JsonArray cargos) {
        cargoContainer.getChildren().clear();
        if (cargos == null || cargos.isEmpty()) {
            cargoContainer.getChildren().add(createInfoLabel("Нет доступных грузов"));
            return;
        }
        Label cnt = new Label("Найдено грузов: " + cargos.size());
        cnt.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-padding: 14 0 8 14; -fx-text-fill: #0f172a;");
        cargoContainer.getChildren().add(cnt);
        for (JsonElement el : cargos) addCargoCard(el.getAsJsonObject(), cargoContainer, false, false);
    }

    private void applyFilters() {
        if (allCargos == null || allCargos.isEmpty()) return;
        String from = fromFilter.getText().trim().toLowerCase();
        String to = toFilter.getText().trim().toLowerCase();
        double minW = parseDouble(minWeightFilter.getText(), 0);
        double maxW = parseDouble(maxWeightFilter.getText(), Double.MAX_VALUE);
        double minP = parseDouble(minPriceFilter.getText(), 0);
        double maxP = parseDouble(maxPriceFilter.getText(), Double.MAX_VALUE);

        JsonArray filtered = new JsonArray();
        for (JsonElement el : allCargos) {
            JsonObject c = el.getAsJsonObject();
            boolean ok = true;
            if (!from.isEmpty() && !getStr(c, "Откуда").toLowerCase().contains(from)) ok = false;
            if (!to.isEmpty() && !getStr(c, "Куда").toLowerCase().contains(to)) ok = false;
            double w = getDbl(c, "Вес"), p = getDbl(c, "ЦенаПоКарте");
            if (w < minW || w > maxW || p < minP || p > maxP) ok = false;
            if (ok) filtered.add(c);
        }
        displayAllCargos(filtered);
    }

    private void loadUserCargos() {
        userCargoContainer.getChildren().clear();
        userCargoContainer.getChildren().add(btnAddCargo);
        showLoading(userCargoContainer);
        authService.getUserCargos(currentUser)
                .thenAccept(array -> Platform.runLater(() -> displayUserCargos(array)))
                .exceptionally(ex -> handleError(userCargoContainer, ex));
    }

    private void displayUserCargos(JsonArray cargos) {
        userCargoContainer.getChildren().removeIf(n ->
                n instanceof Label && ((Label) n).getText().contains("Загрузка"));
        Label cnt = new Label("Ваши грузы: " + (cargos != null ? cargos.size() : 0));
        cnt.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-padding: 14 0 8 14; -fx-text-fill: #0f172a;");
        userCargoContainer.getChildren().add(0, cnt);
        if (cargos == null || cargos.isEmpty()) {
            userCargoContainer.getChildren().add(1, createInfoLabel("У вас пока нет добавленных грузов"));
            return;
        }
        for (JsonElement el : cargos) addCargoCard(el.getAsJsonObject(), userCargoContainer, true, false);
    }

    private void addCargoCard(JsonObject cargo, VBox container, boolean isOwner, boolean isFavoriteTab) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("CargoCard/UserCargoCard.fxml"));
            AnchorPane card = loader.load();
            UserCargoCardController ctrl = loader.getController();

            fillCardFields(ctrl, cargo);

            if (isOwner) {
                setupOwnerCard(ctrl, card, cargo);
            } else {
                setupGuestCard(ctrl, card, cargo, container, isFavoriteTab);
            }

            container.getChildren().add(card);
        } catch (Exception e) {
            Platform.runLater(() -> showError("Ошибка карточки: " + e.getMessage()));
        }
    }

    private void fillCardFields(UserCargoCardController ctrl, JsonObject cargo) {
        ctrl.typeLabel.setText("RUS • " + getStr(cargo, "ТипТС"));

        String fromCity = getStr(cargo, "Откуда").trim();
        String toCity = getStr(cargo, "Куда").trim();
        ctrl.routeLabel.setText(fromCity + " → " + toCity);
        ctrl.routeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-cursor: hand;");
        MapManager.getInstance().showOnClick(ctrl.routeLabel, fromCity, toCity);

        ctrl.vesObemLabel.setText(fmt(cargo, "Вес") + " т • " + fmt(cargo, "Объем") + " м³");
        ctrl.tovarLabel.setText(getStr(cargo, "Товар"));
        ctrl.dateValue.setText(getStr(cargo, "Даты"));
        CargoImageLoader.loadRandom(ctrl.randomImageOnCard);

        String details = getStr(cargo, "ДеталиПогрузки").trim();
        if (details.equals("—") || details.isEmpty()) {
            ctrl.cargoTypeLabel.setVisible(false);
            ctrl.cargoTypeLabel.setManaged(false);
        } else {
            ctrl.cargoTypeLabel.setText(details);
        }

        ctrl.priceKartaLabel.setText(fmtRub(cargo, "ЦенаПоКарте"));
        ctrl.priceNDSLabel.setText(fmtRub(cargo, "ЦенаНДС"));
        ctrl.contactLabel.setText(getStr(cargo, "КонтактныйТелефон"));
        ctrl.tradeLabel.setText(getStr(cargo, "Торг_без_торга"));
    }

    private void setupOwnerCard(UserCargoCardController ctrl, AnchorPane card, JsonObject cargo) {
        int cargoId = cargo.get("id").getAsInt();

        ctrl.deleteLabel.setVisible(true);
        ctrl.deleteLabel.setManaged(true);
        ctrl.deleteLabel.setOnMouseClicked(e -> {
            int bitrixDealId = safeInt(cargo, "bitrix_deal_id");
            if (bitrixDealId > 0) {
                companyService.getWebhookForUser(currentUser).thenAccept(webhook -> {
                    if (!webhook.isEmpty()) {
                        Bitrix24Client.getInstance()
                                .deleteDeal(webhook, bitrixDealId)
                                .thenAccept(ok -> System.out.println("[Bitrix24] Сделка удалена: " + ok))
                                .exceptionally(ex -> {
                                    System.err.println("[Bitrix24] " + ex.getMessage());
                                    return null;
                                });
                    }
                });
            }
            deleteCargo(cargoId, card);
        });

        ctrl.chatButton.setVisible(false);
        ctrl.chatButton.setManaged(false);
        ctrl.favButton.setVisible(false);
        ctrl.favButton.setManaged(false);

        card.setOnMouseClicked(event -> {
            Node target = (Node) event.getTarget();
            if (target instanceof Button) return;
            if (target instanceof Label lbl && lbl == ctrl.deleteLabel) return;
            openCargoCard(cargo, currentUser);
        });
        card.setStyle("-fx-cursor: hand;");
    }

    private void setupGuestCard(UserCargoCardController ctrl, AnchorPane card,
                                JsonObject cargo, VBox container, boolean isFavoriteTab) {
        ctrl.deleteLabel.setVisible(false);
        ctrl.deleteLabel.setManaged(false);

        int cargoId = cargo.get("id").getAsInt();

        setupOwnerInfo(ctrl, card, cargo);
        setupFavoriteButton(ctrl, card, container, cargoId, isFavoriteTab);
    }

    private void setupOwnerInfo(UserCargoCardController ctrl, AnchorPane card, JsonObject cargo) {
        if (cargo.has("заказчик_id") && !cargo.get("заказчик_id").isJsonNull()) {
            int ownerId = cargo.get("заказчик_id").getAsInt();
            authService.supabase.select("users", "login", "id=eq." + ownerId)
                    .thenAccept(rows -> Platform.runLater(() ->
                            applyOwnerInfo(ctrl, card, cargo, resolveOwnerLogin(rows))
                    ));
        } else {
            setupCardWithoutOwner(ctrl, card, cargo);
        }
    }

    private String resolveOwnerLogin(JsonArray rows) {
        if (rows != null && !rows.isEmpty()) {
            return rows.get(0).getAsJsonObject().get("login").getAsString();
        }
        return "";
    }

    private void applyOwnerInfo(UserCargoCardController ctrl, AnchorPane card,
                                JsonObject cargo, String ownerLogin) {
        if (!ownerLogin.isEmpty() && !ownerLogin.equals(currentUser)) {
            ctrl.chatButton.setVisible(true);
            ctrl.chatButton.setManaged(true);
            ctrl.chatButton.setOnAction(e -> {
                companyService.getWebhookForUser(currentUser).thenAccept(myWebhook -> {
                    if (!myWebhook.isEmpty()) {
                        Cargo cargoObj = new Cargo(
                                safeInt(cargo, "id"), safeStr(cargo, "ТипТС"),
                                safeDbl(cargo, "Вес"), safeDbl(cargo, "Объем"),
                                safeStr(cargo, "Товар"), safeStr(cargo, "Откуда"),
                                safeStr(cargo, "Куда"), safeStr(cargo, "ТипПогрузки"),
                                safeStr(cargo, "ДеталиПогрузки"), safeStr(cargo, "Даты"),
                                safeDbl(cargo, "ЦенаПоКарте"), safeDbl(cargo, "ЦенаНДС"),
                                safeStr(cargo, "Торг_без_торга"), safeStr(cargo, "КонтактныйТелефон"), 0);
                        String deadline = LocalDate.now().plusDays(7)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T23:59:59+03:00";
                        Bitrix24Client.getInstance()
                                .createTaskForCarrier(myWebhook, cargoObj, currentUser, deadline)
                                .thenAccept(taskId -> {
                                    System.out.println("[Bitrix24] Задача создана, ID=" + taskId);
                                    if (taskId > 0) {
                                        javafx.application.Platform.runLater(() -> {
                                            ctrl.cancelDealBtn.setUserData(new int[]{taskId});
                                            ctrl.cancelDealBtn.setVisible(true);
                                            ctrl.cancelDealBtn.setManaged(true);
                                            ctrl.cancelDealBtn.setOnAction(ev ->
                                                    cancelCarrierDeal(ctrl, myWebhook, taskId));
                                        });
                                    }
                                })
                                .exceptionally(ex -> {
                                    System.err.println("[Bitrix24] task: " + ex.getMessage());
                                    return null;
                                });
                    }
                    int bitrixDealId = safeInt(cargo, "bitrix_deal_id");
                    if (bitrixDealId > 0) {
                        companyService.getWebhookForUser(ownerLogin).thenAccept(ownerWebhook -> {
                            if (!ownerWebhook.isEmpty()) {
                                Bitrix24Client.getInstance()
                                        .updateDealStageInProgress(ownerWebhook, bitrixDealId)
                                        .thenAccept(ok -> System.out.println("[Bitrix24] Сделка переведена в 'В работе': " + ok))
                                        .exceptionally(ex -> {
                                            System.err.println("[Bitrix24] update: " + ex.getMessage());
                                            return null;
                                        });
                            }
                        });
                    }
                }).exceptionally(ex -> {
                    System.err.println("[Bitrix24] webhook: " + ex.getMessage());
                    return null;
                });
                openChatInline(ownerLogin);
            });
        }
        card.setOnMouseClicked(event -> {
            Node target = (Node) event.getTarget();
            if (target instanceof Button) return;
            if (target instanceof Label lbl && lbl == ctrl.deleteLabel) return;
            openCargoCard(cargo, ownerLogin);
        });
        card.setStyle("-fx-cursor: hand;");
    }

    private void setupCardWithoutOwner(UserCargoCardController ctrl,
                                       AnchorPane card, JsonObject cargo) {
        ctrl.chatButton.setVisible(false);
        ctrl.chatButton.setManaged(false);
        card.setOnMouseClicked(event -> {
            Node target = (Node) event.getTarget();
            if (target instanceof Button) return;
            openCargoCard(cargo, "");
        });
        card.setStyle("-fx-cursor: hand;");
    }

    private void setupFavoriteButton(UserCargoCardController ctrl, AnchorPane card,
                                     VBox container, int cargoId, boolean isFavoriteTab) {
        if (isFavoriteTab) {
            ctrl.removeFromFavButton.setVisible(true);
            ctrl.removeFromFavButton.setManaged(true);
            ctrl.favButton.setVisible(false);
            ctrl.favButton.setManaged(false);
            ctrl.removeFromFavButton.setOnAction(e -> removeFromFavorites(cargoId, card, container));
        } else {
            ctrl.favButton.setVisible(true);
            ctrl.favButton.setManaged(true);
            authService.isFavorite(currentUser, cargoId).thenAccept(alreadyFav ->
                    Platform.runLater(() -> applyFavoriteState(ctrl.favButton, cargoId, alreadyFav))
            );
        }
    }

    private void removeFromFavorites(int cargoId, AnchorPane card, VBox container) {
        authService.removeFavorite(currentUser, cargoId).thenAccept(ok ->
                Platform.runLater(() -> {
                    if (ok) {
                        container.getChildren().remove(card);
                        container.getChildren().stream()
                                .filter(HBox.class::isInstance)
                                .map(n -> (HBox) n)
                                .flatMap(h -> h.getChildren().stream())
                                .filter(n -> n instanceof Label label && label.getText().startsWith("★"))
                                .findFirst()
                                .ifPresent(n -> {
                                    if (n instanceof Label label) {
                                        long count = container.getChildren().stream()
                                                .filter(AnchorPane.class::isInstance).count();
                                        label.setText("★  Избранное: " + count);
                                    }
                                });
                    } else {
                        showError("Не удалось убрать из избранного");
                    }
                })
        );
    }

    private void applyFavoriteState(Button favButton, int cargoId, boolean isFavorite) {
        if (isFavorite) {
            favButton.setText("✅ В избранном");
            favButton.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46;" +
                    "-fx-font-weight: bold; -fx-font-size: 11px;" +
                    "-fx-padding: 5 12; -fx-background-radius: 8;");
            favButton.setOnAction(e ->
                    authService.removeFavorite(currentUser, cargoId).thenAccept(ok ->
                            Platform.runLater(() -> {
                                if (ok) applyFavoriteState(favButton, cargoId, false);
                            })
                    )
            );
        } else {
            favButton.setText("★ В избранное");
            favButton.setStyle("-fx-background-color: #fffbeb; -fx-text-fill: #92400e;" +
                    "-fx-font-weight: bold; -fx-font-size: 11px;" +
                    "-fx-padding: 5 14; -fx-background-radius: 8;" +
                    "-fx-border-color: #fde68a; -fx-border-radius: 8; -fx-cursor: hand;");
            favButton.setOnAction(e ->
                    authService.addFavorite(currentUser, cargoId).thenAccept(ok ->
                            Platform.runLater(() -> {
                                if (ok) applyFavoriteState(favButton, cargoId, true);
                            })
                    )
            );
        }
    }

    private void openCargoCard(JsonObject cargo, String ownerLogin) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("CargoCard/CargoCard.fxml"));
            Parent root = loader.load();

            CargoCardController ctrl = loader.getController();
            ctrl.setCargo(cargo, ownerLogin);

            Stage stage = new Stage();
            stage.setTitle("Карточка груза");
            stage.initOwner(cargoContainer.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.setMinWidth(1000);
            stage.setMinHeight(620);
            stage.show();

        } catch (Exception ex) {
            showError("Не удалось открыть карточку: " + ex.getMessage());
        }
    }

    private void deleteCargo(int cargoId, AnchorPane card) {
        authService.deleteCargo(cargoId)
                .thenAccept(ok -> Platform.runLater(() -> {
                    if (ok) {
                        userCargoContainer.getChildren().remove(card);
                        showSuccess("Груз удалён");
                        refreshAllCargos();
                    } else showError("Не удалось удалить груз");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Ошибка: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void addUsersCargo() {
        authService.getUserProfile(currentUser).thenAccept(arr -> {
            String phone = "";
            if (arr != null && arr.size() > 0) {
                JsonObject u = arr.get(0).getAsJsonObject();
                if (u.has("phone") && !u.get("phone").isJsonNull()) phone = u.get("phone").getAsString();
            }
            final String p = phone;
            Platform.runLater(() -> openAddCargoDialog(currentUser, p));
        }).exceptionally(ex -> {
            Platform.runLater(() -> showError("Ошибка профиля: " + ex.getMessage()));
            return null;
        });
    }

    private void openAddCargoDialog(String user, String phone) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("AddCargoDialog.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Добавить груз");
            stage.initOwner(btnAddCargo.getScene().getWindow());
            stage.setResizable(false);
            AddCargoDialogController ctrl = loader.getController();
            ctrl.setUser(user, phone, this::refreshAllCargos);
            stage.show();
        } catch (Exception e) {
            showError("Ошибка открытия формы: " + e.getMessage());
        }
    }

    private void refreshAllCargos() {
        loadAllCargos();
        loadUserCargos();
    }

    @FXML
    private void goBack() throws IOException {
        Parent root = FXMLLoader.load(HelloApplication.class.getResource("main.fxml"));
        Stage stage = (Stage) LabelUser.getScene().getWindow();
        stage.setResizable(false);
        stage.setMaximized(false);
        stage.setScene(new Scene(root));
        stage.setTitle("Авторизация");
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    public static void userPanel(Stage stage, String user, Object o) throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("UserPanel.fxml"));
        Scene scene = new Scene(loader.load());
        UserPanelController ctrl = loader.getController();
        ctrl.setUser(user);
        stage.setScene(scene);
        stage.setTitle("Панель пользователя • " + user);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.centerOnScreen();
        stage.show();
    }

    private String getStr(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "—";
    }

    private String getStrOrEmpty(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private double getDbl(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return 0;
        try {
            return object.get(key).getAsDouble();
        } catch (Exception _) {
            return 0;
        }
    }

    private String fmt(JsonObject object, String key) {
        return String.format("%.0f", getDbl(object, key));
    }

    private String fmtRub(JsonObject object, String key) {
        return fmt(object, key) + " ₽";
    }

    private double parseDouble(String text, double defaultValue) {
        if (text == null || text.trim().isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException _) {
            return defaultValue;
        }
    }

    private void showLoading(VBox container) {
        Label loadingLabel = new Label("Загрузка...");
        loadingLabel.setStyle("-fx-padding: 20; -fx-font-size: 15; -fx-text-fill: #64748b;");
        container.getChildren().add(loadingLabel);
    }

    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-padding: 20; -fx-font-size: 14; -fx-text-fill: #94a3b8;");
        return label;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setContentText(message);
        alert.show();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успех");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private Void handleError(VBox container, Throwable ex) {
        Platform.runLater(() -> {
            container.getChildren().clear();
            container.getChildren().add(createInfoLabel("Ошибка: " + ex.getMessage()));
        });
        return null;
    }

    private void cancelCarrierDeal(UserCargoCardController ctrl, String webhook, int taskId) {
        Bitrix24Client.getInstance().completeTask(webhook, taskId)
                .thenAccept(ok -> javafx.application.Platform.runLater(() -> {
                    System.out.println("[Bitrix24] Задача завершена: " + ok);
                    ctrl.cancelDealBtn.setVisible(false);
                    ctrl.cancelDealBtn.setManaged(false);
                }))
                .exceptionally(ex -> {
                    System.err.println("[Bitrix24] completeTask error: " + ex.getMessage());
                    return null;
                });
    }

    private static String safeStr(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private static double safeDbl(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return 0;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return 0;
        }
    }

    private static int safeInt(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return 0;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }
}
