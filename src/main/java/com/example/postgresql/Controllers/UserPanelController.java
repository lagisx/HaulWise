package com.example.postgresql.Controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.Controllers.CardControllers.UserCargoCardController;
import com.example.postgresql.HelloApplication;
import com.example.postgresql.UserF.ProfileUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;

public class UserPanelController {

    @FXML private VBox cargoContainer;
    @FXML private VBox userCargoContainer;
    @FXML private Button btnAddCargo;
    @FXML private Label LabelUser;
    @FXML private TabPane tabPane;

    private String currentUser;
    private final AuthService authService = new AuthService();

    private static final String[] IMAGES = {
            "/images/BoxImage.png",
            "/images/CarTwo.png",
            "/images/CarThreeBox.png",
            "/images/CarFour.png",
            "/images/Kran.png",
            "/images/CargoImage.png",
            "/images/ThreeCargo.png",
            "/images/Pallet.png"
    };
    private static final Random random = new Random();

    public static Image getRandomCargoImage() {
        int idx = random.nextInt(IMAGES.length);
        return new Image(UserPanelController.class.getResourceAsStream(IMAGES[idx]));
    }

    @FXML
    private void initialize() {
        TabHidePunktire();
    }

    public void setUser(String login) {
        this.currentUser = login.toLowerCase().trim();
        LabelUser.setText(login);
        loadAllCargos();
        loadUserCargos();
    }

    private void loadAllCargos() {
        cargoContainer.getChildren().clear();
        showLoading(cargoContainer);

        authService.getAllCargos()
                .thenAccept(array -> Platform.runLater(() -> displayAllCargos(array)))
                .exceptionally(ex -> handleError(cargoContainer, ex));
    }

    private void displayAllCargos(JsonArray cargos) {
        cargoContainer.getChildren().clear();

        if (cargos == null || cargos.size() == 0) {
            cargoContainer.getChildren().add(createInfoLabel("Нет доступных грузов"));
            return;
        }

        Label count = new Label("Найдено " + cargos.size() + " грузов");
        count.setStyle("-fx-font-weight: bold; -fx-padding: 10 0;");
        cargoContainer.getChildren().add(count);

        for (JsonElement el : cargos) {
            JsonObject cargo = el.getAsJsonObject();
            addCargoCard(cargo, cargoContainer, false);
        }
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
        userCargoContainer.getChildren().removeIf(node ->
                node instanceof Label && ((Label) node).getText().contains("Загрузка"));

        Label count = new Label("Ваши грузы: " + (cargos != null ? cargos.size() : 0));
        count.setStyle("-fx-font-weight: bold; -fx-padding: 10 0;");
        userCargoContainer.getChildren().add(0, count);

        if (cargos == null || cargos.size() == 0) {
            userCargoContainer.getChildren().add(1, createInfoLabel("У вас пока нет добавленных грузов"));
            return;
        }

        for (JsonElement el : cargos) {
            JsonObject cargo = el.getAsJsonObject();
            addCargoCard(cargo, userCargoContainer, true);
        }
    }

    private void addCargoCard(JsonObject cargo, VBox container, boolean isOwner) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/postgresql/CargoCard/UserCargoCard.fxml"));
            AnchorPane card = loader.load();
            UserCargoCardController ctrl = loader.getController();

            ctrl.typeLabel.setText("RUS • " + getStr(cargo, "ТипТС"));
            ctrl.vesObemLabel.setText(format(cargo, "Вес") + " т. / " + format(cargo, "Объем") + " куб.");
            ctrl.tovarLabel.setText(getStr(cargo, "Товар"));
            ctrl.routeLabel.setText(getStr(cargo, "Откуда") + " → " + getStr(cargo, "Куда"));
            ctrl.zagruzValue.setText(getStr(cargo, "ТипПогрузки"));
            ctrl.dateValue.setText(getStr(cargo, "Даты"));
            ctrl.cargoTypeLabel.setText(getStr(cargo, "ДеталиПогрузки"));
            ctrl.randomImageOnCard.setImage(getRandomCargoImage());

            ctrl.priceKartaLabel.setText(formatRub(cargo, "ЦенаПоКарте"));
            ctrl.priceNDSLabel.setText(formatRub(cargo, "ЦенаНДС"));
            ctrl.tradeLabel.setText(getStr(cargo, "Торг_без_торга"));
            ctrl.contactLabel.setText(getStr(cargo, "КонтактныйТелефон"));

            if (isOwner) {
                ctrl.deleteLabel.setVisible(true);
                ctrl.deleteLabel.setManaged(true);
                int cargoId = cargo.get("id").getAsInt();
                ctrl.deleteLabel.setOnMouseClicked(e -> deleteCargo(cargoId, card));
            }

            container.getChildren().add(card);

        } catch (IOException e) {
            Platform.runLater(() -> showError("Ошибка загрузки карточки груза"));
            e.printStackTrace();
        }
    }

    private void deleteCargo(int cargoId, AnchorPane card) {
        authService.deleteCargo(cargoId)
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        userCargoContainer.getChildren().remove(card);
                        showSuccess("Груз успешно удалён");
                        loadAllCargos();
                    } else {
                        showError("Не удалось удалить груз");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Ошибка при удалении: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void AddUsersCargo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Добавление груза");
        alert.setHeaderText("Функция в разработке");
        alert.setContentText("Скоро здесь будет красивая форма для добавления груза!");
        alert.showAndWait();
    }

    @FXML
    private void profileClick() {
        authService.getUserProfile(currentUser)
                .thenAccept(array -> {
                    if (array != null && array.size() > 0) {
                        JsonObject user = array.get(0).getAsJsonObject();
                        String email = getStr(user, "email");
                        String phone = getStr(user, "phone");

                        Platform.runLater(() -> ProfileUser.profilePanel(
                                (Stage) LabelUser.getScene().getWindow(),
                                currentUser, "", email, phone, currentUser
                        ));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Не удалось загрузить профиль"));
                    return null;
                });
    }

    public static void UserPanel(Stage stage, String user, String password) throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("UserPanel.fxml"));
        Scene scene = new Scene(loader.load());
        UserPanelController controller = loader.getController();
        controller.setUser(user);

        stage.setScene(scene);
        stage.setTitle("Панель пользователя • " + user);
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    private void goBack() throws IOException {
        switchScene("main.fxml", "Авторизация");
    }

    private void switchScene(String fxml, String title) throws IOException {
        Parent root = FXMLLoader.load(HelloApplication.class.getResource(fxml));
        Stage stage = (Stage) LabelUser.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.centerOnScreen();
    }

    private void TabHidePunktire() {
        tabPane.getTabs().forEach(tab -> tab.setClosable(false));
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "—";
    }

    private String format(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return "0";
        return String.format("%.0f", obj.get(key).getAsDouble());
    }

    private String formatRub(JsonObject obj, String key) {
        return format(obj, key) + " руб";
    }

    private void showLoading(VBox box) {
        Label loading = new Label("Загрузка...");
        loading.setStyle("-fx-padding: 20; -fx-font-size: 16; -fx-text-fill: #666;");
        box.getChildren().add(loading);
    }

    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-padding: 20; -fx-font-size: 16; -fx-text-fill: #888;");
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

    private Void handleError(VBox box, Throwable ex) {
        Platform.runLater(() -> {
            box.getChildren().clear();
            box.getChildren().add(createInfoLabel("Ошибка загрузки: " + ex.getMessage()));
        });
        ex.printStackTrace();
        return null;
    }
}