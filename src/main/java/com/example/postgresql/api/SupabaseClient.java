package com.example.postgresql.API;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SupabaseClient {

    public static final String SUPABASE_URL = AppConfig.getInstance().getSupabaseUrl();
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

    public CompletableFuture<JsonArray> selectTwoFilters(String table, String select, String filter1, String filter2) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        HttpUrl.Builder ub = HttpUrl.parse(REST_URL + "/" + table).newBuilder();
        ub.addQueryParameter("select", select);
        addFilter(ub, filter1);
        addFilter(ub, filter2);
        http.newCall(restGet(ub.build(), SUPABASE_ANON_KEY)).enqueue(new JsonArrayCallback(future));
        return future;
    }

    public CompletableFuture<Boolean> deleteTwoFilters(String table, String filter1, String filter2) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        HttpUrl.Builder ub = HttpUrl.parse(REST_URL + "/" + table).newBuilder();
        addFilter(ub, filter1);
        addFilter(ub, filter2);
        Request req = new Request.Builder()
                .url(ub.build())
                .headers(baseHeaders(SUPABASE_ANON_KEY))
                .delete().build();
        http.newCall(req).enqueue(new BoolCallback(future));
        return future;
    }

    private void addFilter(HttpUrl.Builder ub, String filter) {
        if (filter == null || filter.isEmpty()) return;
        int eq = filter.indexOf('=');
        if (eq > 0) ub.addQueryParameter(filter.substring(0, eq), filter.substring(eq + 1));
    }

    public CompletableFuture<JsonArray> selectWithServiceRole(String table, String select, String rawFilter) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        HttpUrl.Builder ub = HttpUrl.parse(REST_URL + "/" + table).newBuilder();
        ub.addQueryParameter("select", select);
        if (rawFilter != null && !rawFilter.isEmpty()) {
            int eq = rawFilter.indexOf('=');
            if (eq > 0) ub.addQueryParameter(rawFilter.substring(0, eq), rawFilter.substring(eq + 1));
        }
        Request req = new Request.Builder().url(ub.build())
                .addHeader("apikey", SUPABASE_SERVICE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                .addHeader("Accept", "application/json")
                .get().build();
        http.newCall(req).enqueue(new JsonArrayCallback(future));
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
        return selectWithRaw("users", "email,email_confirmed", "login=eq." + login)
                .thenApply(result -> {
                    if (result == null || result.isEmpty()) return false;
                    JsonObject user = result.get(0).getAsJsonObject();
                    return user.has("email_confirmed")
                            && !user.get("email_confirmed").isJsonNull()
                            && user.get("email_confirmed").getAsBoolean();
                })
                .exceptionally(ex -> false);
    }

    public CompletableFuture<Boolean> adminUpdateUserPassword(String email, String newPassword) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        selectWithServiceRole("users", "auth_uuid", "email=eq." + email)
                .thenAccept(rows -> {
                    System.out.println("[adminUpdateUserPassword] rows for " + email + ": " + rows);
                    if (rows == null || rows.isEmpty()) {
                        System.err.println("[adminUpdateUserPassword] user not found by email: " + email);
                        future.complete(false);
                        return;
                    }
                    JsonObject u = rows.get(0).getAsJsonObject();
                    if (!u.has("auth_uuid") || u.get("auth_uuid").isJsonNull()) {
                        System.err.println("[adminUpdateUserPassword] auth_uuid is null for email: " + email);
                        future.complete(false);
                        return;
                    }
                    String authUuid = u.get("auth_uuid").getAsString();
                    System.out.println("[adminUpdateUserPassword] changing password for auth_uuid: " + authUuid);

                    JsonObject patch = new JsonObject();
                    patch.addProperty("password", newPassword);

                    Request patchReq = new Request.Builder()
                            .url(AUTH_URL + "/admin/users/" + authUuid)
                            .addHeader("apikey", SUPABASE_SERVICE_KEY)
                            .addHeader("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                            .addHeader("Content-Type", "application/json")
                            .put(RequestBody.create(gson.toJson(patch), MediaType.parse("application/json")))
                            .build();

                    http.newCall(patchReq).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call c, java.io.IOException e) {
                            System.err.println("[adminUpdateUserPassword] HTTP failure: " + e.getMessage());
                            future.complete(false);
                        }

                        @Override
                        public void onResponse(Call c, Response r) throws java.io.IOException {
                            String body = r.body() != null ? r.body().string() : "";
                            System.out.println("[adminUpdateUserPassword] response " + r.code() + ": " + body);
                            future.complete(r.isSuccessful());
                            r.close();
                        }
                    });
                })
                .exceptionally(ex -> {
                    System.err.println("[adminUpdateUserPassword] exception: " + ex.getMessage());
                    future.complete(false);
                    return null;
                });

        return future;
    }

    public CompletableFuture<Boolean> adminDeleteAuthUser(String authUuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (authUuid == null || authUuid.isBlank()) {
            future.complete(false);
            return future;
        }

        Request del = new Request.Builder()
                .url(AUTH_URL + "/admin/users/" + authUuid)
                .addHeader("apikey", SUPABASE_SERVICE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                .delete().build();

        http.newCall(del).enqueue(new BoolCallback(future));
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

        JsonArrayCallback(CompletableFuture<JsonArray> f) {
            this.f = f;
        }

        @Override
        public void onFailure(Call c, IOException e) {
            f.completeExceptionally(e);
        }

        @Override
        public void onResponse(Call c, Response r) throws IOException {
            String body = r.body() != null ? r.body().string() : "[]";
            if (r.isSuccessful()) {
                try {
                    f.complete(gson.fromJson(body, JsonArray.class));
                } catch (Exception ex) {
                    f.completeExceptionally(new IOException("Ожидался массив: " + body));
                }
            } else {
                f.completeExceptionally(new IOException("HTTP " + r.code() + ": " + body));
            }
            r.close();
        }
    }

    private class JsonObjectCallback implements Callback {
        private final CompletableFuture<JsonObject> f;

        JsonObjectCallback(CompletableFuture<JsonObject> f) {
            this.f = f;
        }

        @Override
        public void onFailure(Call c, IOException e) {
            f.completeExceptionally(e);
        }

        @Override
        public void onResponse(Call c, Response r) throws IOException {
            String body = r.body() != null ? r.body().string() : "{}";
            try {
                JsonObject obj = gson.fromJson(body, JsonObject.class);
                if (!r.isSuccessful() && obj != null && !obj.has("error") && !obj.has("error_description")) {
                    obj.addProperty("error", "http_" + r.code());
                    obj.addProperty("error_description", "HTTP " + r.code());
                }
                f.complete(obj);
            } catch (Exception ex) {
                f.completeExceptionally(new IOException("HTTP " + r.code() + ": " + body));
            }
            r.close();
        }
    }

    private static class BoolCallback implements Callback {
        private final CompletableFuture<Boolean> f;

        BoolCallback(CompletableFuture<Boolean> f) {
            this.f = f;
        }

        @Override
        public void onFailure(Call c, IOException e) {
            f.completeExceptionally(e);
        }

        @Override
        public void onResponse(Call c, Response r) throws IOException {
            f.complete(r.isSuccessful());
            r.close();
        }
    }
}