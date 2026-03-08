package com.rconclient;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rconclient.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

public class PlayerInfoActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYER_NAME = "player_name";
    
    private TextView textPlayerName;
    private ProgressBar progressLoading;
    private ImageButton btnRefresh;
    
    private TextView textGuildId;
    private TextView textGuildName;
    
    private LinearLayout layoutInventory;
    private TextView textInventoryEmpty;
    
    private LinearLayout layoutThralls;
    private TextView textThrallsEmpty;
    private TextView textThrallsTitle;
    
    private TextView textStrength;
    private TextView textAgility;
    private TextView textVitality;
    private TextView textGrit;
    private TextView textAuthority;
    private TextView textExpertise;
    private TextView textAttributePoints;
    private Button btnAddAttributePoints;
    
    private Button btnEditStrength;
    private Button btnEditAgility;
    private Button btnEditVitality;
    private Button btnEditGrit;
    private Button btnEditAuthority;
    private Button btnEditExpertise;
    
    private ApiClient apiClient;
    private SharedPreferences prefs;
    private String rconMode = "direct";
    private String playerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_info);
        
        playerName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
        if (playerName == null || playerName.isEmpty()) {
            finish();
            return;
        }
        
        apiClient = ApiClient.getInstance(this);
        prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE);
        rconMode = prefs.getString("rcon_mode", "direct");
        
        initViews();
        loadPlayerInfo();
    }
    
    private void initViews() {
        textPlayerName = findViewById(R.id.text_player_name);
        progressLoading = findViewById(R.id.progress_loading);
        btnRefresh = findViewById(R.id.btn_refresh);
        
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        textGuildId = findViewById(R.id.text_guild_id);
        textGuildName = findViewById(R.id.text_guild_name);
        
        layoutInventory = findViewById(R.id.layout_inventory);
        textInventoryEmpty = findViewById(R.id.text_inventory_empty);
        
        layoutThralls = findViewById(R.id.layout_thralls);
        textThrallsEmpty = findViewById(R.id.text_thralls_empty);
        textThrallsTitle = findViewById(R.id.text_thralls_title);
        
        textStrength = findViewById(R.id.text_strength);
        textAgility = findViewById(R.id.text_agility);
        textVitality = findViewById(R.id.text_vitality);
        textGrit = findViewById(R.id.text_grit);
        textAuthority = findViewById(R.id.text_authority);
        textExpertise = findViewById(R.id.text_expertise);
        textAttributePoints = findViewById(R.id.text_attribute_points);
        btnAddAttributePoints = findViewById(R.id.btn_add_attribute_points);
        
        btnEditStrength = findViewById(R.id.btn_edit_strength);
        btnEditAgility = findViewById(R.id.btn_edit_agility);
        btnEditVitality = findViewById(R.id.btn_edit_vitality);
        btnEditGrit = findViewById(R.id.btn_edit_grit);
        btnEditAuthority = findViewById(R.id.btn_edit_authority);
        btnEditExpertise = findViewById(R.id.btn_edit_expertise);
        
        textPlayerName.setText(playerName);
        
        btnRefresh.setOnClickListener(v -> loadPlayerInfo());
        
        btnAddAttributePoints.setOnClickListener(v -> showAddAttributePointsDialog());
        
        btnEditStrength.setOnClickListener(v -> showEditStatDialog("AttributeMight", "力量", textStrength.getText().toString()));
        btnEditAgility.setOnClickListener(v -> showEditStatDialog("AttributeAthleticism", "灵活", textAgility.getText().toString()));
        btnEditVitality.setOnClickListener(v -> showEditStatDialog("AttributeHealth", "活力", textVitality.getText().toString()));
        btnEditGrit.setOnClickListener(v -> showEditStatDialog("Attributestamina", "毅力", textGrit.getText().toString()));
        btnEditAuthority.setOnClickListener(v -> showEditStatDialog("AttributeLeadership", "权威", textAuthority.getText().toString()));
        btnEditExpertise.setOnClickListener(v -> showEditStatDialog("AttributeEncumbrance", "专长", textExpertise.getText().toString()));
    }
    
    private void loadPlayerInfo() {
        progressLoading.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);
        
        apiClient.getPlayerInventory(playerName, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                    try {
                        android.util.Log.d("PlayerInfoActivity", "API Response: " + response);
                        
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONObject playerInfo = json.optJSONObject("player_info");
                            JSONObject inventory = json.optJSONObject("inventory");
                            JSONArray thralls = json.optJSONArray("thralls");
                            
                            if (playerInfo != null) {
                                renderPlayerInfo(playerInfo, inventory, thralls);
                            } else {
                                textGuildId.setText("未加入");
                                textGuildName.setText("未加入部落");
                                showError("未找到玩家详细信息");
                            }
                            
                            renderInventory(inventory);
                            renderThralls(thralls);
                        } else {
                            showError(json.optString("message", "获取玩家信息失败"));
                        }
                    } catch (Exception e) {
                        android.util.Log.e("PlayerInfoActivity", "解析错误: " + e.getMessage(), e);
                        showError("解析数据失败: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                    showError("网络错误: " + error);
                });
            }
        });
    }
    
    private void renderPlayerInfo(JSONObject playerInfo, JSONObject inventory, JSONArray thralls) {
        try {
            android.util.Log.d("PlayerInfoActivity", "playerInfo: " + playerInfo.toString());
            android.util.Log.d("PlayerInfoActivity", "inventory: " + (inventory != null ? inventory.toString() : "null"));
            android.util.Log.d("PlayerInfoActivity", "thralls: " + (thralls != null ? thralls.toString() : "null"));
            
            String guildId = playerInfo.optString("guild_id", "");
            if (guildId == null || guildId.isEmpty() || "null".equalsIgnoreCase(guildId)) {
                textGuildId.setText("未加入");
            } else {
                textGuildId.setText(guildId);
            }
            
            String guildName = playerInfo.optString("guild_name", "");
            if (guildName == null || guildName.isEmpty() || "null".equalsIgnoreCase(guildName)) {
                textGuildName.setText("未加入部落");
            } else {
                textGuildName.setText(guildName);
            }
            
            JSONObject stats = playerInfo.optJSONObject("stats");
            if (stats != null) {
                textStrength.setText(String.valueOf(stats.optInt("strength", 0)));
                textAgility.setText(String.valueOf(stats.optInt("agility", 0)));
                textVitality.setText(String.valueOf(stats.optInt("vitality", 0)));
                textGrit.setText(String.valueOf(stats.optInt("grit", 0)));
                textAuthority.setText(String.valueOf(stats.optInt("authority", 0)));
                textExpertise.setText(String.valueOf(stats.optInt("expertise", 0)));
            }
            
            int attrPoints = playerInfo.optInt("attribute_points", 0);
            if (stats != null) {
                attrPoints = stats.optInt("attribute_points", attrPoints);
            }
            textAttributePoints.setText(String.valueOf(attrPoints));
            
        } catch (Exception e) {
            showError("渲染信息失败: " + e.getMessage());
        }
    }
    
    private void renderInventory(JSONObject inventory) {
        if (inventory == null) {
            textInventoryEmpty.setVisibility(View.VISIBLE);
            return;
        }
        
        try {
            android.util.Log.d("PlayerInfoActivity", "inventory data: " + inventory.toString());
            
            JSONObject backpack = inventory.optJSONObject("backpack");
            JSONObject equipment = inventory.optJSONObject("equipment");
            JSONObject quickbar = inventory.optJSONObject("quickbar");
            
            int backpackCount = 0;
            int equipmentCount = 0;
            int quickbarCount = 0;
            
            if (backpack != null) {
                java.util.Iterator<String> keys = backpack.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject item = backpack.optJSONObject(key);
                    if (item != null && item.optInt("template_id", 0) > 0) {
                        backpackCount++;
                    }
                }
            }
            
            if (equipment != null) {
                java.util.Iterator<String> keys = equipment.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject item = equipment.optJSONObject(key);
                    if (item != null && item.optInt("template_id", 0) > 0) {
                        equipmentCount++;
                    }
                }
                equipmentCount = Math.max(0, equipmentCount - 2);
            }
            
            if (quickbar != null) {
                java.util.Iterator<String> keys = quickbar.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject item = quickbar.optJSONObject(key);
                    if (item != null && item.optInt("template_id", 0) > 0) {
                        quickbarCount++;
                    }
                }
            }
            
            textInventoryEmpty.setVisibility(View.GONE);
            layoutInventory.removeAllViews();
            
            LinearLayout statsRow = new LinearLayout(this);
            statsRow.setOrientation(LinearLayout.HORIZONTAL);
            statsRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            statsRow.setWeightSum(3);
            
            addInventoryStat(statsRow, "背包", backpackCount, R.color.accent);
            addInventoryStat(statsRow, "装备", equipmentCount, R.color.warning);
            addInventoryStat(statsRow, "快捷栏", quickbarCount, R.color.success);
            
            layoutInventory.addView(statsRow);
            
        } catch (Exception e) {
            android.util.Log.e("PlayerInfoActivity", "renderInventory error: " + e.getMessage(), e);
            textInventoryEmpty.setVisibility(View.VISIBLE);
        }
    }
    
    private void addInventoryStat(LinearLayout parent, String label, int count, int colorRes) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(4, 8, 4, 8);
        container.setLayoutParams(params);
        container.setPadding(10, 10, 10, 10);
        container.setBackgroundResource(R.drawable.bg_card);
        
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        labelView.setTextSize(12);
        labelView.setGravity(android.view.Gravity.CENTER);
        
        TextView countView = new TextView(this);
        countView.setText(String.valueOf(count));
        countView.setTextColor(getResources().getColor(colorRes, null));
        countView.setTextSize(18);
        countView.setTypeface(null, android.graphics.Typeface.BOLD);
        countView.setGravity(android.view.Gravity.CENTER);
        
        container.addView(labelView);
        container.addView(countView);
        parent.addView(container);
    }
    
    private void renderThralls(JSONArray thralls) {
        android.util.Log.d("PlayerInfoActivity", "thralls data: " + (thralls != null ? thralls.toString() : "null"));
        
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
                String name = thrall.optString("thrall_name", thrall.optString("name", "未知奴隶"));
                int level = thrall.optInt("level", 1);
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
                moreView.setText("... 还有 " + (thralls.length() - 5) + " 个奴隶");
                moreView.setTextColor(getResources().getColor(R.color.text_secondary, null));
                moreView.setTextSize(10);
                moreView.setPadding(0, 8, 0, 0);
                layoutThralls.addView(moreView);
            }
            
        } catch (Exception e) {
            android.util.Log.e("PlayerInfoActivity", "renderThralls error: " + e.getMessage(), e);
            textThrallsEmpty.setVisibility(View.VISIBLE);
        }
    }
    
    private void showEditStatDialog(String statCode, String statName, String currentValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修改" + statName);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 10);
        
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(currentValue);
        container.addView(input);
        
        final ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = android.view.Gravity.CENTER;
        progressParams.topMargin = 20;
        progressBar.setLayoutParams(progressParams);
        container.addView(progressBar);
        
        final TextView statusText = new TextView(this);
        statusText.setVisibility(View.GONE);
        statusText.setTextColor(getResources().getColor(R.color.text_secondary, null));
        statusText.setTextSize(12);
        statusText.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = 10;
        statusText.setLayoutParams(statusParams);
        container.addView(statusText);
        
        builder.setView(container);
        
        builder.setPositiveButton("确定", null);
        builder.setNegativeButton("取消", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                input.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText("正在修改...");
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                
                modifyStatWithDialog(statCode, statName, value, dialog, statusText);
            }
        });
        
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> dialog.dismiss());
    }
    
    private void modifyStatWithDialog(String statCode, String statName, String value, 
            AlertDialog dialog, TextView statusText) {
        apiClient.sendCommand("listplayers", rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            String listResponse = json.optString("response", "");
                            String playerIdx = findPlayerIdx(listResponse, playerName);
                            
                            if (playerIdx != null) {
                                statusText.setText("正在执行修改...");
                                String command = "con " + playerIdx + " setstat " + statCode + " " + value;
                                executeModifyStatWithDialog(command, statName, value, dialog, statusText);
                            } else {
                                dialog.dismiss();
                                Toast.makeText(PlayerInfoActivity.this, "玩家不在线或未找到", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            dialog.dismiss();
                            Toast.makeText(PlayerInfoActivity.this, "获取玩家列表失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        dialog.dismiss();
                        Toast.makeText(PlayerInfoActivity.this, "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(PlayerInfoActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void executeModifyStatWithDialog(String command, String statName, String value,
            AlertDialog dialog, TextView statusText) {
        apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            dialog.dismiss();
                            Toast.makeText(PlayerInfoActivity.this, statName + "已修改为 " + value, Toast.LENGTH_SHORT).show();
                            loadPlayerInfo();
                        } else {
                            dialog.dismiss();
                            Toast.makeText(PlayerInfoActivity.this, "修改失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        dialog.dismiss();
                        Toast.makeText(PlayerInfoActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(PlayerInfoActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showAddAttributePointsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("增加未分配属性点");
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 10);
        
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("输入要增加的点数");
        container.addView(input);
        
        final ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = android.view.Gravity.CENTER;
        progressParams.topMargin = 20;
        progressBar.setLayoutParams(progressParams);
        container.addView(progressBar);
        
        final TextView statusText = new TextView(this);
        statusText.setVisibility(View.GONE);
        statusText.setTextColor(getResources().getColor(R.color.text_secondary, null));
        statusText.setTextSize(12);
        statusText.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = 10;
        statusText.setLayoutParams(statusParams);
        container.addView(statusText);
        
        builder.setView(container);
        
        builder.setPositiveButton("确定", null);
        builder.setNegativeButton("取消", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                input.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText("正在增加...");
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                
                addAttributePointsWithDialog(value, dialog, statusText);
            }
        });
        
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> dialog.dismiss());
    }
    
    private void addAttributePointsWithDialog(String points, AlertDialog dialog, TextView statusText) {
        apiClient.sendCommand("listplayers", rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            String listResponse = json.optString("response", "");
                            String playerIdx = findPlayerIdx(listResponse, playerName);
                            
                            if (playerIdx != null) {
                                statusText.setText("正在执行增加...");
                                String command = "con " + playerIdx + " AddUndistributedAttributePoints " + points;
                                executeAddAttributePointsWithDialog(command, points, dialog);
                            } else {
                                dialog.dismiss();
                                Toast.makeText(PlayerInfoActivity.this, "玩家不在线或未找到", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            dialog.dismiss();
                            Toast.makeText(PlayerInfoActivity.this, "获取玩家列表失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        dialog.dismiss();
                        Toast.makeText(PlayerInfoActivity.this, "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(PlayerInfoActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void executeAddAttributePointsWithDialog(String command, String points, AlertDialog dialog) {
        apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            dialog.dismiss();
                            Toast.makeText(PlayerInfoActivity.this, "已增加 " + points + " 属性点", Toast.LENGTH_SHORT).show();
                            int current = Integer.parseInt(textAttributePoints.getText().toString());
                            textAttributePoints.setText(String.valueOf(current + Integer.parseInt(points)));
                        } else {
                            dialog.dismiss();
                            Toast.makeText(PlayerInfoActivity.this, "增加失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        dialog.dismiss();
                        Toast.makeText(PlayerInfoActivity.this, "增加失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(PlayerInfoActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show();
                });
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
    
    private void executeAddAttributePoints(String command, String points) {
        apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            Toast.makeText(PlayerInfoActivity.this, "已增加 " + points + " 属性点", Toast.LENGTH_SHORT).show();
                            int current = Integer.parseInt(textAttributePoints.getText().toString());
                            textAttributePoints.setText(String.valueOf(current + Integer.parseInt(points)));
                        } else {
                            Toast.makeText(PlayerInfoActivity.this, "增加失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerInfoActivity.this, "增加失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerInfoActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
