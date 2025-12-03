package com.espinozameridaal.Models;

public class ParsedMessage {

    public String type;
    public int seq;
    public long sendTs;

    public long userID;
    public String userName;
    public String message;

    public ParsedMessage(String type, int seq, String userName, long sendTs, long userID, String message) {
        this.type = type;
        this.seq = seq;
        this.userName = userName;

        this.sendTs = sendTs;
        this.userID = userID;
        this.message = message;
    }
}
