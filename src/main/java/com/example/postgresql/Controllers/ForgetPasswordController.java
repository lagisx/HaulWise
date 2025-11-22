package com.example.postgresql.Controllers;

import com.example.postgresql.HelloApplication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.Random;

public class ForgetPasswordController {

    @FXML
    public TextField login;

    @FXML
    private Label statusLabel;

    private String Code;
    private Stage codeStage;
    private String currentUser;


    public static String generate(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int digit = random.nextInt(6);
            sb.append(digit);
        }
        System.out.println("Код для подтверждения: " + sb.toString());
        return sb.toString();
    }


    @FXML
    public void ForgetPass(ActionEvent event) {
        String user = login.getText().toLowerCase().trim();
        try {
            if (CheckFindUser(user)) {
                currentUser = user;
                FormCode();
            } else {
                statusLabel.setText("Ошибка! Пользователь не найден");
                statusLabel.setAlignment(Pos.CENTER);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка подключения к БД: " + e.getMessage());
            statusLabel.setAlignment(Pos.CENTER);
        }
    }
    private boolean CheckFindUser(String user) throws SQLException {
        if (user.isEmpty()) {
            statusLabel.setText("Ошибка! Заполните все поля");
            statusLabel.setAlignment(Pos.CENTER);
            return false;
        }
        String sql = "SELECT * FROM users WHERE login = ?";
        try (Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    private void FormCode() {
        Code = generate(5);

        codeStage = new Stage();
        codeStage.setResizable(false);

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(10);
        root.setStyle("-fx-padding: 20;");

        Label label = new Label("Введите код");
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold");

        TextField codeField = new TextField();
        codeField.setPromptText("Введите код");
        codeField.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 10000; -fx-border-radius: 10000; -fx-border-color:  #bdc3c7; -fx-background-color:  white;");

        Button cancel = new Button("Назад");
        cancel.setStyle("-fx-font-size: 14px; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold;");
        cancel.prefWidth(20);
        cancel.maxWidth(70);
        cancel.prefHeight(30);
        cancel.setOnAction(e -> {
            codeStage.close();
        });
        Label codeStatusLabel = new Label();
        codeStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: red;");
        codeStatusLabel.maxWidth(300);
        codeStatusLabel.isWrapText();
        codeStatusLabel.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("Подтвердить");
        confirmBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold;");
        confirmBtn.setOnAction(e -> {
            codeStage.close();
            String usercode = codeField.getText().trim();
            if (usercode.equals(Code)) {
                codeStage.close();
                try {
                    FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("passwordRecover.fxml"));
                    Scene scene = new Scene(loader.load());

                    PasswordRecoverController controller = loader.getController();
                    controller.setUserEmail(currentUser);

                    Stage mainStage = (Stage) login.getScene().getWindow();
                    mainStage.setScene(scene);
                    mainStage.setTitle("Смена пароля");

                } catch (IOException ex) {
                    statusLabel.setText("Ошибка " + ex.getMessage());
                    statusLabel.setAlignment(Pos.CENTER);
                }
            } else {
                codeStatusLabel.setText("Код неверный");
                statusLabel.setAlignment(Pos.CENTER);
            }
        });

        root.getChildren().addAll(label, codeField, confirmBtn, cancel, codeStatusLabel);

        Scene scene = new Scene(root, 300, 200);
        codeStage.setScene(scene);
        codeStage.show();
    }

    @FXML
    private void goBack(ActionEvent event) {
        Stage stage = (Stage) login.getScene().getWindow();
        HelloController helloController = new HelloController();
        helloController.goBack(stage);
    }
}
