package com.example.postgresql.UserF;

public class BlockUsers {
    private int id;
    private String login;
    private String email;
    private String phone;
    private String reason;
    private String blocked_by;
    private String created_at;

    public BlockUsers(int id, String login, String email, String phone, String reason, String blocked_by, String created_at) {
        this.id = id;
        this.login = login;
        this.email=email;
        this.phone=phone;
        this.reason=reason;
        this.blocked_by=blocked_by;
        this.created_at=created_at;
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

    public String getReason() {
        return reason;
    }

    public String getBlocked_by() {
        return blocked_by;
    }

    public String getCreated_at() {
        return created_at;
    }
}
