package com.example.postgresql.API;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SupabaseClient {

    
    public static final String SUPABASE_URL      = AppConfig.getInstance().getSupabaseUrl();
    public static final String SUPABASE_ANON_KEY = AppConfig.getInstance().getAnonKey();
    public static final String SUPABASE_SERVICE_KEY = AppConfig.getInstance().getServiceKey();

    private static final String REST_URL = SUPABASE_URL + "/rest/v1";
    private static final String AUTH_URL = SUPABASE_URL + "/auth/v1";

    private final OkHttpClient http = new OkHttpClient();
    final Gson gson = new Gson();

    public CompletableFuture<JsonArray> select(String table, String select, String filters) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        HttpUrl.Builder ub = HttpUrl.parse(REST_URL + "/" + table).newBuilder();
        ub.addQueryParameter("select", select);
        if (filters == null || filters.isEmpty()) {
            ub.addQueryParameter("offset", "0");
        } else {
            for (String f : filters.split(",")) {
                f = f.trim();
                int eq = f.indexOf('=');
                if (eq > 0) ub.addQueryParameter(f.substring(0, eq).trim(), f.substring(eq + 1).trim());
            }
        }
        http.newCall(restGet(ub.build(), SUPABASE_ANON_KEY)).enqueue(new JsonArrayCallback(future));
        return future;
    }

    public CompletableFuture<JsonArray> selectWithRaw(String table, String select, String rawFilter) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        HttpUrl.Builder ub = HttpUrl.parse(REST_URL + "/" + table).newBuilder();
        ub.addQueryParameter("select", select);
        if (rawFilter != null && !rawFilter.isEmpty()) {
            int eq = rawFilter.indexOf('=');
            if (eq > 0) ub.addQueryParameter(rawFilter.substring(0, eq), rawFilter.substring(eq + 1));
        }
        http.newCall(restGet(ub.build(), SUPABASE_ANON_KEY)).enqueue(new JsonArrayCallback(future));
        return future;
    }

    public CompletableFuture<JsonArray> insert(String table, JsonObject data) {
        return insertWithToken(table, data, SUPABASE_ANON_KEY);
    }

    public CompletableFuture<JsonArray> insertWithToken(String table, JsonObject data, String token) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Request req = new Request.Builder()
                .url(REST_URL + "/" + table)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(json(gson.toJson(data))).build();
        http.newCall(req).enqueue(new JsonArrayCallback(future));
        return future;
    }

    
    public CompletableFuture<JsonArray> insertServiceRole(String table, JsonObject data) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Request req = new Request.Builder()
                .url(REST_URL + "/" + table)
                .addHeader("apikey", SUPABASE_SERVICE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(json(gson.toJson(data))).build();
        http.newCall(req).enqueue(new JsonArrayCallback(future));
        return future;
    }

    public CompletableFuture<Boolean> update(String table, JsonObject data, String filter) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        HttpUrl.Builder ub = HttpUrl.parse(REST_URL + "/" + table).newBuilder();
        if (filter != null && !filter.isEmpty()) {
            int eq = filter.indexOf('=');
            if (eq > 0) ub.addQueryParameter(filter.substring(0, eq), filter.substring(eq + 1));
        }
        Request req = new Request.Builder()
                .url(ub.build())
                .headers(baseHeaders(SUPABASE_ANON_KEY))
                .patch(json(gson.toJson(data))).build();
        http.newCall(req).enqueue(new BoolCallback(future));
        return future;
    }

    public CompletableFuture<Boolean> delete(String table, String filter) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        HttpUrl.Builder ub = HttpUrl.parse(REST_URL + "/" + table).newBuilder();
        if (filter != null && !filter.isEmpty()) {
            int eq = filter.indexOf('=');
            if (eq > 0) ub.addQueryParameter(filter.substring(0, eq), filter.substring(eq + 1));
        }
        Request req = new Request.Builder()
                .url(ub.build())
                .headers(baseHeaders(SUPABASE_ANON_KEY))
                .delete().build();
        http.newCall(req).enqueue(new BoolCallback(future));
        return future;
    }

    public CompletableFuture<JsonObject> authSignUp(String email, String password, JsonObject metadata) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        if (metadata != null) body.add("data", metadata);
        Request req = new Request.Builder()
                .url(AUTH_URL + "/signup")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(json(gson.toJson(body))).build();
        http.newCall(req).enqueue(new JsonObjectCallback(future));
        return future;
    }

    public CompletableFuture<JsonObject> authSignIn(String email, String password) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        Request req = new Request.Builder()
                .url(AUTH_URL + "/token?grant_type=password")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(json(gson.toJson(body))).build();
        http.newCall(req).enqueue(new JsonObjectCallback(future));
        return future;
    }

    public CompletableFuture<Boolean> authResetPassword(String email) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        Request req = new Request.Builder()
                .url(AUTH_URL + "/recover")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(json(gson.toJson(body))).build();
        http.newCall(req).enqueue(new BoolCallback(future));
        return future;
    }

    
    public CompletableFuture<Boolean> authSendRecoveryOTP(String email) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        Request req = new Request.Builder()
                .url(AUTH_URL + "/otp")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(json(gson.toJson(body))).build();
        http.newCall(req).enqueue(new BoolCallback(future));
        return future;
    }

    
    public CompletableFuture<JsonObject> authVerifyOTP(String email, String token) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("token", token);
        body.addProperty("type", "email");
        Request req = new Request.Builder()
                .url(AUTH_URL + "/verify")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(json(gson.toJson(body))).build();
        http.newCall(req).enqueue(new JsonObjectCallback(future));
        return future;
    }

    
    public CompletableFuture<Boolean> authUpdatePassword(String accessToken, String newPassword) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject body = new JsonObject();
        body.addProperty("password", newPassword);
        Request req = new Request.Builder()
                .url(AUTH_URL + "/user")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .put(json(gson.toJson(body))).build();
        http.newCall(req).enqueue(new BoolCallback(future));
        return future;
    }

    
    public CompletableFuture<Boolean> authResendConfirmation(String email) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject body = new JsonObject();
        body.addProperty("type", "signup");
        body.addProperty("email", email);
        Request req = new Request.Builder()
                .url(AUTH_URL + "/resend")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(json(gson.toJson(body))).build();
        http.newCall(req).enqueue(new BoolCallback(future));
        return future;
    }

    
    public CompletableFuture<Boolean> isEmailConfirmed(String login) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/auth/v1/admin/users").newBuilder()
                .build();

        
        selectWithRaw("users", "email,email_confirmed", "login=eq." + login)
                .thenAccept(result -> {
                    if (result == null || result.isEmpty()) {
                        future.complete(false);
                        return;
                    }
                    JsonObject user = result.get(0).getAsJsonObject();
                    if (user.has("email_confirmed") && !user.get("email_confirmed").isJsonNull()) {
                        future.complete(user.get("email_confirmed").getAsBoolean());
                    } else {
                        
                        future.complete(false);
                    }
                })
                .exceptionally(ex -> {
                    future.complete(false);
                    return null;
                });

        return future;
    }

    
    public CompletableFuture<Boolean> adminUpdateUserPassword(String email, String newPassword) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", newPassword);

        
        
        JsonObject linkBody = new JsonObject();
        linkBody.addProperty("type", "recovery");
        linkBody.addProperty("email", email);
        linkBody.addProperty("new_password", newPassword);

        Request req = new Request.Builder()
                .url(AUTH_URL + "/admin/users")
                .addHeader("apikey", SUPABASE_SERVICE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                .addHeader("Content-Type", "application/json")
                .get().build();

        
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) { future.complete(false); }
            @Override public void onResponse(Call c, Response r) throws IOException {
                if (!r.isSuccessful()) { r.close(); future.complete(false); return; }
                String body2 = r.body() != null ? r.body().string() : "{}";
                r.close();
                try {
                    
                    com.google.gson.JsonObject resp = gson.fromJson(body2, com.google.gson.JsonObject.class);
                    com.google.gson.JsonArray users = resp.has("users")
                            ? resp.getAsJsonArray("users")
                            : new com.google.gson.JsonArray();

                    String userId = null;
                    for (com.google.gson.JsonElement el : users) {
                        com.google.gson.JsonObject u = el.getAsJsonObject();
                        if (u.has("email") && email.equalsIgnoreCase(u.get("email").getAsString())) {
                            userId = u.get("id").getAsString();
                            break;
                        }
                    }

                    if (userId == null) { future.complete(false); return; }

                    
                    JsonObject patch = new JsonObject();
                    patch.addProperty("password", newPassword);

                    Request patchReq = new Request.Builder()
                            .url(AUTH_URL + "/admin/users/" + userId)
                            .addHeader("apikey", SUPABASE_SERVICE_KEY)
                            .addHeader("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                            .addHeader("Content-Type", "application/json")
                            .put(RequestBody.create(gson.toJson(patch),
                                    MediaType.parse("application/json")))
                            .build();

                    http.newCall(patchReq).enqueue(new BoolCallback(future));
                } catch (Exception ex) {
                    System.err.println("[SupabaseClient] adminUpdateUserPassword: " + ex.getMessage());
                    future.complete(false);
                }
            }
        });

        return future;
    }

    public CompletableFuture<Boolean> adminDeleteAuthUser(String email) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/auth/v1/admin/users").newBuilder()
                .addQueryParameter("email", email)
                .build();
        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                .get().build();
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) { future.complete(false); }
            @Override public void onResponse(Call c, Response r) throws IOException {
                try {
                    String body = r.body() != null ? r.body().string() : "";
                    com.google.gson.JsonArray arr = gson.fromJson(body, com.google.gson.JsonArray.class);
                    if (arr == null || arr.size() == 0) { future.complete(false); return; }
                    String userId = arr.get(0).getAsJsonObject().get("id").getAsString();
                    Request del = new Request.Builder()
                            .url(AUTH_URL + "/admin/users/" + userId)
                            .addHeader("apikey", SUPABASE_SERVICE_KEY)
                            .addHeader("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                            .delete().build();
                    http.newCall(del).enqueue(new BoolCallback(future));
                } catch (Exception ex) { future.complete(false); }
            }
        });
        return future;
    }

    private Request restGet(HttpUrl url, String token) {
        return new Request.Builder().url(url)
                .headers(baseHeaders(token))
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=representation").get().build();
    }

    private Headers baseHeaders(String token) {
        return new Headers.Builder()
                .add("apikey", SUPABASE_ANON_KEY)
                .add("Authorization", "Bearer " + token)
                .add("Content-Type", "application/json")
                .build();
    }

    private static RequestBody json(String body) {
        return RequestBody.create(body, MediaType.parse("application/json"));
    }

    private class JsonArrayCallback implements Callback {
        private final CompletableFuture<JsonArray> f;
        JsonArrayCallback(CompletableFuture<JsonArray> f) { this.f = f; }
        @Override public void onFailure(Call c, IOException e) { f.completeExceptionally(e); }
        @Override public void onResponse(Call c, Response r) throws IOException {
            String body = r.body() != null ? r.body().string() : "[]";
            if (r.isSuccessful()) {
                try { f.complete(gson.fromJson(body, JsonArray.class)); }
                catch (Exception ex) { f.completeExceptionally(new IOException("Ожидался массив: " + body)); }
            } else {
                f.completeExceptionally(new IOException("HTTP " + r.code() + ": " + body));
            }
            r.close();
        }
    }

    private class JsonObjectCallback implements Callback {
        private final CompletableFuture<JsonObject> f;
        JsonObjectCallback(CompletableFuture<JsonObject> f) { this.f = f; }
        @Override public void onFailure(Call c, IOException e) { f.completeExceptionally(e); }
        @Override public void onResponse(Call c, Response r) throws IOException {
            String body = r.body() != null ? r.body().string() : "{}";
            try { f.complete(gson.fromJson(body, JsonObject.class)); }
            catch (Exception ex) { f.completeExceptionally(new IOException("HTTP " + r.code() + ": " + body)); }
            r.close();
        }
    }

    private static class BoolCallback implements Callback {
        private final CompletableFuture<Boolean> f;
        BoolCallback(CompletableFuture<Boolean> f) { this.f = f; }
        @Override public void onFailure(Call c, IOException e) { f.completeExceptionally(e); }
        @Override public void onResponse(Call c, Response r) throws IOException {
            f.complete(r.isSuccessful());
            r.close();
        }
    }
}