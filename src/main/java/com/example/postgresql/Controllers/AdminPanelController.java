package com.example.postgresql.Controllers;

import com.example.postgresql.HelloApplication;
import com.example.postgresql.UserF.BlockUsers;
import com.example.postgresql.UserF.Cargo;
import com.example.postgresql.UserF.Logs;
import com.example.postgresql.UserF.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.*;

import java.io.IOException;
import java.sql.*;
import java.util.regex.Pattern;

public class AdminPanelController {

    @FXML private TableView<Cargo> cargoTable;
    @FXML private TableColumn<Cargo, Integer> colCargoId;
    @FXML private TableColumn<Cargo, String> colCargoType;
    @FXML private TableColumn<Cargo, Double> colCargoWeight;
    @FXML private TableColumn<Cargo, Double> colCargoVolume;
    @FXML private TableColumn<Cargo, String> colCargoProduct;
    @FXML private TableColumn<Cargo, String> colCargoFrom;
    @FXML private TableColumn<Cargo, String> colCargoTo;
    @FXML private TableColumn<Cargo, String> colCargoLoadType;
    @FXML private TableColumn<Cargo, String> colCargoLoadDetails;
    @FXML private TableColumn<Cargo, String> colCargoDates;
    @FXML private TableColumn<Cargo, Double> colCargoPriceCard;
    @FXML private TableColumn<Cargo, Double> colCargoPriceNDC;
    @FXML private TableColumn<Cargo, String> colCargoTorg;
    @FXML private TableColumn<Cargo, String> colCargoContact;
    @FXML private TableColumn<Cargo, String> colCargoOwner;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUserLogin;
    @FXML private TableColumn<User, String> colUserPassword;
    @FXML private TableColumn<User, String> colUserPhone;
    @FXML private TableColumn<User, String> colUserEmail;
    @FXML private TableColumn<User, String> colUserCreated_at;
    @FXML private TableColumn<User, String> colUserStatus;

    @FXML private TableView <BlockUsers> blacklistTable;
    @FXML private TableColumn<BlockUsers, Integer> colBlockUserId;
    @FXML private TableColumn<BlockUsers, String> colBlockUserLogin;
    @FXML private TableColumn<BlockUsers, String> colBlockUserEmail;
    @FXML private TableColumn<BlockUsers, String> colBlockUserPhone;
    @FXML private TableColumn<BlockUsers, String> colBlockUserReason;
    @FXML private TableColumn<BlockUsers, String> colBlockUserBlocked_by;
    @FXML private TableColumn<BlockUsers, String> colBlockUserCreated_at;

    @FXML public TableView <Logs> logsListTable;
    @FXML private TableColumn<Logs, Integer> colLogsListId;
    @FXML private TableColumn<Logs, String> colLogsListUser;
    @FXML private TableColumn<Logs, String> colLogsListDescription;
    @FXML private TableColumn <Logs, String> colLogsListCreated_at;


    @FXML private Label statusLabelUsers;
    @FXML private Label statusLabelBlockUsers;
    @FXML private Label LabelUser;

    private ObservableList<Cargo> cargoList = FXCollections.observableArrayList();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private ObservableList<BlockUsers> blockUsers = FXCollections.observableArrayList();
    private ObservableList<Logs> logsList = FXCollections.observableArrayList();

    private static String Adminuser;

    @FXML private TabPane adminTabPane;

    @FXML public void initialize() {
        TabHidePunktire();

        loadColCargoList();
        loadColUsersList();
        loadColBlockUserList();
        loadColLogsList();

        loadCargos();
        loadUsers();
        loadBlockListUsers();
        loadLogsList();
    }

    private void loadColCargoList() {
        colCargoId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colCargoType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTypeTC()));
        colCargoWeight.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getWeight()).asObject());
        colCargoVolume.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getVolume()).asObject());
        colCargoProduct.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct()));
        colCargoFrom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFrom()));
        colCargoTo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTo()));
        colCargoLoadType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLoadType()));
        colCargoLoadDetails.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLoadDetails()));
        colCargoDates.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate()));
        colCargoPriceCard.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPriceCard()).asObject());
        colCargoPriceNDC.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPriceNDC()).asObject());
        colCargoTorg.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTorg()));
        colCargoContact.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getContact()));
        colCargoOwner.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOwner()));
    }
    private void loadColUsersList() {
        colUserId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colUserLogin.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLogin()));
        colUserPassword.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPassword()));
        colUserPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        colUserEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colUserCreated_at.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreated_at()));
        colUserStatus.setCellValueFactory(data -> {
            String statusText;
            if (data.getValue().getStatus() == true) {
                statusText = "Заблокирован";
            } else {
                statusText = "Активен";
            }
            return new SimpleStringProperty(statusText);
        });
    }
    private void loadColBlockUserList() {
        colBlockUserId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colBlockUserLogin.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLogin()));
        colBlockUserEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colBlockUserPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        colBlockUserReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason()));
        colBlockUserBlocked_by.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBlocked_by()));
        colBlockUserCreated_at.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreated_at()));
    }
    private void loadColLogsList() {
        colLogsListId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colLogsListUser.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUser()));
        colLogsListDescription.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        colLogsListCreated_at.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreated_at()));
    }

    @FXML private void loadCargos() {
        cargoList.clear();
        String sql = "SELECT g.*, u.login AS заказчик_login " +
                "FROM gruz g " +
                "LEFT JOIN users u ON g.\"заказчик_id\" = u.id " +
                "ORDER BY g.\"id\"";

        try (Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                cargoList.add(new Cargo(
                        rs.getInt("id"),
                        rs.getString("ТипТС"),
                        rs.getDouble("Вес"),
                        rs.getDouble("Объем"),
                        rs.getString("Товар"),
                        rs.getString("Откуда"),
                        rs.getString("Куда"),
                        rs.getString("ТипПогрузки"),
                        rs.getString("ДеталиПогрузки"),
                        rs.getString("Даты"),
                        rs.getDouble("ЦенаПоКарте"),
                        rs.getDouble("ЦенаНДС"),
                        rs.getString("Торг_без_торга"),
                        rs.getString("КонтактныйТелефон"),
                        rs.getString("заказчик_login")
                ));
            }
            cargoTable.setItems(cargoList);

        } catch (SQLException e) {
            statusLabelUsers.setText("Ошибка загрузки грузов: " + e.getMessage());
        }
    }
    @FXML private void loadUsers() {
        userList.clear();
        String sql = "SELECT * FROM users WHERE role != 'admin' ORDER BY id";
        try (Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                userList.add(new User(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("password"),
                        rs.getString("created_at"),
                        rs.getBoolean("status")
                ));
            }
            userTable.setItems(userList);

        } catch (SQLException e) {
            statusLabelUsers.setText("Ошибка загрузки пользователей: " + e.getMessage());
        }
    }

    @FXML private void deleteCargo() {
        Cargo selected = cargoTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabelUsers.setText("Ошибка удаления груза");
            return;
        }
        try (Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM gruz WHERE \"id\" = ?")) {
            ps.setInt(1, selected.getId());
            ps.executeUpdate();
            loadCargos();
            loadUsers();
            loadLogsList();
            logsListTable.refresh();
        } catch (SQLException e) {
            statusLabelUsers.setText("Ошибка удаления груза: " + e.getMessage());
        }
    }
    @FXML private void deleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        String dbUserName = selected.getLogin();
        if (selected == null) {
            statusLabelUsers.setText("Выберите пользователя для удаления!");
            return;
        }
        try (Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement("SELECT delete_user_byAdmin(?, ?, ?)")) {
            ps.setInt(1, selected.getId());
            ps.setString(2, dbUserName);
            ps.setString(3, Adminuser);
            ps.execute();
            loadUsers();
        } catch (SQLException e) {
            statusLabelUsers.setText("Ошибка удаления пользователя: " + e.getMessage());
        }
        loadCargos();
        loadBlockListUsers();
        loadLogsList();
        logsListTable.refresh();
    }

    @FXML private void blockUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabelUsers.setText("Выберите пользователя!");
            return;
        }
        Stage reasonStage = new Stage();
        reasonStage.setResizable(false);
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(10);
        root.setStyle("-fx-padding: 20;");

        Label headLabel = new Label("Укажите причину блокировки " + selected.getLogin());
        headLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold");

        TextField reasonTextField = new TextField();

        Button cancel = new Button("Отмена");
        cancel.setStyle("-fx-font-size: 14px; -fx-padding: 3 5; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        cancel.setOnAction(e -> {
            reasonStage.close();
        });
        Label StatusLabelBlockUser = new Label();
        StatusLabelBlockUser.setStyle("-fx-font-size: 13px; -fx-text-fill: red;");
        StatusLabelBlockUser.maxWidth(300);
        StatusLabelBlockUser.isWrapText();
        StatusLabelBlockUser.setAlignment(Pos.CENTER);

        Button confirm = new Button("Подтвердить");
        HBox buttonhbox = new HBox(10, confirm, cancel);
        buttonhbox.setAlignment(Pos.CENTER);
        confirm.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        confirm.setOnAction(e -> {
            reasonStage.close();
            String reason = reasonTextField.getText();
            if (reason.isEmpty()) {
                reason = "Не указано";
            }
            String addUserInTableBlock = "SELECT block_user(?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
                 PreparedStatement block = connection.prepareStatement(addUserInTableBlock)) {
                block.setString(1, selected.getLogin());
                block.setString(2, selected.getPhone());
                block.setString(3, selected.getEmail());
                block.setString(4, Adminuser);
                block.setString(5, reason);
                block.setString(6, Adminuser);
                block.setInt(7, selected.getId());
                block.execute();

                selected.setStatus(true);
                userTable.refresh();
                loadBlockListUsers();
                loadUsers();
                loadLogsList();
                logsListTable.refresh();
                blacklistTable.refresh();
                statusLabelUsers.setText("Пользователь добавлен в черный список");
                statusLabelUsers.setStyle("-fx-text-fill: green");

            } catch (Exception ex) {
                statusLabelUsers.setText("Ошибка блокировки пользователя: " + ex.getMessage());
            }

        });
        root.getChildren().addAll(headLabel,reasonTextField, buttonhbox, StatusLabelBlockUser);
        Scene scene = new Scene(root,  400, 270);
        reasonStage.setScene(scene);
        reasonStage.show();


    }
    @FXML private void unblockUser(){
        BlockUsers selected = blacklistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabelBlockUsers.setText("Выберите пользователя!");
            return;
        }
        String DelUserBlacklist = "SELECT unblock_user(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(DelUserBlacklist)) {
            ps.setInt(1, selected.getId());
            ps.setString(2, selected.getLogin());
            ps.setString(3, Adminuser);
            ps.execute();

            loadBlockListUsers();
            loadUsers();
            userTable.refresh();
            blacklistTable.refresh();
            loadLogsList();
            logsListTable.refresh();
            statusLabelBlockUsers.setText("Пользователь успешно разблокирован");
            statusLabelBlockUsers.setStyle("-fx-text-fill: green");

        } catch (SQLException e) {
            statusLabelBlockUsers.setText("Ошибка разблокировки пользователя: " + e.getMessage());
        }
    }
    @FXML private void loadBlockListUsers() {
        blockUsers.clear();
    String sql = "SELECT * FROM blacklist ORDER BY id";
    try (Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            blockUsers.add(new BlockUsers(
                    rs.getInt("id"),
                    rs.getString("login"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("reason"),
                    rs.getString("blocked_by"),
                    rs.getString("created_at")
            ));
        }
        blacklistTable.setItems(blockUsers);

    } catch (SQLException e) {
        statusLabelUsers.setText("Ошибка загрузки: " + e.getMessage());
    }
    }

    @FXML public void loadLogsList() {
        logsList.clear();
        String sql = "Select * from logs ORDER BY id DESC";
        try(Connection conn = DriverManager.getConnection(HelloController.DB_URL, HelloController.DB_USER, HelloController.DB_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logsList.add(new Logs(
                        rs.getInt("id"),
                        rs.getString("users"),
                        rs.getString("description"),
                        rs.getString("created_at")
                ));
            }
            logsListTable.setItems(logsList);


        } catch (SQLException e) {
            e.printStackTrace();
        }
        }

    @FXML private void editUser() {

        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabelUsers.setText("Выберите пользователя!");
            return;
        }

        Stage EditStage = new Stage();
        EditStage.setResizable(false);
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(10);
        root.setStyle("-fx-padding: 20;");

        Label headLabel = new Label("Изменение данных пользователя " + selected.getLogin());
        headLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold");

        Label loginLabel = new Label("Новый логин");
        TextField loginTextField = new TextField();
        loginTextField.setPromptText(selected.getLogin());
        HBox loginBox = new HBox(10, loginLabel, loginTextField);
        loginBox.setAlignment(Pos.CENTER);

        Label passwordLabel = new Label("Новый пароль");
        TextField passwordTextField = new TextField();
        passwordTextField.setPromptText(selected.getPassword());
        HBox passwordBox = new HBox(10, passwordLabel, passwordTextField);
        passwordBox.setAlignment(Pos.CENTER);

        Label emailLabel = new Label("Новая почта");
        TextField emailTextField = new TextField();
        emailTextField.setPromptText(selected.getEmail());
        HBox emailBox = new HBox(10, emailLabel, emailTextField);
        emailBox.setAlignment(Pos.CENTER);

        Label phoneLabel = new Label("Новый телефон");
        TextField phoneTextField = new TextField();
        phoneTextField.setPromptText(selected.getPhone());
        HBox phoneBox = new HBox(10, phoneLabel, phoneTextField);
        phoneBox.setAlignment(Pos.CENTER);

        Label StatusLabelEditUser = new Label();
        StatusLabelEditUser.setStyle("-fx-font-size: 13px; -fx-text-fill: red;");
        StatusLabelEditUser.maxWidth(300);
        StatusLabelEditUser.isWrapText();
        StatusLabelEditUser.setAlignment(Pos.CENTER);

        Button cancel = new Button("Отмена");
        cancel.setStyle("-fx-font-size: 14px; -fx-padding: 3 5; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        cancel.setOnAction(e -> {
            EditStage.close();
        });
        Button confirm = new Button("Подтвердить");
        confirm.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        confirm.setOnAction(e -> {
            String newLogin = loginTextField.getText().trim();
            String newPassword = passwordTextField.getText().trim();
            String newPhone = phoneTextField.getText().trim();
            String newEmail = emailTextField.getText().trim();
            String oldLogin = selected.getLogin().trim();

            if (newLogin.isEmpty() && newEmail.isEmpty() && newPhone.isEmpty() && newPassword.isEmpty()) {
                StatusLabelEditUser.setText("Ошибка! Все поля пустые");
                return;
            }

            if (!newLogin.isEmpty() && (newLogin.length() < 3 || newLogin.length() > 20)) {
                StatusLabelEditUser.setText("Логин должен быть от 3 до 20 символов");
                return;
            }

            if (!newPassword.isEmpty() && (newPassword.length() < 8 || newPassword.length() > 15)) {
                StatusLabelEditUser.setText("Пароль должен быть от 8 до 15 символов");
                return;
            }

            if (!newEmail.isEmpty() && !isValidEmail(newEmail)) {
                StatusLabelEditUser.setText("Введите корректный email");
                return;
            }

            if (!newPhone.isEmpty() && !newPhone.matches("\\+?\\d{10,15}")) {
                StatusLabelEditUser.setText("Введите корректный номер телефона");
                return;
            }

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

                if (!newPassword.isEmpty()) {
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

                loadUsers();
                userTable.refresh();
                loadLogsList();
                logsListTable.refresh();

                StatusLabelEditUser.setText("Данные успешно обновлены");
                StatusLabelEditUser.setStyle("-fx-text-fill: green;");
                EditStage.close();

            } catch (SQLException ex) {
                ex.printStackTrace();
                StatusLabelEditUser.setText("Ошибка изменения пользователя: " + ex.getMessage());
            }
        });

        HBox buttons = new HBox(10, confirm, cancel);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(headLabel, loginBox, passwordBox, emailBox, phoneBox, buttons, StatusLabelEditUser);
        Scene scene = new Scene(root,  400, 270);
        EditStage.setScene(scene);
        EditStage.show();

    }
    private static boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-\\.]+@[\\w-\\.]+\\.[a-zA-Z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }

    @FXML private void goBack() throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("main.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) cargoTable.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Авторизация");
        stage.show();
        stage.centerOnScreen();
    }

    public void getPassUsername(String user, String password) {
        this.Adminuser = user;
    }

    public void setNameUser(String user) {
        LabelUser.setText(user);
    }

    public static void AdminPanel(Stage stage, String user, String password) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AdminPanelController.class.getResource("/com/example/postgresql/AdminPanel.fxml"));
        Scene userScene = new Scene(fxmlLoader.load());
        AdminPanelController admincontroller = fxmlLoader.getController();
        admincontroller.getPassUsername(user, password);
        admincontroller.setNameUser(user);
        stage.setTitle("Панель пользователя");
        stage.setScene(userScene);
        stage.show();
        stage.centerOnScreen();
    }

    private void TabHidePunktire() {
        adminTabPane.setFocusTraversable(false);

        adminTabPane.getTabs().forEach(tab -> {
            tab.setClosable(false);
            if (tab.getContent() != null) {
                tab.getContent().setFocusTraversable(false);
            }
        });
    }
}

