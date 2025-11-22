package com.example.postgresql.UserF;

public class User {
    private int id;
    private String login;
    private String email;
    private String phone;
    private String password;
    private String created_at;
    private boolean status;

    public User(int id, String login, String email, String phone, String password, String created_at, boolean status) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.created_at = created_at;
        this.status=status;
    }

    public int getId() {
        return id;
    }
    public String getLogin() {
        return login;
    }
    public String getEmail() {
        return email;
    }
    public String getPhone() {
        return phone;
    }
    public String getPassword() {
        return password;
    }
    public String getCreated_at() {
        return created_at;
    }
    public boolean getStatus() {
        return status;
    }
    public void setStatus(boolean status) {
        this.status = status;
    }
}
