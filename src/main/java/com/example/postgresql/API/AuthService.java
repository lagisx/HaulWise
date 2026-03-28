package com.example.postgresql.API;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class AuthService {

    public final SupabaseClient supabase = new SupabaseClient();

    private void logAction(String userLogin, String description) {
        JsonObject log = new JsonObject();
        log.addProperty("users", userLogin != null && !userLogin.isEmpty() ? userLogin : "system");
        log.addProperty("description", description);
        log.addProperty("created_at", Instant.now().toString());

        CompletableFuture.runAsync(() -> {
            try {
                var client = new OkHttpClient();
                var body = RequestBody.create(log.toString(), MediaType.get("application/json"));
                var req = new Request.Builder()
                        .url(SupabaseClient.SUPABASE_URL + "/rest/v1/logs")
                        .addHeader("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .post(body).build();
                try (var r = client.newCall(req).execute()) {
                    if (r.body() != null) r.body().close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    

    
    public CompletableFuture<String> registerUser(String login, String password, String email, String phone) {
        
        return supabase.selectWithRaw("users", "login,email",
                        "or=(login.eq." + login + ",email.eq." + email + ")")
                .thenCompose(existing -> {
                    if (existing != null && existing.size() > 0) {
                        com.google.gson.JsonObject found = existing.get(0).getAsJsonObject();
                        String foundLogin = found.has("login") && !found.get("login").isJsonNull()
                                ? found.get("login").getAsString() : "";
                        if (foundLogin.equalsIgnoreCase(login))
                            return CompletableFuture.completedFuture("Логин уже занят");
                        return CompletableFuture.completedFuture("Email уже зарегистрирован");
                    }

                    JsonObject meta = new JsonObject();
                    meta.addProperty("login", login);
                    meta.addProperty("phone", phone);

                    return supabase.authSignUp(email, password, meta)
                            .thenCompose(authResp -> {
                                
                                System.out.println("[Auth] signUp response: " + (authResp != null ? authResp.toString() : "null"));

                                if (authResp == null)
                                    return CompletableFuture.completedFuture("Нет ответа от сервера");

                                
                                if (authResp.has("error_code") || authResp.has("error")) {
                                    String errMsg = "";
                                    if (authResp.has("message") && !authResp.get("message").isJsonNull())
                                        errMsg = authResp.get("message").getAsString();
                                    else if (authResp.has("msg") && !authResp.get("msg").isJsonNull())
                                        errMsg = authResp.get("msg").getAsString();
                                    else if (authResp.has("error") && !authResp.get("error").isJsonNull())
                                        errMsg = authResp.get("error").getAsString();
                                    
                                    String lower = errMsg.toLowerCase();
                                    if (lower.contains("already") || lower.contains("exists") || lower.contains("registered"))
                                        return CompletableFuture.completedFuture("Email уже зарегистрирован в системе");
                                    return CompletableFuture.completedFuture("Ошибка Auth: " + errMsg);
                                }
                                if (authResp.has("code") && authResp.has("msg")) {
                                    try {
                                        if (authResp.get("code").getAsInt() >= 400)
                                            return CompletableFuture.completedFuture(
                                                authResp.get("msg").getAsString());
                                    } catch (Exception ignored) {}
                                }

                                JsonObject row = new JsonObject();
                                row.addProperty("login", login);
                                row.addProperty("email", email);
                                row.addProperty("phone", phone);
                                row.addProperty("role", "user");
                                row.addProperty("status", false);
                                row.addProperty("email_confirmed", false);

                                return supabase.insertServiceRole("users", row)
                                        .thenApply(r -> {
                                            logAction(login, "Зарегистрирован. Email: " + email);
                                            return (String) null;
                                        })
                                        .exceptionally(ex -> {
                                            supabase.adminDeleteAuthUser(email);
                                            return "Ошибка записи в БД: " + ex.getMessage();
                                        });
                            })
                            .exceptionally(ex -> "Ошибка сети: " + ex.getMessage());
                })
                .exceptionally(ex -> "Ошибка сервера: " + ex.getMessage());
    }

    

    public CompletableFuture<UserAuthResult> authenticate(String loginOrEmail, String password) {
        return resolveEmail(loginOrEmail).thenCompose(email -> {
            if (email == null)
                return CompletableFuture.completedFuture(new UserAuthResult(false, null, "Пользователь не найден"));

            return supabase.authSignIn(email, password)
                    .thenCompose(authResp -> {
                        if (authResp == null || authResp.has("error_description")) {
                            String msg = (authResp != null && authResp.has("error_description"))
                                    ? authResp.get("error_description").getAsString()
                                    : "Неверный логин или пароль";
                            return CompletableFuture.completedFuture(new UserAuthResult(false, null, msg));
                        }

                        return supabase.select("users", "role,status,login,email_confirmed", "email=eq." + email)
                                .thenCompose(rows -> {
                                    if (rows == null || rows.isEmpty())
                                        return CompletableFuture.completedFuture(
                                                new UserAuthResult(false, null, "Профиль не найден."));

                                    JsonObject u = rows.get(0).getAsJsonObject();
                                    String role = u.has("role") ? u.get("role").getAsString().trim().toLowerCase() : "user";
                                    boolean banned = u.has("status") && u.get("status").getAsBoolean();
                                    if ("admin".equals(role)) banned = false;

                                    if (banned) {
                                        return supabase.select("blacklist", "reason", "login=eq." + loginOrEmail)
                                                .thenApply(bl -> new UserAuthResult(false, null,
                                                        "Аккаунт заблокирован",
                                                        bl.size() > 0
                                                                ? bl.get(0).getAsJsonObject().get("reason").getAsString()
                                                                : "Причина не указана"));
                                    }

                                    
                                    boolean emailConfirmed = u.has("email_confirmed")
                                            && !u.get("email_confirmed").isJsonNull()
                                            && u.get("email_confirmed").getAsBoolean();

                                    logAction(loginOrEmail, "Успешный вход. Email подтверждён: " + emailConfirmed);
                                    return CompletableFuture.completedFuture(
                                            new UserAuthResult(true, role, "Успешный вход", null, emailConfirmed));
                                });
                    })
                    .exceptionally(ex -> new UserAuthResult(false, null, "Ошибка соединения"));
        });
    }

    private CompletableFuture<String> resolveEmail(String loginOrEmail) {
        if (loginOrEmail.contains("@"))
            return CompletableFuture.completedFuture(loginOrEmail);
        return supabase.select("users", "email", "login=eq." + loginOrEmail)
                .thenApply(rows -> rows.size() > 0
                        ? rows.get(0).getAsJsonObject().get("email").getAsString()
                        : null);
    }

    

    public CompletableFuture<Boolean> sendPasswordResetEmail(String loginOrEmail) {
        return resolveEmail(loginOrEmail).thenCompose(email -> {
            if (email == null) return CompletableFuture.completedFuture(false);
            return supabase.authResetPassword(email)
                    .thenApply(ok -> {
                        if (ok) logAction(loginOrEmail, "Запрос сброса пароля по email");
                        return ok;
                    });
        });
    }

    
    public CompletableFuture<Boolean> sendPasswordResetOTP(String loginOrEmail) {
        return resolveEmail(loginOrEmail).thenCompose(email -> {
            if (email == null) return CompletableFuture.completedFuture(false);
            return supabase.authSendRecoveryOTP(email)
                    .thenApply(ok -> {
                        if (ok) logAction(loginOrEmail, "Запрос OTP-кода для сброса пароля");
                        return ok;
                    });
        });
    }

    
    public CompletableFuture<String> verifyPasswordResetOTP(String email, String otpCode) {
        return supabase.authVerifyOTP(email, otpCode)
                .thenApply(resp -> {
                    if (resp != null && resp.has("access_token") && !resp.get("access_token").isJsonNull()) {
                        return resp.get("access_token").getAsString();
                    }
                    return null;
                })
                .exceptionally(ex -> null);
    }

    
    public CompletableFuture<Boolean> updatePasswordWithToken(String accessToken, String newPassword) {
        return supabase.authUpdatePassword(accessToken, newPassword)
                .thenApply(ok -> {
                    if (ok) logAction(null, "Пароль успешно изменён через OTP");
                    return ok;
                })
                .exceptionally(ex -> false);
    }

    
    public CompletableFuture<String> resolveEmailPublic(String loginOrEmail) {
        return resolveEmail(loginOrEmail);
    }

    public CompletableFuture<PasswordResetInfo> findUserForPasswordReset(String identifier) {
        String id = identifier.toLowerCase();
        return supabase.selectWithRaw("users", "login,email",
                        "or=(login.eq." + id + ",email.eq." + id + ")")
                .thenApply(r -> {
                    if (r == null || r.isEmpty()) return null;
                    JsonObject u = r.get(0).getAsJsonObject();
                    String login = u.has("login") ? u.get("login").getAsString() : null;
                    String email = u.has("email") && !u.get("email").isJsonNull()
                            ? u.get("email").getAsString() : null;
                    return new PasswordResetInfo(login, email);
                });
    }

    

    public CompletableFuture<Boolean> blockUser(int userId, String login, String phone, String email,
                                                String reason, String blockedBy) {
        JsonObject entry = new JsonObject();
        entry.addProperty("login", login);
        entry.addProperty("email", email);
        entry.addProperty("phone", phone);
        entry.addProperty("reason", reason);
        entry.addProperty("blocked_by", blockedBy);
        entry.addProperty("created_at", Instant.now().toString());

        return supabase.insert("blacklist", entry)
                .thenCompose(v -> {
                    JsonObject upd = new JsonObject();
                    upd.addProperty("status", true);
                    return supabase.update("users", upd, "id=eq." + userId);
                })
                .thenApply(v -> {
                    logAction(login, "Заблокирован администратором " + blockedBy + ". Причина: " + reason);
                    return true;
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> unblockUser(int userId, String login, String blockedBy) {
        return supabase.delete("blacklist", "login=eq." + login)
                .thenCompose(del -> {
                    if (!del) return CompletableFuture.completedFuture(false);
                    JsonObject upd = new JsonObject();
                    upd.addProperty("status", false);
                    return supabase.update("users", upd, "id=eq." + userId);
                })
                .thenApply(v -> {
                    logAction(login, "Разблокирован администратором " + blockedBy);
                    return true;
                })
                .exceptionally(ex -> false);
    }

    

    public CompletableFuture<Boolean> deleteUserAndCargo(int userId, String login) {
        return supabase.delete("cargo", "заказчик_id=eq." + userId)
                .thenCompose(v -> supabase.delete("users", "id=eq." + userId))
                .thenApply(v -> {
                    logAction(login, "Удалён администратором вместе со всеми грузами");
                    return true;
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> deleteCargo(int cargoId) {
        return supabase.delete("cargo", "id=eq." + cargoId)
                .thenApply(v -> {
                    logAction(null, "Удалён груз ID=" + cargoId + " (админ)");
                    return true;
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> deleteUser(String login) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.select("users", "id", "login=eq." + login)
                        .thenCompose(r -> {
                            if (r.isEmpty()) return CompletableFuture.completedFuture(false);
                            int uid = r.get(0).getAsJsonObject().get("id").getAsInt();
                            return supabase.delete("cargo", "заказчик_id=eq." + uid)
                                    .thenCompose(v -> supabase.delete("users", "login=eq." + login))
                                    .thenApply(del -> {
                                        if (del) logAction(login, "Пользователь " + login + " удалил свой аккаунт");
                                        return del;
                                    });
                        })
                        .exceptionally(ex -> false).join();
            } catch (Exception e) { e.printStackTrace(); return false; }
        });
    }

    

    public CompletableFuture<Boolean> updateUserProfile(String username, String newLogin, String newEmail, String newPhone) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.select("users", "id", "login=eq." + username)
                        .thenCompose(r -> {
                            if (r.isEmpty()) return CompletableFuture.completedFuture(false);
                            int uid = r.get(0).getAsJsonObject().get("id").getAsInt();
                            JsonObject upd = new JsonObject();
                            if (newLogin != null && !newLogin.isEmpty()) upd.addProperty("login", newLogin);
                            if (newEmail != null && !newEmail.isEmpty()) upd.addProperty("email", newEmail);
                            if (newPhone != null && !newPhone.isEmpty()) upd.addProperty("phone", newPhone);
                            if (upd.size() == 0) return CompletableFuture.completedFuture(true);
                            return supabase.update("users", upd, "id=eq." + uid)
                                    .thenApply(v -> { logAction(username, "Обновил профиль"); return true; });
                        })
                        .exceptionally(ex -> false).join();
            } catch (Exception e) { e.printStackTrace(); return false; }
        });
    }

    

    public CompletableFuture<JsonArray> getAllCargos()  { return supabase.select("cargo", "*", null); }
    public CompletableFuture<JsonArray> getAllUsers()   { return supabase.select("users", "*", "role=neq.admin"); }
    public CompletableFuture<JsonArray> getBlacklist() { return supabase.select("blacklist", "*", null); }
    public CompletableFuture<JsonArray> getLogs()      { return supabase.select("logs", "*", "order=created_at.desc"); }

    public CompletableFuture<JsonArray> getUserProfile(String login) {
        return supabase.select("users", "email,phone", "login=eq." + login);
    }

    public CompletableFuture<JsonArray> getUserCargos(String login) {
        return supabase.select("users", "id", "login=eq." + login)
                .thenCompose(r -> {
                    if (r.isEmpty()) return CompletableFuture.completedFuture(new JsonArray());
                    int userId = r.get(0).getAsJsonObject().get("id").getAsInt();
                    return supabase.select("cargo", "*", "заказчик_id=eq." + userId);
                });
    }

    public CompletableFuture<BlockStatus> checkBlacklist(String login) {
        return supabase.select("blacklist", "*", "login=eq." + login)
                .thenApply(r -> r.size() > 0
                        ? new BlockStatus(true, r.get(0).getAsJsonObject().get("reason").getAsString())
                        : new BlockStatus(false, null));
    }

    

    public CompletableFuture<Boolean> sendMessage(String senderLogin, String receiverLogin, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("sender_login", senderLogin);
        msg.addProperty("receiver_login", receiverLogin);
        msg.addProperty("content", content);
        msg.addProperty("is_read", false);

        return supabase.insert("messages", msg)
                .thenApply(r -> true)
                .exceptionally(ex -> false);
    }

    public CompletableFuture<com.google.gson.JsonArray> getChatHistory(String login1, String login2) {
        
        String filter = "or=(and(sender_login.eq." + login1 + ",receiver_login.eq." + login2 + "),and(sender_login.eq." + login2 + ",receiver_login.eq." + login1 + "))";
        return supabase.selectWithRaw("messages", "*,created_at", filter + "&order=created_at.asc");
    }

    public CompletableFuture<com.google.gson.JsonArray> getMyConversations(String login) {
        
        return supabase.selectWithRaw("messages", "*",
                "or=(sender_login.eq." + login + ",receiver_login.eq." + login + ")&order=created_at.desc");
    }

    

    public CompletableFuture<Boolean> addFavorite(String login, int cargoId) {
        return supabase.select("users", "id", "login=eq." + login)
                .thenCompose(r -> {
                    if (r.isEmpty()) return CompletableFuture.completedFuture(false);
                    int userId = r.get(0).getAsJsonObject().get("id").getAsInt();
                    JsonObject fav = new JsonObject();
                    fav.addProperty("user_id", userId);
                    fav.addProperty("cargo_id", cargoId);
                    return supabase.insert("favorites", fav)
                            .thenApply(res -> true)
                            .exceptionally(ex -> false);
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> removeFavorite(String login, int cargoId) {
        return supabase.select("users", "id", "login=eq." + login)
                .thenCompose(r -> {
                    if (r.isEmpty()) return CompletableFuture.completedFuture(false);
                    int userId = r.get(0).getAsJsonObject().get("id").getAsInt();
                    return supabase.delete("favorites", "user_id=eq." + userId + ",cargo_id=eq." + cargoId);
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<com.google.gson.JsonArray> getFavoriteCargos(String login) {
        return supabase.select("users", "id", "login=eq." + login)
                .thenCompose(r -> {
                    if (r.isEmpty()) return CompletableFuture.completedFuture(new com.google.gson.JsonArray());
                    int userId = r.get(0).getAsJsonObject().get("id").getAsInt();
                    
                    return supabase.select("favorites", "cargo_id", "user_id=eq." + userId)
                            .thenCompose(favs -> {
                                if (favs.isEmpty()) return CompletableFuture.completedFuture(new com.google.gson.JsonArray());
                                
                                StringBuilder ids = new StringBuilder("(");
                                for (int i = 0; i < favs.size(); i++) {
                                    ids.append(favs.get(i).getAsJsonObject().get("cargo_id").getAsInt());
                                    if (i < favs.size() - 1) ids.append(",");
                                }
                                ids.append(")");
                                return supabase.selectWithRaw("cargo", "*", "id=in." + ids);
                            });
                })
                .exceptionally(ex -> new com.google.gson.JsonArray());
    }


    public static class BlockStatus {
        public final boolean isBlocked;
        public final String reason;
        public BlockStatus(boolean isBlocked, String reason) { this.isBlocked = isBlocked; this.reason = reason; }
    }

    public static class UserAuthResult {
        public final boolean success;
        public final String role, message, blockReason;
        public final boolean emailConfirmed;

        public UserAuthResult(boolean s, String r, String m) {
            this(s, r, m, null, false);
        }
        public UserAuthResult(boolean s, String r, String m, String br) {
            this(s, r, m, br, false);
        }
        public UserAuthResult(boolean s, String r, String m, String br, boolean emailConfirmed) {
            success = s; role = r; message = m; blockReason = br;
            this.emailConfirmed = emailConfirmed;
        }

        public boolean isSuccess()         { return success; }
        public String getRole()            { return role; }
        public String getMessage()         { return message; }
        public String getBlockReason()     { return blockReason; }
        public boolean isEmailConfirmed()  { return emailConfirmed; }
    }

    public static class PasswordResetInfo {
        public final String login, email;
        public PasswordResetInfo(String l, String e) { login=l; email=e; }
    }

    
    public CompletableFuture<String> getServiceTokenForEmail(String email) {
        
        
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                
                String tempPass = "Tmp_" + System.currentTimeMillis();

                
                Boolean changed = supabase.adminUpdateUserPassword(email, tempPass).get();
                if (!changed) return null;

                
                var json = supabase.authSignIn(email, tempPass).get();
                if (json == null || !json.has("access_token")) return null;

                return json.get("access_token").getAsString();
            } catch (Exception e) {
                System.err.println("[AuthService] getServiceTokenForEmail error: " + e.getMessage());
                return null;
            }
        });
    }

    

    public CompletableFuture<Boolean> checkLoginExists(String login) {
        return supabase.selectWithRaw("users", "id", "login=eq." + login)
                .thenApply(arr -> arr != null && arr.size() > 0)
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> checkEmailExists(String email) {
        return supabase.selectWithRaw("users", "id", "email=eq." + email)
                .thenApply(arr -> arr != null && arr.size() > 0)
                .exceptionally(ex -> false);
    }
}