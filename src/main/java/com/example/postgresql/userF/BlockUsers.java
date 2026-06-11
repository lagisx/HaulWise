package com.example.postgresql.UserF;

public class BlockUsers {
    private int    id;
    private String login;
    private String email;
    private String phone;
    private String reason;
    private String blockedBy;
    private String createdAt;

    public BlockUsers(int id, String login, String email, String phone,
                      String reason, String blockedBy, String createdAt) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.phone = phone;
        this.reason = reason;
        this.blockedBy = blockedBy;
        this.createdAt = createdAt;
    }

    public int    getId()        { return id; }
    public String getLogin()     { return login; }
    public String getEmail()     { return email; }
    public String getPhone()     { return phone; }
    public String getReason()    { return reason; }
    public String getBlockedBy() { return blockedBy; }
    public String getCreatedAt() { return createdAt; }
}
