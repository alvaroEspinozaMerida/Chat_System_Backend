package com.espinozameridaal.Models;

public class FriendRequest {
    private long id;
    private long senderId;
    private long receiverId;
    private String status;
    private String createdAt;
    private String respondedAt;

    public FriendRequest(long id, long senderId, long receiverId,
                         String status, String createdAt, String respondedAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
        this.createdAt = createdAt;
        this.respondedAt = respondedAt;
    }

    public long getId() {
        return id;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getRespondedAt() {
        return respondedAt;
    }
}