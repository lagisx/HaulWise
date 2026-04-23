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
                System.err.println("loadRandom error: " + e.getMessage());
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
            System.out.println("[LOG] Начало загрузки изображения: " + url);
            Image img = new Image(url, true);

            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("[LOG] Прогресс загрузки: " + newVal);
                if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                    Platform.runLater(() -> {
                        imageView.setImage(img);
                        imageView.setVisible(true);
                        imageView.setManaged(true);
                        System.out.println("[LOG] Изображение успешно загружено");
                    });
                }
            });

            img.errorProperty().addListener((obs, oldVal, isError) -> {
                if (isError) {
                    System.err.println("[LOG] ОШИБКА: изображение не загружено: " + url);
                }
            });

        } catch (Exception e) {
            System.err.println("[LOG] ИСКЛЮЧЕНИЕ: " + e.getMessage());
        }
    }
}