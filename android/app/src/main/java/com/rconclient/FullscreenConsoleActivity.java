package com.rconclient;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rconclient.network.ApiClient;
import com.rconclient.network.SseClient;

import org.json.JSONObject;

public class FullscreenConsoleActivity extends AppCompatActivity {

    public static final String EXTRA_CONSOLE_CONTENT = "console_content";
    public static final String EXTRA_RCON_MODE = "rcon_mode";
    public static final String EXTRA_IS_CONNECTED = "is_connected";

    private TextView textConsole;
    private ScrollView scrollConsole;
    private EditText editCommand;
    private Button btnSend;
    private Button btnClose;
    private Button btnListPlayers;

    private ApiClient apiClient;
    private SseClient sseClient;
    private String rconMode = "direct";
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_console);

        apiClient = ApiClient.getInstance(this);
        sseClient = SseClient.getInstance(this);

        Intent intent = getIntent();
        String consoleContent = intent.getStringExtra(EXTRA_CONSOLE_CONTENT);
        rconMode = intent.getStringExtra(EXTRA_RCON_MODE);
        if (rconMode == null) rconMode = "direct";
        isConnected = intent.getBooleanExtra(EXTRA_IS_CONNECTED, false);

        initViews();
        loadConsoleContent(consoleContent);
        setupSseListener();
    }

    private void initViews() {
        textConsole = findViewById(R.id.text_console_fullscreen);
        scrollConsole = findViewById(R.id.scroll_console_fullscreen);
        editCommand = findViewById(R.id.edit_command_fullscreen);
        btnSend = findViewById(R.id.btn_send_fullscreen);
        btnClose = findViewById(R.id.btn_close);
        btnListPlayers = findViewById(R.id.btn_list_players_fullscreen);

        editCommand.setEnabled(isConnected || "sse".equals(rconMode));
        btnSend.setEnabled(isConnected || "sse".equals(rconMode));
        btnListPlayers.setEnabled(isConnected || "sse".equals(rconMode));

        btnClose.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendCommand());
        btnListPlayers.setOnClickListener(v -> sendListPlayersCommand());
    }

    private void loadConsoleContent(String content) {
        if (!TextUtils.isEmpty(content)) {
            textConsole.setText(content);
            scrollConsole.post(() -> scrollConsole.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void setupSseListener() {
        sseClient.setListener(new SseClient.SseEventListener() {
            @Override
            public void onPlayerJoin(String playerName) {
                appendConsole(playerName + " 加入游戏", "success");
            }

            @Override
            public void onPlayerLeave(String playerName) {
                appendConsole(playerName + " 退出游戏", "warning");
            }

            @Override
            public void onChatMessage(String charName, String message) {
                appendConsole(charName + "：" + message, "chat");
            }

            @Override
            public void onServerStats(JSONObject stats) {
            }

            @Override
            public void onPlayerRespawn(String charName) {
                appendConsole(charName + " 已经重生", "success");
            }

            @Override
            public void onNewPlayer(String charName) {
                appendConsole("发现新玩家 " + charName, "success");
            }

            @Override
            public void onCommandExecuted(String playerName, String commandName, String ruleName, boolean success) {
                String statusText = success ? "✅" : "❌";
                appendConsole(statusText + " " + ruleName + ": 对 " + playerName + " 执行命令 " + commandName, success ? "info" : "error");
            }

            @Override
            public void onGoldChanged(String playerName, String operationText, double amount, double oldGold, double newGold, String ruleName) {
                appendConsole("💰 " + ruleName + ": " + playerName + " 金额" + operationText + " " + (int)amount + " (" + (int)oldGold + " → " + (int)newGold + ")", "gold");
            }

            @Override
            public void onTagChanged(String playerName, String oldTag, String newTag, String ruleName) {
                appendConsole("🏷️ " + ruleName + ": " + playerName + " 标签变更 (" + oldTag + " → " + newTag + ")", "tag");
            }

            @Override
            public void onConnectionChanged(boolean connected) {
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void sendCommand() {
        String command = editCommand.getText().toString().trim();
        if (TextUtils.isEmpty(command)) {
            Toast.makeText(this, "请输入命令", Toast.LENGTH_SHORT).show();
            return;
        }

        appendConsole("> " + command, "info");
        editCommand.setText("");
        btnSend.setEnabled(false);

        apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            String resp = json.optString("response", "");
                            appendConsole(resp, "success");
                        } else {
                            appendConsole(json.optString("message", "命令执行失败"), "error");
                        }
                    } catch (Exception e) {
                        appendConsole("命令执行失败: " + e.getMessage(), "error");
                    }
                    btnSend.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    appendConsole("发送失败: " + error, "error");
                    btnSend.setEnabled(true);
                });
            }
        });
    }

    private void sendListPlayersCommand() {
        if (!isConnected && !"sse".equals(rconMode)) {
            Toast.makeText(this, "请先连接服务器", Toast.LENGTH_SHORT).show();
            return;
        }

        appendConsole("> listplayers", "info");
        btnListPlayers.setEnabled(false);

        apiClient.sendCommand("listplayers", rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            String resp = json.optString("response", "");
                            appendConsole(resp, "success");
                        } else {
                            appendConsole(json.optString("message", "命令执行失败"), "error");
                        }
                    } catch (Exception e) {
                        appendConsole("命令执行失败: " + e.getMessage(), "error");
                    }
                    btnListPlayers.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    appendConsole("发送失败: " + error, "error");
                    btnListPlayers.setEnabled(true);
                });
            }
        });
    }

    private void appendConsole(String message, String type) {
        String color = "#b0bec5";
        switch (type) {
            case "success":
                color = "#4caf50";
                break;
            case "error":
                color = "#ef5350";
                break;
            case "warning":
                color = "#ff9800";
                break;
            case "chat":
                color = "#00bcd4";
                break;
            case "gold":
                color = "#ffd700";
                break;
            case "tag":
                color = "#9c27b0";
                break;
        }
        String formattedMessage = formatListPlayersOutput(message);
        String html = String.format("<font color=\"%s\">%s</font><br>", color, formattedMessage);
        textConsole.append(android.text.Html.fromHtml(html));
        scrollConsole.post(() -> scrollConsole.fullScroll(View.FOCUS_DOWN));
    }

    private String formatListPlayersOutput(String message) {
        if (message == null || !message.contains("Idx") || !message.contains("Char name")) {
            return message;
        }
        String[] lines = message.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("Idx")) {
                result.append("Idx | Char name          | Player name<br>");
            } else if (trimmedLine.isEmpty()) {
                continue;
            } else {
                String[] parts = trimmedLine.split("\\|");
                if (parts.length >= 3) {
                    String idx = parts[0].trim();
                    String charName = parts[1].trim();
                    String playerName = parts[2].trim();
                    result.append(String.format("%-3s | %-18s | %s<br>", idx, charName, playerName));
                } else {
                    result.append(trimmedLine).append("<br>");
                }
            }
        }
        return result.toString();
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_CONSOLE_CONTENT, textConsole.getText().toString());
        setResult(RESULT_OK, resultIntent);
        super.finish();
    }
}
