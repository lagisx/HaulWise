package com.example.postgresql.utils;

import com.example.postgresql.API.SupabaseStorageClient;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CargoImageLoader {

    private static final SupabaseStorageClient storageClient = new SupabaseStorageClient();

    private static final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build();

    public static void loadRandom(ImageView imageView) {
        if (imageView == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                String url = storageClient.getRandomPublicImageUrl();
                loadByUrl(imageView, url);
            } catch (Exception e) {
                System.err.println("[CargoImageLoader] loadRandom error: " + e.getMessage());
            }
        });
    }

    public static void loadFromUrl(ImageView imageView, String imageUrl) {
        if (imageView == null) return;
        if (imageUrl == null || imageUrl.isBlank()) return;

        CompletableFuture.runAsync(() -> loadByUrl(imageView, imageUrl));
    }

    private static void loadByUrl(ImageView imageView, String url) {
        try {
            System.out.println("Загрузка изображения: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.err.println("HTTP " + response.code() + " для " + url);
                    return;
                }

                byte[] bytes = response.body().bytes();
                InputStream stream = new ByteArrayInputStream(bytes);

                Image img = new Image(stream);
                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    System.out.println("Прогресс загрузки: " + newVal.doubleValue());
                });

                if (!img.isError()) {
                    Platform.runLater(() -> {
                        imageView.setImage(img);
                        imageView.setVisible(true);
                        imageView.setManaged(true);
                        System.out.println("Изображение успешно загружено и отображено");
                    });
                } else {
                    System.err.println("Ошибка загрузки изображения: " + url);
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка загрузки " + url + " : " + e.getMessage());
        }
    }
}