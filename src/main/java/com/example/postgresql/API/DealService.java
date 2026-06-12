package com.example.postgresql.API;

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
                    for (int i = rows.size() - 1; i >= 0; i--) {
                        JsonObject row = rows.get(i).getAsJsonObject();
                        String status = row.has("status") && !row.get("status").isJsonNull()
                                ? row.get("status").getAsString() : "";
                        if ("active".equals(status)) return row;
                    }
                    return null;
                });
    }

    public CompletableFuture<Boolean> updateDealStatus(int dealId, String status) {
        JsonObject data = new JsonObject();
        data.addProperty("status", status);
        return supabase.updateServiceRole("deals", data, "id=eq." + dealId);
    }
}
