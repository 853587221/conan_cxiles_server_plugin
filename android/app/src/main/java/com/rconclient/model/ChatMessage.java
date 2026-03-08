package com.rconclient.model;

import org.json.JSONObject;

public class ChatMessage {
    private int id;
    private String charName;
    private String message;
    private long timestamp;
    
    public ChatMessage() {
    }
    
    public ChatMessage(int id, String charName, String message, long timestamp) {
        this.id = id;
        this.charName = charName;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public static ChatMessage fromJson(JSONObject json) {
        ChatMessage msg = new ChatMessage();
        msg.id = json.optInt("id");
        msg.charName = json.optString("char_name", "未知玩家");
        msg.message = json.optString("message", "");
        msg.timestamp = json.optLong("timestamp", 0);
        return msg;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getCharName() {
        return charName;
    }
    
    public void setCharName(String charName) {
        this.charName = charName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
