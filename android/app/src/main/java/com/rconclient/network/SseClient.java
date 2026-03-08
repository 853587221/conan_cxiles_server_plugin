package com.rconclient.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class SseClient {
    private static SseClient instance;
    private OkHttpClient client;
    private EventSource eventSource;
    private Handler mainHandler;
    private SseEventListener listener;
    private boolean isConnected = false;
    private String baseUrl;
    private PersistentCookieJar cookieJar;

    public interface SseEventListener {
        void onPlayerJoin(String playerName);
        void onPlayerLeave(String playerName);
        void onChatMessage(String charName, String message);
        void onServerStats(JSONObject stats);
        void onPlayerRespawn(String charName);
        void onNewPlayer(String charName);
        void onCommandExecuted(String playerName, String commandName, String ruleName, boolean success);
        void onGoldChanged(String playerName, String operationText, double amount, double oldGold, double newGold, String ruleName);
        void onTagChanged(String playerName, String oldTag, String newTag, String ruleName);
        void onConnectionChanged(boolean connected);
        void onError(String error);
    }

    private SseClient(Context context) {
        cookieJar = new PersistentCookieJar(context);
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized SseClient getInstance(Context context) {
        if (instance == null) {
            instance = new SseClient(context.getApplicationContext());
        }
        return instance;
    }

    public void setBaseUrl(String url) {
        this.baseUrl = url;
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }

    public void setListener(SseEventListener listener) {
        this.listener = listener;
    }

    public void connect() {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return;
        }
        
        if (eventSource != null) {
            disconnect();
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/api/events")
                .build();

        eventSource = EventSources.createFactory(client)
                .newEventSource(request, new EventSourceListener() {
                    @Override
                    public void onOpen(EventSource eventSource, Response response) {
                        isConnected = true;
                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onConnectionChanged(true);
                            }
                        });
                    }

                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        try {
                            JSONObject json = new JSONObject(data);
                            String eventType = json.optString("type");
                            JSONObject eventData = json.optJSONObject("data");

                            if (eventType == null || eventData == null) return;

                            mainHandler.post(() -> {
                                if (listener == null) return;

                                try {
                                    switch (eventType) {
                                        case "player_join":
                                            String logIn = eventData.optString("log_in");
                                            if (!logIn.isEmpty()) {
                                                listener.onPlayerJoin(logIn);
                                            }
                                            break;

                                        case "player_leave":
                                            String logOut = eventData.optString("log_out");
                                            if (!logOut.isEmpty()) {
                                                listener.onPlayerLeave(logOut);
                                            }
                                            break;

                                        case "chat_message":
                                            JSONObject playerInfo = eventData.optJSONObject("player_info");
                                            if (playerInfo != null) {
                                                String charName = playerInfo.optString("Char_name");
                                                String message = eventData.optString("said");
                                                listener.onChatMessage(charName, message);
                                            }
                                            break;

                                        case "server_stats":
                                            listener.onServerStats(eventData);
                                            break;

                                        case "player_respawn":
                                            String respawnName = eventData.optString("respawn");
                                            if (respawnName.isEmpty()) {
                                                respawnName = eventData.optString("char_name");
                                            }
                                            if (!respawnName.isEmpty()) {
                                                listener.onPlayerRespawn(respawnName);
                                            }
                                            break;

                                        case "new_player":
                                            String newPlayerName = eventData.optString("log_in");
                                            if (newPlayerName.isEmpty()) {
                                                JSONObject newPlayerInfo = eventData.optJSONObject("player_info");
                                                if (newPlayerInfo != null) {
                                                    newPlayerName = newPlayerInfo.optString("Char_name");
                                                }
                                            }
                                            if (!newPlayerName.isEmpty()) {
                                                listener.onNewPlayer(newPlayerName);
                                            }
                                            break;

                                        case "command_executed":
                                            String playerName = eventData.optString("player_name");
                                            String commandName = eventData.optString("command_name");
                                            String ruleName = eventData.optString("rule_name");
                                            boolean success = eventData.optBoolean("success");
                                            listener.onCommandExecuted(playerName, commandName, ruleName, success);
                                            break;

                                        case "gold_changed":
                                            String goldPlayerName = eventData.optString("player_name");
                                            String operationText = eventData.optString("operation_text");
                                            double amount = eventData.optDouble("amount");
                                            double oldGold = eventData.optDouble("old_gold");
                                            double newGold = eventData.optDouble("new_gold");
                                            String goldRuleName = eventData.optString("rule_name");
                                            listener.onGoldChanged(goldPlayerName, operationText, amount, oldGold, newGold, goldRuleName);
                                            break;

                                        case "tag_changed":
                                            String tagPlayerName = eventData.optString("player_name");
                                            String oldTag = eventData.optString("old_tag");
                                            String newTag = eventData.optString("new_tag");
                                            String tagRuleName = eventData.optString("rule_name");
                                            listener.onTagChanged(tagPlayerName, oldTag, newTag, tagRuleName);
                                            break;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onClosed(EventSource eventSource) {
                        isConnected = false;
                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onConnectionChanged(false);
                            }
                        });
                    }

                    @Override
                    public void onFailure(EventSource eventSource, Throwable t, Response response) {
                        isConnected = false;
                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onConnectionChanged(false);
                                if (t != null) {
                                    listener.onError(t.getMessage());
                                }
                            }
                        });
                    }
                });
    }

    public void disconnect() {
        if (eventSource != null) {
            eventSource.cancel();
            eventSource = null;
        }
        isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
