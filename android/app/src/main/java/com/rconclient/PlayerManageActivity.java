package com.rconclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rconclient.adapter.PlayerAdapter;
import com.rconclient.model.Player;
import com.rconclient.network.ApiClient;
import com.rconclient.ThrallDetailActivity;
import com.rconclient.PlayerInfoActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PlayerManageActivity extends AppCompatActivity {

    private static final String TAG = "PlayerManageActivity";
    
    private EditText editSearch;
    private RecyclerView recyclerPlayers;
    private TextView textEmpty;
    private ImageButton btnRefresh;

    private ApiClient apiClient;
    private SharedPreferences prefs;
    private String rconMode = "direct";
    private List<Player> allPlayers = new ArrayList<>();
    private List<Player> filteredPlayers = new ArrayList<>();
    private PlayerAdapter playerAdapter;
    private Set<String> onlinePlayerNames = new HashSet<>();
    private Set<String> onlineCharNames = new HashSet<>();
    private Set<String> onlineUserIds = new HashSet<>();
    private Set<String> onlinePlatformIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_manage);

        apiClient = ApiClient.getInstance(this);
        prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE);
        rconMode = prefs.getString("rcon_mode", "direct");

        initViews();
        loadOnlinePlayersAndPlayerList();
    }

    private void initViews() {
        editSearch = findViewById(R.id.edit_search_player);
        recyclerPlayers = findViewById(R.id.recycler_players);
        textEmpty = findViewById(R.id.text_empty);
        btnRefresh = findViewById(R.id.btn_refresh);

        playerAdapter = new PlayerAdapter();
        recyclerPlayers.setLayoutManager(new LinearLayoutManager(this));
        recyclerPlayers.setAdapter(playerAdapter);

        playerAdapter.setOnPlayerClickListener(player -> {
            showPlayerDetailDialog(player);
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterPlayers(s.toString().trim());
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> loadOnlinePlayersAndPlayerList());
    }

    private void loadOnlinePlayersAndPlayerList() {
        textEmpty.setText("加载中...");
        textEmpty.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);
        
        apiClient.sendCommand("listplayers", rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    parseOnlinePlayers(response);
                    loadPlayers();
                    btnRefresh.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "listplayers error: " + error);
                    Toast.makeText(PlayerManageActivity.this, "获取在线玩家失败: " + error, Toast.LENGTH_SHORT).show();
                    loadPlayers();
                    btnRefresh.setEnabled(true);
                });
            }
        });
    }

    private void parseOnlinePlayers(String response) {
        onlinePlayerNames.clear();
        onlineCharNames.clear();
        onlineUserIds.clear();
        onlinePlatformIds.clear();
        
        try {
            JSONObject json = new JSONObject(response);
            if (json.optBoolean("success")) {
                String listResponse = json.optString("response", "");
                Log.d(TAG, "listplayers raw response: " + listResponse);
                
                String[] lines = listResponse.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    Log.d(TAG, "Parsing line: " + line);
                    
                    if (line.isEmpty() || line.contains("Idx") || line.contains("---") || 
                        line.startsWith("There are") || line.startsWith("No players") || line.startsWith("Total")) {
                        continue;
                    }
                    
                    String[] parts = line.split("\\|");
                    Log.d(TAG, "Parts count: " + parts.length);
                    for (int i = 0; i < parts.length; i++) {
                        Log.d(TAG, "  Part[" + i + "]: " + parts[i].trim());
                    }
                    
                    if (parts.length >= 3) {
                        String idx = parts[0].trim();
                        String charName = parts[1].trim();
                        String playerName = parts.length >= 3 ? parts[2].trim() : "";
                        String userId = parts.length >= 4 ? parts[3].trim() : "";
                        String platformId = parts.length >= 5 ? parts[4].trim() : "";
                        
                        if (!charName.isEmpty()) {
                            onlineCharNames.add(charName);
                            Log.d(TAG, "Added charName: " + charName);
                        }
                        if (!playerName.isEmpty()) {
                            onlinePlayerNames.add(playerName);
                            Log.d(TAG, "Added playerName: " + playerName);
                        }
                        if (!userId.isEmpty()) {
                            onlineUserIds.add(userId);
                            Log.d(TAG, "Added userId: " + userId);
                        }
                        if (!platformId.isEmpty()) {
                            onlinePlatformIds.add(platformId);
                            Log.d(TAG, "Added platformId: " + platformId);
                        }
                    }
                }
            }
            
            Log.d(TAG, "Online summary: charNames=" + onlineCharNames.size() + 
                  ", playerNames=" + onlinePlayerNames.size() + 
                  ", userIds=" + onlineUserIds.size() + 
                  ", platformIds=" + onlinePlatformIds.size());
            
        } catch (Exception e) {
            Log.e(TAG, "parseOnlinePlayers error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isPlayerOnline(Player player) {
        if (player.getCharName() != null && onlineCharNames.contains(player.getCharName())) {
            return true;
        }
        if (player.getPlayerName() != null && onlinePlayerNames.contains(player.getPlayerName())) {
            return true;
        }
        if (player.getSteamId() != null && onlineUserIds.contains(player.getSteamId())) {
            return true;
        }
        if (player.getPlatformId() != null && onlinePlatformIds.contains(player.getPlatformId())) {
            return true;
        }
        return false;
    }

    private void loadPlayers() {
        apiClient.getPlayers(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("players");
                            allPlayers.clear();
                            int onlineCount = 0;
                            for (int i = 0; i < arr.length(); i++) {
                                Player player = Player.fromJson(arr.getJSONObject(i));
                                boolean online = isPlayerOnline(player);
                                player.setOnline(online);
                                if (online) onlineCount++;
                                Log.d(TAG, "Player: " + player.getCharName() + 
                                      ", playerName=" + player.getPlayerName() + 
                                      ", steamId=" + player.getSteamId() + 
                                      ", platformId=" + player.getPlatformId() + 
                                      ", online=" + online);
                                allPlayers.add(player);
                            }
                            Log.d(TAG, "Total players: " + allPlayers.size() + ", online: " + onlineCount);
                            filteredPlayers.clear();
                            filteredPlayers.addAll(allPlayers);
                            playerAdapter.setPlayers(filteredPlayers);
                            updateEmptyView();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "loadPlayers error: " + e.getMessage());
                        e.printStackTrace();
                        Toast.makeText(PlayerManageActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void filterPlayers(String keyword) {
        filteredPlayers.clear();
        if (keyword.isEmpty()) {
            filteredPlayers.addAll(allPlayers);
        } else {
            for (Player player : allPlayers) {
                if (player.getCharName().toLowerCase().contains(keyword.toLowerCase()) ||
                    (player.getPlayerName() != null && player.getPlayerName().toLowerCase().contains(keyword.toLowerCase()))) {
                    filteredPlayers.add(player);
                }
            }
        }
        playerAdapter.setPlayers(filteredPlayers);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredPlayers.isEmpty()) {
            textEmpty.setText("暂无玩家数据");
            textEmpty.setVisibility(View.VISIBLE);
        } else {
            textEmpty.setVisibility(View.GONE);
        }
    }

    private void showPlayerDetailDialog(Player player) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("玩家详情");

        View view = getLayoutInflater().inflate(R.layout.dialog_player_detail, null);
        
        TextView textPlayerName = view.findViewById(R.id.text_player_name);
        TextView textOnlineStatus = view.findViewById(R.id.text_online_status);
        TextView textVipStatus = view.findViewById(R.id.text_vip_status);
        EditText editGold = view.findViewById(R.id.edit_gold);
        EditText editTag = view.findViewById(R.id.edit_tag);
        Button btnSendNotification = view.findViewById(R.id.btn_send_notification);
        Button btnVip = view.findViewById(R.id.btn_vip);
        Button btnPlayerInfo = view.findViewById(R.id.btn_player_info);
        Button btnInventory = view.findViewById(R.id.btn_inventory);
        Button btnThralls = view.findViewById(R.id.btn_thralls);
        Button btnSave = view.findViewById(R.id.btn_save);
        Button btnDelete = view.findViewById(R.id.btn_delete);

        textPlayerName.setText(player.getCharName());
        textOnlineStatus.setText(player.isOnline() ? "在线" : "离线");
        textOnlineStatus.setTextColor(player.isOnline() ? getColor(R.color.success) : getColor(R.color.text_secondary));
        
        long vipExpiry = player.getMonthlyCardExpiry();
        long now = System.currentTimeMillis() / 1000;
        if (vipExpiry > 0) {
            if (vipExpiry > now) {
                int remainingDays = (int) Math.ceil((vipExpiry - now) / 86400.0);
                textVipStatus.setText("👑 会员: 有效期剩余 " + remainingDays + " 天");
                textVipStatus.setTextColor(getColor(R.color.success));
            } else {
                textVipStatus.setText("👑 会员: 已过期");
                textVipStatus.setTextColor(getColor(R.color.text_secondary));
            }
            textVipStatus.setVisibility(View.VISIBLE);
        } else {
            textVipStatus.setVisibility(View.GONE);
        }
        
        editGold.setText(String.format("%.0f", player.getGold()));
        editTag.setText(String.valueOf(player.getPermissionLevel()));

        btnSendNotification.setVisibility(player.isOnline() ? View.VISIBLE : View.GONE);
        
        btnSendNotification.setOnClickListener(v -> showSendNotificationDialog(player));
        btnVip.setOnClickListener(v -> showVipDialog(player));
        btnPlayerInfo.setOnClickListener(v -> showPlayerInfoDialog(player));
        btnInventory.setOnClickListener(v -> showInventoryDialog(player));
        btnThralls.setOnClickListener(v -> showThrallsDialog(player));
        
        btnSave.setOnClickListener(v -> {
            double gold = Double.parseDouble(editGold.getText().toString().trim());
            int permissionLevel = Integer.parseInt(editTag.getText().toString().trim());
            savePlayer(player, gold, permissionLevel);
        });
        
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog(player));

        builder.setView(view);
        builder.setNegativeButton("关闭", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showTeleportConfirmDialog(String title, String position) {
        String normalizedPosition = position.replace(",", " ").replaceAll("\\s+", " ").trim();
        String[] coords = normalizedPosition.split(" ");
        if (coords.length < 3) {
            Toast.makeText(this, "坐标格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double x = Double.parseDouble(coords[0]);
        double y = Double.parseDouble(coords[1]);
        double z = Double.parseDouble(coords[2]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 10);
        
        TextView coordText = new TextView(this);
        coordText.setText("目标坐标: " + position);
        container.addView(coordText);
        
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = android.view.Gravity.CENTER;
        progressParams.topMargin = 20;
        progressBar.setLayoutParams(progressParams);
        container.addView(progressBar);
        
        TextView loadingText = new TextView(this);
        loadingText.setText("正在获取在线玩家...");
        loadingText.setGravity(android.view.Gravity.CENTER);
        loadingText.setTextColor(getResources().getColor(R.color.text_secondary, null));
        LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingParams.topMargin = 10;
        loadingText.setLayoutParams(loadingParams);
        container.addView(loadingText);
        
        builder.setView(container);
        AlertDialog dialog = builder.create();
        dialog.show();
        
        apiClient.sendCommand("listplayers", rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            String listResponse = json.optString("response", "");
                            java.util.List<String> playerIdxList = new java.util.ArrayList<>();
                            java.util.List<String> playerNameList = new java.util.ArrayList<>();
                            playerNameList.add("-- 选择在线玩家 --");
                            
                            String[] lines = listResponse.split("\n");
                            for (String line : lines) {
                                line = line.trim();
                                if (line.isEmpty() || line.contains("Idx") || line.contains("---") || 
                                    line.startsWith("There are") || line.startsWith("No players")) {
                                    continue;
                                }
                                
                                String[] parts = line.split("\\|");
                                if (parts.length >= 2) {
                                    String idx = parts[0].trim();
                                    String charName = parts[1].trim();
                                    playerIdxList.add(idx);
                                    playerNameList.add(charName + " (" + idx + ")");
                                }
                            }
                            
                            progressBar.setVisibility(View.GONE);
                            loadingText.setVisibility(View.GONE);
                            
                            if (playerIdxList.isEmpty()) {
                                loadingText.setText("当前没有在线玩家");
                                loadingText.setVisibility(View.VISIBLE);
                                return;
                            }
                            
                            android.widget.Spinner spinner = new android.widget.Spinner(PlayerManageActivity.this);
                            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                                PlayerManageActivity.this,
                                android.R.layout.simple_spinner_item,
                                playerNameList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner.setAdapter(adapter);
                            container.addView(spinner);
                            
                            TextView resultText = new TextView(PlayerManageActivity.this);
                            resultText.setVisibility(View.GONE);
                            resultText.setPadding(0, 16, 0, 0);
                            container.addView(resultText);
                            
                            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                    if (position > 0 && position <= playerIdxList.size()) {
                                        String selectedIdx = playerIdxList.get(position - 1);
                                        String selectedName = playerNameList.get(position);
                                        String command = "con " + selectedIdx + " TeleportPlayer " + (int)x + " " + (int)y + " " + (int)z;
                                        
                                        resultText.setText("正在传送 " + selectedName + "...");
                                        resultText.setTextColor(getResources().getColor(R.color.info, null));
                                        resultText.setVisibility(View.VISIBLE);
                                        
                                        apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
                                            @Override
                                            public void onSuccess(String response) {
                                                runOnUiThread(() -> {
                                                    try {
                                                        JSONObject json = new JSONObject(response);
                                                        if (json.optBoolean("success")) {
                                                            resultText.setText("✓ 传送成功!");
                                                            resultText.setTextColor(getResources().getColor(R.color.success, null));
                                                        } else {
                                                            resultText.setText("✗ 传送失败: " + json.optString("message"));
                                                            resultText.setTextColor(getResources().getColor(R.color.error, null));
                                                        }
                                                    } catch (Exception e) {
                                                        resultText.setText("✗ 传送失败");
                                                        resultText.setTextColor(getResources().getColor(R.color.error, null));
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(String error) {
                                                runOnUiThread(() -> {
                                                    resultText.setText("✗ 网络错误: " + error);
                                                    resultText.setTextColor(getResources().getColor(R.color.error, null));
                                                });
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                            });
                        } else {
                            loadingText.setText("获取在线玩家失败");
                            loadingText.setTextColor(getResources().getColor(R.color.error, null));
                        }
                    } catch (Exception e) {
                        loadingText.setText("解析失败: " + e.getMessage());
                        loadingText.setTextColor(getResources().getColor(R.color.error, null));
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    loadingText.setText("网络错误: " + error);
                    loadingText.setTextColor(getResources().getColor(R.color.error, null));
                });
            }
        });
    }

    private void showSendNotificationDialog(Player player) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("发送通知给 " + player.getCharName());

        final EditText input = new EditText(this);
        input.setHint("输入消息内容");
        input.setPadding(50, 30, 50, 30);
        builder.setView(input);

        builder.setPositiveButton("发送", (dialog, which) -> {
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                sendNotification(player, message);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void sendNotification(Player player, String message) {
        String targetName = (player.getPlayerName() != null && player.getCharName().contains(" ")) 
                ? player.getPlayerName() : player.getCharName();
        String command = "con 0 playermessage \"" + targetName + "\" \"" + message + "\"";
        
        apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            Toast.makeText(PlayerManageActivity.this, "通知发送成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PlayerManageActivity.this, "发送失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerManageActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showPlayerInfoDialog(Player player) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📋 玩家信息 - " + player.getCharName());

        View view = getLayoutInflater().inflate(R.layout.dialog_player_basic_info, null);
        
        ProgressBar progressLoading = view.findViewById(R.id.progress_loading);
        LinearLayout layoutContent = view.findViewById(R.id.layout_content);
        
        TextView textPlayerId = view.findViewById(R.id.text_player_id);
        TextView textCharName = view.findViewById(R.id.text_char_name);
        TextView textLevel = view.findViewById(R.id.text_level);
        TextView textHealth = view.findViewById(R.id.text_health);
        TextView textFood = view.findViewById(R.id.text_food);
        TextView textPosition = view.findViewById(R.id.text_position);
        TextView textSpawnPoint = view.findViewById(R.id.text_spawn_point);
        Button btnTeleportHere = view.findViewById(R.id.btn_teleport_here);
        Button btnTeleportSpawn = view.findViewById(R.id.btn_teleport_spawn);
        
        TextView textThrallsTitle = view.findViewById(R.id.text_thralls_title);
        LinearLayout layoutThralls = view.findViewById(R.id.layout_thralls);
        TextView textThrallsEmpty = view.findViewById(R.id.text_thralls_empty);
        
        TextView textStrength = view.findViewById(R.id.text_strength);
        TextView textAgility = view.findViewById(R.id.text_agility);
        TextView textVitality = view.findViewById(R.id.text_vitality);
        TextView textGrit = view.findViewById(R.id.text_grit);
        TextView textAuthority = view.findViewById(R.id.text_authority);
        TextView textExpertise = view.findViewById(R.id.text_expertise);
        TextView textAttributePoints = view.findViewById(R.id.text_attribute_points);
        Button btnAddAttributePoints = view.findViewById(R.id.btn_add_attribute_points);
        Button btnEditStrength = view.findViewById(R.id.btn_edit_strength);
        Button btnEditAgility = view.findViewById(R.id.btn_edit_agility);
        Button btnEditVitality = view.findViewById(R.id.btn_edit_vitality);
        Button btnEditGrit = view.findViewById(R.id.btn_edit_grit);
        Button btnEditAuthority = view.findViewById(R.id.btn_edit_authority);
        Button btnEditExpertise = view.findViewById(R.id.btn_edit_expertise);
        
        layoutContent.setVisibility(View.GONE);
        progressLoading.setVisibility(View.VISIBLE);
        
        builder.setView(view);
        builder.setNegativeButton("关闭", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        
        final String[] currentPosition = {null};
        final String[] spawnPoint = {null};
        
        apiClient.getPlayerInventory(player.getCharName(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONObject playerInfo = json.optJSONObject("player_info");
                            JSONArray thralls = json.optJSONArray("thralls");
                            if (thralls == null) {
                                JSONObject inventory = json.optJSONObject("inventory");
                                if (inventory != null) {
                                    thralls = inventory.optJSONArray("thralls");
                                }
                            }
                            if (thralls == null) {
                                JSONObject thrallsObj = json.optJSONObject("thralls");
                                if (thrallsObj != null) {
                                    thralls = new JSONArray();
                                    Iterator<String> keys = thrallsObj.keys();
                                    while (keys.hasNext()) {
                                        String key = keys.next();
                                        JSONObject thrall = thrallsObj.optJSONObject(key);
                                        if (thrall != null) {
                                            thralls.put(thrall);
                                        }
                                    }
                                }
                            }
                            if (thralls == null) {
                                JSONObject inventory = json.optJSONObject("inventory");
                                if (inventory != null) {
                                    JSONObject thrallsObj = inventory.optJSONObject("thralls");
                                    if (thrallsObj != null) {
                                        thralls = new JSONArray();
                                        Iterator<String> keys = thrallsObj.keys();
                                        while (keys.hasNext()) {
                                            String key = keys.next();
                                            JSONObject thrall = thrallsObj.optJSONObject(key);
                                            if (thrall != null) {
                                                thralls.put(thrall);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (playerInfo != null) {
                                textPlayerId.setText(playerInfo.optString("player_id", "未知"));
                                textCharName.setText(playerInfo.optString("char_name", player.getCharName()));
                                textLevel.setText(String.valueOf(playerInfo.optInt("level", player.getLevel())));
                                textHealth.setText(String.valueOf(playerInfo.optInt("health", 0)));
                                textFood.setText(playerInfo.optInt("food", 0) + "%");
                                
                                currentPosition[0] = playerInfo.optString("position", "未知");
                                textPosition.setText(currentPosition[0]);
                                
                                spawnPoint[0] = playerInfo.optString("spawn_point", "未设置");
                                textSpawnPoint.setText(spawnPoint[0]);
                                
                                JSONObject stats = playerInfo.optJSONObject("stats");
                                if (stats != null) {
                                    textStrength.setText(String.valueOf(stats.optInt("strength", 0)));
                                    textAgility.setText(String.valueOf(stats.optInt("agility", 0)));
                                    textVitality.setText(String.valueOf(stats.optInt("vitality", 0)));
                                    textGrit.setText(String.valueOf(stats.optInt("grit", 0)));
                                    textAuthority.setText(String.valueOf(stats.optInt("authority", 0)));
                                    textExpertise.setText(String.valueOf(stats.optInt("expertise", 0)));
                                } else {
                                    textStrength.setText("0");
                                    textAgility.setText("0");
                                    textVitality.setText("0");
                                    textGrit.setText("0");
                                    textAuthority.setText("0");
                                    textExpertise.setText("0");
                                }
                                
                                int attrPoints = playerInfo.optInt("attribute_points", -1);
                                if (attrPoints == -1 && stats != null) {
                                    attrPoints = stats.optInt("attribute_points", 0);
                                }
                                textAttributePoints.setText(String.valueOf(attrPoints));
                            }
                            
                            renderThrallsOverview(thralls, layoutThralls, textThrallsEmpty, textThrallsTitle);
                            
                            progressLoading.setVisibility(View.GONE);
                            layoutContent.setVisibility(View.VISIBLE);
                        } else {
                            progressLoading.setVisibility(View.GONE);
                            Toast.makeText(PlayerManageActivity.this, "获取信息失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        progressLoading.setVisibility(View.GONE);
                        Toast.makeText(PlayerManageActivity.this, "解析数据失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        btnTeleportHere.setOnClickListener(v -> {
            if (currentPosition[0] == null || currentPosition[0].equals("未知")) {
                Toast.makeText(this, "坐标未知", Toast.LENGTH_SHORT).show();
                return;
            }
            showTeleportConfirmDialog("传送玩家到此坐标", currentPosition[0]);
        });
        
        btnTeleportSpawn.setOnClickListener(v -> {
            if (spawnPoint[0] == null || spawnPoint[0].equals("未设置")) {
                Toast.makeText(this, "复活点未设置", Toast.LENGTH_SHORT).show();
                return;
            }
            showTeleportConfirmDialog("传送玩家到复活点", spawnPoint[0]);
        });
        
        btnAddAttributePoints.setOnClickListener(v -> showAddAttributePointsDialog(player, textAttributePoints));
        btnEditStrength.setOnClickListener(v -> showEditStatDialog(player, "AttributeMight", "力量", textStrength.getText().toString(), textStrength));
        btnEditAgility.setOnClickListener(v -> showEditStatDialog(player, "AttributeAthleticism", "灵活", textAgility.getText().toString(), textAgility));
        btnEditVitality.setOnClickListener(v -> showEditStatDialog(player, "AttributeHealth", "活力", textVitality.getText().toString(), textVitality));
        btnEditGrit.setOnClickListener(v -> showEditStatDialog(player, "Attributestamina", "毅力", textGrit.getText().toString(), textGrit));
        btnEditAuthority.setOnClickListener(v -> showEditStatDialog(player, "AttributeLeadership", "权威", textAuthority.getText().toString(), textAuthority));
        btnEditExpertise.setOnClickListener(v -> showEditStatDialog(player, "AttributeEncumbrance", "专长", textExpertise.getText().toString(), textExpertise));
    }
    
    private void renderThrallsOverview(JSONArray thralls, LinearLayout layoutThralls, TextView textThrallsEmpty, TextView textThrallsTitle) {
        int thrallCount = (thralls != null) ? thralls.length() : 0;
        textThrallsTitle.setText("👤 奴隶列表 (" + thrallCount + ")");
        
        if (thralls == null || thralls.length() == 0) {
            textThrallsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        
        try {
            textThrallsEmpty.setVisibility(View.GONE);
            layoutThralls.removeAllViews();
            
            int displayCount = Math.min(thralls.length(), 5);
            for (int i = 0; i < displayCount; i++) {
                JSONObject thrall = thralls.getJSONObject(i);
                String name = thrall.optString("thrall_name", null);
                if (name == null || name.isEmpty() || "null".equals(name)) {
                    name = thrall.optString("thrall_type", "未知奴隶");
                    if (name != null && name.length() > 0) {
                        name = name.replaceAll("([A-Z])", " $1").replaceAll("\\s+", " ").trim();
                    } else {
                        name = "未知奴隶";
                    }
                }
                int level = thrall.optInt("level", 0);
                if (level < 1) level = 1;
                int health = thrall.optInt("health", 0);
                
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                row.setPadding(8, 8, 8, 8);
                row.setBackgroundResource(R.drawable.bg_card);
                
                TextView nameView = new TextView(this);
                nameView.setText("👤 " + name);
                nameView.setTextColor(getResources().getColor(R.color.pink, null));
                nameView.setTextSize(12);
                nameView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                
                TextView infoView = new TextView(this);
                infoView.setText("Lv." + level + " | ❤️" + health);
                infoView.setTextColor(getResources().getColor(R.color.text_secondary, null));
                infoView.setTextSize(12);
                
                row.addView(nameView);
                row.addView(infoView);
                layoutThralls.addView(row);
            }
            
            if (thralls.length() > 5) {
                TextView moreView = new TextView(this);
                moreView.setText("还有 " + (thralls.length() - 5) + " 个奴隶...");
                moreView.setTextColor(getResources().getColor(R.color.text_secondary, null));
                moreView.setTextSize(11);
                moreView.setPadding(8, 8, 8, 8);
                layoutThralls.addView(moreView);
            }
        } catch (Exception e) {
            textThrallsEmpty.setVisibility(View.VISIBLE);
        }
    }
    
    private void showEditStatDialog(Player player, String statCode, String statName, String currentValue, TextView resultView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修改" + statName);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 10);
        
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(currentValue);
        container.addView(input);
        
        builder.setView(container);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                executeStatCommand(player, statCode, value, resultView);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void executeStatCommand(Player player, String statCode, String value, TextView resultView) {
        apiClient.sendCommand("listplayers", rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            String listResponse = json.optString("response", "");
                            String playerIdx = findPlayerIdx(listResponse, player.getCharName());
                            
                            if (playerIdx != null) {
                                String command = "con " + playerIdx + " setstat " + statCode + " " + value;
                                apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        runOnUiThread(() -> {
                                            try {
                                                JSONObject json = new JSONObject(response);
                                                if (json.optBoolean("success")) {
                                                    Toast.makeText(PlayerManageActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                                                    resultView.setText(value);
                                                } else {
                                                    Toast.makeText(PlayerManageActivity.this, "修改失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                                                }
                                            } catch (Exception e) {
                                                Toast.makeText(PlayerManageActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {
                                        runOnUiThread(() -> Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
                                    }
                                });
                            } else {
                                Toast.makeText(PlayerManageActivity.this, "玩家不在线", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerManageActivity.this, "解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void showAddAttributePointsDialog(Player player, TextView resultView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("增加未分配属性点");
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 10);
        
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("输入要增加的点数");
        container.addView(input);
        
        builder.setView(container);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                executeAddAttributePoints(player, value, resultView);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void executeAddAttributePoints(Player player, String points, TextView resultView) {
        apiClient.sendCommand("listplayers", rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            String listResponse = json.optString("response", "");
                            String playerIdx = findPlayerIdx(listResponse, player.getCharName());
                            
                            if (playerIdx != null) {
                                String command = "con " + playerIdx + " AddUndistributedAttributePoints " + points;
                                apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        runOnUiThread(() -> {
                                            try {
                                                JSONObject json = new JSONObject(response);
                                                if (json.optBoolean("success")) {
                                                    Toast.makeText(PlayerManageActivity.this, "已增加 " + points + " 属性点", Toast.LENGTH_SHORT).show();
                                                    int current = Integer.parseInt(resultView.getText().toString());
                                                    resultView.setText(String.valueOf(current + Integer.parseInt(points)));
                                                } else {
                                                    Toast.makeText(PlayerManageActivity.this, "增加失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                                                }
                                            } catch (Exception e) {
                                                Toast.makeText(PlayerManageActivity.this, "增加失败", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {
                                        runOnUiThread(() -> Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
                                    }
                                });
                            } else {
                                Toast.makeText(PlayerManageActivity.this, "玩家不在线", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerManageActivity.this, "解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private String findPlayerIdx(String listResponse, String targetName) {
        String[] lines = listResponse.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.contains("Idx") || line.contains("---") || 
                line.startsWith("There are") || line.startsWith("No players")) {
                continue;
            }
            
            String[] parts = line.split("\\|");
            if (parts.length >= 2) {
                String idx = parts[0].trim();
                String charName = parts[1].trim();
                if (charName.equals(targetName)) {
                    return idx;
                }
            }
        }
        return null;
    }

    private void showInventoryDialog(Player player) {
        String charName = player.getCharName();
        if (charName == null || charName.isEmpty()) {
            Toast.makeText(this, "玩家角色名为空", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PlayerItemsActivity.class);
        intent.putExtra(PlayerItemsActivity.EXTRA_PLAYER_NAME, charName);
        startActivity(intent);
    }

    private void showThrallsDialog(Player player) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("👤 奴隶信息 - " + player.getCharName());

        View view = getLayoutInflater().inflate(R.layout.dialog_thralls_list, null);
        
        EditText editSearch = view.findViewById(R.id.edit_search);
        ProgressBar progressLoading = view.findViewById(R.id.progress_loading);
        TextView textError = view.findViewById(R.id.text_error);
        ScrollView scrollContent = view.findViewById(R.id.scroll_content);
        LinearLayout layoutThrallsList = view.findViewById(R.id.layout_thralls_list);
        
        builder.setView(view);
        builder.setNegativeButton("关闭", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        
        final JSONArray[] allThralls = {new JSONArray()};

        apiClient.getPlayerThralls(player.getCharName(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success") || json.has("thralls")) {
                            JSONArray thrallArray = json.optJSONArray("thralls");
                            if (thrallArray == null) {
                                JSONObject inventory = json.optJSONObject("inventory");
                                if (inventory != null) {
                                    thrallArray = inventory.optJSONArray("thralls");
                                }
                            }
                            if (thrallArray == null) {
                                JSONObject thrallsObj = json.optJSONObject("thralls");
                                if (thrallsObj != null) {
                                    thrallArray = new JSONArray();
                                    Iterator<String> keys = thrallsObj.keys();
                                    while (keys.hasNext()) {
                                        String key = keys.next();
                                        JSONObject thrall = thrallsObj.optJSONObject(key);
                                        if (thrall != null) {
                                            thrallArray.put(thrall);
                                        }
                                    }
                                }
                            }
                            if (thrallArray == null) {
                                JSONObject inventory = json.optJSONObject("inventory");
                                if (inventory != null) {
                                    JSONObject thrallsObj = inventory.optJSONObject("thralls");
                                    if (thrallsObj != null) {
                                        thrallArray = new JSONArray();
                                        Iterator<String> keys = thrallsObj.keys();
                                        while (keys.hasNext()) {
                                            String key = keys.next();
                                            JSONObject thrall = thrallsObj.optJSONObject(key);
                                            if (thrall != null) {
                                                thrallArray.put(thrall);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            progressLoading.setVisibility(View.GONE);
                            
                            if (thrallArray != null && thrallArray.length() > 0) {
                                allThralls[0] = thrallArray;
                                renderThrallsList(layoutThrallsList, thrallArray, "");
                                scrollContent.setVisibility(View.VISIBLE);
                            } else {
                                textError.setText("暂无奴隶数据");
                                textError.setVisibility(View.VISIBLE);
                            }
                        } else {
                            progressLoading.setVisibility(View.GONE);
                            textError.setText("加载失败: " + json.optString("message", "未知错误"));
                            textError.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        progressLoading.setVisibility(View.GONE);
                        textError.setText("解析失败: " + e.getMessage());
                        textError.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    textError.setText("网络错误: " + error + "\n\n请确保桌面客户端正在运行");
                    textError.setVisibility(View.VISIBLE);
                });
            }
        });
        
        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String searchTerm = s.toString().toLowerCase().trim();
                filterThralls(layoutThrallsList, allThralls[0], searchTerm);
            }
        });
    }
    
    private void renderThrallsList(LinearLayout layoutThrallsList, JSONArray thralls, String searchTerm) {
        layoutThrallsList.removeAllViews();
        
        for (int i = 0; i < thralls.length(); i++) {
            JSONObject thrall = thralls.optJSONObject(i);
            if (thrall != null) {
                String name = getThrallName(thrall);
                long thrallId = thrall.optLong("thrall_id", 0);
                String idStr = String.valueOf(thrallId);
                
                boolean matches = searchTerm.isEmpty() 
                    || name.toLowerCase().contains(searchTerm) 
                    || idStr.contains(searchTerm);
                
                if (matches) {
                    View thrallCard = createThrallCard(thrall);
                    layoutThrallsList.addView(thrallCard);
                }
            }
        }
    }
    
    private void filterThralls(LinearLayout layoutThrallsList, JSONArray thralls, String searchTerm) {
        renderThrallsList(layoutThrallsList, thralls, searchTerm);
    }
    
    private String getThrallName(JSONObject thrall) {
        String name = thrall.optString("thrall_name", null);
        if (name == null || name.isEmpty() || "null".equals(name)) {
            name = thrall.optString("thrall_type", "未知奴隶");
            if (name != null && name.length() > 0) {
                name = name.replaceAll("([A-Z])", " $1").replaceAll("\\s+", " ").trim();
            } else {
                name = "未知奴隶";
            }
        }
        return name;
    }
    
    private View createThrallCard(JSONObject thrall) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);
        card.setPadding(16, 16, 16, 16);
        
        String name = getThrallName(thrall);
        
        int level = thrall.optInt("level", 0);
        if (level < 1) level = 1;
        int health = thrall.optInt("health", 0);
        long thrallId = thrall.optLong("thrall_id", 0);
        String position = thrall.optString("position", "未知");
        
        JSONObject stats = thrall.optJSONObject("stats");
        int food = 0;
        if (stats != null) {
            food = stats.optInt("food", 0);
        }
        
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        
        TextView nameView = new TextView(this);
        nameView.setText("👤 " + name);
        nameView.setTextColor(getResources().getColor(R.color.pink, null));
        nameView.setTextSize(14);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        
        TextView idView = new TextView(this);
        idView.setText("ID: " + thrallId);
        idView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        idView.setTextSize(11);
        
        headerRow.addView(nameView);
        headerRow.addView(idView);
        card.addView(headerRow);
        
        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(0, 12, 0, 12);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
        card.addView(divider);
        
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(2);
        gridLayout.setRowCount(3);
        gridLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        
        addGridItem(gridLayout, "⚔️ 等级", String.valueOf(level), R.color.success);
        addGridItem(gridLayout, "❤️ 生命", String.valueOf(health), R.color.error);
        addGridItem(gridLayout, "🍖 饱食", food + "%", R.color.warning);
        addGridItem(gridLayout, "📍 坐标", position, R.color.info);
        
        card.addView(gridLayout);
        
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, ThrallDetailActivity.class);
            intent.putExtra(ThrallDetailActivity.EXTRA_THRALL_DATA, thrall.toString());
            startActivity(intent);
        });
        
        return card;
    }
    
    private void addGridItem(GridLayout grid, String label, String value, int colorRes) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(8, 8, 8, 8);
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.width = 0;
        container.setLayoutParams(params);
        
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        labelView.setTextSize(12);
        
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(getResources().getColor(colorRes, null));
        valueView.setTextSize(13);
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        container.addView(labelView);
        container.addView(valueView);
        grid.addView(container);
    }

    private void savePlayer(Player player, double gold, int permissionLevel) {
        apiClient.updatePlayer(player.getId(), gold, permissionLevel, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            Toast.makeText(PlayerManageActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                            player.setGold(gold);
                            player.setPermissionLevel(permissionLevel);
                            player.setTag(permissionLevel);
                        } else {
                            Toast.makeText(PlayerManageActivity.this, "保存失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerManageActivity.this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showDeleteConfirmDialog(Player player) {
        new AlertDialog.Builder(this)
                .setTitle("删除玩家")
                .setMessage("确定要删除玩家 \"" + player.getCharName() + "\" 吗？\n\n此操作将删除该玩家的记录信息。\n删除后玩家重新登录时会重新同步数据！")
                .setPositiveButton("删除", (dialog, which) -> deletePlayer(player))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deletePlayer(Player player) {
        apiClient.deletePlayer(player.getId(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            Toast.makeText(PlayerManageActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                            allPlayers.remove(player);
                            filteredPlayers.remove(player);
                            playerAdapter.setPlayers(filteredPlayers);
                            updateEmptyView();
                        } else {
                            Toast.makeText(PlayerManageActivity.this, "删除失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerManageActivity.this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void showVipDialog(Player player) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("👑 会员管理 - " + player.getCharName());
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 10);
        
        long vipExpiry = player.getMonthlyCardExpiry();
        long now = System.currentTimeMillis() / 1000;
        
        TextView textStatus = new TextView(this);
        if (vipExpiry > 0 && vipExpiry > now) {
            int remainingDays = (int) Math.ceil((vipExpiry - now) / 86400.0);
            textStatus.setText("当前状态: 有效（剩余 " + remainingDays + " 天）");
            textStatus.setTextColor(getResources().getColor(R.color.success, null));
        } else if (vipExpiry > 0) {
            textStatus.setText("当前状态: 已过期");
            textStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
        } else {
            textStatus.setText("当前状态: 未开通");
            textStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
        }
        container.addView(textStatus);
        
        TextView textHint = new TextView(this);
        textHint.setText("\n选择会员时长：");
        textHint.setTextColor(getResources().getColor(R.color.text_primary, null));
        container.addView(textHint);
        
        LinearLayout customDaysLayout = new LinearLayout(this);
        customDaysLayout.setOrientation(LinearLayout.HORIZONTAL);
        customDaysLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        customDaysLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        TextView customLabel = new TextView(this);
        customLabel.setText("自定义天数: ");
        customLabel.setTextColor(getResources().getColor(R.color.text_secondary, null));
        customDaysLayout.addView(customLabel);
        
        EditText editCustomDays = new EditText(this);
        editCustomDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        editCustomDays.setHint("输入天数");
        editCustomDays.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        editCustomDays.setTextColor(getResources().getColor(R.color.text_primary, null));
        editCustomDays.setHintTextColor(getResources().getColor(R.color.text_hint, null));
        editCustomDays.setBackgroundTintList(getResources().getColorStateList(R.color.accent, null));
        customDaysLayout.addView(editCustomDays);
        
        container.addView(customDaysLayout);
        
        TextView textHint2 = new TextView(this);
        textHint2.setText("\n快捷选项：");
        textHint2.setTextColor(getResources().getColor(R.color.text_primary, null));
        container.addView(textHint2);
        
        String[] options = {"7天", "30天", "90天", "取消会员"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);
        
        builder.setView(container);
        
        builder.setAdapter(adapter, (dialog, which) -> {
            long newExpiry = 0;
            switch (which) {
                case 0:
                    newExpiry = now + 7 * 86400;
                    break;
                case 1:
                    newExpiry = now + 30 * 86400;
                    break;
                case 2:
                    newExpiry = now + 90 * 86400;
                    break;
                case 3:
                    newExpiry = 0;
                    break;
            }
            setVip(player, newExpiry);
        });
        
        builder.setPositiveButton("确定", (dialog, which) -> {
            String daysStr = editCustomDays.getText().toString().trim();
            if (daysStr.isEmpty()) {
                Toast.makeText(this, "请输入天数", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int days = Integer.parseInt(daysStr);
                if (days <= 0) {
                    Toast.makeText(this, "天数必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
                long newExpiry = now + days * 86400L;
                setVip(player, newExpiry);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("关闭", null);
        builder.show();
    }
    
    private void setVip(Player player, long expiryTimestamp) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("id", player.getId());
            requestBody.put("monthly_card_expiry", expiryTimestamp);
        } catch (Exception e) {
            Toast.makeText(this, "构建请求失败", Toast.LENGTH_SHORT).show();
            return;
        }
        
        apiClient.updatePlayerVip(requestBody, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            player.setMonthlyCardExpiry(expiryTimestamp);
                            Toast.makeText(PlayerManageActivity.this, "会员设置成功", Toast.LENGTH_SHORT).show();
                            loadPlayers();
                        } else {
                            Toast.makeText(PlayerManageActivity.this, "设置失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerManageActivity.this, "设置失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerManageActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
