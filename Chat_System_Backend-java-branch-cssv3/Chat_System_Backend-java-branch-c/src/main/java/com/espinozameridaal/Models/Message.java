package com.espinozameridaal.Models;

public class Message {
    public long id;
    public long senderId;
    public long receiverId;
    public String content;
    public String createdAt;

    public Message(long id, long senderId, long receiverId, String content, String createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.createdAt = createdAt;
    }
}