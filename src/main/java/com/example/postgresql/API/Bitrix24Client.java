package com.example.postgresql.API;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class Bitrix24Client {

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();
    private static Bitrix24Client instance;

    private Bitrix24Client() {
    }

    public static Bitrix24Client getInstance() {
        if (instance == null) instance = new Bitrix24Client();
        return instance;
    }


    public CompletableFuture<Integer> createDealFromCargo(String webhook, com.example.postgresql.UserF.Cargo cargo) {
        if (webhook == null || webhook.isBlank()) return CompletableFuture.completedFuture(-1);

        CompletableFuture<Integer> future = new CompletableFuture<>();
        JsonObject fields = new JsonObject();
        fields.addProperty("TITLE", "Груз: " + cargo.getProduct() + " | " + cargo.getFromCity() + " → " + cargo.getToCity());
        fields.addProperty("OPPORTUNITY", cargo.getPriceCard());
        fields.addProperty("CURRENCY_ID", "RUB");
        fields.addProperty("STAGE_ID", "NEW");
        fields.addProperty("COMMENTS", "Тип ТС: " + cargo.getVehicleType() + "\n"
                + "Вес: " + cargo.getWeight() + " т\n"
                + "Объём: " + cargo.getVolume() + " м³\n"
                + "Тип загрузки: " + cargo.getLoadType() + "\n"
                + "Детали: " + cargo.getLoadDetails() + "\n"
                + "Дата: " + cargo.getDate() + "\n"
                + "Торг: " + cargo.getBargain() + "\n"
                + "Телефон: " + cargo.getContactPhone());
        JsonObject body = new JsonObject();
        body.add("fields", fields);

        http.newCall(buildPost(webhook, "crm.deal.add.json", body)).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                System.err.println("[Bitrix24] createDeal error: " + e.getMessage());
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                String raw = bodyStr(r);
                try {
                    JsonObject json = gson.fromJson(raw, JsonObject.class);
                    if (json.has("result") && !json.get("result").isJsonNull()) {
                        int id = json.get("result").getAsInt();
                        System.out.println("[Bitrix24] Сделка создана, ID=" + id);
                        future.complete(id);
                    } else {
                        System.err.println("[Bitrix24] createDeal: " + raw);
                        future.complete(-1);
                    }
                } catch (Exception ex) {
                    future.completeExceptionally(new IOException(raw, ex));
                }
            }
        });
        return future;
    }


    private CompletableFuture<Integer> getResponsibleId(String webhook) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Request req = new Request.Builder().url(webhook.endsWith("/") ? webhook + "profile.json" : webhook + "/profile.json").get().build();
        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                future.complete(1);
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                String raw = bodyStr(r);
                try {
                    JsonObject json = gson.fromJson(raw, JsonObject.class);
                    if (json.has("result") && json.getAsJsonObject("result").has("ID")) {
                        future.complete(json.getAsJsonObject("result").get("ID").getAsInt());
                    } else {
                        future.complete(1);
                    }
                } catch (Exception ex) {
                    future.complete(1);
                }
            }
        });
        return future;
    }

    public CompletableFuture<Integer> createTaskForCarrier(String webhook, com.example.postgresql.UserF.Cargo cargo, String carrierName, String deadline) {
        if (webhook == null || webhook.isBlank()) return CompletableFuture.completedFuture(-1);

        return getResponsibleId(webhook).thenCompose(responsibleId -> {
            CompletableFuture<Integer> future = new CompletableFuture<>();
            JsonObject fields = new JsonObject();
            fields.addProperty("TITLE", "Перевозка: " + cargo.getFromCity() + " → " + cargo.getToCity() + " | " + cargo.getProduct());
            fields.addProperty("DESCRIPTION", "Логист: " + carrierName + "\n" + "Груз: " + cargo.getProduct() + "\n" + "Откуда: " + cargo.getFromCity() + "\n" + "Куда: " + cargo.getToCity() + "\n" + "Вес: " + cargo.getWeight() + " т\n" + "Объём: " + cargo.getVolume() + " м³\n" + "Дата отправки: " + cargo.getDate() + "\n" + "Телефон: " + cargo.getContactPhone());
            fields.addProperty("DEADLINE", deadline);
            fields.addProperty("PRIORITY", "1");
            fields.addProperty("RESPONSIBLE_ID", responsibleId);
            JsonObject body = new JsonObject();
            body.add("fields", fields);

            http.newCall(buildPost(webhook, "tasks.task.add.json", body)).enqueue(new Callback() {
                @Override
                public void onFailure(Call c, IOException e) {
                    System.err.println("[Bitrix24] createTask error: " + e.getMessage());
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call c, Response r) throws IOException {
                    String raw = bodyStr(r);
                    try {
                        JsonObject json = gson.fromJson(raw, JsonObject.class);
                        if (json.has("result")) {
                            JsonObject res = json.getAsJsonObject("result");
                            if (res.has("task")) {
                                int id = res.getAsJsonObject("task").get("id").getAsInt();
                                System.out.println("[Bitrix24] Задача создана, ID=" + id);
                                future.complete(id);
                            } else {
                                future.complete(-1);
                            }
                        } else {
                            System.err.println("[Bitrix24] createTask: " + raw);
                            future.complete(-1);
                        }
                    } catch (Exception ex) {
                        future.completeExceptionally(new IOException(raw, ex));
                    }
                }
            });
            return future;
        });
    }


    public CompletableFuture<Boolean> cancelTask(String webhook, int taskId) {
        if (webhook == null || webhook.isBlank() || taskId <= 0) return CompletableFuture.completedFuture(false);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject fields = new JsonObject();
        fields.addProperty("STATUS", 6);
        JsonObject body = new JsonObject();
        body.addProperty("taskId", taskId);
        body.add("fields", fields);
        http.newCall(buildPost(webhook, "tasks.task.update.json", body)).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                System.err.println("[Bitrix24] cancelTask error: " + e.getMessage());
                future.complete(false);
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                String raw = bodyStr(r);
                System.out.println("[Bitrix24] cancelTask STATUS=6: " + raw);
                future.complete(r.isSuccessful());
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> deleteDeal(String webhook, int dealId) {
        if (webhook == null || webhook.isBlank() || dealId <= 0) return CompletableFuture.completedFuture(false);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject body = new JsonObject();
        body.addProperty("id", dealId);

        http.newCall(buildPost(webhook, "crm.deal.delete.json", body)).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                System.err.println("[Bitrix24] deleteDeal error: " + e.getMessage());
                future.complete(false);
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                System.out.println("[Bitrix24] deleteDeal: " + bodyStr(r));
                future.complete(r.isSuccessful());
            }
        });
        return future;
    }


    public CompletableFuture<Boolean> completeTask(String webhook, int taskId) {
        if (webhook == null || webhook.isBlank() || taskId <= 0) return CompletableFuture.completedFuture(false);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject fields = new JsonObject();
        fields.addProperty("STATUS", 5);
        JsonObject body = new JsonObject();
        body.addProperty("taskId", taskId);
        body.add("fields", fields);

        http.newCall(buildPost(webhook, "tasks.task.update.json", body)).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                System.err.println("[Bitrix24] completeTask error: " + e.getMessage());
                future.complete(false);
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                String raw = bodyStr(r);
                System.out.println("[Bitrix24] completeTask: " + raw);
                future.complete(r.isSuccessful());
            }
        });
        return future;
    }


    public CompletableFuture<Boolean> updateDealStageInProgress(String webhook, int dealId) {
        if (webhook == null || webhook.isBlank() || dealId <= 0) return CompletableFuture.completedFuture(false);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject fields = new JsonObject();
        fields.addProperty("STAGE_ID", "IN_PROCESS");
        JsonObject body = new JsonObject();
        body.addProperty("id", dealId);
        body.add("fields", fields);

        http.newCall(buildPost(webhook, "crm.deal.update.json", body)).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                System.err.println("[Bitrix24] updateDeal error: " + e.getMessage());
                future.complete(false);
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                System.out.println("[Bitrix24] updateDeal: " + bodyStr(r));
                future.complete(r.isSuccessful());
            }
        });
        return future;
    }


    private Request buildPost(String webhookUrl, String method, JsonObject body) {
        String base = webhookUrl.endsWith("/") ? webhookUrl : webhookUrl + "/";
        return new Request.Builder().url(base + method).post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json"))).build();
    }

    private String bodyStr(Response r) throws IOException {
        String s = r.body() != null ? r.body().string() : "{}";
        r.close();
        return s;
    }
}
