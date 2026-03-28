package com.example.postgresql.utils;

import com.example.postgresql.API.SupabaseStorageClient;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.concurrent.CompletableFuture;

public class CargoImageLoader {

    private static final SupabaseStorageClient storageClient = new SupabaseStorageClient();

    
    public static void loadRandom(ImageView imageView) {
        if (imageView == null) return;

        
        CompletableFuture.runAsync(() -> {
            try {
                String url = storageClient.getRandomPublicImageUrl();
                Image img = new Image(url, true); 

                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                        Platform.runLater(() -> {
                            imageView.setImage(img);
                            imageView.setVisible(true);
                            imageView.setManaged(true);
                        });
                    }
                });

                img.errorProperty().addListener((obs, oldVal, isError) -> {
                    if (isError) {
                        
                        tryLocalFallback(imageView);
                    }
                });

            } catch (Exception e) {
                tryLocalFallback(imageView);
            }
        });
    }

    
    public static void loadFromUrl(ImageView imageView, String imageUrl) {
        if (imageView == null) return;
        if (imageUrl == null || imageUrl.isEmpty()) {
            tryLocalFallback(imageView);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Image img = new Image(imageUrl, true);
                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                        Platform.runLater(() -> {
                            imageView.setImage(img);
                            imageView.setVisible(true);
                            imageView.setManaged(true);
                        });
                    }
                });
                img.errorProperty().addListener((obs, oldVal, isError) -> {
                    if (isError) tryLocalFallback(imageView);
                });
            } catch (Exception e) {
                tryLocalFallback(imageView);
            }
        });
    }

    
    private static void tryLocalFallback(ImageView imageView) {
        String[] local = {
            "/images/BoxImage.png", "/images/CarTwo.png", "/images/CarThreeBox.png",
            "/images/CarFour.png",  "/images/Kran.png",   "/images/CargoImage.png",
            "/images/ThreeCargo.png", "/images/Pallet.png"
        };
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < local.length; i++) {
            var stream = CargoImageLoader.class.getResourceAsStream(local[rnd.nextInt(local.length)]);
            if (stream != null) {
                Image img = new Image(stream);
                Platform.runLater(() -> {
                    imageView.setImage(img);
                    imageView.setVisible(true);
                    imageView.setManaged(true);
                });
                return;
            }
        }
        
    }
}
