package com.example.postgresql.UserF;

public class Company {
    private int id;
    private String name;
    private String bitrix24Webhook;
    private String ownerLogin;
    private String createdAt;

    public Company(int id, String name, String bitrix24Webhook,
                   String ownerLogin, String createdAt) {
        this.id = id;
        this.name = name;
        this.bitrix24Webhook = bitrix24Webhook;
        this.ownerLogin = ownerLogin;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBitrix24Webhook() {
        return bitrix24Webhook;
    }

    public String getOwnerLogin() {
        return ownerLogin;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBitrix24Webhook(String bitrix24Webhook) {
        this.bitrix24Webhook = bitrix24Webhook;
    }
}
