package com.example.postgresql.UserF;

import com.example.postgresql.Controllers.HelloController;
import com.example.postgresql.Controllers.UserPanelController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.regex.Pattern;

public class ProfileUser {
    @FXML private Label LabelUser;

    public String user;
    public String password;


    @FXML public TextField login;
    @FXML private TextField email;
    @FXML private TextField phone;
    @FXML private PasswordField passwordVIEW;

    @FXML private Label statusLabel;

    private int userId;
    public void setLabelUserText(String user) {
            LabelUser.setText(user);
    }
    public void setPromt(String user, String mail, String phonenum) {
        if(!user.isEmpty()) {
            login.setPromptText(user);
        }
       if (!mail.isEmpty()) {
           email.setPromptText(mail);
       }
       if (!phonenum.isEmpty()) {
           phone.setPromptText(phonenum);
       }
    }

    public void SetPassUsername(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public void changeParam(ActionEvent event) {
        String newLogin = login.getText().trim();
        String newEmail = email.getText().trim();
        String newPhone = phone.getText().trim();
        String newPassword = passwordVIEW.getText().trim();

        if (newLogin.isEmpty() && newEmail.isEmpty() && newPhone.isEmpty() && newPassword.isEmpty()) {
            statusLabel.setText("Ошибка! Все поля пустые");
            return;
        }
        if (!newPhone.matches("\\+?\\d{10,15}")) {
            statusLabel.setText("Введите корректный номер телефона");
            return;
        }

        if (!isValidEmail(newEmail)) {
            statusLabel.setText("Введите корректную почту");
            return;
        }
        String oldLogin = this.user;

        try (Connection con = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD)) {

            if (!newLogin.isEmpty() && !newLogin.equals(oldLogin)) {
                String sqlAlter = "ALTER USER \"" + oldLogin + "\" RENAME TO \"" + newLogin + "\"";
                try (Statement stmt = con.createStatement()) {
                    stmt.execute(sqlAlter);
                }

                String sqlUpdateLogin = "UPDATE users SET login = ? WHERE login = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlUpdateLogin)) {
                    ps.setString(1, newLogin);
                    ps.setString(2, oldLogin);
                    ps.executeUpdate();
                }

                this.user = newLogin;
                oldLogin = newLogin;
            }

            if (!newEmail.isEmpty()) {
                String sqlUpdateEmail = "UPDATE users SET email = ? WHERE login = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlUpdateEmail)) {
                    ps.setString(1, newEmail);
                    ps.setString(2, oldLogin);
                    ps.executeUpdate();
                }
            }

            if (!newPhone.isEmpty()) {
                String sqlUpdatePhone = "UPDATE users SET phone = ? WHERE login = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlUpdatePhone)) {
                    ps.setString(1, newPhone);
                    ps.setString(2, oldLogin);
                    ps.executeUpdate();
                }
            }

            if (newPassword != null && !newPassword.isEmpty()) {
                if (newPassword.length() < 8) {
                    statusLabel.setText("Пароль должен быть от 8 символов");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    return;
                } else if (newPassword.length() > 15) {
                    statusLabel.setText("Пароль должен быть не более 15 символов");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setAlignment(Pos.CENTER);
                    return;
                } else {
                    String sqlAlterPassword = "ALTER ROLE \"" + oldLogin + "\" WITH PASSWORD '" + newPassword + "'";
                    try (Statement stmt = con.createStatement()) {
                        stmt.execute(sqlAlterPassword);
                    }
                    String sqlUpdatePass = "UPDATE users SET password = ? WHERE login = ?";
                    try (PreparedStatement ps = con.prepareStatement(sqlUpdatePass)) {
                        ps.setString(1, newPassword);
                        ps.setString(2, oldLogin);
                        ps.executeUpdate();
                    }
                }
            }

            statusLabel.setText("Данные успешно обновлены");
            statusLabel.setStyle("-fx-text-fill: green;");
            setPromt(newLogin, newEmail, newPhone);
            setLabelUserText(newLogin);

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка при обновлении данных");
        }
    }

    private static boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-\\.]+@[\\w-\\.]+\\.[a-zA-Z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }

    @FXML public void deleteUser(ActionEvent event) {
        try (Connection conn = DriverManager.getConnection(
                HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement("SELECT delete_user(?, ?)")) {

            ps.setInt(1, userId);
            ps.setString(2, user);
            ps.execute();

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM \"" + user + "\"");
                stmt.executeUpdate("REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM \"" + user + "\"");
                stmt.executeUpdate("REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM \"" + user + "\"");
                stmt.executeUpdate("REVOKE ALL PRIVILEGES ON SCHEMA public FROM \"" + user + "\"");
                stmt.executeUpdate("REVOKE ALL PRIVILEGES ON DATABASE \"JavaBD\" FROM \"" + user + "\"");
                stmt.executeUpdate("DROP USER \"" + user + "\"");
            }

            Stage stage = (Stage) LabelUser.getScene().getWindow();
            stage.close();
            goBack();

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка при удалении пользователя");
        }
    }

    @FXML public void goBackUserPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/postgresql/UserPanel.fxml"));
            Scene scene = new Scene(loader.load());
            UserPanelController controller = loader.getController();

            Stage stage = (Stage) LabelUser.getScene().getWindow();
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void profilePanel(Stage stage, String user, String password, String mail, String phonenum, String LabelUser) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ProfileUser.class.getResource("ProfileUser.fxml"));
            Parent root = fxmlLoader.load();
            ProfileUser controller = fxmlLoader.getController();
            controller.SetPassUsername(user, password);
            controller.setLabelUserText(user);
            controller.setPromt(user, mail, phonenum);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Профиль пользователя");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void goBack() {
        Stage stage = (Stage) LabelUser.getScene().getWindow();
        HelloController helloController = new HelloController();
        helloController.goBack(stage);
    }

}
