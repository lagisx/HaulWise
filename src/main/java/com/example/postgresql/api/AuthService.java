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

        return supabase.selectWithRaw("users", "login,email,phone",
                        "or=(login.eq." + login + ",email.eq." + email + ",phone.eq." + phone + ")")
                .thenCompose(existing -> {
                    if (existing != null && existing.size() > 0) {
                        JsonObject found = existing.get(0).getAsJsonObject();
                        String foundLogin = found.has("login") && !found.get("login").isJsonNull()
                                ? found.get("login").getAsString() : "";
                        String foundEmail = found.has("email") && !found.get("email").isJsonNull()
                                ? found.get("email").getAsString() : "";
                        String foundPhone = found.has("phone") && !found.get("phone").isJsonNull()
                                ? found.get("phone").getAsString() : "";

                        if (foundLogin.equalsIgnoreCase(login))
                            return CompletableFuture.completedFuture("Логин уже занят");
                        if (foundEmail.equalsIgnoreCase(email))
                            return CompletableFuture.completedFuture("Email уже зарегистрирован");
                        if (foundPhone.equals(phone))
                            return CompletableFuture.completedFuture("Телефон уже используется");
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
                                    if (authResp.has("msg") && !authResp.get("msg").isJsonNull())
                                        errMsg = authResp.get("msg").getAsString();
                                    else if (authResp.has("message") && !authResp.get("message").isJsonNull())
                                        errMsg = authResp.get("message").getAsString();
                                    else if (authResp.has("error") && !authResp.get("error").isJsonNull())
                                        errMsg = authResp.get("error").getAsString();

                                    String lower = errMsg.toLowerCase();
                                    if (lower.contains("already") || lower.contains("exists") || lower.contains("registered"))
                                        return CompletableFuture.completedFuture("Email уже зарегистрирован в системе");
                                    if (lower.contains("password") || lower.contains("weak"))
                                        return CompletableFuture.completedFuture("Пароль слишком простой. Используйте минимум 6 символов");
                                    if (lower.contains("invalid") && lower.contains("email"))
                                        return CompletableFuture.completedFuture("Введите корректный email-адрес");
                                    return CompletableFuture.completedFuture("Не удалось создать аккаунт. Попробуйте позже");
                                }

                                if (authResp.has("code") && authResp.has("msg")) {
                                    try {
                                        if (authResp.get("code").getAsInt() >= 400)
                                            return CompletableFuture.completedFuture(
                                                    authResp.get("msg").getAsString());
                                    } catch (Exception ignored) {
                                    }
                                }

                                String authUuid = null;
                                if (authResp.has("user") && !authResp.get("user").isJsonNull()) {
                                    JsonObject userObj = authResp.getAsJsonObject("user");
                                    if (userObj.has("id") && !userObj.get("id").isJsonNull())
                                        authUuid = userObj.get("id").getAsString();
                                }

                                if (authUuid == null) {
                                    return CompletableFuture.completedFuture("Не удалось получить ID пользователя от Auth");
                                }

                                final String finalAuthUuid = authUuid;

                                JsonObject row = new JsonObject();
                                row.addProperty("login", login);
                                row.addProperty("email", email);
                                row.addProperty("phone", phone);
                                row.addProperty("role", "user");
                                row.addProperty("status", false);
                                row.addProperty("email_confirmed", false);
                                row.addProperty("auth_uuid", finalAuthUuid);

                                return supabase.insertServiceRole("users", row)
                                        .thenApply(r -> {
                                            logAction(login, "Зарегистрирован. Email: " + email);
                                            return (String) null;
                                        })
                                        .exceptionally(ex -> {
                                            supabase.adminDeleteAuthUser(finalAuthUuid);
                                            return "Не удалось сохранить данные. Попробуйте позже";
                                        });
                            })
                            .exceptionally(ex -> "Не удалось подключиться к серверу. Проверьте интернет-соединение");
                })
                .exceptionally(ex -> "Не удалось подключиться к серверу. Проверьте интернет-соединение");
    }

    public CompletableFuture<UserAuthResult> authenticate(String loginOrEmail, String password) {
        return resolveEmail(loginOrEmail).thenCompose(email -> {
            if (email == null)
                return CompletableFuture.completedFuture(new UserAuthResult(false, null, "Неверный логин или пароль"));

            return supabase.authSignIn(email, password)
                    .thenCompose(authResp -> {
                        System.out.println("[AUTH] signIn response for " + email + ": " + authResp);
                        if (authResp == null
                                || authResp.has("error_description")
                                || authResp.has("error")) {
                            System.out.println("[AUTH] login rejected, error in response");
                            return CompletableFuture.completedFuture(new UserAuthResult(false, null, "Неверный логин или пароль"));
                        }
                        System.out.println("[AUTH] signIn accepted, fetching user profile...");

                        return supabase.select("users", "role,status,login,email_confirmed", "email=eq." + email)
                                .thenCompose(rows -> {
                                    if (rows == null || rows.isEmpty())
                                        return CompletableFuture.completedFuture(
                                                new UserAuthResult(false, null, "Неверный логин или пароль"));

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
                    .exceptionally(ex -> new UserAuthResult(false, null, "Не удалось подключиться к серверу. Проверьте интернет-соединение."));
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

    public CompletableFuture<Boolean> deleteUser(String login) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.selectWithServiceRole("users", "id,auth_uuid", "login=eq." + login)
                        .thenCompose(r -> {
                            if (r.isEmpty()) return CompletableFuture.completedFuture(false);

                            JsonObject userRow = r.get(0).getAsJsonObject();
                            int uid = userRow.get("id").getAsInt();
                            String authUuid = userRow.has("auth_uuid") && !userRow.get("auth_uuid").isJsonNull()
                                    ? userRow.get("auth_uuid").getAsString() : null;

                            return supabase.delete("cargo", "заказчик_id=eq." + uid)
                                    .thenCompose(v -> supabase.delete("users", "login=eq." + login))
                                    .thenCompose(del -> {
                                        if (!del) return CompletableFuture.completedFuture(false);
                                        logAction(login, "Пользователь " + login + " удалил свой аккаунт");
                                        if (authUuid != null) {
                                            return supabase.adminDeleteAuthUser(authUuid);
                                        }
                                        return CompletableFuture.completedFuture(true);
                                    });
                        })
                        .exceptionally(ex -> false).join();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> deleteUserAndCargo(int userId, String login) {
        return supabase.selectWithServiceRole("users", "auth_uuid", "id=eq." + userId)
                .thenCompose(r -> {
                    String authUuid = (!r.isEmpty() && r.get(0).getAsJsonObject().has("auth_uuid")
                            && !r.get(0).getAsJsonObject().get("auth_uuid").isJsonNull())
                            ? r.get(0).getAsJsonObject().get("auth_uuid").getAsString() : null;

                    return supabase.delete("cargo", "заказчик_id=eq." + userId)
                            .thenCompose(v -> supabase.delete("users", "id=eq." + userId))
                            .thenCompose(del -> {
                                logAction(login, "Удалён администратором вместе со всеми грузами");
                                if (authUuid != null) {
                                    return supabase.adminDeleteAuthUser(authUuid);
                                }
                                return CompletableFuture.completedFuture(true);
                            });
                })
                .exceptionally(ex -> false);
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
                    if (resp != null && resp.has("access_token") && !resp.get("access_token").isJsonNull())
                        return resp.get("access_token").getAsString();
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
        return supabase.selectWithRaw("users", "login,email,email_confirmed",
                        "or=(login.eq." + id + ",email.eq." + id + ")")
                .thenApply(r -> {
                    if (r == null || r.isEmpty()) return null;
                    JsonObject u = r.get(0).getAsJsonObject();
                    String login = u.has("login") ? u.get("login").getAsString() : null;
                    String email = u.has("email") && !u.get("email").isJsonNull()
                            ? u.get("email").getAsString() : null;
                    boolean confirmed = u.has("email_confirmed") && !u.get("email_confirmed").isJsonNull()
                            && u.get("email_confirmed").getAsBoolean();
                    return new PasswordResetInfo(login, email, confirmed);
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

    public CompletableFuture<Boolean> deleteCargo(int cargoId) {
        return supabase.delete("cargo", "id=eq." + cargoId)
                .thenApply(v -> {
                    logAction(null, "Удалён груз ID=" + cargoId + " (админ)");
                    return true;
                })
                .exceptionally(ex -> false);
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
                                    .thenApply(v -> {
                                        logAction(username, "Обновил профиль");
                                        return true;
                                    });
                        })
                        .exceptionally(ex -> false).join();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<JsonArray> getAllCargos() {
        return supabase.select("cargo", "*", null);
    }

    public CompletableFuture<JsonArray> getAllUsers() {
        return supabase.select("users", "*", "role=neq.admin");
    }

    public CompletableFuture<JsonArray> getBlacklist() {
        return supabase.select("blacklist", "*", null);
    }

    public CompletableFuture<JsonArray> getLogs() {
        return supabase.select("logs", "*", "order=created_at.desc");
    }

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
                    if (r == null || r.isEmpty()) return CompletableFuture.completedFuture(false);
                    int userId = r.get(0).getAsJsonObject().get("id").getAsInt();
                    return supabase.selectTwoFilters("favorites", "id",
                                    "user_id=eq." + userId, "cargo_id=eq." + cargoId)
                            .thenCompose(existing -> {
                                if (existing != null && !existing.isEmpty())
                                    return CompletableFuture.completedFuture(true);
                                JsonObject fav = new JsonObject();
                                fav.addProperty("user_id", userId);
                                fav.addProperty("cargo_id", cargoId);
                                return supabase.insert("favorites", fav)
                                        .thenApply(res -> res != null)
                                        .exceptionally(ex -> {
                                            System.err.println("[addFavorite] insert error: " + ex.getMessage());
                                            return false;
                                        });
                            });
                })
                .exceptionally(ex -> {
                    System.err.println("[addFavorite] error: " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Boolean> removeFavorite(String login, int cargoId) {
        return supabase.select("users", "id", "login=eq." + login)
                .thenCompose(r -> {
                    if (r == null || r.isEmpty()) return CompletableFuture.completedFuture(false);
                    int userId = r.get(0).getAsJsonObject().get("id").getAsInt();
                    return supabase.deleteTwoFilters("favorites",
                            "user_id=eq." + userId, "cargo_id=eq." + cargoId);
                })
                .exceptionally(ex -> {
                    System.err.println("[removeFavorite] error: " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<com.google.gson.JsonArray> getFavoriteCargos(String login) {
        return supabase.select("users", "id", "login=eq." + login)
                .thenCompose(r -> {
                    if (r == null || r.isEmpty())
                        return CompletableFuture.completedFuture(new com.google.gson.JsonArray());
                    int userId = r.get(0).getAsJsonObject().get("id").getAsInt();
                    return supabase.selectWithRaw("favorites", "cargo_id", "user_id=eq." + userId)
                            .thenCompose(favs -> {
                                if (favs == null || favs.isEmpty())
                                    return CompletableFuture.completedFuture(new com.google.gson.JsonArray());
                                StringBuilder ids = new StringBuilder("(");
                                for (int i = 0; i < favs.size(); i++) {
                                    ids.append(favs.get(i).getAsJsonObject().get("cargo_id").getAsInt());
                                    if (i < favs.size() - 1) ids.append(",");
                                }
                                ids.append(")");
                                return supabase.selectWithRaw("cargo", "*", "id=in." + ids);
                            });
                })
                .exceptionally(ex -> {
                    System.err.println("[getFavoriteCargos] error: " + ex.getMessage());
                    return new com.google.gson.JsonArray();
                });
    }

    public CompletableFuture<Boolean> isFavorite(String login, int cargoId) {
        return supabase.select("users", "id", "login=eq." + login)
                .thenCompose(r -> {
                    if (r == null || r.isEmpty()) return CompletableFuture.completedFuture(false);
                    int userId = r.get(0).getAsJsonObject().get("id").getAsInt();
                    return supabase.selectTwoFilters("favorites", "id",
                                    "user_id=eq." + userId, "cargo_id=eq." + cargoId)
                            .thenApply(res -> res != null && !res.isEmpty());
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> adminChangePassword_byLogin(String login, String newPassword) {
        return supabase.selectWithServiceRole("users", "email", "login=eq." + login)
                .thenCompose(rows -> {
                    if (rows == null || rows.isEmpty()) {
                        System.err.println("[adminChangePassword_byLogin] user not found: " + login);
                        return CompletableFuture.completedFuture(false);
                    }
                    String email = rows.get(0).getAsJsonObject().get("email").getAsString();
                    System.out.println("[adminChangePassword_byLogin] found email: " + email + " for login: " + login);
                    return adminChangePassword(email, newPassword);
                })
                .exceptionally(ex -> {
                    System.err.println("[adminChangePassword_byLogin] error: " + ex.getMessage());
                    return false;
                });
    }


    public CompletableFuture<Boolean> adminChangePassword(String email, String newPassword) {
        return supabase.adminUpdateUserPassword(email, newPassword)
                .thenApply(ok -> {
                    if (ok) logAction(null, "Пароль изменён через admin API для: " + email);
                    return ok;
                })
                .exceptionally(ex -> {
                    System.err.println("[AuthService] adminChangePassword error: " + ex.getMessage());
                    return false;
                });
    }

    @Deprecated
    public CompletableFuture<String> getServiceTokenForEmail(String email) {
        return CompletableFuture.completedFuture(null);
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
        public final String role, message, blockReason;
        public final boolean emailConfirmed;

        public UserAuthResult(boolean s, String r, String m) {
            this(s, r, m, null, false);
        }

        public UserAuthResult(boolean s, String r, String m, String br) {
            this(s, r, m, br, false);
        }

        public UserAuthResult(boolean s, String r, String m, String br, boolean emailConfirmed) {
            success = s;
            role = r;
            message = m;
            blockReason = br;
            this.emailConfirmed = emailConfirmed;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getRole() {
            return role;
        }

        public String getMessage() {
            return message;
        }

        public String getBlockReason() {
            return blockReason;
        }

        public boolean isEmailConfirmed() {
            return emailConfirmed;
        }
    }

    public static class PasswordResetInfo {
        public final String login, email;
        public final boolean emailConfirmed;

        public PasswordResetInfo(String l, String e) {
            this(l, e, false);
        }

        public PasswordResetInfo(String l, String e, boolean confirmed) {
            login = l;
            email = e;
            emailConfirmed = confirmed;
        }
    }
}