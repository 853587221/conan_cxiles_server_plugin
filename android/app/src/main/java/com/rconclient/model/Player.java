package com.rconclient.model;

import org.json.JSONObject;

public class Player {
    private int id;
    private String idx;
    private String steamId;
    private String platformId;
    private String playerName;
    private String charName;
    private String tribeName;
    private int level;
    private double gold;
    private int tag;
    private int permissionLevel;
    private String playtime;
    private boolean isOnline;
    private long monthlyCardExpiry;
    
    public Player() {}
    
    private static String fixNull(String value) {
        if (value == null || value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }
    
    public static Player fromJson(JSONObject json) {
        Player player = new Player();
        player.id = json.optInt("id");
        player.idx = fixNull(json.optString("idx", json.optString("Idx")));
        player.steamId = fixNull(json.optString("user_id", json.optString("steam_id", json.optString("SteamId"))));
        player.platformId = fixNull(json.optString("platform_id", ""));
        player.playerName = fixNull(json.optString("player_name", json.optString("PlayerName")));
        player.charName = fixNull(json.optString("char_name", json.optString("CharName")));
        player.tribeName = fixNull(json.optString("guild_name", json.optString("tribe_name", json.optString("TribeName"))));
        player.level = json.optInt("level", json.optInt("Level"));
        player.gold = json.optDouble("gold", json.optDouble("Gold"));
        player.tag = json.optInt("permission_level", 0);
        player.permissionLevel = json.optInt("permission_level", 0);
        player.playtime = fixNull(json.optString("online_time", json.optString("playtime", json.optString("Playtime"))));
        player.isOnline = json.optBoolean("is_online", false);
        player.monthlyCardExpiry = json.optLong("monthly_card_expiry", 0);
        return player;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getIdx() { return idx; }
    public void setIdx(String idx) { this.idx = idx; }
    public String getSteamId() { return steamId; }
    public void setSteamId(String steamId) { this.steamId = steamId; }
    public String getPlatformId() { return platformId; }
    public void setPlatformId(String platformId) { this.platformId = platformId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getCharName() { return charName; }
    public void setCharName(String charName) { this.charName = charName; }
    public String getTribeName() { return tribeName; }
    public void setTribeName(String tribeName) { this.tribeName = tribeName; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public double getGold() { return gold; }
    public void setGold(double gold) { this.gold = gold; }
    public int getTag() { return tag; }
    public void setTag(int tag) { this.tag = tag; }
    public int getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(int permissionLevel) { this.permissionLevel = permissionLevel; }
    public String getPlaytime() { return playtime; }
    public void setPlaytime(String playtime) { this.playtime = playtime; }
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    public long getMonthlyCardExpiry() { return monthlyCardExpiry; }
    public void setMonthlyCardExpiry(long monthlyCardExpiry) { this.monthlyCardExpiry = monthlyCardExpiry; }
}
