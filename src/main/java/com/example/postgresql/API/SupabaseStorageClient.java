package com.example.postgresql.API;

import okhttp3.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class SupabaseStorageClient {

    private static final String BUCKET = "image";

    
    
    private static final List<String> KNOWN_FILES = Arrays.asList(
            "BoxImage.png",
            "CarFour.png",
            "CargoImage.png",
            "CarThreeBox.png",
            "CarTwo.png",
            "Kran.png",
            "Pallet.png",
            "ThreeCargo.png"
    );

    private static final Random RANDOM = new Random();
    private final OkHttpClient http = new OkHttpClient();

    
    
    

    
    public String getRandomPublicImageUrl() {
        String fileName = KNOWN_FILES.get(RANDOM.nextInt(KNOWN_FILES.size()));
        return buildPublicUrl(fileName);
    }

    
    public String buildPublicUrl(String fileName) {
        return SupabaseClient.SUPABASE_URL
                + "/storage/v1/object/public/"
                + BUCKET + "/"
                + fileName;
    }

    
    
    

    
    public CompletableFuture<List<String>> listFiles() {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        String url = SupabaseClient.SUPABASE_URL + "/storage/v1/object/list/" + BUCKET;
        String body = "{\"limit\": 100, \"offset\": 0, \"sortBy\": {\"column\": \"name\", \"order\": \"asc\"}}";

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                
                future.complete(KNOWN_FILES);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String respBody = response.body() != null ? response.body().string() : "[]";
                    if (!response.isSuccessful()) {
                        future.complete(KNOWN_FILES);
                        return;
                    }
                    com.google.gson.JsonArray arr = new com.google.gson.Gson()
                            .fromJson(respBody, com.google.gson.JsonArray.class);

                    List<String> names = new java.util.ArrayList<>();
                    for (com.google.gson.JsonElement el : arr) {
                        com.google.gson.JsonObject obj = el.getAsJsonObject();
                        if (obj.has("name") && !obj.get("name").isJsonNull()) {
                            String name = obj.get("name").getAsString();
                            
                            if (name.matches("(?i).*\\.(png|jpg|jpeg|webp|gif)")) {
                                names.add(name);
                            }
                        }
                    }
                    future.complete(names.isEmpty() ? KNOWN_FILES : names);
                } catch (Exception e) {
                    future.complete(KNOWN_FILES);
                } finally {
                    response.close();
                }
            }
        });

        return future;
    }

    
    public CompletableFuture<String> getRandomPublicImageUrlAsync() {
        return listFiles().thenApply(files -> {
            if (files.isEmpty()) return buildPublicUrl(KNOWN_FILES.get(0));
            String fileName = files.get(RANDOM.nextInt(files.size()));
            return buildPublicUrl(fileName);
        });
    }
}
