package com.example.postgresql.controllers;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class CargoCardController {

    @FXML
    private WebView mapWebView;

    @FXML
    private Label labelRoute;
    @FXML
    private Label labelProduct;
    @FXML
    private Label labelVehicleType;
    @FXML
    private Label labelWeight;
    @FXML
    private Label labelVolume;
    @FXML
    private Label labelLoadType;
    @FXML
    private Label labelDate;
    @FXML
    private Label labelPriceCard;
    @FXML
    private Label labelPriceNds;
    @FXML
    private Label labelBargain;
    @FXML
    private Label labelContact;
    @FXML
    private Label labelOwner;

    public void setCargo(JsonObject cargo, String ownerLogin) {
        String fromCity = getStr(cargo, "Откуда");
        String toCity = getStr(cargo, "Куда");
        labelRoute.setText(fromCity + "  →  " + toCity);
        labelProduct.setText(getStr(cargo, "Товар"));
        labelVehicleType.setText(getStr(cargo, "ТипТС"));
        labelWeight.setText(fmt(cargo, "Вес") + " т");
        labelVolume.setText(fmt(cargo, "Объем") + " м³");
        labelLoadType.setText(getStr(cargo, "ДеталиПогрузки"));
        labelDate.setText(getStr(cargo, "Даты"));
        labelPriceCard.setText(fmtRub(cargo, "ЦенаПоКарте"));
        labelPriceNds.setText(fmtRub(cargo, "ЦенаНДС"));
        labelBargain.setText(getStr(cargo, "Торг_без_торга"));
        labelContact.setText(getStr(cargo, "КонтактныйТелефон"));
        labelOwner.setText(ownerLogin.isEmpty() ? "—" : ownerLogin);
        initMap(fromCity, toCity);
    }

    private void initMap(String fromCity, String toCity) {
        WebEngine engine = mapWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        var url = getClass().getResource("/map.html");
        if (url == null) {
            return;
        }

        engine.load(url.toExternalForm());

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                String from = fromCity.replace("'", "\\'");
                String to = toCity.replace("'", "\\'");
                Platform.runLater(() ->
                        engine.executeScript("showRoute('" + from + "', '" + to + "');")
                );
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("[CargoCardController] Карта не загружена — map.html недоступен");
            }
        });
    }

    private String getStr(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return "—";
        return o.get(key).getAsString().trim();
    }

    private String fmt(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return "—";
        try {
            double v = o.get(key).getAsDouble();
            return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
        } catch (Exception e) {
            return "—";
        }
    }

    private String fmtRub(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return "—";
        try {
            long v = Math.round(o.get(key).getAsDouble());
            return String.format("%,d ₽", v).replace(',', ' ');
        } catch (Exception e) {
            return "—";
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) labelRoute.getScene().getWindow();
        stage.close();
    }
}