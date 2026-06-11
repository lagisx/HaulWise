package com.example.postgresql.API;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

public class CompanyService {

    private final SupabaseClient supabase = new SupabaseClient();


    public CompletableFuture<Integer> createCompany(String name,
                                                    String bitrix24Webhook,
                                                    String ownerLogin) {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("bitrix24_webhook", bitrix24Webhook);
        data.addProperty("owner_login", ownerLogin);

        return supabase.insertServiceRole("companies", data)
                .thenApply(arr -> {
                    if (arr != null && !arr.isEmpty()) {
                        JsonObject row = arr.get(0).getAsJsonObject();
                        return row.has("id") ? row.get("id").getAsInt() : -1;
                    }
                    return -1;
                });
    }


    public CompletableFuture<Boolean> linkUserToCompany(String login,
                                                        int companyId,
                                                        String role) {
        JsonObject data = new JsonObject();
        data.addProperty("company_id", companyId);
        data.addProperty("company_role", role);

        String encodedLogin = java.net.URLEncoder.encode(login,
                java.nio.charset.StandardCharsets.UTF_8);
        return supabase.update("users", data, "login=eq." + encodedLogin);
    }


    public CompletableFuture<JsonObject> getUserCompany(String login) {
        String encodedLogin = java.net.URLEncoder.encode(login,
                java.nio.charset.StandardCharsets.UTF_8);

        return supabase.select("users", "company_id,company_role", "login=eq." + encodedLogin)
                .thenCompose(rows -> {
                    if (rows == null || rows.isEmpty()) return done(null);
                    JsonObject user = rows.get(0).getAsJsonObject();
                    if (!user.has("company_id") || user.get("company_id").isJsonNull())
                        return done(null);

                    int companyId = user.get("company_id").getAsInt();
                    return supabase.select("companies",
                                    "id,name,bitrix24_webhook,owner_login,created_at",
                                    "id=eq." + companyId)
                            .thenApply(companies -> {
                                if (companies == null || companies.isEmpty()) return null;
                                JsonObject company = companies.get(0).getAsJsonObject();
                                company.add("my_role", user.get("company_role"));
                                return company;
                            });
                });
    }


    public CompletableFuture<String> getWebhookForUser(String login) {
        return getUserCompany(login).thenApply(company -> {
            if (company == null) return "";
            if (!company.has("bitrix24_webhook")
                    || company.get("bitrix24_webhook").isJsonNull()) return "";
            return company.get("bitrix24_webhook").getAsString().trim();
        });
    }


    public CompletableFuture<JsonArray> getCompanyMembers(int companyId) {
        return supabase.select("users", "id,login,email,phone,company_role",
                "company_id=eq." + companyId);
    }


    public CompletableFuture<Boolean> inviteEmployee(String employeeLogin, int companyId) {
        String enc = java.net.URLEncoder.encode(employeeLogin,
                java.nio.charset.StandardCharsets.UTF_8);

        return supabase.select("users", "login,company_id", "login=eq." + enc)
                .thenCompose(rows -> {
                    if (rows == null || rows.isEmpty())
                        return CompletableFuture.completedFuture(false);

                    JsonObject user = rows.get(0).getAsJsonObject();
                    if (user.has("company_id") && !user.get("company_id").isJsonNull())
                        return CompletableFuture.completedFuture(false);

                    return linkUserToCompany(employeeLogin, companyId, "employee");
                });
    }


    public CompletableFuture<Boolean> leaveCompany(String login) {
        JsonObject data = new JsonObject();
        data.add("company_id", com.google.gson.JsonNull.INSTANCE);
        data.add("company_role", com.google.gson.JsonNull.INSTANCE);
        String enc = java.net.URLEncoder.encode(login,
                java.nio.charset.StandardCharsets.UTF_8);
        return supabase.update("users", data, "login=eq." + enc);
    }


    public CompletableFuture<Boolean> updateWebhook(int companyId, String newWebhook) {
        JsonObject data = new JsonObject();
        data.addProperty("bitrix24_webhook", newWebhook);
        return supabase.update("companies", data, "id=eq." + companyId);
    }


    public CompletableFuture<Boolean> deleteCompany(int companyId) {
        JsonObject nullData = new JsonObject();
        nullData.add("company_id", com.google.gson.JsonNull.INSTANCE);
        nullData.add("company_role", com.google.gson.JsonNull.INSTANCE);

        return supabase.update("users", nullData, "company_id=eq." + companyId)
                .thenCompose(ok -> supabase.delete("companies", "id=eq." + companyId));
    }

    private <T> CompletableFuture<T> done(T value) {
        return CompletableFuture.completedFuture(value);
    }
}
