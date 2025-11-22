package com.example.postgresql.API;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class AuthService {

    public final SupabaseClient supabase;

    public AuthService() {
        this.supabase = new SupabaseClient();
    }
    private void logAction(String userLogin, String description) {
        JsonObject log = new JsonObject();
        log.addProperty("users", userLogin != null && !userLogin.isEmpty() ? userLogin : "system");
        log.addProperty("description", description);
        log.addProperty("created_at", Instant.now().toString());

        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("ОТПРАВЛЯЮ ЛОГ В БАЗУ → " + description);
                System.out.println("Тело запроса: " + log);

                var client = new OkHttpClient();
                var body = RequestBody.create(log.toString(), MediaType.get("application/json"));

                var request = new Request.Builder()
                        .url(SupabaseClient.SUPABASE_URL  + "/rest/v1/logs")
                        .addHeader("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .post(body)
                        .build();

                try (var response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "пусто";
                    if (response.isSuccessful()) {
                        System.out.println("ЛОГ УСПЕШНО ЗАПИСАН В БАЗУ: " + description);
                    } else {
                        System.out.println("ЛОГ НЕ ЗАПИСАН! HTTP " + response.code());
                        System.out.println("Ответ Supabase: " + responseBody);
                    }
                }
            } catch (Exception e) {
                System.out.println("ИСКЛЮЧЕНИЕ ПРИ ОТПРАВКЕ ЛОГА: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Boolean> registerUser(String login, String password, String email, String phone) {
        JsonObject data = new JsonObject();
        data.addProperty("login", login);
        data.addProperty("password", password);
        data.addProperty("email", email);
        data.addProperty("phone", phone);
        data.addProperty("role", "user");
        data.addProperty("status", false);

        return supabase.insert("users", data)
                .thenApply(result -> {
                    logAction(login, "Регистрация нового пользователя");
                    return true;
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> blockUser(
            int userId, String login, String phone, String email, String reason, String blockedBy) {

        JsonObject entry = new JsonObject();
        entry.addProperty("login", login);
        entry.addProperty("email", email);
        entry.addProperty("phone", phone);
        entry.addProperty("reason", reason);
        entry.addProperty("blocked_by", blockedBy);
        entry.addProperty("created_at", Instant.now().toString());

        return supabase.insert("blacklist", entry)
                .thenCompose(v -> {
                    JsonObject update = new JsonObject();
                    update.addProperty("status", true);
                    return supabase.update("users", update, "id=eq." + userId);
                })
                .thenApply(v -> {
                    logAction(login, "Заблокирован администратором " + blockedBy + ". Причина: " + reason);
                    return true;
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> unblockUser(int userId, String login, String blockedBy) {
        String encoded = URLEncoder.encode(login, StandardCharsets.UTF_8);

        return supabase.delete("blacklist", "login=eq." + encoded)
                .thenCompose(deleted -> {
                    if (!deleted) return CompletableFuture.completedFuture(false);

                    JsonObject update = new JsonObject();
                    update.addProperty("status", false);
                    return supabase.update("users", update, "id=eq." + userId);
                })
                .thenApply(v -> {
                    logAction(login, "Разблокирован администратором" + blockedBy);
                    return true;
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> deleteUserAndCargo(int userId, String login) {
        return supabase.delete("gruz", "\"заказчик_id\"=eq." + userId)
                .thenCompose(v -> supabase.delete("users", "id=eq." + userId))
                .thenApply(v -> {
                    logAction(login, "Удалён администратором вместе со всеми грузами");
                    return true;
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> deleteCargo(int cargoId) {
        return supabase.delete("gruz", "id=eq." + cargoId)
                .thenApply(v -> {
                    logAction(null, "Удалён груз с ID=" + cargoId + " (админ)");
                    return true;
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<UserAuthResult> authenticate(String login, String password) {
        String filter = "login=eq." + URLEncoder.encode(login, StandardCharsets.UTF_8) +
                ",password=eq." + URLEncoder.encode(password, StandardCharsets.UTF_8);

        return supabase.select("users", "*", filter)
                .thenCompose(result -> {
                    if (result == null || result.size() == 0) {
                        return CompletableFuture.completedFuture(new UserAuthResult(false, null, "Неверный логин или пароль"));
                    }

                    JsonObject user = result.get(0).getAsJsonObject();
                    String role = user.has("role") ? user.get("role").getAsString().trim().toLowerCase() : "user";
                    boolean banned = user.has("status") && user.get("status").getAsBoolean();
                    if (banned && "admin".equals(role)) banned = false;

                    if (banned) {
                        return supabase.select("blacklist", "reason", "login=eq." + URLEncoder.encode(login, StandardCharsets.UTF_8))
                                .thenApply(bl -> new UserAuthResult(false, null, "Аккаунт заблокирован",
                                        bl.size() > 0 ? bl.get(0).getAsJsonObject().get("reason").getAsString() : "Причина не указана"));
                    }
                    return CompletableFuture.completedFuture(new UserAuthResult(true, role, "Успешный вход"));
                });
    }

    public CompletableFuture<JsonArray> getAllCargos() { return supabase.select("gruz", "*,заказчик_id", null); }
    public CompletableFuture<JsonArray> getUserCargos(String login) {
        return supabase.select("users", "id", "login=eq." + URLEncoder.encode(login, StandardCharsets.UTF_8)).thenCompose(r -> r.size() == 0 ? CompletableFuture.completedFuture(new JsonArray()) : supabase.select("gruz", "*", "\"заказчик_id\"=eq." + r.get(0).getAsJsonObject().get("id").getAsInt()));
    }
    public CompletableFuture<JsonArray> getUserProfile(String login) {
        return supabase.select("users", "email,phone", "login=eq." + URLEncoder.encode(login, StandardCharsets.UTF_8));
    }
    public CompletableFuture<JsonArray> getAllUsers() { return supabase.select("users", "*", "role=neq.admin"); }
    public CompletableFuture<JsonArray> getBlacklist() { return supabase.select("blacklist", "*", null); }
    public CompletableFuture<JsonArray> getLogs() { return supabase.select("logs", "*", "order=created_at.desc"); }
    public CompletableFuture<BlockStatus> checkBlacklist(String login) {
        return supabase.select("blacklist", "*", "login=eq." + URLEncoder.encode(login, StandardCharsets.UTF_8))
                .thenApply(r -> r.size() > 0 ? new BlockStatus(true, r.get(0).getAsJsonObject().get("reason").getAsString())
                        : new BlockStatus(false, null));
    }

    public static class BlockStatus {
        public final boolean isBlocked;
        public final String reason;
        public BlockStatus(boolean isBlocked, String reason) {
            this.isBlocked = isBlocked;
            this.reason = reason;
        }
    }

    public static class UserAuthResult {
        public final boolean success;
        public final String role;
        public final String message;
        public final String blockReason;
        public UserAuthResult(boolean success, String role, String message) { this(success, role, message, null); }
        public UserAuthResult(boolean success, String role, String message, String blockReason) {
            this.success = success; this.role = role; this.message = message; this.blockReason = blockReason;
        }
        public String getMessage() { return message; }
        public boolean isSuccess() { return success; }
        public String getRole() { return role; }
        public String getBlockReason() { return blockReason; }
    }
}