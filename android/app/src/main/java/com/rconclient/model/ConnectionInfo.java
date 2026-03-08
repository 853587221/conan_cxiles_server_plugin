package com.rconclient.model;

import org.json.JSONObject;

public class ConnectionInfo {
    private String host;
    private String password;
    private int port;
    private String rconMode;
    
    public ConnectionInfo() {}
    
    public static ConnectionInfo fromJson(JSONObject json) {
        ConnectionInfo info = new ConnectionInfo();
        info.host = json.optString("host");
        info.password = json.optString("password");
        info.port = json.optInt("port", 25575);
        info.rconMode = json.optString("rcon_mode", "direct");
        return info;
    }
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getRconMode() { return rconMode; }
    public void setRconMode(String rconMode) { this.rconMode = rconMode; }
}
