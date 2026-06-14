package com.example.postgresql.controllers;

import com.example.postgresql.API.CompanyService;
import com.example.postgresql.HelloApplication;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CompanyPanelController {

    @FXML
    private VBox noCompanyBox;
    @FXML
    private Button createCompanyBtn;

    @FXML
    private VBox companyBox;
    @FXML
    private Label companyNameLabel;
    @FXML
    private Label companyRoleLabel;

    @FXML
    private VBox ownerSettingsBox;
    @FXML
    private TextField webhookField;
    @FXML
    private Button saveWebhookBtn;
    @FXML
    private Button deleteCompanyBtn;

    @FXML
    private TextField inviteLoginField;
    @FXML
    private Button inviteBtn;
    @FXML
    private Label inviteStatusLabel;

    @FXML
    private VBox membersContainer;

    @FXML
    private Label statusLabel;
    @FXML
    private Button backBtn;

    private String currentUser;
    private int companyId = -1;
    private String myRole = "";

    private final CompanyService companyService = new CompanyService();


    public void setCurrentUser(String login) {
        this.currentUser = login;
        loadCompanyData();
    }

    private void loadCompanyData() {
        companyService.getUserCompany(currentUser)
                .thenAccept(company -> Platform.runLater(() -> {
                    if (company == null) {
                        showNoCompany();
                    } else {
                        showCompany(company);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showStatus("Ошибка загрузки: " + ex.getMessage(), "red"));
                    return null;
                });
    }


    private void showNoCompany() {
        noCompanyBox.setVisible(true);
        noCompanyBox.setManaged(true);
        companyBox.setVisible(false);
        companyBox.setManaged(false);
    }


    private void showCompany(JsonObject company) {
        noCompanyBox.setVisible(false);
        noCompanyBox.setManaged(false);
        companyBox.setVisible(true);
        companyBox.setManaged(true);

        companyId = company.get("id").getAsInt();
        myRole = getStr(company, "my_role");

        companyNameLabel.setText(getStr(company, "name"));
        companyRoleLabel.setText(myRole.equals("owner") ? "👑 Владелец" : "👤 Сотрудник");

        boolean isOwner = "owner".equals(myRole);
        ownerSettingsBox.setVisible(isOwner);
        ownerSettingsBox.setManaged(isOwner);

        if (isOwner) {
            String webhook = getStr(company, "bitrix24_webhook");
            webhookField.setText(webhook);
            webhookField.setPromptText("https://ДОМЕН.bitrix24.ru/rest/USERID/ТОКЕН/");
        }

        loadMembers();
    }


    @FXML
    private void onCreateCompany() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("CreateCompany.fxml"));
            Scene scene = new Scene(loader.load());
            CreateCompanyController ctrl = loader.getController();
            ctrl.setData(currentUser, () -> Platform.runLater(this::loadCompanyData));

            Stage stage = new Stage();
            stage.setTitle("Создать компанию");
            stage.setScene(scene);
            stage.setMinWidth(500);
            stage.setMinHeight(400);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            showStatus("Ошибка открытия формы: " + e.getMessage(), "red");
        }
    }


    @FXML
    private void onSaveWebhook() {
        String newWebhook = webhookField.getText().trim();
        if (newWebhook.isEmpty()) {
            showStatus("Введите URL вебхука", "orange");
            return;
        }
        if (!newWebhook.contains("bitrix24")) {
            showStatus("Проверьте URL — он должен содержать bitrix24", "orange");
            return;
        }

        saveWebhookBtn.setDisable(true);
        companyService.updateWebhook(companyId, newWebhook)
                .thenAccept(ok -> Platform.runLater(() -> {
                    saveWebhookBtn.setDisable(false);
                    if (ok) showStatus("✅ Вебхук сохранён", "#16a34a");
                    else showStatus("Не удалось сохранить вебхук", "red");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        saveWebhookBtn.setDisable(false);
                        showStatus("Ошибка: " + ex.getMessage(), "red");
                    });
                    return null;
                });
    }


    @FXML
    private void onInvite() {
        String login = inviteLoginField.getText().trim();
        if (login.isEmpty()) {
            setInviteStatus("Введите логин сотрудника", "orange");
            return;
        }
        if (login.equals(currentUser)) {
            setInviteStatus("Нельзя пригласить самого себя", "orange");
            return;
        }

        inviteBtn.setDisable(true);
        companyService.inviteEmployee(login, companyId)
                .thenAccept(ok -> Platform.runLater(() -> {
                    inviteBtn.setDisable(false);
                    if (ok) {
                        setInviteStatus("✅ " + login + " добавлен в компанию", "#16a34a");
                        inviteLoginField.clear();
                        loadMembers();
                    } else {
                        setInviteStatus("Пользователь не найден или уже состоит в компании", "red");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        inviteBtn.setDisable(false);
                        setInviteStatus("Ошибка: " + ex.getMessage(), "red");
                    });
                    return null;
                });
    }


    private void loadMembers() {
        if (companyId < 0) return;
        membersContainer.getChildren().clear();

        companyService.getCompanyMembers(companyId)
                .thenAccept(members -> Platform.runLater(() -> {
                    if (members == null || members.isEmpty()) {
                        membersContainer.getChildren().add(makeLabel("Сотрудников пока нет", "#94a3b8"));
                        return;
                    }
                    for (JsonElement el : members) {
                        JsonObject m = el.getAsJsonObject();
                        membersContainer.getChildren().add(buildMemberRow(m));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> membersContainer.getChildren()
                            .add(makeLabel("Ошибка загрузки сотрудников", "red")));
                    return null;
                });
    }

    private HBox buildMemberRow(JsonObject member) {
        String login = getStr(member, "login");
        String role = getStr(member, "company_role");
        String email = getStr(member, "email");

        HBox row = new HBox(12);
        row.setStyle("-fx-padding: 8 12; -fx-background-color: #f8fafc; " +
                "-fx-background-radius: 8; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 8;");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label icon = makeLabel("owner".equals(role) ? "👑" : "👤", "#64748b");
        Label name = makeLabel(login, "#0f172a");
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        Label mail = makeLabel(email.isEmpty() ? "" : "  " + email, "#64748b");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(icon, name, mail, spacer);

        if ("owner".equals(myRole) && !"owner".equals(role)) {
            Button kick = new Button("✕");
            kick.setStyle("-fx-background-color: #fff1f2; -fx-text-fill: #ef4444; " +
                    "-fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; " +
                    "-fx-cursor: hand;");
            kick.setOnAction(e -> kickEmployee(login, row));
            row.getChildren().add(kick);
        }

        return row;
    }

    private void kickEmployee(String login, HBox row) {
        companyService.leaveCompany(login)
                .thenAccept(ok -> Platform.runLater(() -> {
                    if (ok) membersContainer.getChildren().remove(row);
                    else showStatus("Не удалось удалить сотрудника", "red");
                }));
    }


    @FXML
    private void onDeleteCompany() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить компанию? Все сотрудники будут отвязаны.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Подтверждение");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                companyService.deleteCompany(companyId)
                        .thenAccept(ok -> Platform.runLater(() -> {
                            if (ok) {
                                companyId = -1;
                                showNoCompany();
                            } else showStatus("Не удалось удалить компанию", "red");
                        }));
            }
        });
    }


    @FXML
    private void onBack() {
        try {
            Stage stage = (Stage) backBtn.getScene().getWindow();
            com.example.postgresql.UserF.ProfileUser.profilePanel(stage, currentUser, "", "");
        } catch (Exception e) {
            showStatus("Ошибка перехода: " + e.getMessage(), "red");
        }
    }


    public static void companyPanel(Stage stage, String login) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("CompanyPanel.fxml"));
            Scene scene = new Scene(loader.load());
            CompanyPanelController ctrl = loader.getController();
            ctrl.setCurrentUser(login);
            stage.setScene(scene);
            stage.setTitle("Моя компания");
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showStatus(String text, String color) {
        if (statusLabel == null) return;
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private void setInviteStatus(String text, String color) {
        if (inviteStatusLabel == null) return;
        inviteStatusLabel.setText(text);
        inviteStatusLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    private Label makeLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13;");
        return l;
    }

    private String getStr(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString().trim();
    }
}
