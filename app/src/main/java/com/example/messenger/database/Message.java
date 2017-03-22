package com.example.messenger.database;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class Message {
    private String user;
    private String message;
    private Long timestamp;
    private int type;

    @Exclude
    public static final int TEXT = 1;
    @Exclude
    public static final int IMAGE = 2;

    public Message() {}

    public Message(String user, String message) {
        this.user = user;
        this.message = message;
        this.type = TEXT;
    }

    public Message(String user, String message, int type) {
        this.user = user;
        this.message = message;
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getTimestamp() {
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public Long getTimestampLong() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
