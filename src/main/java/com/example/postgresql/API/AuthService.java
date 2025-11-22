package com.example.postgresql.API;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

public class AuthService {

    private final SupabaseClient supabase;

    public AuthService() {
        this.supabase = new SupabaseClient();
    }

    public CompletableFuture<BlockStatus> checkBlacklist(String login) {
        String[] filter = {"login=eq." + login};
        return supabase.select("blacklist", "*", filter)
                .thenApply(result -> {
                    if (result != null && result.size() > 0) {
                        String reason = result.get(0).getAsJsonObject().get("reason").getAsString();
                        return new BlockStatus(true, reason);
                    }
                    return new BlockStatus(false, null);
                });
    }

    public CompletableFuture<UserAuthResult> authenticate(String login, String password) {
        System.out.println("Попытка входа: " + login + " / " + password);
        String[] filters = {
                "login=eq." + login,
                "password=eq." + password
        };

        return supabase.select("users", "*", filters)
                .thenApply(result -> {
                    System.out.println("Найдено пользователей: " + (result != null ? result.size() : "null"));

                    if (result != null && result.size() > 0) {
                        JsonObject user = result.get(0).getAsJsonObject();
                        boolean active = user.has("status") ? user.get("status").getAsBoolean() : true;
                        if (!active) {
                            return new UserAuthResult(false, null, "Аккаунт заблокирован");
                        }
                        String role = user.has("role") ? user.get("role").getAsString().trim().toLowerCase() : "user";
                        return new UserAuthResult(true, role, "Успешный вход");
                    }
                    return new UserAuthResult(false, null, "Неверный логин или пароль");
                });
    }

    public CompletableFuture<Boolean> registerUser(String login, String password, String email, String phone) {
        JsonObject userData = new JsonObject();
        userData.addProperty("login", login);
        userData.addProperty("password", password);
        userData.addProperty("email", email);
        userData.addProperty("phone", phone);
        userData.addProperty("role", "user");

        return supabase.insert("users", userData)
                .thenApply(result -> result != null && !result.isJsonNull());
    }


    public CompletableFuture<JsonArray> getAllCargos() {
        return supabase.select("gruz", "*", null);
    }
    public CompletableFuture<JsonArray> getUserCargos(String login) {
        String[] userFilter = {"login=eq." + login};
        return supabase.select("users", "id", userFilter)
                .thenCompose(result -> {
                    if (result == null || result.size() == 0) {
                        return CompletableFuture.completedFuture(new JsonArray());
                    }
                    int userId = result.get(0).getAsJsonObject().get("id").getAsInt();

                    String[] cargoFilter = {"\"заказчик_id\"=eq." + userId};
                    return supabase.select("gruz", "*", cargoFilter);
                });
    }

    public CompletableFuture<Boolean> checkGruz(String login) {
        String[] userFilter = {"login=eq." + login};
        return supabase.select("users", "id", userFilter)
                .thenCompose(result -> {
                    if (result == null || result.size() == 0) {
                        return CompletableFuture.completedFuture(false);
                    }
                    int userId = result.get(0).getAsJsonObject().get("id").getAsInt();

                    String[] cargoFilter = {"\"заказчик_id\"=eq." + userId};
                    return supabase.select("gruz", "*", cargoFilter)
                            .thenApply(res -> res != null && res.size() > 0);
                });
    }

    public CompletableFuture<Boolean> deleteCargo(int cargoId) {
        String filter = "id=eq." + cargoId;
        return supabase.delete("gruz", filter);
    }

    public CompletableFuture<JsonArray> getUserProfile(String login) {
        String[] filter = {"login=eq." + login};
        return supabase.select("users", "email,phone", filter);
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

        public UserAuthResult(boolean success, String role, String message) {
            this.success = success;
            this.role = role;
            this.message = message;
        }

    }
}
