package com.example.postgresql.API;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static AppConfig instance;
    private final Properties props = new Properties();

    private AppConfig() {
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is == null) {
                throw new RuntimeException(
                    "Файл config.properties не найден!\n" +
                    "Создайте src/main/resources/config.properties"
                );
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения config.properties: " + e.getMessage(), e);
        }
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    

    public String getSupabaseUrl() {
        return get("supabase.url");
    }

    public String getAnonKey() {
        return get("supabase.anon_key");
    }

    public String getServiceKey() {
        return get("supabase.service_key");
    }

    

    public String getGeocoderApiKey() {
        return get("geocoder.api_key");
    }

    

    public String getResendApiKey() {
        return get("resend.api_key");
    }

    public String getResendFrom() {
        return get("resend.from");
    }

    

    private String get(String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Ключ '" + key + "' не найден в config.properties");
        }
        return value.trim();
    }
}
