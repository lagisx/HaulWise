package com.example.postgresql.Controllers;

import com.example.postgresql.API.AuthService;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AddCargoDialogController {

    @FXML private TextField typeTS, weight, volume, product, loadingType, priceCard, priceNDS, details;
    @FXML private ComboBox<String> from, to, trade;
    @FXML private DatePicker dateFrom, dateTo;
    @FXML private Label statusLabel;

    private final AuthService authService = new AuthService();
    private String currentUser;
    private String userPhone = "";
    private Runnable onSuccess;

    private final ObservableList<String> cities = FXCollections.observableArrayList(
            "Москва",
            "Санкт-Петербург",
            "Екатеринбург",
            "Новосибирск",
            "Казань",
            "Нижний Новгород",
            "Челябинск",
            "Красноярск",
            "Самара",
            "Уфа",
            "Ростов-на-Дону",
            "Омск",
            "Краснодар",
            "Воронеж",
            "Пермь",
            "Волгоград",
            "Саратов",
            "Тюмень",
            "Тольятти",
            "Ижевск",
            "Барнаул",
            "Ульяновск",
            "Иркутск",
            "Хабаровск",
            "Ярославль",
            "Махачкала",
            "Владивосток",
            "Оренбург",
            "Томск",
            "Кемерово"
    );

    @FXML
    private void initialize() {
        from.setItems(cities);
        to.setItems(cities);
        from.setEditable(true);
        to.setEditable(true);

        setupAutoComplete(from);
        setupAutoComplete(to);

        // Запрет одинаковых городов — реагируем и на выбор, и на ввод текста
        from.valueProperty().addListener((obs, oldVal, newVal) -> preventSameCities(true));
        to.valueProperty().addListener((obs, oldVal, newVal) -> preventSameCities(false));

        from.getEditor().textProperty().addListener((obs, oldText, newText) -> preventSameCities(true));
        to.getEditor().textProperty().addListener((obs, oldText, newText) -> preventSameCities(false));

        priceCard.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                priceNDS.setText("");
                return;
            }
            String digits = newValue.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                priceNDS.setText("");
                return;
            }
            try {
                double price = Double.parseDouble(digits);
                double withNds = price * 1.2;
                priceNDS.setText(String.format("%,.0f", withNds));
            } catch (Exception e) {
                priceNDS.setText("Ошибка");
            }
        });

        dateFrom.setValue(LocalDate.now());
        dateTo.setValue(LocalDate.now().plusDays(7));
    }

    private void setupAutoComplete(ComboBox<String> comboBox) {
        TextField editor = comboBox.getEditor();

        editor.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.trim().isEmpty()) {
                comboBox.hide();
                comboBox.setItems(cities);
                return;
            }

            String lower = newText.toLowerCase();
            ObservableList<String> filtered = cities.filtered(city ->
                    city.toLowerCase().contains(lower));

            if (!filtered.isEmpty()) {
                comboBox.setItems(filtered);
                comboBox.show();
            } else {
                comboBox.hide();
            }
        });

        comboBox.setOnAction(e -> {
            if (comboBox.getValue() != null) {
                editor.setText(comboBox.getValue());
            }
            comboBox.hide();
        });

        editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                comboBox.hide();
            }
        });
    }

    // Метод, который не даёт полям "Откуда" и "Куда" быть одинаковыми
    private void preventSameCities(boolean changedFrom) {
        String fromCity = from.getEditor().getText().trim();
        String toCity   = to.getEditor().getText().trim();

        if (!fromCity.isEmpty() && !toCity.isEmpty()
                && fromCity.equalsIgnoreCase(toCity)) {

            if (changedFrom) {
                from.getEditor().clear();
                from.setValue(null);
                showStatus("Города 'Откуда' и 'Куда' не могут совпадать", "#ef4444");
            } else {
                to.getEditor().clear();
                to.setValue(null);
                showStatus("Города 'Откуда' и 'Куда' не могут совпадать", "#ef4444");
            }
        }
    }

    public void setUser(String username, String phone, Runnable callback) {
        this.currentUser = username;
        this.userPhone = phone.isEmpty() ? "Не указан" : phone;
        this.onSuccess = callback;
    }

    @FXML
    private void onSave() {
        String error = "";

        if (typeTS.getText().trim().isEmpty()) error += "• Тип ТС\n";
        if (loadingType.getText().trim().isEmpty()) error += "• Тип погрузки\n";
        if (weight.getText().trim().isEmpty()) error += "• Вес\n";
        if (volume.getText().trim().isEmpty()) error += "• Объём\n";
        if (product.getText().trim().isEmpty()) error += "• Товар\n";
        if (from.getEditor().getText().trim().isEmpty()) error += "• Откуда\n";
        if (to.getEditor().getText().trim().isEmpty()) error += "• Куда\n";
        if (dateFrom.getValue() == null) error += "• Дата погрузки (с)\n";
        if (priceCard.getText().trim().isEmpty()) error += "• Цена без НДС\n";
        if (trade.getValue() == null) error += "• Торг\n";

        String fromCity = from.getEditor().getText().trim();
        String toCity   = to.getEditor().getText().trim();

        // Дополнительная проверка на всякий случай при сохранении
        if (!fromCity.isEmpty() && !toCity.isEmpty()
                && fromCity.equalsIgnoreCase(toCity)) {
            error += "• Откуда и Куда не могут быть одним городом!\n";
        }

        if (!error.isEmpty()) {
            showStatus("Исправьте ошибки:\n" + error, "#ef4444");
            return;
        }

        JsonObject cargo = new JsonObject();
        cargo.addProperty("ТипТС", typeTS.getText().trim());
        cargo.addProperty("Вес", getNumber(weight.getText()));
        cargo.addProperty("Объем", getNumber(volume.getText()));
        cargo.addProperty("Товар", product.getText().trim());
        cargo.addProperty("Откуда", fromCity);
        cargo.addProperty("Куда", toCity);
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

        authService.supabase.select("users", "id",
                        "login=eq." + URLEncoder.encode(currentUser, StandardCharsets.UTF_8))
                .thenCompose(result -> {
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
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            showStatus("Ошибка: " + ex.getMessage(), "#ef4444"));
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
        new Timeline(new KeyFrame(javafx.util.Duration.seconds(8),
                e -> statusLabel.setText(""))).play();
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
