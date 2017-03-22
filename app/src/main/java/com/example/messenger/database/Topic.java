package com.example.messenger.database;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.Map;
import java.util.Random;

public class Topic {

    private String name;
    private String userUid;
    private Long timestamp;
    private String image;
    private Map<String, Integer> users;
    private Map<String, Integer> activeUsers;
    private Long lastMessageTimestamp;
    private Long messageSaveDuration;
    private int priority;

    public Topic() {}

    public Topic(String name, String userUid) {
        this.name = name;
        this.userUid = userUid;
        image = "images/topic/" + name + "/" + name + ".jpg";
        priority = 100;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Map<String, Integer> getUsers() {
        return users;
    }

    public void setUsers(Map<String, Integer> users) {
        this.users = users;
    }

    public Map<String, Integer> getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(Map<String, Integer> activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public Long getMessageSaveDuration() {
        return messageSaveDuration;
    }

    public void setMessageSaveDuration(Long messageSaveDuration) {
        this.messageSaveDuration = messageSaveDuration;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
