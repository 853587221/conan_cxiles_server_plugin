package com.rconclient;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rconclient.adapter.CommandAdapter;
import com.rconclient.model.Category;
import com.rconclient.model.Command;
import com.rconclient.network.ApiClient;
import com.rconclient.network.SseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_FULLSCREEN_CONSOLE = 1001;

    private EditText editHost;
    private EditText editRconPassword;
    private EditText editPort;
    private EditText editCommand;
    private CheckBox checkboxSave;
    private SwitchCompat switchRconMode;
    private Button btnConnect;
    private Button btnSend;
    private Button btnAutoCommand;
    private Button btnPlayerManage;
    private Button btnShop;
    private Button btnMore;
    private Button btnLogout;
    private Button btnEditCategories;
    private Button btnEditCommands;
    private Button btnListPlayers;
    private Button btnFullscreenConsole;
    private TextView textConsole;
    private TextView textStatus;
    private View viewStatusDot;
    private ScrollView scrollConsole;
    private Spinner spinnerCategory;
    private RecyclerView recyclerCommands;
    private LinearLayout layoutConnectionHeader;
    private LinearLayout layoutConnectionSettings;
    private TextView textConnectionToggle;
    private boolean isConnectionSettingsExpanded = true;

    private ApiClient apiClient;
    private SseClient sseClient;
    private SharedPreferences prefs;
    private boolean isConnected = false;
    private String rconMode = "direct";
    private String currentCategory = "all";
    private List<Category> categories = new ArrayList<>();
    private List<Command> commands = new ArrayList<>();
    private CommandAdapter commandAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiClient = ApiClient.getInstance(this);
        sseClient = SseClient.getInstance(this);
        prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE);

        String baseUrl = prefs.getString("base_url", "");
        if (!TextUtils.isEmpty(baseUrl)) {
            sseClient.setBaseUrl(baseUrl);
            sseClient.connect();
        }

        initViews();
        setupRecyclerView();
        setupSseListener();
        loadConnectionInfo();
        loadCategories();
        loadCommands();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sseClient != null) {
            sseClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FULLSCREEN_CONSOLE && resultCode == RESULT_OK && data != null) {
            String consoleContent = data.getStringExtra(FullscreenConsoleActivity.EXTRA_CONSOLE_CONTENT);
            if (consoleContent != null) {
                textConsole.setText(consoleContent);
            }
        }
    }

    private void openFullscreenConsole() {
        Intent intent = new Intent(this, FullscreenConsoleActivity.class);
        intent.putExtra(FullscreenConsoleActivity.EXTRA_CONSOLE_CONTENT, textConsole.getText().toString());
        intent.putExtra(FullscreenConsoleActivity.EXTRA_RCON_MODE, rconMode);
        intent.putExtra(FullscreenConsoleActivity.EXTRA_IS_CONNECTED, isConnected);
        startActivityForResult(intent, REQUEST_CODE_FULLSCREEN_CONSOLE);
    }

    private void initViews() {
        editHost = findViewById(R.id.edit_host);
        editRconPassword = findViewById(R.id.edit_rcon_password);
        editPort = findViewById(R.id.edit_port);
        editCommand = findViewById(R.id.edit_command);
        checkboxSave = findViewById(R.id.checkbox_save);
        switchRconMode = findViewById(R.id.switch_rcon_mode);
        btnConnect = findViewById(R.id.btn_connect);
        btnSend = findViewById(R.id.btn_send);
        btnAutoCommand = findViewById(R.id.btn_auto_command);
        btnPlayerManage = findViewById(R.id.btn_player_manage);
        btnShop = findViewById(R.id.btn_shop);
        btnMore = findViewById(R.id.btn_more);
        btnLogout = findViewById(R.id.btn_logout);
        btnEditCategories = findViewById(R.id.btn_edit_categories);
        btnEditCommands = findViewById(R.id.btn_edit_commands);
        btnListPlayers = findViewById(R.id.btn_list_players);
        btnFullscreenConsole = findViewById(R.id.btn_fullscreen_console);
        textConsole = findViewById(R.id.text_console);
        textStatus = findViewById(R.id.text_status);
        viewStatusDot = findViewById(R.id.view_status_dot);
        scrollConsole = findViewById(R.id.scroll_console);
        spinnerCategory = findViewById(R.id.spinner_category);
        recyclerCommands = findViewById(R.id.recycler_commands);
        layoutConnectionHeader = findViewById(R.id.layout_connection_header);
        layoutConnectionSettings = findViewById(R.id.layout_connection_settings);
        textConnectionToggle = findViewById(R.id.text_connection_toggle);

        layoutConnectionHeader.setOnClickListener(v -> toggleConnectionSettings());

        btnConnect.setOnClickListener(v -> connect());
        btnSend.setOnClickListener(v -> sendCommand());
        btnAutoCommand.setOnClickListener(v -> startActivity(new Intent(this, AutoCommandActivity.class)));
        btnPlayerManage.setOnClickListener(v -> startActivity(new Intent(this, PlayerManageActivity.class)));
        btnShop.setOnClickListener(v -> startActivity(new Intent(this, ShopActivity.class)));
        btnLogout.setOnClickListener(v -> logout());
        btnMore.setOnClickListener(v -> showMoreDialog());
        btnEditCategories.setOnClickListener(v -> showCategoryManageDialog());
        btnEditCommands.setOnClickListener(v -> showCommandManageDialog());
        btnListPlayers.setOnClickListener(v -> sendListPlayersCommand());
        btnFullscreenConsole.setOnClickListener(v -> openFullscreenConsole());

        switchRconMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rconMode = isChecked ? "sse" : "direct";
            prefs.edit().putString("rcon_mode", rconMode).apply();
            if (isChecked) {
                appendConsole("无公网模式已启用，通过插件发送命令", "warning");
                editCommand.setEnabled(true);
                btnSend.setEnabled(true);
            } else if (!isConnected) {
                editCommand.setEnabled(false);
                btnSend.setEnabled(false);
            }
        });

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    currentCategory = "all";
                } else if (position <= categories.size()) {
                    currentCategory = categories.get(position - 1).getName();
                }
                loadCommands();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupRecyclerView() {
        commandAdapter = new CommandAdapter();
        recyclerCommands.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerCommands.setAdapter(commandAdapter);

        commandAdapter.setOnCommandClickListener(command -> {
            editCommand.setText(command.getExample());
            editCommand.requestFocus();
        });
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

    private void toggleConnectionSettings() {
        if (isConnectionSettingsExpanded) {
            collapseConnectionSettings();
        } else {
            expandConnectionSettings();
        }
    }

    private void collapseConnectionSettings() {
        layoutConnectionSettings.setVisibility(View.GONE);
        textConnectionToggle.setText("▶");
        isConnectionSettingsExpanded = false;
    }

    private void expandConnectionSettings() {
        layoutConnectionSettings.setVisibility(View.VISIBLE);
        textConnectionToggle.setText("▼");
        isConnectionSettingsExpanded = true;
    }

    private void loadConnectionInfo() {
        apiClient.getConnectionInfo(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONObject info = json.getJSONObject("connection_info");
                            editHost.setText(info.optString("host", "127.0.0.1"));
                            editRconPassword.setText(info.optString("password", ""));
                            editPort.setText(String.valueOf(info.optInt("port", 25575)));
                            
                            String mode = info.optString("rcon_mode", "direct");
                            rconMode = mode;
                            prefs.edit().putString("rcon_mode", mode).apply();
                            switchRconMode.setChecked("sse".equals(mode));

                            if ("sse".equals(mode)) {
                                editCommand.setEnabled(true);
                                btnSend.setEnabled(true);
                                appendConsole("无公网模式已启用", "warning");
                            } else if (!TextUtils.isEmpty(info.optString("host")) 
                                    && !TextUtils.isEmpty(info.optString("password"))) {
                                autoConnect();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void autoConnect() {
        String host = editHost.getText().toString().trim();
        String password = editRconPassword.getText().toString().trim();
        int port = Integer.parseInt(editPort.getText().toString().trim());

        appendConsole("正在自动连接...", "info");

        apiClient.connect(host, password, port, rconMode, true, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            setConnected(true);
                            appendConsole(json.optString("message", "连接成功"), "success");
                            collapseConnectionSettings();
                        } else {
                            appendConsole(json.optString("message", "连接失败"), "error");
                        }
                    } catch (Exception e) {
                        appendConsole("连接失败: " + e.getMessage(), "error");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> appendConsole("连接失败: " + error, "error"));
            }
        });
    }

    private void connect() {
        String host = editHost.getText().toString().trim();
        String password = editRconPassword.getText().toString().trim();
        String portStr = editPort.getText().toString().trim();

        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(password) || TextUtils.isEmpty(portStr)) {
            Toast.makeText(this, "请填写所有连接信息", Toast.LENGTH_SHORT).show();
            return;
        }

        int port = Integer.parseInt(portStr);
        boolean saveConnection = checkboxSave.isChecked();

        appendConsole("正在连接...", "info");
        btnConnect.setEnabled(false);

        apiClient.connect(host, password, port, rconMode, saveConnection, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            setConnected(true);
                            appendConsole(json.optString("message", "连接成功"), "success");
                            collapseConnectionSettings();
                        } else {
                            appendConsole(json.optString("message", "连接失败"), "error");
                        }
                    } catch (Exception e) {
                        appendConsole("连接失败: " + e.getMessage(), "error");
                    }
                    btnConnect.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    appendConsole("连接失败: " + error, "error");
                    btnConnect.setEnabled(true);
                });
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

    private void loadCategories() {
        apiClient.getCategories(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("categories");
                            categories.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                categories.add(Category.fromJson(arr.getJSONObject(i)));
                            }
                            updateCategorySpinner();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void updateCategorySpinner() {
        List<String> items = new ArrayList<>();
        items.add("全部命令");
        for (Category cat : categories) {
            items.add(cat.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void loadCommands() {
        apiClient.getCommands(currentCategory, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("commands");
                            commands.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                commands.add(Command.fromJson(arr.getJSONObject(i)));
                            }
                            commandAdapter.setCommands(commands);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void setConnected(boolean connected) {
        isConnected = connected;
        if (connected) {
            viewStatusDot.setBackgroundResource(R.drawable.status_dot_connected);
            textStatus.setText("已连接");
            editCommand.setEnabled(true);
            btnSend.setEnabled(true);
        } else {
            viewStatusDot.setBackgroundResource(R.drawable.status_dot_disconnected);
            textStatus.setText("未连接");
            if (!switchRconMode.isChecked()) {
                editCommand.setEnabled(false);
                btnSend.setEnabled(false);
            }
        }
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

    private void logout() {
        apiClient.clearCookies();
        prefs.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showCategoryManageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_category_manage, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText editName = view.findViewById(R.id.edit_category_name);
        EditText editDesc = view.findViewById(R.id.edit_category_desc);
        Button btnAdd = view.findViewById(R.id.btn_add_category);
        RecyclerView recyclerCategories = view.findViewById(R.id.recycler_categories);

        CategoryManageAdapter adapter = new CategoryManageAdapter();
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategories.setAdapter(adapter);

        loadCategoriesForManage(adapter);

        btnAdd.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String desc = editDesc.getText().toString().trim();
            
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show();
                return;
            }

            apiClient.createCategory(name, desc, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success")) {
                                Toast.makeText(MainActivity.this, "分类添加成功", Toast.LENGTH_SHORT).show();
                                editName.setText("");
                                editDesc.setText("");
                                loadCategoriesForManage(adapter);
                                loadCategories();
                            } else {
                                Toast.makeText(MainActivity.this, "添加失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "添加失败: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private void loadCategoriesForManage(CategoryManageAdapter adapter) {
        apiClient.getCategories(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("categories");
                            List<Category> list = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                list.add(Category.fromJson(arr.getJSONObject(i)));
                            }
                            adapter.setCategories(list);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void showCommandManageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_command_manage, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText editName = view.findViewById(R.id.edit_command_name);
        EditText editDesc = view.findViewById(R.id.edit_command_desc);
        EditText editExample = view.findViewById(R.id.edit_command_example);
        Spinner spinnerCategory = view.findViewById(R.id.spinner_command_category);
        Spinner spinnerFilter = view.findViewById(R.id.spinner_filter_category);
        Button btnAdd = view.findViewById(R.id.btn_add_command);
        Button btnVariables = view.findViewById(R.id.btn_variables);
        View layoutVariables = view.findViewById(R.id.layout_variables);
        Button btnVarRespawn = view.findViewById(R.id.btn_var_respawn);
        Button btnVarCharname = view.findViewById(R.id.btn_var_charname);
        Button btnVarTribe = view.findViewById(R.id.btn_var_tribe);
        Button btnVarLevel = view.findViewById(R.id.btn_var_level);
        Button btnVarStrength = view.findViewById(R.id.btn_var_strength);
        Button btnVarAgility = view.findViewById(R.id.btn_var_agility);
        Button btnVarVitality = view.findViewById(R.id.btn_var_vitality);
        Button btnVarGrit = view.findViewById(R.id.btn_var_grit);
        Button btnVarAuthority = view.findViewById(R.id.btn_var_authority);
        Button btnVarExpertise = view.findViewById(R.id.btn_var_expertise);
        RecyclerView recyclerCommands = view.findViewById(R.id.recycler_commands);

        CommandManageAdapter adapter = new CommandManageAdapter();
        recyclerCommands.setLayoutManager(new LinearLayoutManager(this));
        recyclerCommands.setAdapter(adapter);

        loadCategorySpinners(spinnerCategory, spinnerFilter);

        btnVariables.setOnClickListener(v -> {
            if (layoutVariables.getVisibility() == View.VISIBLE) {
                layoutVariables.setVisibility(View.GONE);
            } else {
                layoutVariables.setVisibility(View.VISIBLE);
            }
        });

        btnVarRespawn.setOnClickListener(v -> insertVariable(editExample, "@复活点"));
        btnVarCharname.setOnClickListener(v -> insertVariable(editExample, "@角色名"));
        btnVarTribe.setOnClickListener(v -> insertVariable(editExample, "@同部落角色名"));
        btnVarLevel.setOnClickListener(v -> insertVariable(editExample, "@玩家等级"));
        btnVarStrength.setOnClickListener(v -> insertVariable(editExample, "@玩家力量"));
        btnVarAgility.setOnClickListener(v -> insertVariable(editExample, "@玩家灵活"));
        btnVarVitality.setOnClickListener(v -> insertVariable(editExample, "@玩家活力"));
        btnVarGrit.setOnClickListener(v -> insertVariable(editExample, "@玩家毅力"));
        btnVarAuthority.setOnClickListener(v -> insertVariable(editExample, "@玩家权威"));
        btnVarExpertise.setOnClickListener(v -> insertVariable(editExample, "@玩家专长"));

        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                String category = position == 0 ? "all" : categories.get(position - 1).getName();
                loadCommandsForManage(adapter, category);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btnAdd.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String desc = editDesc.getText().toString().trim();
            String example = editExample.getText().toString().trim();
            int catPos = spinnerCategory.getSelectedItemPosition();
            
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(example)) {
                Toast.makeText(this, "请填写命令名称和示例", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (catPos <= 0 || catPos > categories.size()) {
                Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show();
                return;
            }

            String category = categories.get(catPos - 1).getName();

            apiClient.createCommand(name, desc, category, example, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success")) {
                                Toast.makeText(MainActivity.this, "命令添加成功", Toast.LENGTH_SHORT).show();
                                editName.setText("");
                                editDesc.setText("");
                                editExample.setText("");
                                String filterCat = spinnerFilter.getSelectedItemPosition() == 0 ? "all" : 
                                    categories.get(spinnerFilter.getSelectedItemPosition() - 1).getName();
                                loadCommandsForManage(adapter, filterCat);
                                loadCommands();
                            } else {
                                Toast.makeText(MainActivity.this, "添加失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "添加失败: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private void insertVariable(EditText editText, String variable) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        String text = editText.getText().toString();
        String newText = text.substring(0, start) + variable + text.substring(end);
        editText.setText(newText);
        editText.setSelection(start + variable.length());
        editText.requestFocus();
    }

    private void loadCategorySpinners(Spinner spinnerCategory, Spinner spinnerFilter) {
        List<String> items = new ArrayList<>();
        items.add("选择分类");
        for (Category cat : categories) {
            items.add(cat.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        spinnerFilter.setAdapter(adapter);
    }

    private void loadCommandsForManage(CommandManageAdapter adapter, String category) {
        apiClient.getCommands(category, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("commands");
                            List<Command> list = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                list.add(Command.fromJson(arr.getJSONObject(i)));
                            }
                            adapter.setCommands(list);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private class CategoryManageAdapter extends RecyclerView.Adapter<CategoryManageAdapter.ViewHolder> {
        private List<Category> categoryList = new ArrayList<>();

        public void setCategories(List<Category> list) {
            categoryList = list;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_manage, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Category cat = categoryList.get(position);
            holder.textName.setText(cat.getName());
            holder.textDesc.setText(cat.getDescription() != null ? cat.getDescription() : "无描述");

            holder.btnEdit.setOnClickListener(v -> showEditCategoryDialog(cat));
            holder.btnDelete.setOnClickListener(v -> showDeleteCategoryDialog(cat));
        }

        @Override
        public int getItemCount() {
            return categoryList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName;
            TextView textDesc;
            Button btnEdit;
            Button btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_category_name);
                textDesc = itemView.findViewById(R.id.text_category_desc);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }

    private void showEditCategoryDialog(Category cat) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText editName = new EditText(this);
        editName.setHint("分类名称");
        editName.setText(cat.getName());
        layout.addView(editName);

        final EditText editDesc = new EditText(this);
        editDesc.setHint("分类描述");
        editDesc.setText(cat.getDescription());
        layout.addView(editDesc);

        builder.setTitle("✏️ 编辑分类");
        builder.setView(layout);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String name = editName.getText().toString().trim();
            String desc = editDesc.getText().toString().trim();
            
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show();
                return;
            }

            apiClient.updateCategory(cat.getId(), name, desc, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success")) {
                                Toast.makeText(MainActivity.this, "分类更新成功", Toast.LENGTH_SHORT).show();
                                loadCategories();
                            } else {
                                Toast.makeText(MainActivity.this, "更新失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "更新失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "更新失败: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showDeleteCategoryDialog(Category cat) {
        new AlertDialog.Builder(this)
                .setTitle("删除分类")
                .setMessage("确定要删除分类 \"" + cat.getName() + "\" 吗？此操作会同时删除该分类下的所有命令！")
                .setPositiveButton("删除", (dialog, which) -> {
                    apiClient.deleteCategory(cat.getId(), new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                try {
                                    JSONObject json = new JSONObject(response);
                                    if (json.optBoolean("success")) {
                                        Toast.makeText(MainActivity.this, "分类删除成功", Toast.LENGTH_SHORT).show();
                                        loadCategories();
                                    } else {
                                        Toast.makeText(MainActivity.this, "删除失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "删除失败: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private class CommandManageAdapter extends RecyclerView.Adapter<CommandManageAdapter.ViewHolder> {
        private List<Command> commandList = new ArrayList<>();

        public void setCommands(List<Command> list) {
            commandList = list;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_command_manage, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Command cmd = commandList.get(position);
            holder.textName.setText(cmd.getName());
            holder.textCategory.setText(cmd.getCategory());
            holder.textDesc.setText(cmd.getDescription() != null ? cmd.getDescription() : "无描述");
            holder.textExample.setText(cmd.getExample());

            holder.btnEdit.setOnClickListener(v -> showEditCommandDialog(cmd));
            holder.btnDelete.setOnClickListener(v -> showDeleteCommandDialog(cmd));
        }

        @Override
        public int getItemCount() {
            return commandList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName;
            TextView textCategory;
            TextView textDesc;
            TextView textExample;
            Button btnEdit;
            Button btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_command_name);
                textCategory = itemView.findViewById(R.id.text_command_category);
                textDesc = itemView.findViewById(R.id.text_command_desc);
                textExample = itemView.findViewById(R.id.text_command_example);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }

    private void showEditCommandDialog(Command cmd) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_command_edit, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        EditText editName = view.findViewById(R.id.edit_command_name);
        EditText editDesc = view.findViewById(R.id.edit_command_desc);
        EditText editExample = view.findViewById(R.id.edit_command_example);
        Spinner spinnerCategory = view.findViewById(R.id.spinner_command_category);
        Button btnSave = view.findViewById(R.id.btn_save);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnVariables = view.findViewById(R.id.btn_variables);
        View layoutVariables = view.findViewById(R.id.layout_variables);
        Button btnVarRespawn = view.findViewById(R.id.btn_var_respawn);
        Button btnVarCharname = view.findViewById(R.id.btn_var_charname);
        Button btnVarTribe = view.findViewById(R.id.btn_var_tribe);
        Button btnVarLevel = view.findViewById(R.id.btn_var_level);
        Button btnVarStrength = view.findViewById(R.id.btn_var_strength);
        Button btnVarAgility = view.findViewById(R.id.btn_var_agility);
        Button btnVarVitality = view.findViewById(R.id.btn_var_vitality);
        Button btnVarGrit = view.findViewById(R.id.btn_var_grit);
        Button btnVarAuthority = view.findViewById(R.id.btn_var_authority);
        Button btnVarExpertise = view.findViewById(R.id.btn_var_expertise);

        editName.setText(cmd.getName());
        editDesc.setText(cmd.getDescription());
        editExample.setText(cmd.getExample());

        btnVariables.setOnClickListener(v -> {
            if (layoutVariables.getVisibility() == View.VISIBLE) {
                layoutVariables.setVisibility(View.GONE);
            } else {
                layoutVariables.setVisibility(View.VISIBLE);
            }
        });

        btnVarRespawn.setOnClickListener(v -> insertVariable(editExample, "@复活点"));
        btnVarCharname.setOnClickListener(v -> insertVariable(editExample, "@角色名"));
        btnVarTribe.setOnClickListener(v -> insertVariable(editExample, "@同部落角色名"));
        btnVarLevel.setOnClickListener(v -> insertVariable(editExample, "@玩家等级"));
        btnVarStrength.setOnClickListener(v -> insertVariable(editExample, "@玩家力量"));
        btnVarAgility.setOnClickListener(v -> insertVariable(editExample, "@玩家灵活"));
        btnVarVitality.setOnClickListener(v -> insertVariable(editExample, "@玩家活力"));
        btnVarGrit.setOnClickListener(v -> insertVariable(editExample, "@玩家毅力"));
        btnVarAuthority.setOnClickListener(v -> insertVariable(editExample, "@玩家权威"));
        btnVarExpertise.setOnClickListener(v -> insertVariable(editExample, "@玩家专长"));

        List<String> items = new ArrayList<>();
        items.add("选择分类");
        for (Category cat : categories) {
            items.add(cat.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getName().equals(cmd.getCategory())) {
                spinnerCategory.setSelection(i + 1);
                break;
            }
        }

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String desc = editDesc.getText().toString().trim();
            String example = editExample.getText().toString().trim();
            int catPos = spinnerCategory.getSelectedItemPosition();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(example)) {
                Toast.makeText(this, "请填写命令名称和示例", Toast.LENGTH_SHORT).show();
                return;
            }

            if (catPos <= 0 || catPos > categories.size()) {
                Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show();
                return;
            }

            String category = categories.get(catPos - 1).getName();

            apiClient.updateCommand(cmd.getId(), name, desc, category, example, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success")) {
                                Toast.makeText(MainActivity.this, "命令更新成功", Toast.LENGTH_SHORT).show();
                                loadCommands();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(MainActivity.this, "更新失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "更新失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "更新失败: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteCommandDialog(Command cmd) {
        new AlertDialog.Builder(this)
                .setTitle("删除命令")
                .setMessage("确定要删除命令 \"" + cmd.getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    apiClient.deleteCommand(cmd.getId(), new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                try {
                                    JSONObject json = new JSONObject(response);
                                    if (json.optBoolean("success")) {
                                        Toast.makeText(MainActivity.this, "命令删除成功", Toast.LENGTH_SHORT).show();
                                        loadCommands();
                                    } else {
                                        Toast.makeText(MainActivity.this, "删除失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "删除失败: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showMoreDialog() {
        String[] items = {"💬 聊天记录", "📥 下载插件", "📱 下载安卓APP"};
        
        new AlertDialog.Builder(this)
                .setTitle("📋 更多功能")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            startActivity(new Intent(this, ChatHistoryActivity.class));
                            break;
                        case 1:
                            downloadPlugin();
                            break;
                        case 2:
                            downloadAndroidApp();
                            break;
                    }
                })
                .show();
    }

    private void downloadPlugin() {
        String pluginUrl = apiClient.getBaseUrl() + "/plug-in/RCONDesktopClient.exe";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(pluginUrl));
            startActivity(intent);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("下载插件")
                    .setMessage("无法打开浏览器，请手动访问：\n\n" + pluginUrl)
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    private void downloadAndroidApp() {
        String apkUrl = apiClient.getBaseUrl() + "/plug-in/LFZKNGLQ.apk";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
            startActivity(intent);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("下载安卓APP")
                    .setMessage("无法打开浏览器，请手动访问：\n\n" + apkUrl)
                    .setPositiveButton("确定", null)
                    .show();
        }
    }
}
