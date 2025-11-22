package com.example.postgresql.Controllers;

import com.example.postgresql.API.SupabaseClient;
import com.example.postgresql.Controllers.CardControllers.GuestCargoCardController;
import com.example.postgresql.HelloApplication;
import com.example.postgresql.UserF.Cargo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class GuestPanelController {

    @FXML private VBox cargoContainer;

    private final SupabaseClient supabase = new SupabaseClient();
    private final Random random = new Random();

    public void initialize() {
        loadCargos();
    }

    private void loadCargos() {
        cargoContainer.getChildren().clear();
        cargoContainer.getChildren().add(createLoadingLabel());

        supabase.select("gruz", "*", null)
                .thenAccept(jsonArray -> Platform.runLater(() -> {
                    cargoContainer.getChildren().clear();

                    List<Cargo> cargos = parseCargos(jsonArray);
                    int count = cargos.size();

                    Label countLabel = new Label("Найдено " + count + " грузов");
                    countLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 15 0 10 15; -fx-text-fill: #333;");
                    cargoContainer.getChildren().add(countLabel);

                    if (count == 0) {
                        Label empty = new Label("Пока нет доступных грузов");
                        empty.setStyle("-fx-font-size: 14; -fx-text-fill: gray; -fx-padding: 20;");
                        cargoContainer.getChildren().add(empty);
                        return;
                    }

                    for (Cargo cargo : cargos) {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/postgresql/CargoCard/GuestCargoCard.fxml"));
                            AnchorPane card = loader.load();
                            GuestCargoCardController controller = loader.getController();

                            controller.typeLabel.setText("RUS • " + nullToEmpty(cargo.getTypeTC()));
                            controller.vesObemLabel.setText(String.format("%.0f т. / %.0f куб.", cargo.getWeight(), cargo.getVolume()));
                            controller.tovarLabel.setText(nullToEmpty(cargo.getProduct()));
                            controller.routeLabel.setText(nullToEmpty(cargo.getFrom()) + " → " + nullToEmpty(cargo.getTo()));
                            controller.zagruzValue.setText(nullToEmpty(cargo.getLoadType()));
                            controller.dateValue.setText(nullToEmpty(cargo.getDate()));
                            controller.cargoTypeLabel.setText(nullToEmpty(cargo.getLoadDetails()));
                            controller.randomImageOnCard.setImage(randomImageGuestCard());

                            controller.priceHidden.setText("Скрыто");
                            controller.contactLabel.setText("Войдите, чтобы увидеть данные");

                            cargoContainer.getChildren().add(card);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        cargoContainer.getChildren().clear();
                        Label error = new Label("Ошибка загрузки грузов: " + ex.getMessage());
                        error.setStyle("-fx-text-fill: red; -fx-padding: 20;");
                        cargoContainer.getChildren().add(error);
                    });
                    ex.printStackTrace();
                    return null;
                });
    }

    private List<Cargo> parseCargos(JsonArray array) {
        List<Cargo> list = new ArrayList<>();
        if (array == null) return list;

        for (JsonElement el : array) {
            try {
                var obj = el.getAsJsonObject();

                Cargo cargo = new Cargo(
                        obj.get("id").getAsInt(),
                        safeString(obj, "ТипТС"),
                        safeDouble(obj, "Вес"),
                        safeDouble(obj, "Объем"),
                        safeString(obj, "Товар"),
                        safeString(obj, "Откуда"),
                        safeString(obj, "Куда"),
                        safeString(obj, "ТипПогрузки"),
                        safeString(obj, "ДеталиПогрузки"),
                        safeString(obj, "Даты"),
                        safeDouble(obj, "ЦенаКарточка"),
                        safeDouble(obj, "ЦенаНДС"),
                        safeString(obj, "Торг"),
                        safeString(obj, "Контакт"),
                        safeString(obj, "owner_login")
                );
                list.add(cargo);
            } catch (Exception e) {
                System.err.println("Ошибка парсинга груза: " + e.getMessage());
            }
        }
        return list;
    }

    private String safeString(com.google.gson.JsonObject obj, String key) {
        var el = obj.get(key);
        return el == null || el.isJsonNull() ? "" : el.getAsString();
    }

    private double safeDouble(com.google.gson.JsonObject obj, String key) {
        var el = obj.get(key);
        if (el == null || el.isJsonNull()) return 0.0;
        try {
            return el.getAsDouble();
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private Label createLoadingLabel() {
        Label loading = new Label("Загрузка грузов...");
        loading.setStyle("-fx-font-size: 14; -fx-text-fill: gray; -fx-padding: 20;");
        return loading;
    }

    public Image randomImageGuestCard() {
        String[] images = {
                "/images/BoxImage.png",
                "/images/CarTwo.png",
                "/images/CarThreeBox.png",
                "/images/CarFour.png",
                "/images/Kran.png",
                "/images/CargoImage.png",
                "/images/ThreeCargo.png",
                "/images/Pallet.png"
        };
        int idx = random.nextInt(images.length);
        return new Image(getClass().getResourceAsStream(images[idx]));
    }

    @FXML private void goBack() throws IOException { switchTo("main.fxml", "Авторизация"); }
    @FXML private void goAutoriz() throws IOException { switchTo("main.fxml", "Авторизация"); }
    @FXML private void goReg() throws IOException { switchTo("reg.fxml", "Регистрация"); }

    private void switchTo(String fxml, String title) throws IOException {
        Parent root = FXMLLoader.load(HelloApplication.class.getResource(fxml));
        Stage stage = (Stage) cargoContainer.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.centerOnScreen();
        stage.show();
    }

    public static void GuestPanel(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("GuestPanel.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Панель гостя");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}