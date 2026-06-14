package com.example.postgresql.API;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

public class DealService {

    private final SupabaseClient supabase = new SupabaseClient();

    public CompletableFuture<Integer> createDeal(int cargoId, String ownerLogin,
                                                 String carrierLogin,
                                                 int bitrixTaskId, int bitrixDealId) {
        JsonObject data = new JsonObject();
        data.addProperty("cargo_id", cargoId);
        data.addProperty("owner_login", ownerLogin);
        data.addProperty("carrier_login", carrierLogin);
        if (bitrixTaskId > 0) data.addProperty("bitrix_task_id", bitrixTaskId);
        if (bitrixDealId > 0) data.addProperty("bitrix_deal_id", bitrixDealId);
        data.addProperty("status", "active");

        return supabase.insertServiceRole("deals", data)
                .thenCompose(arr -> {
                    if (arr != null && !arr.isEmpty()) {
                        JsonObject row = arr.get(0).getAsJsonObject();
                        int id = row.has("id") && !row.get("id").isJsonNull()
                                ? row.get("id").getAsInt() : -1;
                        if (id > 0) return CompletableFuture.completedFuture(id);
                    }
                    return supabase.selectWithServiceRoleTwoFilters("deals", "id",
                                    "owner_login=eq." + ownerLogin,
                                    "carrier_login=eq." + carrierLogin)
                            .thenApply(rows -> {
                                if (rows == null || rows.isEmpty()) return -1;
                                JsonObject row = rows.get(rows.size() - 1).getAsJsonObject();
                                return row.has("id") ? row.get("id").getAsInt() : -1;
                            });
                });
    }

    public CompletableFuture<JsonObject> getDeal(String ownerLogin, String carrierLogin) {
        return supabase.selectWithServiceRoleTwoFilters("deals",
                        "id,cargo_id,owner_login,carrier_login,bitrix_task_id,bitrix_deal_id,status",
                        "owner_login=eq." + ownerLogin,
                        "carrier_login=eq." + carrierLogin)
                .thenApply(rows -> {
                    if (rows == null || rows.isEmpty()) return null;
                    JsonObject lastAny = null;
                    for (int i = rows.size() - 1; i >= 0; i--) {
                        JsonObject row = rows.get(i).getAsJsonObject();
                        String status = row.has("status") && !row.get("status").isJsonNull()
                                ? row.get("status").getAsString() : "";
                        if ("active".equals(status)) return row;
                        if (lastAny == null) lastAny = row;
                    }
                    return lastAny;
                });
    }

    public CompletableFuture<JsonObject> getDealByCargoId(String ownerLogin,
                                                          String carrierLogin, int cargoId) {
        String encOwner = java.net.URLEncoder.encode(ownerLogin,
                java.nio.charset.StandardCharsets.UTF_8);
        String encCarrier = java.net.URLEncoder.encode(carrierLogin,
                java.nio.charset.StandardCharsets.UTF_8);
        return supabase.selectWithServiceRole("deals",
                        "id,cargo_id,owner_login,carrier_login,bitrix_task_id,bitrix_deal_id,status",
                        "cargo_id=eq." + cargoId)
                .thenApply(rows -> {
                    if (rows == null || rows.isEmpty()) return null;
                    for (int i = rows.size() - 1; i >= 0; i--) {
                        JsonObject row = rows.get(i).getAsJsonObject();
                        String oLogin = row.has("owner_login") && !row.get("owner_login").isJsonNull()
                                ? row.get("owner_login").getAsString() : "";
                        String cLogin = row.has("carrier_login") && !row.get("carrier_login").isJsonNull()
                                ? row.get("carrier_login").getAsString() : "";
                        if (oLogin.equals(ownerLogin) && cLogin.equals(carrierLogin)) return row;
                    }
                    return null;
                });
    }

    public CompletableFuture<JsonArray> getAllDealsBetween(String ownerLogin, String carrierLogin) {
        return supabase.selectWithServiceRoleTwoFilters("deals",
                "id,cargo_id,owner_login,carrier_login,bitrix_task_id,bitrix_deal_id,status",
                "owner_login=eq." + ownerLogin,
                "carrier_login=eq." + carrierLogin);
    }

    public CompletableFuture<Boolean> updateDealStatus(int dealId, String status) {
        JsonObject data = new JsonObject();
        data.addProperty("status", status);
        return supabase.updateServiceRole("deals", data, "id=eq." + dealId);
    }

    public CompletableFuture<Boolean> updateTaskId(int dealId, int taskId) {
        JsonObject data = new JsonObject();
        data.addProperty("bitrix_task_id", taskId);
        return supabase.updateServiceRole("deals", data, "id=eq." + dealId);
    }

    public CompletableFuture<Boolean> updateDealId(int dealId, int bitrixDealId) {
        JsonObject data = new JsonObject();
        data.addProperty("bitrix_deal_id", bitrixDealId);
        return supabase.updateServiceRole("deals", data, "id=eq." + dealId);
    }
}