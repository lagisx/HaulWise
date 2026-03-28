package com.example.postgresql.Controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.Controllers.CardControllers.UserCargoCardController;
import com.example.postgresql.HelloApplication;
import com.example.postgresql.UserF.ProfileUser;
import com.example.postgresql.utils.MapManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import com.example.postgresql.utils.CargoImageLoader;

public class UserPanelController {

    
    @FXML private VBox cargoContainer;
    @FXML private VBox userCargoContainer;
    @FXML private VBox favoritesContainer;
    @FXML private TabPane tabPane;
    @FXML private Button btnAddCargo;
    @FXML private Label  LabelUser;

    
    @FXML private TextField fromFilter, toFilter;
    @FXML private TextField minWeightFilter, maxWeightFilter;
    @FXML private TextField minPriceFilter, maxPriceFilter;
    @FXML private Button applyFilterButton;

    
    @FXML private VBox chatListContainer;   
    @FXML private VBox chatContentPane;     
    @FXML private VBox chatPlaceholder;     

    
    private JsonArray allCargos;
    private static String currentUser;
    private final AuthService authService = new AuthService();

    
    private String activeChatPartner = null;
    
    private ChatController activeChatController = null;

    
    private final LinkedHashSet<String> openedChats = new LinkedHashSet<>();

    public static void setCurrentUser(String u) { currentUser = u; }
    public static String getCurrentUser()        { return currentUser; }

    
    
    

    @FXML
    private void initialize() {
        tabPane.getTabs().forEach(t -> t.setClosable(false));
        if (applyFilterButton != null)
            applyFilterButton.setOnAction(e -> applyFilters());
    }

    public void setUser(String login) {
        currentUser = login;
        LabelUser.setText(login);
        loadAllCargos();
        loadUserCargos();
        loadFavorites();
        loadChatList();
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

    
    
    

    private void loadChatList() {
        if (chatListContainer == null) return;
        renderChatList();

        authService.getMyConversations(currentUser)
            .thenAccept(array -> Platform.runLater(() -> {
                if (array != null) {
                    for (JsonElement el : array) {
                        JsonObject msg = el.getAsJsonObject();
                        String sender   = str(msg, "sender_login");
                        String receiver = str(msg, "receiver_login");
                        if (!sender.isEmpty()   && !sender.equals(currentUser))   openedChats.add(sender);
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
        
        Label avatar = new Label(partner.substring(0, 1).toUpperCase());
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setAlignment(Pos.CENTER);
        boolean isActive = partner.equals(activeChatPartner);
        avatar.setStyle("-fx-background-color: " + (isActive ? "#1e40af" : "#dbeafe") + ";" +
                        "-fx-text-fill: " + (isActive ? "white" : "#1e40af") + ";" +
                        "-fx-font-weight: bold; -fx-font-size: 17; -fx-background-radius: 20;");

        Label name = new Label(partner);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: " + (isActive ? "#1e40af" : "#0f172a") + ";");
        HBox.setHgrow(name, Priority.ALWAYS);

        
        Button delBtn = new Button("✕");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 13; -fx-padding: 2 6; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            openedChats.remove(partner);
            if (partner.equals(activeChatPartner)) clearChatView();
            renderChatList();
        });
        
        delBtn.setVisible(false);

        HBox row = new HBox(10, avatar, name, delBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: " + (isActive ? "#eff6ff" : "transparent") + ";" +
                     "-fx-cursor: hand; -fx-border-color: transparent transparent #f1f5f9 transparent;");

        row.setOnMouseEntered(e -> {
            delBtn.setVisible(true);
            if (!partner.equals(activeChatPartner))
                row.setStyle("-fx-background-color: #f8fafc; -fx-cursor: hand;" +
                             "-fx-border-color: transparent transparent #f1f5f9 transparent;");
        });
        row.setOnMouseExited(e -> {
            delBtn.setVisible(false);
            if (!partner.equals(activeChatPartner))
                row.setStyle("-fx-background-color: transparent; -fx-cursor: hand;" +
                             "-fx-border-color: transparent transparent #f1f5f9 transparent;");
        });
        row.setOnMouseClicked(e -> openChatInline(partner));

        return row;
    }

    
    public void openChatInline(String partnerLogin) {
        boolean isNew = openedChats.add(partnerLogin);
        activeChatPartner = partnerLogin;

        
        Platform.runLater(this::renderChatList);

        
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("Chat.fxml"));
            Parent chatRoot = loader.load();
            activeChatController = loader.getController();
            activeChatController.init(currentUser, partnerLogin);

            chatContentPane.getChildren().clear();
            VBox.setVgrow(chatRoot, Priority.ALWAYS);
            
            if (chatRoot instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
                VBox.setVgrow(region, Priority.ALWAYS);
                HBox.setHgrow(region, Priority.ALWAYS);
            }
            chatContentPane.getChildren().add(chatRoot);

            
            tabPane.getSelectionModel().select(3);
        } catch (Exception e) {
            showError("Не удалось открыть чат: " + e.getMessage());
        }
    }

    
    public void openChatWith(String partnerLogin) {
        openChatInline(partnerLogin);
    }

    private void clearChatView() {
        activeChatPartner = null;
        activeChatController = null;
        chatContentPane.getChildren().clear();
        if (chatPlaceholder != null)
            chatContentPane.getChildren().add(chatPlaceholder);
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
                Label cnt = new Label("★  Избранное: " + array.size());
                cnt.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-padding: 14 0 8 14; -fx-text-fill: #0f172a;");
                favoritesContainer.getChildren().add(cnt);
                for (JsonElement el : array) addCargoCard(el.getAsJsonObject(), favoritesContainer, false);
            }))
            .exceptionally(ex -> handleError(favoritesContainer, ex));
    }

    
    
    

    private void loadAllCargos() {
        cargoContainer.getChildren().clear();
        showLoading(cargoContainer);
        authService.getAllCargos()
            .thenAccept(array -> Platform.runLater(() -> { allCargos = array; displayAllCargos(array); }))
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
        for (JsonElement el : cargos) addCargoCard(el.getAsJsonObject(), cargoContainer, false);
    }

    private void applyFilters() {
        if (allCargos == null || allCargos.isEmpty()) return;
        String from = fromFilter.getText().trim().toLowerCase();
        String to   = toFilter.getText().trim().toLowerCase();
        double minW = parseDouble(minWeightFilter.getText(), 0);
        double maxW = parseDouble(maxWeightFilter.getText(), Double.MAX_VALUE);
        double minP = parseDouble(minPriceFilter.getText(), 0);
        double maxP = parseDouble(maxPriceFilter.getText(), Double.MAX_VALUE);

        JsonArray filtered = new JsonArray();
        for (JsonElement el : allCargos) {
            JsonObject c = el.getAsJsonObject();
            boolean ok = true;
            if (!from.isEmpty() && !getStr(c, "Откуда").toLowerCase().contains(from)) ok = false;
            if (!to.isEmpty()   && !getStr(c, "Куда").toLowerCase().contains(to))     ok = false;
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
        for (JsonElement el : cargos) addCargoCard(el.getAsJsonObject(), userCargoContainer, true);
    }

    
    
    

    private void addCargoCard(JsonObject cargo, VBox container, boolean isOwner) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("CargoCard/UserCargoCard.fxml"));
            AnchorPane card = loader.load();
            UserCargoCardController ctrl = loader.getController();

            ctrl.typeLabel.setText("RUS • " + getStr(cargo, "ТипТС"));

            String fromCity = getStr(cargo, "Откуда").trim();
            String toCity   = getStr(cargo, "Куда").trim();
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

            if (isOwner) {
                int cargoId = cargo.get("id").getAsInt();
                ctrl.deleteLabel.setVisible(true);
                ctrl.deleteLabel.setManaged(true);
                ctrl.deleteLabel.setOnMouseClicked(e -> deleteCargo(cargoId, card));
                ctrl.chatButton.setVisible(false);
                ctrl.chatButton.setManaged(false);
                ctrl.favButton.setVisible(false);
                ctrl.favButton.setManaged(false);
            } else {
                ctrl.deleteLabel.setVisible(false);
                ctrl.deleteLabel.setManaged(false);

                
                if (cargo.has("заказчик_id") && !cargo.get("заказчик_id").isJsonNull()) {
                    int ownerId = cargo.get("заказчик_id").getAsInt();
                    authService.supabase.select("users", "login", "id=eq." + ownerId)
                        .thenAccept(ur -> Platform.runLater(() -> {
                            String ownerLogin = (ur != null && !ur.isEmpty())
                                    ? ur.get(0).getAsJsonObject().get("login").getAsString() : "";
                            if (!ownerLogin.isEmpty() && !ownerLogin.equals(currentUser)) {
                                ctrl.chatButton.setVisible(true);
                                ctrl.chatButton.setManaged(true);
                                ctrl.chatButton.setOnAction(e -> openChatInline(ownerLogin));
                            }
                        }));
                } else {
                    ctrl.chatButton.setVisible(false);
                    ctrl.chatButton.setManaged(false);
                }

                
                int cargoId = cargo.get("id").getAsInt();
                ctrl.favButton.setVisible(true);
                ctrl.favButton.setManaged(true);
                ctrl.favButton.setOnAction(e ->
                    authService.addFavorite(currentUser, cargoId).thenAccept(ok ->
                        Platform.runLater(() -> {
                            if (ok) {
                                ctrl.favButton.setText("✅ В избранном");
                                ctrl.favButton.setStyle(
                                    "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;" +
                                    "-fx-font-weight: bold; -fx-font-size: 11px;" +
                                    "-fx-padding: 5 12; -fx-background-radius: 8;");
                            }
                        })
                    )
                );
            }

            container.getChildren().add(card);
        } catch (Exception e) {
            Platform.runLater(() -> showError("Ошибка карточки: " + e.getMessage()));
        }
    }

    private void deleteCargo(int cargoId, AnchorPane card) {
        authService.deleteCargo(cargoId)
            .thenAccept(ok -> Platform.runLater(() -> {
                if (ok) { userCargoContainer.getChildren().remove(card); showSuccess("Груз удалён"); refreshAllCargos(); }
                else showError("Не удалось удалить груз");
            }))
            .exceptionally(ex -> { Platform.runLater(() -> showError("Ошибка: " + ex.getMessage())); return null; });
    }

    
    
    

    @FXML
    private void AddUsersCargo() {
        authService.getUserProfile(currentUser).thenAccept(arr -> {
            String phone = "";
            if (arr != null && arr.size() > 0) {
                JsonObject u = arr.get(0).getAsJsonObject();
                if (u.has("phone") && !u.get("phone").isJsonNull()) phone = u.get("phone").getAsString();
            }
            final String p = phone;
            Platform.runLater(() -> openAddCargoDialog(currentUser, p));
        }).exceptionally(ex -> { Platform.runLater(() -> showError("Ошибка профиля: " + ex.getMessage())); return null; });
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

    private void refreshAllCargos() { loadAllCargos(); loadUserCargos(); }

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

    public static void UserPanel(Stage stage, String user, String password) throws IOException {
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

    
    
    

    private String getStr(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "—";
    }
    private String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
    private double getDbl(JsonObject o, String k) {
        if (!o.has(k) || o.get(k).isJsonNull()) return 0;
        try { return o.get(k).getAsDouble(); } catch (Exception e) { return 0; }
    }
    private String fmt(JsonObject o, String k)    { return String.format("%.0f", getDbl(o, k)); }
    private String fmtRub(JsonObject o, String k) { return fmt(o, k) + " ₽"; }
    private double parseDouble(String s, double def) {
        if (s == null || s.trim().isEmpty()) return def;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }
    private void showLoading(VBox b) {
        Label l = new Label("Загрузка...");
        l.setStyle("-fx-padding: 20; -fx-font-size: 15; -fx-text-fill: #64748b;");
        b.getChildren().add(l);
    }
    private Label createInfoLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-padding: 20; -fx-font-size: 14; -fx-text-fill: #94a3b8;");
        return l;
    }
    private void showError(String m) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Ошибка"); a.setContentText(m); a.show();
    }
    private void showSuccess(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("Успех"); a.setHeaderText(null); a.setContentText(m); a.show();
    }
    private Void handleError(VBox b, Throwable ex) {
        Platform.runLater(() -> { b.getChildren().clear(); b.getChildren().add(createInfoLabel("Ошибка: " + ex.getMessage())); });
        return null;
    }
}
