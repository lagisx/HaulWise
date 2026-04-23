package com.example.postgresql.userF;

public class Logs {
    private int    id;
    private String userLogin;
    private String description;
    private String createdAt;

    public Logs(int id, String userLogin, String description, String createdAt) {
        this.id          = id;
        this.userLogin   = userLogin;
        this.description = description;
        this.createdAt   = createdAt;
    }

    public int    getId()          { return id; }
    public String getUserLogin()   { return userLogin; }
    public String getDescription() { return description; }
    public String getCreatedAt()   { return createdAt; }
}
