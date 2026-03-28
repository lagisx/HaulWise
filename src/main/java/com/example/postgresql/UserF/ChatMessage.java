package com.example.postgresql.UserF;

public class ChatMessage {
    private final int id;
    private final String senderLogin;
    private final String receiverLogin;
    private final String content;
    private final boolean isRead;
    private final String createdAt;

    public ChatMessage(int id, String senderLogin, String receiverLogin,
                       String content, boolean isRead, String createdAt) {
        this.id = id;
        this.senderLogin = senderLogin;
        this.receiverLogin = receiverLogin;
        this.content = content;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public int getId()               { return id; }
    public String getSenderLogin()   { return senderLogin; }
    public String getReceiverLogin() { return receiverLogin; }
    public String getContent()       { return content; }
    public boolean isRead()          { return isRead; }
    public String getCreatedAt()     { return createdAt; }
}
