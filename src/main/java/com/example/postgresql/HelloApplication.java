package com.example.postgresql;

import com.example.postgresql.API.TelegramBindBot;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    private static HostServices hostServices;

    @Override
    public void start(Stage stage) throws IOException {
        hostServices = getHostServices();
        new Thread(() -> {
            TelegramBindBot.main(new String[]{});
        }).start();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Биржа грузоперевозок");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
    public static HostServices getAppHostServices() {
        return hostServices;
    }

    public static void main(String[] args) {
        launch();
    }
}