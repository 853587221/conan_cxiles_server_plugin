package com.rconclient.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.rconclient.model.AutoExecuteRule;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static ApiClient instance;
    private OkHttpClient client;
    private SharedPreferences prefs;
    private String baseUrl;
    private Handler mainHandler;
    private PersistentCookieJar cookieJar;
    
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private ApiClient(Context context) {
        prefs = context.getSharedPreferences("rcon_prefs", Context.MODE_PRIVATE);
        baseUrl = prefs.getString("base_url", "");
        mainHandler = new Handler(Looper.getMainLooper());
        
        cookieJar = new PersistentCookieJar(context);
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();
    }
    
    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }
    
    public void clearCookies() {
        cookieJar.clear();
    }
    
    public void setBaseUrl(String url) {
        this.baseUrl = url;
        prefs.edit().putString("base_url", url).apply();
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public String getUsername() {
        return prefs.getString("username", "");
    }
    
    public void post(String path, JSONObject body, ApiCallback callback) {
        RequestBody requestBody = RequestBody.create(body.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(requestBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void uploadShopImage(byte[] imageBytes, ApiCallback callback) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.jpg", 
                        RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                .build();
        
        Request request = new Request.Builder()
                .url(baseUrl + "/api/shop/admin/product/upload")
                .post(requestBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void login(String username, String password, ApiCallback callback) {
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        
        Request request = new Request.Builder()
                .url(baseUrl + "/api/login")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void register(String username, String password, ApiCallback callback) {
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        
        Request request = new Request.Builder()
                .url(baseUrl + "/api/register")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void verifySession(ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/verify-session")
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getConnectionInfo(ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/rcon/connection-info")
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void connect(String host, String password, int port, String rconMode, boolean saveConnection, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("host", host);
            json.put("password", password);
            json.put("port", port);
            json.put("rcon_mode", rconMode);
            json.put("saveConnection", saveConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/rcon/connect")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void sendCommand(String command, String rconMode, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("command", command);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        String url = "sse".equals(rconMode) ? "/api/rcon/send-via-sse" : "/api/rcon/send";
        
        Request request = new Request.Builder()
                .url(baseUrl + url)
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getCategories(ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/categories")
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getCommands(String category, ApiCallback callback) {
        String url = baseUrl + "/api/commands";
        if (category != null && !"all".equals(category)) {
            url = baseUrl + "/api/commands/category/" + category;
        }
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void createCategory(String name, String description, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("description", description);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/categories/create")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void createCommand(String name, String description, String category, String example, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("description", description);
            json.put("category", category);
            json.put("example", example);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/commands/create")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void updateCategory(int id, String name, String description, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
            json.put("description", description);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/categories/update")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void deleteCategory(int id, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/categories/delete")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void updateCommand(int id, String name, String description, String category, String example, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
            json.put("description", description);
            json.put("category", category);
            json.put("example", example);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/commands/update")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void deleteCommand(int id, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/commands/delete")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getPlayers(ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/players")
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getPlayerInfo(String playerId, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/player/" + playerId + "/info")
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getPlayerInventory(String playerName, ApiCallback callback) {
        try {
            String encodedName = java.net.URLEncoder.encode(playerName, "UTF-8");
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/inventory/" + encodedName)
                    .get()
                    .build();
            
            executeRequest(request, callback);
        } catch (Exception e) {
            callback.onError("编码错误: " + e.getMessage());
        }
    }
    
    public void getPlayerThralls(String playerName, ApiCallback callback) {
        try {
            String encodedName = java.net.URLEncoder.encode(playerName, "UTF-8");
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/inventory/" + encodedName)
                    .get()
                    .build();
            
            executeRequest(request, callback);
        } catch (Exception e) {
            callback.onError("编码错误: " + e.getMessage());
        }
    }
    
    public void updatePlayer(int playerId, double gold, int permissionLevel, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", playerId);
            json.put("gold", gold);
            json.put("permission_level", permissionLevel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/players/update")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void updatePlayerVip(JSONObject requestBody, ApiCallback callback) {
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/players/update")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void deletePlayer(int playerId, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", playerId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/players/delete")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getShopCategories(String username, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/shop/categories?username=" + username)
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getShopProducts(String username, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/shop/products?username=" + username)
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void searchProducts(String username, String keyword, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/shop/search?username=" + username + "&keyword=" + keyword)
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getAutoExecuteRules(ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/auto-trigger-rules")
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    public void createAutoExecuteRule(JSONObject rule, ApiCallback callback) {
        RequestBody body = RequestBody.create(rule.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/auto-trigger-rules/create")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void updateAutoExecuteRule(AutoExecuteRule rule, ApiCallback callback) {
        RequestBody body = RequestBody.create(rule.toJson().toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/auto-trigger-rules/update")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void deleteAutoExecuteRule(int ruleId, ApiCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", ruleId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/auto-trigger-rules/delete")
                .post(body)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void toggleAutoExecuteRule(int ruleId, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/auto-trigger-rules/" + ruleId + "/toggle")
                .post(RequestBody.create("", JSON))
                .build();
        
        executeRequest(request, callback);
    }
    
    public void getChatMessages(int offset, int limit, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/chat-messages?offset=" + offset + "&limit=" + limit)
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    private void executeRequest(Request request, ApiCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                mainHandler.post(() -> {
                    if (response.isSuccessful()) {
                        callback.onSuccess(responseBody);
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                });
            }
        });
    }
    
    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
