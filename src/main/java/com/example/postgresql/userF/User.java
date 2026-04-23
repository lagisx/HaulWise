package com.example.postgresql.userF;

public class User {
    private int    id;
    private String login;
    private String email;
    private String phone;
    private String createdAt;
    private boolean status;
    private String role;

    public User(int id, String login, String email, String phone,
                String createdAt, boolean status, String role) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
        this.status = status;
        this.role = role;
    }

    public int     getId()        { return id; }
    public String  getLogin()     { return login; }
    public String  getEmail()     { return email; }
    public String  getPhone()     { return phone; }
    public String  getCreatedAt() { return createdAt; }
    public boolean getStatus()    { return status; }
    public String  getRole()      { return role; }
    public void    setStatus(boolean status) { this.status = status; }
}
