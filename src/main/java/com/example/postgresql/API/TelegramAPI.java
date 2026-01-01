package com.example.postgresql.API;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class TelegramAPI {
    private static final String BOT_TOKEN = "8593856865:AAHBoB8m-nex0KWulBTGMjOdMHDol-qwZGE";
    private static final String API_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Random random = new Random();

    public static String generateCode(int length) {
        return String.format("%0" + length + "d", random.nextInt((int) Math.pow(10, length)));
    }

    public static boolean sendCode(long chatId, String code) {
        try {
            String message = "🔐 Ваш код восстановления: " + code + "\n Не давайте код никому!!" + "\nДействителен 5 минут.";
            String url = API_URL + "?chat_id=" + chatId + "&text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}