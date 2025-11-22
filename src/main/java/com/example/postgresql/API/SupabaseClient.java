package com.example.postgresql.API;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SupabaseClient {
    public static final String SUPABASE_URL = "https://mkdwltdoayuhuikzycod.supabase.co";
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1rZHdsdGRvYXl1aHVpa3p5Y29kIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4MDc5MTQsImV4cCI6MjA3OTE2NzkxNH0.3z8WeD6-03mNN5y421-Ujl9ZdpdTdfJUaf9ersZG25A";
    private static final String API_URL = SUPABASE_URL + "/rest/v1";

    private final OkHttpClient client;
    private final Gson gson;

    public SupabaseClient() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public CompletableFuture<JsonArray> select(String table, String select, String filters) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(API_URL + "/" + table).newBuilder();
        urlBuilder.addQueryParameter("select", select);

        if (filters == null || filters.isEmpty()) {
            urlBuilder.addQueryParameter("offset", "0");
        } else {
            String[] filterParts = filters.split(",");
            for (String filter : filterParts) {
                String trimmed = filter.trim();
                if (trimmed.contains("=")) {
                    String[] kv = trimmed.split("=", 2);
                    if (kv.length == 2) {
                        urlBuilder.addQueryParameter(kv[0].trim(), kv[1].trim());
                    }
                }
            }
        }

        HttpUrl url = urlBuilder.build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=representation")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";

                if (response.isSuccessful()) {
                    JsonArray result = gson.fromJson(body, JsonArray.class);
                    future.complete(result);
                } else {
                    System.err.println("Supabase SELECT failed: " + response.code() + " " + body);
                    future.complete(new JsonArray());
                }
                response.close();
            }
        });

        return future;
    }
    public CompletableFuture<JsonObject> insert(String table, JsonObject data) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        RequestBody body = RequestBody.create(
                gson.toJson(data),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(API_URL + "/" + table)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonArray result = gson.fromJson(responseBody, JsonArray.class);
                    if (result.size() > 0) {
                        future.complete(result.get(0).getAsJsonObject());
                    } else {
                        future.complete(new JsonObject());
                    }
                } else {
                    future.completeExceptionally(
                            new IOException("Insert failed: " + response.code())
                    );
                }
                response.close();
            }
        });

        return future;
    }
    public CompletableFuture<Boolean> update(String table, JsonObject data, String filter) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(API_URL + "/" + table).newBuilder();

        if (filter != null && !filter.isEmpty()) {
            String[] parts = filter.split("=");
            if (parts.length == 2) {
                urlBuilder.addQueryParameter(parts[0], parts[1]);
            }
        }

        RequestBody body = RequestBody.create(
                gson.toJson(data),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .patch(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                future.complete(response.isSuccessful());
                response.close();
            }
        });

        return future;
    }
    public CompletableFuture<Boolean> delete(String table, String filter) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(API_URL + "/" + table).newBuilder();

        if (filter != null && !filter.isEmpty()) {
            String[] parts = filter.split("=");
            if (parts.length == 2) {
                urlBuilder.addQueryParameter(parts[0], parts[1]);
            }
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                future.complete(response.isSuccessful());
                response.close();
            }
        });

        return future;
    }
}
