package com.example.postgresql.Controllers;

import com.example.postgresql.API.AuthService;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AddCargoDialogController {

    @FXML private TextField typeTS, weight, volume, product, from, to, loadingType, priceCard, priceNDS, details;
    @FXML private ComboBox<String> trade;
    @FXML private DatePicker dateFrom, dateTo;
    @FXML private Label statusLabel;

    private final AuthService authService = new AuthService();
    private String currentUser;
    private String userPhone = "";
    private Runnable onSuccess;
    @FXML
    private void initialize() {
        priceCard.textProperty().addListener((obs, old, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                priceNDS.setText("");
                return;
            }
            String numbersOnly = newValue.replaceAll("[^0-9]", "");
            if (numbersOnly.isEmpty()) {
                priceNDS.setText("");
                return;
            }
            try {
                double price = Double.parseDouble(numbersOnly);
                double withNds = price * 1.2;
                priceNDS.setText(String.format("%,.0f", withNds));
            } catch (Exception e) {
                priceNDS.setText("Ошибка");
            }
        });

        dateFrom.setValue(LocalDate.now());
        dateTo.setValue(LocalDate.now().plusDays(7));
    }

    public void setUser(String username, String phone, Runnable callback) {
        this.currentUser = username;
        this.userPhone = phone.isEmpty() ? "Не указан" : phone;
        this.onSuccess = callback;
    }

    @FXML
    private void onSave() {
        String error = "";

        if (typeTS.getText().trim().isEmpty())
            error += "• Тип ТС\n";
        if (loadingType.getText().trim().isEmpty())
            error += "• Тип погрузки\n";
        if (weight.getText().trim().isEmpty())
            error += "• Вес\n";
        if (volume.getText().trim().isEmpty())
            error += "• Объём\n";
        if (product.getText().trim().isEmpty())
            error += "• Товар\n";
        if (from.getText().trim().isEmpty())
            error += "• Откуда\n";
        if (to.getText().trim().isEmpty())
            error += "• Куда\n";
        if (dateFrom.getValue() == null)
            error += "• Дата погрузки (с)\n";
        if (priceCard.getText().trim().isEmpty())
            error += "• Цена без НДС\n";
        if (trade.getValue() == null)
            error += "• Торг\n";

        if (!error.isEmpty()) {
            showStatus("Заполните обязательные поля:\n" + error, "#ef4444");
            return;
        }
        JsonObject cargo = new JsonObject();
        cargo.addProperty("ТипТС", typeTS.getText().trim());
        cargo.addProperty("Вес", getNumber(weight.getText()));
        cargo.addProperty("Объем", getNumber(volume.getText()));
        cargo.addProperty("Товар", product.getText().trim());
        cargo.addProperty("Откуда", from.getText().trim());
        cargo.addProperty("Куда", to.getText().trim());
        cargo.addProperty("ТипПогрузки", loadingType.getText().trim());
        String datesText = dateFrom.getValue().format(DateTimeFormatter.ofPattern("dd.MM"));
        if (dateTo.getValue() != null && !dateTo.getValue().equals(dateFrom.getValue())) {
            datesText += " – " + dateTo.getValue().format(DateTimeFormatter.ofPattern("dd.MM"));
        }
        cargo.addProperty("Даты", datesText);

        cargo.addProperty("ДеталиПогрузки", details.getText().trim());
        cargo.addProperty("ЦенаПоКарте", getNumber(priceCard.getText()));
        cargo.addProperty("ЦенаНДС", getNumber(priceNDS.getText()));
        cargo.addProperty("Торг_без_торга", trade.getValue());
        cargo.addProperty("КонтактныйТелефон", userPhone);

        showStatus("Публикация груза...", "#6366f1");
        authService.supabase.select("users", "id", "login=eq." + URLEncoder.encode(currentUser, StandardCharsets.UTF_8)).thenCompose(result -> {
                    if (result.isEmpty()) throw new RuntimeException("Пользователь не найден");
                    int userId = result.get(0).getAsJsonObject().get("id").getAsInt();
                    cargo.addProperty("заказчик_id", userId);
                    return authService.supabase.insert("gruz", cargo);
            })
                .thenAccept(v -> Platform.runLater(() -> {
                    showStatus("Груз опубликован!", "#16a34a");
                    new Timeline(new KeyFrame(javafx.util.Duration.seconds(1.5), e -> {
                        if (onSuccess != null) onSuccess.run();
                        closeWindow();
                    })).play();
                }))
                .exceptionally(ex -> {Platform.runLater(() -> showStatus("Ошибка: " + ex.getMessage(), "#ef4444"));
                    return null;
                });
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) typeTS.getScene().getWindow();
        if (stage != null) stage.close();
    }

    private void showStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 15px;");
        new Timeline(new KeyFrame(javafx.util.Duration.seconds(8), e -> statusLabel.setText(""))).play();
    }

    private double getNumber(String text) {
        if (text == null || text.isEmpty()) return 0;
        String numbers = text.replaceAll("[^0-9]", "");
        try {
            return numbers.isEmpty() ? 0 : Double.parseDouble(numbers);
        } catch (Exception e) {
            return 0;
        }
    }
}