package com.rconclient.utils;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static ApiClient instance;
    private OkHttpClient client;
    private Gson gson;
    private String baseUrl;
    private Handler mainHandler;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private ApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        gson = new GsonBuilder().create();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    public void setBaseUrl(String url) {
        this.baseUrl = url;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
    }

    public void login(String username, String password, ApiCallback<JsonObject> callback) {
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "api/login")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void register(String username, String password, ApiCallback<JsonObject> callback) {
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "api/register")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void verifySession(ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/verify-session")
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void connectRcon(String host, String password, String port, String rconMode, ApiCallback<JsonObject> callback) {
        JsonObject json = new JsonObject();
        json.addProperty("host", host);
        json.addProperty("password", password);
        json.addProperty("port", port);
        json.addProperty("rcon_mode", rconMode);
        json.addProperty("saveConnection", true);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "api/rcon/connect")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void sendCommand(String command, String rconMode, ApiCallback<JsonObject> callback) {
        JsonObject json = new JsonObject();
        json.addProperty("command", command);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );

        String endpoint = "sse".equals(rconMode) ? "api/rcon/send-via-sse" : "api/rcon/send";

        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void getCategories(ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/categories")
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void getCommands(String category, ApiCallback<JsonObject> callback) {
        String url = "all".equals(category) ? 
                baseUrl + "api/commands" : 
                baseUrl + "api/commands/category/" + category;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void getPlayers(ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/players")
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void getPlayerInfo(String playerIndex, ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/player/" + playerIndex)
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void getPlayerInventory(String playerIndex, ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/player/" + playerIndex + "/inventory")
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void getShopCategories(String username, ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/shop/categories?username=" + username)
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void getShopProducts(String username, ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/shop/products?username=" + username)
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void searchProducts(String username, String keyword, ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/shop/search?username=" + username + "&keyword=" + keyword)
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void getAutoCommands(ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/auto-commands")
                .get()
                .build();

        executeRequest(request, callback);
    }

    public void saveAutoCommand(JsonObject commandData, ApiCallback<JsonObject> callback) {
        RequestBody body = RequestBody.create(
                commandData.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "api/auto-commands/save")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void deleteAutoCommand(int ruleId, ApiCallback<JsonObject> callback) {
        JsonObject json = new JsonObject();
        json.addProperty("id", ruleId);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "api/auto-commands/delete")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void getConnectionInfo(ApiCallback<JsonObject> callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "api/rcon/connection-info")
                .get()
                .build();

        executeRequest(request, callback);
    }

    private void executeRequest(Request request, final ApiCallback<JsonObject> callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    mainHandler.post(() -> callback.onSuccess(result));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }
}
