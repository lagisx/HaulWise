package com.example.postgresql.controllers;

import com.example.postgresql.API.AuthService;
import com.example.postgresql.API.Bitrix24Client;
import com.example.postgresql.API.CompanyService;
import com.example.postgresql.UserF.Cargo;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AddCargoDialogController {

    @FXML
    private TextField typeTS, weight, volume, product, loadingType, priceCard, priceNDS, details;
    @FXML
    private ComboBox<String> from, to, trade;
    @FXML
    private DatePicker dateFrom, dateTo;
    @FXML
    private Label statusLabel;

    private final AuthService authService = new AuthService();
    private String currentUser;
    private String userPhone = "";
    private Runnable onSuccess;

    private final ObservableList<String> cities = FXCollections.observableArrayList(
            "Москва", "Санкт-Петербург", "Екатеринбург", "Новосибирск", "Казань",
            "Нижний Новгород", "Челябинск", "Красноярск", "Самара", "Уфа", "Ростов-на-Дону",
            "Омск", "Краснодар", "Воронеж", "Пермь", "Волгоград", "Саратов", "Тюмень",
            "Тольятти", "Ижевск", "Барнаул", "Ульяновск", "Иркутск", "Хабаровск",
            "Ярославль", "Махачкала", "Владивосток", "Оренбург", "Томск", "Кемерово"
    );

    @FXML
    private void initialize() {
        from.setItems(cities);
        to.setItems(cities);
        from.setEditable(true);
        to.setEditable(true);

        setupAutoComplete(from);
        setupAutoComplete(to);

        from.valueProperty().addListener((obs, o, n) -> preventSameCities(true));
        to.valueProperty().addListener((obs, o, n) -> preventSameCities(false));
        from.getEditor().textProperty().addListener((obs, o, n) -> preventSameCities(true));
        to.getEditor().textProperty().addListener((obs, o, n) -> preventSameCities(false));

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
                priceNDS.setText(String.format("%,.0f", price * 1.2));
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
            if (newText == null || newText.isEmpty()) {
                comboBox.setItems(cities);
                return;
            }
            ObservableList<String> filtered = FXCollections.observableArrayList();
            for (String city : cities) {
                if (city.toLowerCase().contains(newText.toLowerCase())) filtered.add(city);
            }
            comboBox.setItems(filtered);
            if (!filtered.isEmpty() && !comboBox.isShowing()) comboBox.show();
        });
    }

    private void preventSameCities(boolean changedFrom) {
        String fromVal = from.getEditor().getText();
        String toVal = to.getEditor().getText();
        if (fromVal != null && toVal != null && !fromVal.isEmpty() && fromVal.equals(toVal)) {
            showStatus("Откуда и Куда не могут совпадать", "#ef4444");
        }
    }

    public void setUser(String login, String phone, Runnable onSuccess) {
        this.currentUser = login;
        this.userPhone = phone == null || phone.isEmpty() ? "Не указан" : phone;
        this.onSuccess = onSuccess;
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
        authService.supabase.select("users", "id", "login=eq." + currentUser)
                .thenCompose(result -> {
                    if (result.isEmpty()) throw new RuntimeException("Пользователь не найден");
                    int userId = result.get(0).getAsJsonObject().get("id").getAsInt();
                    cargo.addProperty("заказчик_id", userId);
                    return authService.supabase.insert("cargo", cargo);
                })
                .thenAccept(v -> Platform.runLater(() -> {
                    showStatus("Груз успешно опубликован!", "#16a34a");

                    Cargo cargoObj = new Cargo(
                            0,
                            getStr(cargo, "ТипТС"),
                            getDbl(cargo, "Вес"),
                            getDbl(cargo, "Объем"),
                            getStr(cargo, "Товар"),
                            getStr(cargo, "Откуда"),
                            getStr(cargo, "Куда"),
                            getStr(cargo, "ТипПогрузки"),
                            getStr(cargo, "ДеталиПогрузки"),
                            getStr(cargo, "Даты"),
                            getDbl(cargo, "ЦенаПоКарте"),
                            getDbl(cargo, "ЦенаНДС"),
                            getStr(cargo, "Торг_без_торга"),
                            getStr(cargo, "КонтактныйТелефон"),
                            0
                    );
                    new CompanyService().getWebhookForUser(currentUser).thenAccept(webhook -> {
                        if (webhook != null && !webhook.isEmpty()) {
                            Bitrix24Client.getInstance()
                                    .createDealFromCargo(webhook, cargoObj)
                                    .thenAccept(dealId -> {
                                        System.out.println("[Bitrix24] Сделка создана, ID=" + dealId);
                                        /**/
                                    })
                                    .exceptionally(ex -> {
                                        System.err.println("[Bitrix24] " + ex.getMessage());
                                        return null;
                                    });
                        } else {
                            System.out.println("[Bitrix24] Пользователь не в компании — сделка не создаётся");
                        }
                    }).exceptionally(ex -> {
                        System.err.println("[Bitrix24] webhook: " + ex.getMessage());
                        return null;
                    });

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
                            alert.setContentText("Груз с такими параметрами уже существует.");
                            alert.showAndWait();
                        } else {
                            showStatus("Ошибка: " + message, "#ef4444");
                        }
                    });
                    return null;
                });
    }

    private JsonObject buildCargoObject() {
        String fromCity = from.getEditor().getText().trim();
        String toCity = to.getEditor().getText().trim();

        String dateFromStr = dateFrom.getValue() != null
                ? dateFrom.getValue().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
        String dateToStr = dateTo.getValue() != null
                ? dateTo.getValue().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
        String datesText = dateFromStr.isEmpty() ? dateToStr
                : (dateToStr.isEmpty() ? dateFromStr : dateFromStr + " – " + dateToStr);

        JsonObject cargo = new JsonObject();
        cargo.addProperty("ТипТС", typeTS.getText().trim());
        cargo.addProperty("Вес", getNumber(weight.getText()));
        cargo.addProperty("Объем", getNumber(volume.getText()));
        cargo.addProperty("Товар", product.getText().trim());
        cargo.addProperty("Откуда", fromCity);
        cargo.addProperty("Куда", toCity);
        cargo.addProperty("ТипПогрузки", loadingType.getText().trim());
        cargo.addProperty("Даты", datesText);
        cargo.addProperty("ДеталиПогрузки", details.getText().trim().isEmpty() ? "—" : details.getText().trim());
        cargo.addProperty("ЦенаПоКарте", getNumber(priceCard.getText()));
        cargo.addProperty("ЦенаНДС", getNumber(priceNDS.getText()));
        cargo.addProperty("Торг_без_торга", trade.getValue());
        cargo.addProperty("КонтактныйТелефон", userPhone);
        return cargo;
    }

    private String validateFields() {
        StringBuilder sb = new StringBuilder();
        if (typeTS.getText().trim().isEmpty()) sb.append("• Тип ТС\n");
        if (from.getEditor().getText().trim().isEmpty()) sb.append("• Откуда\n");
        if (to.getEditor().getText().trim().isEmpty()) sb.append("• Куда\n");
        String fromVal = from.getEditor().getText().trim();
        String toVal = to.getEditor().getText().trim();
        if (!fromVal.isEmpty() && fromVal.equals(toVal)) sb.append("• Откуда и Куда совпадают\n");
        if (weight.getText().trim().isEmpty()) sb.append("• Вес\n");
        if (volume.getText().trim().isEmpty()) sb.append("• Объём\n");
        if (product.getText().trim().isEmpty()) sb.append("• Товар\n");
        return sb.toString();
    }

    private double getNumber(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(text.trim().replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showStatus(String msg, String color) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private String getStr(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private double getDbl(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return 0;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return 0;
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
    }
}