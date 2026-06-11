package com.example.postgresql.API;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class Bitrix24Client {

    private static final String BASE_URL =
            AppConfig.getInstance().getBitrix24WebhookUrl();

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();

    private static Bitrix24Client instance;

    private Bitrix24Client() {}

    public static Bitrix24Client getInstance() {
        if (instance == null) {
            instance = new Bitrix24Client();
        }
        return instance;
    }

    public CompletableFuture<Integer> createDealFromCargo(com.example.postgresql.UserF.Cargo cargo) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        JsonObject fields = new JsonObject();
        fields.addProperty("TITLE",
                "Груз: " + cargo.getProduct() + " | " + cargo.getFromCity() + " → " + cargo.getToCity());
        fields.addProperty("OPPORTUNITY", cargo.getPriceCard());
        fields.addProperty("CURRENCY_ID", "RUB");
        fields.addProperty("STAGE_ID", "NEW");
        fields.addProperty("COMMENTS",
                "Тип ТС: "      + cargo.getVehicleType()  + "\n" +
                "Вес: "         + cargo.getWeight()        + " т\n" +
                "Объём: "       + cargo.getVolume()        + " м³\n" +
                "Тип загрузки: "+ cargo.getLoadType()      + "\n" +
                "Детали: "      + cargo.getLoadDetails()   + "\n" +
                "Дата: "        + cargo.getDate()          + "\n" +
                "Торг: "        + cargo.getBargain()       + "\n" +
                "Телефон: "     + cargo.getContactPhone());

        JsonObject body = new JsonObject();
        body.add("fields", fields);

        Request request = new Request.Builder()
                .url(BASE_URL + "/crm.deal.add.json")
                .post(jsonBody(body))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("[Bitrix24] createDeal onFailure: " + e.getMessage());
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String raw = response.body() != null ? response.body().string() : "{}";
                response.close();
                try {
                    JsonObject json = gson.fromJson(raw, JsonObject.class);
                    if (json.has("result") && !json.get("result").isJsonNull()) {
                        int dealId = json.get("result").getAsInt();
                        System.out.println("[Bitrix24] Сделка создана, ID=" + dealId);
                        future.complete(dealId);
                    } else {
                        System.err.println("[Bitrix24] createDeal ошибка: " + raw);
                        future.complete(-1);
                    }
                } catch (Exception ex) {
                    future.completeExceptionally(new IOException("Ответ: " + raw, ex));
                }
            }
        });

        return future;
    }
    public CompletableFuture<Integer> createTaskForCarrier(
            com.example.postgresql.UserF.Cargo cargo,
            String carrierName,
            String deadline) {

        CompletableFuture<Integer> future = new CompletableFuture<>();

        JsonObject fields = new JsonObject();
        fields.addProperty("TITLE",
                "Перевозка: " + cargo.getFromCity() + " → " + cargo.getToCity()
                + " | " + cargo.getProduct());
        fields.addProperty("DESCRIPTION",
                "Перевозчик: "  + carrierName           + "\n" +
                "Груз: "        + cargo.getProduct()    + "\n" +
                "Откуда: "      + cargo.getFromCity()   + "\n" +
                "Куда: "        + cargo.getToCity()     + "\n" +
                "Вес: "         + cargo.getWeight()     + " т\n" +
                "Объём: "       + cargo.getVolume()     + " м³\n" +
                "Дата отправки: "+ cargo.getDate()      + "\n" +
                "Телефон: "     + cargo.getContactPhone());
        fields.addProperty("DEADLINE", deadline);
        fields.addProperty("PRIORITY", "1");

        JsonObject body = new JsonObject();
        body.add("fields", fields);

        Request request = new Request.Builder()
                .url(BASE_URL + "/tasks.task.add.json")
                .post(jsonBody(body))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("[Bitrix24] createTask onFailure: " + e.getMessage());
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String raw = response.body() != null ? response.body().string() : "{}";
                response.close();
                try {
                    JsonObject json = gson.fromJson(raw, JsonObject.class);
                    if (json.has("result")) {
                        JsonObject result = json.getAsJsonObject("result");
                        if (result.has("task")) {
                            int taskId = result.getAsJsonObject("task").get("id").getAsInt();
                            System.out.println("[Bitrix24] Задача создана, ID=" + taskId);
                            future.complete(taskId);
                        } else {
                            future.complete(-1);
                        }
                    } else {
                        System.err.println("[Bitrix24] createTask ошибка: " + raw);
                        future.complete(-1);
                    }
                } catch (Exception ex) {
                    future.completeExceptionally(new IOException("Ответ: " + raw, ex));
                }
            }
        });

        return future;
    }

    private RequestBody jsonBody(JsonObject obj) {
        return RequestBody.create(gson.toJson(obj), MediaType.parse("application/json"));
    }
}
