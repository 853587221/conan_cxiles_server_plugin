package com.rconclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ItemConfigManager {
    private static final String TAG = "ItemConfigManager";
    private static final String CONFIG_URL = "/Icons_PNG/000000.json";
    
    private static ItemConfigManager instance;
    private final Context context;
    private final OkHttpClient client;
    private final SharedPreferences prefs;
    private String serverUrl;
    
    private Map<String, JSONObject> itemConfigData = new HashMap<>();
    private boolean loaded = false;
    
    private ItemConfigManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("rcon_prefs", Context.MODE_PRIVATE);
        this.serverUrl = prefs.getString("base_url", "");
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }
    
    public static synchronized ItemConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new ItemConfigManager(context);
        }
        return instance;
    }
    
    public void updateServerUrl(String url) {
        this.serverUrl = url;
    }
    
    public void loadConfig(Callback callback) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            Log.e(TAG, "Server URL is empty");
            if (callback != null) callback.onError("服务器地址未设置");
            return;
        }
        
        new Thread(() -> {
            try {
                String url = serverUrl + CONFIG_URL + "?v=" + System.currentTimeMillis();
                Log.d(TAG, "Loading item config from: " + url);
                
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new Exception("HTTP error: " + response.code());
                }
                
                String body = response.body().string();
                JSONArray data = new JSONArray(body);
                
                Map<String, JSONObject> newConfig = new HashMap<>();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    String rowName = item.optString("RowName");
                    if (rowName != null && !rowName.isEmpty()) {
                        newConfig.put(rowName, item);
                    }
                }
                
                itemConfigData = newConfig;
                loaded = true;
                
                Log.d(TAG, "Item config loaded: " + itemConfigData.size() + " items");
                
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                    mainHandler.post(() -> callback.onSuccess());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to load item config: " + e.getMessage(), e);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
    }
    
    public String getIconPath(String templateId) {
        JSONObject item = itemConfigData.get(templateId);
        if (item == null) return null;
        
        String icon = item.optString("Icon");
        if (icon != null && !icon.isEmpty() && !"None".equals(icon)) {
            return serverUrl + "/Icons_PNG/" + icon + ".png";
        }
        
        String iconLayers = item.optString("IconLayers");
        if (iconLayers != null && !iconLayers.isEmpty() && !"None".equals(iconLayers)) {
            return serverUrl + "/Icons_PNG/" + iconLayers + ".png";
        }
        
        return null;
    }
    
    public String getIconPath(int templateId) {
        return getIconPath(String.valueOf(templateId));
    }
    
    public String getItemName(String templateId) {
        JSONObject item = itemConfigData.get(templateId);
        if (item == null) return null;
        return item.optString("Name");
    }
    
    public String getItemName(int templateId) {
        return getItemName(String.valueOf(templateId));
    }
    
    public String getItemCategory(String templateId) {
        JSONObject item = itemConfigData.get(templateId);
        if (item == null) return null;
        return item.optString("GUICategory");
    }
    
    public String getItemCategory(int templateId) {
        return getItemCategory(String.valueOf(templateId));
    }
    
    public JSONObject getItemConfig(String templateId) {
        return itemConfigData.get(templateId);
    }
    
    public JSONObject getItemConfig(int templateId) {
        return getItemConfig(String.valueOf(templateId));
    }
    
    public boolean isLoaded() {
        return loaded;
    }
    
    public int getItemCount() {
        return itemConfigData.size();
    }
    
    public interface Callback {
        void onSuccess();
        void onError(String error);
    }
}
