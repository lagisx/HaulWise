package com.example.postgresql.API;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TelegramBindBot {
    private static final String BOT_TOKEN = "8593856865:AAHBoB8m-nex0KWulBTGMjOdMHDol-qwZGE";
    private static final String API_URL = "https://api.telegram.org/bot" + BOT_TOKEN;

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    private static long lastUpdateId = 0;

    public static void main(String[] args) {
        while (true) {
            getUpdates();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    private static void getUpdates() {
        try {
            String url = API_URL + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=30";

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "{}";

                if (!response.isSuccessful()) {
                    if (response.code() == 409) {
                        return;
                    }
                    if (response.code() >= 500) {
                        return;
                    }
                    return;
                }

                JsonObject json = gson.fromJson(body, JsonObject.class);
                if (json == null || !json.has("ok") || !json.get("ok").getAsBoolean()) {
                    return;
                }

                JsonArray updates = json.getAsJsonArray("result");
                if (updates == null || updates.size() == 0) {
                    return;
                }
                for (JsonElement elem : updates) {
                    JsonObject update = elem.getAsJsonObject();
                    long updateId = update.get("update_id").getAsLong();
                    lastUpdateId = Math.max(lastUpdateId, updateId);

                    if (!update.has("message")) {
                        continue;
                    }

                    JsonObject message = update.getAsJsonObject("message");
                    JsonObject chat = message.getAsJsonObject("chat");
                    long chatId = chat.get("id").getAsLong();
                    String text = message.has("text") ? message.get("text").getAsString().trim() : "";
                    if (text.startsWith("/start")) {
                        handleStart(chatId, text);
                    }
                }
            }
        } catch (java.net.SocketTimeoutException e) {

        } catch (Exception e) {

        }
    }

    private static void handleStart(long chatId, String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            sendMessage(chatId, "Привет! 👋\n\nЧтобы привязать аккаунт, отправьте команду:\n/start ваш логин или email");
            return;
        }

        String identifier = parts[1].trim().toLowerCase();

        if (bindToAccount(identifier, chatId)) {
            sendMessage(chatId, "✅ Telegram успешно привязан!\n\nТеперь вы можете восстанавливать пароль через этот бот.");
        } else {
            sendMessage(chatId, "❌ Аккаунт с логином или email \"" + identifier + "\" не найден.\n\nПроверьте правильность данных и попробуйте снова.");
        }
    }

    private static boolean bindToAccount(String identifier, long chatId) {
        SupabaseClient supabase = new SupabaseClient();

        JsonObject update = new JsonObject();
        update.addProperty("telegram_chat_id", chatId);
        update.addProperty("telegram_linked", true);

        String filter = "or=(login.eq." + URLEncoder.encode(identifier, StandardCharsets.UTF_8) +
                ",email.eq." + URLEncoder.encode(identifier.toLowerCase(), StandardCharsets.UTF_8) + ")";

        try {
            supabase.update("users", update, filter).join();
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка привязки: " + e.getMessage());
            return false;
        }
    }

    private static void sendMessage(long chatId, String text) {
        try {
            String url = API_URL + "/sendMessage?chat_id=" + chatId +
                    "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8) +
                    "&parse_mode=HTML";

            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("Ошибка отправки сообщения: " + response.code());
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }
}