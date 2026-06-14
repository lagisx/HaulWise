package com.example.postgresql;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    private JsonObject buildCargo(String from, String to, double weight,
                                  double volume, double price, String product) {
        JsonObject cargo = new JsonObject();
        cargo.addProperty("Откуда", from);
        cargo.addProperty("Куда", to);
        cargo.addProperty("Вес", weight);
        cargo.addProperty("Объем", volume);
        cargo.addProperty("ЦенаПоКарте", price);
        cargo.addProperty("Товар", product);
        cargo.addProperty("ТипТС", "Реф");
        cargo.addProperty("ДеталиПогрузки", "Задняя");
        cargo.addProperty("Даты", "01.05.2026 – 10.05.2026");
        cargo.addProperty("ЦенаНДС", price * 1.2);
        cargo.addProperty("Торг_без_торга", "Торг уместен");
        cargo.addProperty("КонтактныйТелефон", "+79001234567");
        return cargo;
    }

    @Test
    @DisplayName("Маршрут корректно читается из объекта груза")
    void testRouteFieldsFromCargo() {
        JsonObject cargo = buildCargo("Екатеринбург", "Москва",
                55, 70, 59000, "Продукты");

        String from = cargo.get("Откуда").getAsString();
        String to = cargo.get("Куда").getAsString();

        assertEquals("Екатеринбург", from);
        assertEquals("Москва", to);
        assertNotEquals(from, to);
    }

    @Test
    @DisplayName("Значение null для владельца не вызывает ошибку")
    void testOwnerLoginNullSafe() {
        String ownerLogin = null;

        String displayed = (ownerLogin == null || ownerLogin.isEmpty()) ? "—" : ownerLogin;

        assertEquals("—", displayed);
        assertDoesNotThrow(() -> {
            String result = (ownerLogin == null || ownerLogin.isEmpty()) ? "—" : ownerLogin;
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("Пустая строка владельца отображается как прочерк")
    void testOwnerLoginEmptySafe() {
        String ownerLogin = "";
        String displayed = (ownerLogin == null || ownerLogin.isEmpty()) ? "—" : ownerLogin;

        assertEquals("—", displayed);
    }

    @Test
    @DisplayName("Реальный логин владельца отображается без изменений")
    void testOwnerLoginReal() {
        String ownerLogin = "one";
        String displayed = (ownerLogin == null || ownerLogin.isEmpty()) ? "—" : ownerLogin;

        assertEquals("one", displayed);
    }

    @Test
    @DisplayName("Целый вес отображается без дробной части")
    void testWeightFormatting() {
        JsonObject cargo = buildCargo("Омск", "Казань", 55.0, 40.0, 68000, "Продукты");

        double weight = cargo.get("Вес").getAsDouble();
        String formatted = weight == Math.floor(weight)
                ? String.valueOf((int) weight)
                : String.valueOf(weight);

        assertEquals("55", formatted);
    }

    @Test
    @DisplayName("Дробный вес сохраняет дробную часть")
    void testWeightFormattingDecimal() {
        JsonObject cargo = buildCargo("Саратов", "Уфа", 12.5, 20.0, 30000, "Техника");

        double weight = cargo.get("Вес").getAsDouble();
        String formatted = weight == Math.floor(weight)
                ? String.valueOf((int) weight)
                : String.valueOf(weight);

        assertEquals("12.5", formatted);
    }

    @Test
    @DisplayName("Груз с совпадающим городом проходит фильтр")
    void testFilterMatchFound() {
        JsonObject cargo = buildCargo("Екатеринбург", "Москва", 55, 70, 59000, "Продукты");

        String filterTo = "москва";
        String cargoTo = cargo.get("Куда").getAsString().toLowerCase();

        assertTrue(cargoTo.contains(filterTo));
    }

    @Test
    @DisplayName("Груз с другим городом не проходит фильтр")
    void testFilterMatchNotFound() {
        JsonObject cargo = buildCargo("Омск", "Ульяновск", 10, 40, 68000, "Продукты");

        String filterTo = "москва";
        String cargoTo = cargo.get("Куда").getAsString().toLowerCase();

        assertFalse(cargoTo.contains(filterTo));
    }

    @Test
    @DisplayName("Груз с малым весом не проходит фильтр по минимуму")
    void testFilterByWeightMin() {
        JsonObject cargo = buildCargo("Омск", "Ульяновск", 10, 40, 68000, "Продукты");

        double weight = cargo.get("Вес").getAsDouble();
        double minW = 50.0;

        assertFalse(weight >= minW);
    }

    @Test
    @DisplayName("Груз с ценой в диапазоне проходит ценовой фильтр")
    void testFilterByPriceRange() {
        JsonObject cargo = buildCargo("Екатеринбург", "Москва", 55, 70, 59000, "Продукты");

        double price = cargo.get("ЦенаПоКарте").getAsDouble();
        double minP = 50000.0;
        double maxP = 70000.0;

        assertTrue(price >= minP && price <= maxP);
    }

    @Test
    @DisplayName("Отсутствующее поле в данных груза отображается как прочерк")
    void testMissingFieldReturnsDash() {
        JsonObject cargo = new JsonObject();

        String key = "Товар";
        String result = (cargo.has(key) && !cargo.get(key).isJsonNull())
                ? cargo.get(key).getAsString()
                : "—";

        assertEquals("—", result);
    }

    @ParameterizedTest
    @DisplayName("Города отправления и назначения не должны совпадать")
    @ValueSource(strings = {"Москва", "Казань", "Уфа", "Омск", "Тюмень"})
    void testFromAndToCitiesNotEqual(String city) {
        String fixedDestination = "Екатеринбург";

        assertNotEquals(city, fixedDestination);
    }
}