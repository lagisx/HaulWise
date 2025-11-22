package com.example.postgresql.support;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class SupportTechController {

    @FXML
    private TextField subjectField;

    @FXML
    private TextArea messageArea;

    @FXML
    private void sendSupportRequest(ActionEvent event) {
        String subject = subjectField.getText();
        String message = messageArea.getText();

        if (subject.isEmpty() || message.isEmpty()) {
            showAlert("Ошибка", "Заполните все поля перед отправкой!");
            return;
        }

        showAlert("Успешно", "Ваше обращение успешно отправлено. Мы свяжемся с вами в ближайшее время!");

        subjectField.clear();
        messageArea.clear();
    }

    @FXML
    private void goBack(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/postgresql/main.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Регистрация");
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void SupportTechPanelWithPrefill(String login, String subject, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/postgresql/SupportTechPanel.fxml"));
            Parent root = loader.load();
            SupportTechController controller = loader.getController();

            controller.subjectField.setText(subject);
            controller.messageArea.setText(
                    message + "\n\n" +
                            "Логин: " + login + "\n" +
                            "Время обращения: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            );

            Stage stage = new Stage();
            stage.setTitle("Техническая поддержка");
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
