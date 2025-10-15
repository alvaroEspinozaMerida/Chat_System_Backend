package com.espinozameridaal.Models;

public class ParsedMessage {
    public long userID;
    public String userName;
    public String message;

    public ParsedMessage(long userID, String userName, String message) {
        this.userID = userID;
        this.userName = userName;
        this.message = message;
    }
}
