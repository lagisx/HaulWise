package com.example.postgresql.UserF;

public class Logs {
    private int id;
    private String user;
    private String description;
    private String created_at;

public Logs(int id, String user, String description, String created_at) {
    this.id = id;
    this.user=user;
    this.description=description;
    this.created_at=created_at;
}

    public int getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public String getDescription() {
        return description;
    }

    public String getCreated_at() {
        return created_at;
    }

}
