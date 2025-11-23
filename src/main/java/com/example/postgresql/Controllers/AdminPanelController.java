package com.example.postgresql.Controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.HelloApplication;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AdminPanelController {

    @FXML private TableView<JsonObject> cargoTable, userTable, blacklistTable, logsListTable;

    @FXML private TableColumn<JsonObject, String> colCargoId, colCargoOwner, colCargoType, colCargoWeight,
            colCargoVolume, colCargoProduct, colCargoFrom, colCargoTo, colCargoLoadType,
            colCargoLoadDetails, colCargoDates, colCargoPriceCard, colCargoPriceNDC, colCargoTorg, colCargoContact;

    @FXML private TableColumn<JsonObject, String> colUserId, colUserLogin, colUserPassword,
            colUserEmail, colUserPhone, colUserCreated_at, colUserStatus;

    @FXML private TableColumn<JsonObject, String> colBlockUserId, colBlockUserLogin, colBlockUserEmail,
            colBlockUserPhone, colBlockUserReason, colBlockUserBlocked_by, colBlockUserCreated_at;

    @FXML private TableColumn<JsonObject, String> colLogsListId, colLogsListUser, colLogsListDescription, colLogsListCreated_at;

    @FXML private Label statusLabelUsers, statusLabelBlockUsers, LabelUser;
    @FXML private TabPane adminTabPane;

    private final AuthService authService = new AuthService();
    private static String Adminuser = "admin";

    private final ObservableList<JsonObject> cargos = FXCollections.observableArrayList();
    private final ObservableList<JsonObject> users = FXCollections.observableArrayList();
    private final ObservableList<JsonObject> blacklist = FXCollections.observableArrayList();
    private final ObservableList<JsonObject> logs = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupCargoColumns();
        setupUserColumns();
        setupBlacklistColumns();
        setupLogsColumns();

        cargoTable.setItems(cargos);
        userTable.setItems(users);
        blacklistTable.setItems(blacklist);
        logsListTable.setItems(logs);

        loadAllData();
        TabHidePunktire();
    }
    private String safeString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "";
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        return el.getAsString();
    }
    private void setupCargoColumns() {
        colCargoId.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "id")));
        colCargoOwner.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "owner_login")));
        colCargoType.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "ТипТС")));
        colCargoWeight.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "Вес")));
        colCargoVolume.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "Объем")));
        colCargoProduct.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "Товар")));
        colCargoFrom.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "Откуда")));
        colCargoTo.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "Куда")));
        colCargoLoadType.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "ТипПогрузки")));
        colCargoLoadDetails.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "ДеталиПогрузки")));
        colCargoDates.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "Даты")));
        colCargoPriceCard.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "ЦенаПоКарте")));
        colCargoPriceNDC.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "ЦенаНДС")));
        colCargoTorg.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "Торг/без_торга")));
        colCargoContact.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "КонтактныйТелефон")));
    }
    private void setupUserColumns() {
        colUserId.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "id")));
        colUserLogin.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "login")));
        colUserPassword.setCellValueFactory(cell -> new SimpleStringProperty("••••••••"));
        colUserEmail.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "email")));
        colUserPhone.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "phone")));
        colUserCreated_at.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "created_at")));
        colUserStatus.setCellValueFactory(cell -> {
            JsonObject obj = cell.getValue();
            boolean blocked = obj != null && obj.has("status") && obj.get("status").getAsBoolean();
            return new SimpleStringProperty(blocked ? "Заблокирован" : "Активен");
        });
    }
    private void setupBlacklistColumns() {
        colBlockUserId.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "id")));
        colBlockUserLogin.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "login")));
        colBlockUserEmail.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "email")));
        colBlockUserPhone.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "phone")));
        colBlockUserReason.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "reason")));
        colBlockUserBlocked_by.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "blocked_by")));
        colBlockUserCreated_at.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "created_at")));
    }
    private void setupLogsColumns() {
        colLogsListId.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "id")));
        colLogsListUser.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "users")));
        colLogsListDescription.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "description")));
        colLogsListCreated_at.setCellValueFactory(cell -> new SimpleStringProperty(safeString(cell.getValue(), "created_at")));
    }

    private void loadAllData() {
        loadCargos();
        loadUsers();
        loadBlacklist();
        loadLogs();
    }
    @FXML private void loadCargos() {
        cargos.clear();
        statusLabelUsers.setText("Загрузка грузов...");
        statusLabelUsers.setStyle("-fx-text-fill: blue;");

        authService.getAllCargos().thenAccept(array -> Platform.runLater(() -> {
            if (array == null || array.size() == 0) {
                statusLabelUsers.setText("Нет грузов");
                statusLabelUsers.setStyle("-fx-text-fill: gray;");
                return;
            }

            for (JsonElement el : array) {
                JsonObject cargo = el.getAsJsonObject();
                cargo.addProperty("owner_login", "Загрузка...");
                cargos.add(cargo);
            }

            for (int i = 0; i < cargos.size(); i++) {
                JsonObject cargo = cargos.get(i);
                final int index = i;

                if (cargo.has("заказчик_id")) {
                    int userId = cargo.get("заказчик_id").getAsInt();
                    authService.supabase.select("users", "login", "id=eq." + userId)
                            .thenAccept(userArray -> Platform.runLater(() -> {
                                String login = userArray.size() > 0
                                        ? userArray.get(0).getAsJsonObject().get("login").getAsString()
                                        : "Удалён";
                                cargos.get(index).addProperty("owner_login", login);
                                cargoTable.refresh();
                            }));
                } else {
                    cargo.addProperty("owner_login", "Гость");
                }
            }

            statusLabelUsers.setText("Грузы загружены: " + cargos.size());
            statusLabelUsers.setStyle("-fx-text-fill: green;");
            cargoTable.refresh();
        })).exceptionally(ex -> {
            handleError(ex, "грузов");
            return null;
        });
    }
    @FXML private void loadUsers() {
        users.clear();
        authService.getAllUsers().thenAccept(array -> Platform.runLater(() -> {
            if (array != null) {
                users.addAll(array.getAsJsonArray().asList().stream()
                        .map(JsonElement::getAsJsonObject)
                        .toList());
            }
            userTable.refresh();
        })).exceptionally(ex -> {
            handleError(ex, "пользователей");
            return null;
        });
    }
    @FXML private void loadBlacklist() {
        blacklist.clear();
        authService.getBlacklist().thenAccept(array -> Platform.runLater(() -> {
            if (array != null) {
                blacklist.addAll(array.getAsJsonArray().asList().stream()
                        .map(JsonElement::getAsJsonObject)
                        .toList());
            }
            blacklistTable.refresh();
        })).exceptionally(ex -> {
            handleError(ex, "черного списка");
            return null;
        });
    }
    @FXML private void loadLogs() {
        logs.clear();
        authService.getLogs().thenAccept(array -> Platform.runLater(() -> {
            if (array != null) {
                logs.addAll(array.getAsJsonArray().asList().stream()
                        .map(JsonElement::getAsJsonObject)
                        .toList());
            }
            logsListTable.refresh();
        })).exceptionally(ex -> {
            handleError(ex, "логов");
            return null;
        });
    }

    private void handleError(Throwable ex, String what) {
        Platform.runLater(() -> {
            statusLabelUsers.setText("Ошибка загрузки " + what);
            statusLabelUsers.setStyle("-fx-text-fill: red;");
            ex.printStackTrace();
        });
    }

    @FXML private void deleteCargo() {
        JsonObject selected = cargoTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Выберите груз для удаления", "orange");
            return;
        }

        authService.deleteCargo(selected.get("id").getAsInt())
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        cargos.remove(selected);
                        showStatus("Груз удалён", "green");
                    }
                }));
    }

    @FXML private void blockUser() {
        JsonObject user = userTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            showStatus("Выберите пользователя", "orange");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Блокировка");
        dialog.setHeaderText("Причина блокировки для " + safeString(user, "login"));
        dialog.showAndWait().ifPresent(reason -> {
            if (reason.trim().isEmpty()) reason = "Нарушение правил площадки";

            authService.blockUser(
                    user.get("id").getAsInt(),
                    safeString(user, "login"),
                    safeString(user, "phone"),
                    safeString(user, "email"),
                    reason,
                    Adminuser
            ).thenAccept(success -> Platform.runLater(() -> {
                if (success) {
                    user.addProperty("status", true);
                    userTable.refresh();
                    loadBlacklist();
                    showStatus("Пользователь заблокирован", "green");
                }
            }));
        });
    }

    @FXML private void unblockUser() {
        JsonObject blocked = blacklistTable.getSelectionModel().getSelectedItem();
        if (blocked == null) {
            showStatus("Выберите пользователя для разблокировки", "red");
            return;
        }
        String login = safeString(blocked, "login");
        authService.supabase.select("users", "id", "login=eq." + login)
                .thenCompose(result -> {
                    if (result == null || result.size() == 0) {
                        Platform.runLater(() -> showStatus("Пользователь не найден в базе", "red"));
                        return CompletableFuture.completedFuture(false);
                    }

                    int realUserId = result.get(0).getAsJsonObject().get("id").getAsInt();
                    return authService.unblockUser(realUserId, login, Adminuser);
                })
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        loadBlacklist();
                        loadUsers();
                        showStatus("Пользователь разблокирован", "green");
                    } else {
                        showStatus("Ошибка при разблокировке", "red");
                    }
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> showStatus("Ошибка: " + ex.getMessage(), "red"));
                    return null;
                });
    }
    @FXML private void deleteUser() {
        JsonObject user = userTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            showStatus("Выберите пользователя", "orange");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление");
        alert.setHeaderText("Удалить пользователя " + safeString(user, "login") + "?");
        alert.setContentText("Все его грузы тоже будут удалены!");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            authService.deleteUserAndCargo(user.get("id").getAsInt(), safeString(user, "login"))
                    .thenAccept(success -> Platform.runLater(() -> {
                        if (success) {
                            users.remove(user);
                            loadCargos();
                            showStatus("Пользователь и грузы удалены", "green");
                        }
                    }));
        }
    }
    @FXML
    private void editUser() {
        JsonObject selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showStatus("Выберите пользователя для редактирования", "orange");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/postgresql/EditUserDialog.fxml"));
            Parent root = loader.load();
            EditUserDialogController controller = loader.getController();
            controller.setUser(selectedUser, () -> {
                loadUsers();
                showStatus("Пользователь обновлён", "green");
            });

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редактирование пользователя");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.initOwner(userTable.getScene().getWindow());
            dialogStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Ошибка: не удалось открыть редактирование", "red");
        }
    }

    private void showStatus(String text, String color) {
        statusLabelUsers.setText(text);
        statusLabelUsers.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        new Timeline(new KeyFrame(Duration.seconds(10), e -> statusLabelUsers.setText(""))).play();
    }

    @FXML private void goBack() throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("main.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) cargoTable.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Авторизация");
        stage.centerOnScreen();
    }

    public static void AdminPanel(Stage stage, String user, String password) throws IOException {
        FXMLLoader loader = new FXMLLoader(AdminPanelController.class.getResource("/com/example/postgresql/AdminPanel.fxml"));
        Scene scene = new Scene(loader.load());
        AdminPanelController ctrl = loader.getController();
        Adminuser = user;
        ctrl.LabelUser.setText(user);
        stage.setScene(scene);
        stage.setTitle("Админ-панель");
        stage.centerOnScreen();
        stage.show();
    }

    private void TabHidePunktire() {
        adminTabPane.setFocusTraversable(false);
        adminTabPane.getTabs().forEach(tab -> tab.setClosable(false));
    }}