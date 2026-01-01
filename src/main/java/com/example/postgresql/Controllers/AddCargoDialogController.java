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
            "Москва", "Санкт-Петербург", "Екатеринбург", "Новосибирск", "Казань",
            "Нижний Новгород", "Челябинск", "Красноярск", "Самара", "Уфа", "Ростов-на-Дону",
            "Омск", "Краснодар", "Воронеж", "Пермь", "Волгоград", "Саратов", "Тюмень",
            "Тольятти", "Ижевск", "Барнаул",
            "Ульяновск", "Иркутск", "Хабаровск", "Ярославль", "Махачкала",
            "Владивосток", "Оренбург", "Томск", "Кемерово"
    );

    @FXML
    private void initialize() {
        from.setItems(cities);
        to.setItems(cities);
        from.setEditable(true);
        to.setEditable(true);

        setupAutoComplete(from);
        setupAutoComplete(to);

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
        String error = validateFields();
        if (!error.isEmpty()) {
            showStatus("Исправьте ошибки:\n" + error, "#ef4444");
            return;
        }

        JsonObject cargo = buildCargoObject();

        showStatus("Публикация груза...", "#6366f1");

        authService.supabase.select("users", "id",
                        "login=eq." + URLEncoder.encode(currentUser, StandardCharsets.UTF_8))
                .thenCompose(result -> {
                    if (result.isEmpty()) {
                        throw new RuntimeException("Пользователь не найден в системе");
                    }
                    int userId = result.get(0).getAsJsonObject().get("id").getAsInt();
                    cargo.addProperty("заказчик_id", userId);
                    return authService.supabase.insert("gruz", cargo);
                })
                .thenAccept(v -> Platform.runLater(() -> {
                    showStatus("Груз успешно опубликован!", "#16a34a");
                    new Timeline(new KeyFrame(javafx.util.Duration.seconds(1.5), e -> {
                        if (onSuccess != null) onSuccess.run();
                        closeWindow();
                    })).play();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String message = cause.getMessage() != null ? cause.getMessage() : "Неизвестная ошибка";

                        if (message.contains("409") ||
                                message.toLowerCase().contains("conflict") ||
                                message.contains("duplicate key value violates unique constraint")) {

                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Груз уже существует");
                            alert.setHeaderText("Нельзя добавить дублирующий груз");
                            alert.setContentText(
                                    "Вы уже опубликовали груз с такими же параметрами:\n" +
                                            "• Маршрут: " + cargo.get("Откуда").getAsString() + " → " + cargo.get("Куда").getAsString() + "\n" +
                                            "• Даты: " + cargo.get("Даты").getAsString() + "\n\n" +
                                            "Если нужно изменить — удалите существующий в разделе «Мои грузы»."
                            );
                            alert.getDialogPane().setPrefSize(520, 320);
                            alert.showAndWait();

                            showStatus("Дубликат не добавлен", "#f59e0b");
                        }
                        else {
                            showStatus("Ошибка публикации: " + message, "#ef4444");
                        }
                    });
                    return null;
                });
    }
    private String validateFields() {
        StringBuilder error = new StringBuilder();

        if (typeTS.getText().trim().isEmpty()) error.append("• Тип ТС\n");
        if (loadingType.getText().trim().isEmpty()) error.append("• Тип погрузки\n");
        if (weight.getText().trim().isEmpty()) error.append("• Вес\n");
        if (volume.getText().trim().isEmpty()) error.append("• Объём\n");
        if (product.getText().trim().isEmpty()) error.append("• Товар\n");
        if (from.getEditor().getText().trim().isEmpty()) error.append("• Откуда\n");
        if (to.getEditor().getText().trim().isEmpty()) error.append("• Куда\n");
        if (dateFrom.getValue() == null) error.append("• Дата погрузки (с)\n");
        if (priceCard.getText().trim().isEmpty()) error.append("• Цена без НДС\n");
        if (trade.getValue() == null) error.append("• Торг\n");

        String fromCity = from.getEditor().getText().trim();
        String toCity = to.getEditor().getText().trim();
        if (!fromCity.isEmpty() && !toCity.isEmpty() && fromCity.equalsIgnoreCase(toCity)) {
            error.append("• Города отправления и назначения не могут совпадать\n");
        }

        return error.toString();
    }

    private JsonObject buildCargoObject() {
        String fromCity = from.getEditor().getText().trim();
        String toCity = to.getEditor().getText().trim();

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

        cargo.addProperty("ДеталиПогрузки", details.getText().trim().isEmpty() ? "—" : details.getText().trim());
        cargo.addProperty("ЦенаПоКарте", getNumber(priceCard.getText()));
        cargo.addProperty("ЦенаНДС", getNumber(priceNDS.getText()));
        cargo.addProperty("Торг_без_торга", trade.getValue());
        cargo.addProperty("КонтактныйТелефон", userPhone);

        return cargo;
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
