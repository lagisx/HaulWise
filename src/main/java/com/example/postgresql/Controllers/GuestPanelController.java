package com.example.postgresql.Controllers;

import com.example.postgresql.API.SupabaseClient;
import com.example.postgresql.Controllers.CardControllers.GuestCargoCardController;
import com.example.postgresql.HelloApplication;
import com.example.postgresql.UserF.Cargo;
import com.example.postgresql.utils.MapManager;
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

public class GuestPanelController {

    @FXML private VBox cargoContainer;
    private final SupabaseClient supabase = new SupabaseClient();
    private final Random random = new Random();

    public void initialize() {
        loadCargos();
    }

    private void loadCargos() {
        cargoContainer.getChildren().clear();
        cargoContainer.getChildren().add(loadingLabel());

        supabase.select("gruz", "*", null)
                .thenAccept(json -> Platform.runLater(() -> showCargos(json)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> error(ex.getMessage()));
                    return null;
                });
    }

    private void showCargos(JsonArray array) {
        cargoContainer.getChildren().clear();

        List<Cargo> cargos = parse(array);
        Label count = new Label("Найдено " + cargos.size() + " грузов");
        count.setStyle("-fx-font-weight: bold; -fx-font-size: 17; -fx-padding: 15 0 10 15; -fx-text-fill: #1d4ed8;");
        cargoContainer.getChildren().add(count);

        if (cargos.isEmpty()) {
            cargoContainer.getChildren().add(emptyLabel());
            return;
        }

        for (Cargo c : cargos) {
            try {
                FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("CargoCard/GuestCargoCard.fxml"));
                AnchorPane card = loader.load();
                GuestCargoCardController ctrl = loader.getController();

                ctrl.typeLabel.setText("RUS • " + nullToEmpty(c.getTypeTC()));
                ctrl.vesObemLabel.setText(String.format("%.0f т / %.0f м³", c.getWeight(), c.getVolume()));
                ctrl.tovarLabel.setText(nullToEmpty(c.getProduct()));
                ctrl.zagruzValue.setText(nullToEmpty(c.getLoadType()));
                ctrl.dateValue.setText(nullToEmpty(c.getDate()));
                ctrl.cargoTypeLabel.setText(nullToEmpty(c.getLoadDetails()));
                ctrl.randomImageOnCard.setImage(randomImage());

                String from = nullToEmpty(c.getFrom()).trim();
                String to = nullToEmpty(c.getTo()).trim();
                ctrl.routeLabel.setText(from + " → " + to);

                // Стиль и курсор
                ctrl.routeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-cursor: hand;");

                // ПРАВИЛЬНАЯ ПРИВЯЗКА КАРТЫ К НАСТОЯЩЕМУ ЛЕЙБЛУ
                MapManager.getInstance().showOnClick(ctrl.routeLabel, from, to);

                ctrl.priceHidden.setText("Скрыто");
                ctrl.contactLabel.setText("Войдите, чтобы увидеть контакты");

                cargoContainer.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }

    private List<Cargo> parse(JsonArray a) {
        List<Cargo> list = new ArrayList<>();
        if (a == null) return list;
        for (JsonElement e : a) {
            try {
                var o = e.getAsJsonObject();
                list.add(new Cargo(
                        o.get("id").getAsInt(),
                        s(o, "ТипТС"), d(o, "Вес"), d(o, "Объем"),
                        s(o, "Товар"), s(o, "Откуда"), s(o, "Куда"),
                        s(o, "ТипПогрузки"), s(o, "ДеталиПогрузки"),
                        s(o, "Даты"), d(o, "ЦенаКарточка"), d(o, "ЦенаНДС"),
                        s(o, "Торг"), s(o, "Контакт"), s(o, "owner_login")
                ));
            } catch (Exception ignored) {}
        }
        return list;
    }

    private String s(com.google.gson.JsonObject o, String k) {
        var e = o.get(k); return e == null || e.isJsonNull() ? "" : e.getAsString();
    }
    private double d(com.google.gson.JsonObject o, String k) {
        var e = o.get(k); return e == null || e.isJsonNull() ? 0 : e.getAsDouble();
    }

    private Label loadingLabel() {
        Label l = new Label("Загрузка грузов...");
        l.setStyle("-fx-padding: 30; -fx-font-size: 16; -fx-text-fill: #666;");
        return l;
    }

    private Label emptyLabel() {
        Label l = new Label("Пока нет доступных грузов");
        l.setStyle("-fx-padding: 40; -fx-font-size: 16; -fx-text-fill: #999;");
        return l;
    }

    private void error(String msg) {
        cargoContainer.getChildren().clear();
        Label l = new Label("Ошибка: " + msg);
        l.setStyle("-fx-text-fill: red; -fx-padding: 30;");
        cargoContainer.getChildren().add(l);
    }

    private Image randomImage() {
        String[] imgs = {"/images/BoxImage.png", "/images/CarTwo.png", "/images/CarThreeBox.png",
                "/images/CarFour.png", "/images/Kran.png", "/images/CargoImage.png",
                "/images/ThreeCargo.png", "/images/Pallet.png"};
        return new Image(getClass().getResourceAsStream(imgs[random.nextInt(imgs.length)]));
    }

    @FXML private void goBack() throws IOException { switchTo("main.fxml", "Авторизация"); }
    @FXML private void goAutoriz() throws IOException { switchTo("main.fxml", "Авторизация"); }
    @FXML private void goReg() throws IOException { switchTo("reg.fxml", "Регистрация"); }

    private void switchTo(String fxml, String title) throws IOException {
        Parent root = FXMLLoader.load(HelloApplication.class.getResource(fxml));
        Stage s = (Stage) cargoContainer.getScene().getWindow();
        s.setScene(new Scene(root));
        s.setTitle(title);
        s.centerOnScreen();
    }

    public static void GuestPanel(Stage stage) throws IOException {
        FXMLLoader l = new FXMLLoader(HelloApplication.class.getResource("GuestPanel.fxml"));
        stage.setScene(new Scene(l.load()));
        stage.setTitle("Гость • Поиск грузов");
        stage.centerOnScreen();
        stage.show();
    }
}