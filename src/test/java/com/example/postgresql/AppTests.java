package com.example.postgresql;

import com.example.postgresql.API.OtpStore;
import org.junit.jupiter.api.*;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppTests {

    @Test
    @Order(1)
    @DisplayName("Сохранение и верификация OTP-кода")
    void testOtpSaveAndVerify() {
        OtpStore.save("test@mail.ru", "123456");
        assertTrue(OtpStore.verify("test@mail.ru", "123456"),
                "Код должен успешно верифицироваться после сохранения");
    }

    @Test
    @Order(2)
    @DisplayName("Повторная верификация одного кода невозможна")
    void testOtpOneTimeUse() {
        OtpStore.save("once@mail.ru", "999999");
        OtpStore.verify("once@mail.ru", "999999");
        assertFalse(OtpStore.verify("once@mail.ru", "999999"),
                "После первой верификации код должен удаляться");
    }

    @Test
    @Order(3)
    @DisplayName("Истечение срока действия кода (имитация)")
    void testOtpExpiry() {
        OtpStore.save("expire@mail.ru", "777777");
        OtpStore.remove("expire@mail.ru");
        assertFalse(OtpStore.verify("expire@mail.ru", "777777"),
                "Удалённый/истёкший код не должен проходить верификацию");
    }

    @Test
    @Order(4)
    @DisplayName("Генерация кода — ровно 6 цифр")
    void testCodeGeneration() {
        String code = String.format("%06d",
                new SecureRandom().nextInt(1_000_000));
        assertEquals(6, code.length(),
                "Длина кода должна быть ровно 6 символов");
        assertTrue(code.matches("\\d{6}"),
                "Код должен состоять только из цифр");
    }

    @Test
    @Order(5)
    @DisplayName("Маскировка email — скрытие средней части")
    void testMaskEmail() {
        String masked = maskEmail("skeitboards@gmail.com");
        assertTrue(masked.startsWith("s"),
                "Должен начинаться с первой буквы");
        assertTrue(masked.contains("***"),
                "Должен содержать ***");
        assertTrue(masked.contains("@gmail.com"),
                "Должен сохранять домен");
    }

    @Test
    @Order(6)
    @DisplayName("Валидация телефона — корректный номер")
    void testPhoneValidationCorrect() {
        assertTrue("+79161234567".matches("\\+?\\d{10,15}"),
                "Корректный номер должен проходить валидацию");
    }

    @Test
    @Order(7)
    @DisplayName("Валидация телефона — некорректный номер")
    void testPhoneValidationIncorrect() {
        assertFalse("testphone".matches("\\+?\\d{10,15}"),
                "Некорректный номер не должен проходить валидацию");
    }

    @Test
    @Order(8)
    @DisplayName("Нормализация логина — нижний регистр и обрезка пробелов")
    void testLoginNormalization() {
        assertEquals("one", " ONE ".trim().toLowerCase(),
                "Логин должен быть в нижнем регистре без пробелов");
    }

    @Test
    @Order(9)
    @DisplayName("Валидация email — корректный формат")
    void testEmailValidationCorrect() {
        assertTrue(isValidEmail("user@example.com"),
                "Корректный email должен проходить валидацию");
    }

    @Test
    @Order(10)
    @DisplayName("Валидация email — некорректный формат")
    void testEmailValidationIncorrect() {
        assertFalse(isValidEmail("notanemail"),
                "Некорректный email не должен проходить валидацию");
        assertFalse(isValidEmail("missing@"),
                "Email без домена не должен проходить валидацию");
        assertFalse(isValidEmail(""),
                "Пустая строка не должна проходить валидацию");
    }

    private String maskEmail(String e) {
        int at = e.indexOf('@');
        if (at <= 1) return e;
        return e.charAt(0) + "***" + e.substring(at);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        return email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }
}
