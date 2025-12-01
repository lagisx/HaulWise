package com.example.postgresql.utils;

import com.example.postgresql.API.RouteService;
import com.google.gson.*;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MapManager {
    private static final MapManager INSTANCE = new MapManager();
    public static MapManager getInstance() { return INSTANCE; }

    private final Gson gson = new Gson();
    private final ConcurrentHashMap<String, List<double[]>> routeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, double[]> cityCache = new ConcurrentHashMap<>();

    private final Stage mapStage;
    private final WebView webView;
    private final WebEngine engine;
    private boolean mapReady = false;

    private MapManager() {
        mapStage = new Stage(StageStyle.UNDECORATED);
        mapStage.setWidth(940);
        mapStage.setHeight(680);
        mapStage.setAlwaysOnTop(true);

        webView = new WebView();
        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        Label closeBtn = new Label("×");
        closeBtn.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ef4444; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-cursor: hand;"));
        closeBtn.setOnMouseClicked(e -> mapStage.hide());

        Label titleLabel = new Label("Маршрут на карте");
        titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleBar = new HBox(20, titleLabel, spacer, closeBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(12, 25, 12, 25));
        titleBar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        VBox rootBox = new VBox(titleBar, webView);
        rootBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 40, 0.4, 0, 15); -fx-background-radius: 20;");
        webView.setStyle("-fx-background-radius: 0 0 20 20;");

        rootBox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getTarget() == rootBox) {
                mapStage.hide();
            }
        });
        webView.setOnMouseClicked(e -> e.consume());

        mapStage.setScene(new javafx.scene.Scene(rootBox));

        Platform.runLater(() -> {
            var url = getClass().getResource("/map.html");
            if (url != null) {
                engine.load(url.toExternalForm());
            } else {
                System.err.println("ОШИБКА: map.html не найден!");
            }
        });

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                mapReady = true;
                System.out.println("Карта загружена и готова!");
            }
        });
    }

    public void showOnClick(Label routeLabel, String fromCity, String toCity) {
        routeLabel.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;

            String cacheKey = fromCity.trim() + "→" + toCity.trim();

            if (routeCache.containsKey(cacheKey)) {
                drawRoute(routeCache.get(cacheKey), fromCity, toCity);
                return;
            }

            getCoordsAsync(fromCity).thenCombine(
                    getCoordsAsync(toCity),
                    (fromCoords, toCoords) -> {
                        if (fromCoords == null || toCoords == null) return null;

                        final List<double[]>[] resultHolder = new List[1];
                        RouteService.getRouteCoordsYandex(
                                fromCoords[0], fromCoords[1],
                                toCoords[0],   toCoords[1],
                                route -> resultHolder[0] = route
                        );
                        return resultHolder[0];
                    }
            ).thenAccept(route -> {
                if (route != null && !route.isEmpty()) {
                    routeCache.put(cacheKey, route);
                    Platform.runLater(() -> drawRoute(route, fromCity, toCity));
                }
            });
        });
    }

    private CompletableFuture<double[]> getCoordsAsync(String cityName) {
        String key = cityName.trim().toLowerCase();
        if (key.isEmpty()) {
            return CompletableFuture.completedFuture(new double[]{37.6173, 55.7558});
        }

        if (cityCache.containsKey(key)) {
            return CompletableFuture.completedFuture(cityCache.get(key));
        }

        double[] known = getPopularCityCoords(key);
        if (known != null) {
            cityCache.put(key, known);
            return CompletableFuture.completedFuture(known);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = URLEncoder.encode(cityName + ", Россия", StandardCharsets.UTF_8);
                String url = "https://geocode-maps.yandex.ru/1.x/?format=json&geocode=" + query + "&results=1";

                String response = java.net.http.HttpClient.newHttpClient()
                        .send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build(),
                                java.net.http.HttpResponse.BodyHandlers.ofString())
                        .body();

                JsonObject json = gson.fromJson(response, JsonObject.class);
                JsonArray features = json
                        .getAsJsonObject("response")
                        .getAsJsonObject("GeoObjectCollection")
                        .getAsJsonArray("featureMember");

                if (!features.isEmpty()) {
                    String pos = features.get(0).getAsJsonObject()
                            .getAsJsonObject("GeoObject")
                            .getAsJsonObject("Point")
                            .get("pos").getAsString();

                    String[] parts = pos.split(" ");
                    double[] coords = {Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
                    cityCache.put(key, coords);
                    return coords;
                }
            } catch (Exception e) {
                System.out.println("Геокодер не нашёл: " + cityName);
            }
            return new double[]{37.6173, 55.7558};
        });
    }

    private double[] getPopularCityCoords(String city) {
        return switch (city.toLowerCase()) {
            case "москва" -> new double[]{37.6173, 55.7558};
            case "санкт-петербург", "петербург" -> new double[]{30.3351, 59.9343};
            case "екатеринбург" -> new double[]{60.6057, 56.8389};
            case "новосибирск" -> new double[]{82.9346, 55.0415};
            case "казань" -> new double[]{49.0661, 55.8304};
            case "нижний новгород" -> new double[]{44.0059, 56.3269};
            case "челябинск" -> new double[]{61.4026, 55.1644};
            case "красноярск" -> new double[]{92.8932, 56.0153};
            case "самара" -> new double[]{50.2212, 53.2415};
            case "уфа" -> new double[]{55.9721, 54.7351};
            case "ростов-на-дону", "ростов" -> new double[]{39.7015, 47.2357};
            case "омск" -> new double[]{73.3682, 54.9884};
            case "краснодар" -> new double[]{38.9760, 45.0355};
            case "воронеж" -> new double[]{39.2074, 51.6608};
            case "пермь" -> new double[]{56.2294, 58.0105};
            case "волгоград" -> new double[]{44.5133, 48.7080};
            case "саратов" -> new double[]{46.0347, 51.5331};
            case "тюмень" -> new double[]{65.5343, 57.1530};
            case "тольятти" -> new double[]{49.4181, 53.5089};
            case "ижевск" -> new double[]{53.2055, 56.8526};
            case "барнаул" -> new double[]{83.7636, 53.3548};
            case "ульяновск" -> new double[]{48.3946, 54.3142};
            case "иркутск" -> new double[]{104.3050, 52.2864};
            case "хабаровск" -> new double[]{135.0719, 48.4800};
            case "ярославль" -> new double[]{39.8758, 57.6261};
            case "махачкала" -> new double[]{47.5047, 42.9830};
            case "владивосток" -> new double[]{131.9113, 43.1332};
            case "оренбург" -> new double[]{55.1006, 51.7682};
            case "томск" -> new double[]{84.9744, 56.4846};
            case "кемерово" -> new double[]{86.0884, 55.3552};
            default -> null;
        };
    }

    private void drawRoute(List<double[]> coordinates, String fromCity, String toCity) {
        if (!mapReady || coordinates == null || coordinates.size() < 2) return;

        String json = gson.toJson(coordinates);
        String js = """
        if (typeof showRoute === 'function') {
            showRoute(%s, '%s', '%s');
        }
        """.formatted(json, fromCity.replace("'", "\\'"), toCity.replace("'", "\\'"));

        Platform.runLater(() -> {
            try {
                engine.executeScript("if (typeof clearMap === 'function') clearMap();");
                Thread.sleep(50);
                engine.executeScript(js);
            } catch (Exception ignored) {}

            if (!mapStage.isShowing()) {
                Stage main = webView.getScene() != null ? (Stage) webView.getScene().getWindow() : null;
                if (main != null) {
                    mapStage.setX(main.getX() + (main.getWidth() - mapStage.getWidth()) / 2);
                    mapStage.setY(main.getY() + (main.getHeight() - mapStage.getHeight()) / 2);
                }
                mapStage.show();
                mapStage.toFront();
            }
        });
    }
}