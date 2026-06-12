package com.example.postgresql.utils;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MapManager {

    private static final MapManager INSTANCE = new MapManager();

    public static MapManager getInstance() {
        return INSTANCE;
    }

    private final Gson gson;
    private final Stage mapStage;
    private final WebView webView;
    private final WebEngine engine;
    private boolean mapReady = false;

    private MapManager() {
        gson = new Gson();

        mapStage = new Stage(StageStyle.UNDECORATED);
        mapStage.setWidth(960);
        mapStage.setHeight(700);
        mapStage.setAlwaysOnTop(true);

        webView = new WebView();
        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);


        Label closeBtn = new Label("×");
        closeBtn.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#64748b;-fx-cursor:hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#ef4444;-fx-cursor:hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#64748b;-fx-cursor:hand;"));
        closeBtn.setOnMouseClicked(e -> mapStage.hide());

        Label titleLabel = new Label("Маршрут на карте");
        titleLabel.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleBar = new HBox(20, titleLabel, spacer, closeBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(12, 25, 12, 25));
        titleBar.setStyle("-fx-background-color:#ffffff;-fx-border-color:#e2e8f0;-fx-border-width:0 0 1 0;");

        VBox rootBox = new VBox(titleBar, webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        rootBox.setStyle("-fx-background-color:white;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),40,0.4,0,15);" +
                "-fx-background-radius:20;");

        mapStage.setScene(new javafx.scene.Scene(rootBox));


        Platform.runLater(() -> {
            var url = getClass().getResource("/map.html");
            if (url != null) {
                engine.load(url.toExternalForm());
            } else {
                System.err.println("[MapManager] ОШИБКА: map.html не найден!");
            }
        });

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                mapReady = true;
            }
        });
    }


    public void showOnClick(Label routeLabel, String fromCity, String toCity) {
        if (fromCity == null || fromCity.isBlank() || toCity == null || toCity.isBlank()) return;

        String from = fromCity.trim();
        String to = toCity.trim();

        routeLabel.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            showRoute(from, to);
        });
    }


    public void showRoute(String fromCity, String toCity) {
        Platform.runLater(() -> {

            if (!mapStage.isShowing()) {
                centerOnScreen();
                mapStage.show();
                mapStage.toFront();
            }

            if (mapReady) {
                callShowRoute(fromCity, toCity);
            } else {

                engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                    if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                        Platform.runLater(() -> callShowRoute(fromCity, toCity));
                    }
                });
            }
        });
    }


    private void callShowRoute(String fromCity, String toCity) {

        String from = fromCity.replace("\\", "\\\\").replace("'", "\\'");
        String to = toCity.replace("\\", "\\\\").replace("'", "\\'");
        String js = "showRoute('" + from + "', '" + to + "');";

        System.out.println("Запрос маршрута: " + fromCity + " - " + toCity);

        try {
            engine.executeScript(js);
            System.out.println("маршрут передан в WebView успешно");
        } catch (Exception e) {
            System.err.println("JS error: " + e.getMessage());
        }
    }


    private void centerOnScreen() {

        javafx.geometry.Rectangle2D visualBounds = javafx.stage.Screen.getPrimary().getVisualBounds();


        double maxW = visualBounds.getWidth() - 40;
        double maxH = visualBounds.getHeight() - 40;

        double w = Math.min(mapStage.getWidth(), maxW);
        double h = Math.min(mapStage.getHeight(), maxH);
        mapStage.setWidth(w);
        mapStage.setHeight(h);


        mapStage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - w) / 2);
        mapStage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - h) / 2);
    }
}
