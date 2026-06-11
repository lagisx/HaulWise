package com.example.postgresql.controllers;

import com.example.postgresql.API.SupabaseClient;
import com.example.postgresql.controllers.CardControllers.GuestCargoCardController;
import com.example.postgresql.utils.CargoImageLoader;
import com.example.postgresql.HelloApplication;
import com.example.postgresql.UserF.Cargo;
import com.example.postgresql.utils.MapManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuestPanelController {

    @FXML
    private VBox cargoContainer;

    @FXML
    private TextField fromFilter;
    @FXML
    private TextField toFilter;
    @FXML
    private TextField minWeightFilter;
    @FXML
    private TextField maxWeightFilter;
    @FXML
    private Button applyFilterButton;

    private final SupabaseClient supabase = new SupabaseClient();

    private JsonArray allCargos;

    public void initialize() {
        loadCargos();

        if (applyFilterButton != null) {
            applyFilterButton.setOnAction(e -> applyFilters());
        }
    }

    private void loadCargos() {
        cargoContainer.getChildren().clear();
        cargoContainer.getChildren().add(loadingLabel());

        supabase.select("cargo", "*", null)
                .thenAccept(json -> Platform.runLater(() -> {
                    allCargos = json;
                    showCargos(json);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> error("Ошибка загрузки: " + ex.getMessage()));
                    return null;
                });
    }

    private void showCargos(JsonArray array) {
        cargoContainer.getChildren().clear();

        List<Cargo> cargos = parse(array);

        Label count = new Label("Найдено " + cargos.size() + " грузов");
        count.setStyle("-fx-font-weight: bold; -fx-font-size: 17; -fx-padding: 15 0 10 15; -fx-text-fill: black");
        cargoContainer.getChildren().add(count);

        if (cargos.isEmpty()) {
            cargoContainer.getChildren().add(emptyLabel());
            return;
        }

        for (Cargo c : cargos) {
            addCargoCard(c);
        }
    }

    private void applyFilters() {
        if (allCargos == null) return;

        String from = fromFilter.getText().trim().toLowerCase();
        String to = toFilter.getText().trim().toLowerCase();
        double minWeight = parseDouble(minWeightFilter.getText(), 0);
        double maxWeight = parseDouble(maxWeightFilter.getText(), Double.MAX_VALUE);

        List<Cargo> filtered = new ArrayList<>();

        for (Cargo c : parse(allCargos)) {
            boolean matches = true;

            if (!from.isEmpty() && !c.getFromCity().toLowerCase().contains(from)) matches = false;
            if (!to.isEmpty() && !c.getToCity().toLowerCase().contains(to)) matches = false;
            if (c.getWeight() < minWeight || c.getWeight() > maxWeight) matches = false;

            if (matches) filtered.add(c);
        }

        cargoContainer.getChildren().clear();
        Label count = new Label("Найдено " + filtered.size() + " грузов");
        count.setStyle("-fx-font-weight: bold; -fx-font-size: 17; -fx-padding: 15 0 10 15; -fx-text-fill: black");
        cargoContainer.getChildren().add(count);

        if (filtered.isEmpty()) {
            cargoContainer.getChildren().add(emptyLabel());
        } else {
            for (Cargo c : filtered) {
                addCargoCard(c);
            }
        }
    }

    private double parseDouble(String text, double defaultValue) {
        if (text == null || text.trim().isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void addCargoCard(Cargo c) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("CargoCard/GuestCargoCard.fxml"));
            AnchorPane card = loader.load();
            GuestCargoCardController ctrl = loader.getController();

            ctrl.typeLabel.setText("RUS • " + nullToEmpty(c.getVehicleType()));
            ctrl.vesObemLabel.setText(String.format("%.0f т / %.0f м³", c.getWeight(), c.getVolume()));
            ctrl.tovarLabel.setText(nullToEmpty(c.getProduct()));
            ctrl.zagruzValue.setText(nullToEmpty(c.getLoadType()));
            ctrl.dateValue.setText(nullToEmpty(c.getDate()));
            ctrl.cargoTypeLabel.setText(nullToEmpty(c.getLoadDetails()));
            CargoImageLoader.loadRandom(ctrl.randomImageOnCard);

            String from = nullToEmpty(c.getFromCity()).trim();
            String to = nullToEmpty(c.getToCity()).trim();
            ctrl.routeLabel.setText(from + " → " + to);
            ctrl.routeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-cursor: hand;");

            MapManager.getInstance().showOnClick(ctrl.routeLabel, from, to);

            ctrl.priceHidden.setText("Скрыто");
            ctrl.contactLabel.setText("Войдите, чтобы увидеть контакты");

            cargoContainer.getChildren().add(card);
        } catch (IOException e) {
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private List<Cargo> parse(JsonArray a) {
        List<Cargo> list = new ArrayList<>();
        if (a == null) return list;
        for (JsonElement e : a) {
            try {
                JsonObject o = e.getAsJsonObject();
                list.add(new Cargo(
                        o.get("id").getAsInt(),
                        s(o, "ТипТС"), d(o, "Вес"), d(o, "Объем"),
                        s(o, "Товар"), s(o, "Откуда"), s(o, "Куда"),
                        s(o, "ТипПогрузки"), s(o, "ДеталиПогрузки"),
                        s(o, "Даты"), d(o, "ЦенаПоКарте"), d(o, "ЦенаНДС"),
                        s(o, "Торг_без_торга"), s(o, "КонтактныйТелефон"),
                        o.has("заказчик_id") && !o.get("заказчик_id").isJsonNull()
                                ? o.get("заказчик_id").getAsInt() : 0));
            } catch (Exception ex) {
                System.err.println("Ошибка парсинга груза: " + ex.getMessage());
            }
        }
        return list;
    }

    private String s(JsonObject o, String k) {
        var e = o.get(k);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    private double d(JsonObject o, String k) {
        var e = o.get(k);
        return e == null || e.isJsonNull() ? 0 : e.getAsDouble();
    }

    private Label loadingLabel() {
        Label l = new Label("Загрузка грузов...");
        l.setStyle("-fx-padding: 30; -fx-font-size: 16; -fx-text-fill: #666;");
        return l;
    }

    private Label emptyLabel() {
        Label l = new Label("Грузы не найдены по вашему запросу");
        l.setStyle("-fx-padding: 40; -fx-font-size: 16; -fx-text-fill: #999;");
        return l;
    }

    private void error(String msg) {
        cargoContainer.getChildren().clear();
        Label l = new Label("Ошибка загрузки: " + msg);
        l.setStyle("-fx-text-fill: red; -fx-padding: 30;");
        cargoContainer.getChildren().add(l);
    }

    @FXML
    private void goBack() throws IOException {
        switchTo("main.fxml", "Авторизация");
    }

    @FXML
    private void goAutoriz() throws IOException {
        switchTo("main.fxml", "Авторизация");
    }

    @FXML
    private void goReg() throws IOException {
        switchTo("reg.fxml", "Регистрация");
    }

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
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.show();
    }
}