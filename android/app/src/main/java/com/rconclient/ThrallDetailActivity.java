package com.rconclient;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.rconclient.network.ApiClient;
import com.rconclient.utils.ItemConfigManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ThrallDetailActivity extends AppCompatActivity {

    public static final String EXTRA_THRALL_DATA = "thrall_data";
    public static final String EXTRA_THRALL_ID = "thrall_id";
    public static final String EXTRA_THRALL_NAME = "thrall_name";
    public static final String EXTRA_OWNER_NAME = "owner_name";

    private ApiClient apiClient;
    private SharedPreferences prefs;
    private String rconMode = "direct";
    private ItemConfigManager itemConfigManager;

    private ProgressBar progressLoading;
    private LinearLayout layoutContent;

    private TextView textThrallId;
    private TextView textThrallName;
    private TextView textLevel;
    private TextView textHealth;
    private TextView textPosition;
    private Button btnTeleport;
    private LinearLayout layoutTeleportSelect;
    private Spinner spinnerPlayers;
    private TextView textTeleportResult;

    private TextView textOwnerName;
    private TextView textGuildName;
    private TextView textGuildId;

    private TextView textFood;
    private TextView textStrength;
    private TextView textAgility;
    private TextView textVitality;
    private TextView textGrit;

    private LinearLayout layoutBackpack;
    private TextView textBackpackEmpty;

    private TextView textThrallType;
    private TextView textPerkType;
    private TextView textPerk1;
    private TextView textPerk2;
    private TextView textPerk3;

    private LinearLayout layoutEquipment;
    private TextView textEquipmentEmpty;

    private JSONObject thrallData;
    private String thrallPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thrall_detail);

        apiClient = ApiClient.getInstance(this);

        initViews();
        loadThrallData();
    }

    private void initViews() {
        progressLoading = findViewById(R.id.progress_loading);
        layoutContent = findViewById(R.id.layout_content);

        textThrallId = findViewById(R.id.text_thrall_id);
        textThrallName = findViewById(R.id.text_thrall_name);
        textLevel = findViewById(R.id.text_level);
        textHealth = findViewById(R.id.text_health);
        textPosition = findViewById(R.id.text_position);
        btnTeleport = findViewById(R.id.btn_teleport);
        layoutTeleportSelect = findViewById(R.id.layout_teleport_select);
        spinnerPlayers = findViewById(R.id.spinner_players);
        textTeleportResult = findViewById(R.id.text_teleport_result);

        textOwnerName = findViewById(R.id.text_owner_name);
        textGuildName = findViewById(R.id.text_guild_name);
        textGuildId = findViewById(R.id.text_guild_id);

        textFood = findViewById(R.id.text_food);
        textStrength = findViewById(R.id.text_strength);
        textAgility = findViewById(R.id.text_agility);
        textVitality = findViewById(R.id.text_vitality);
        textGrit = findViewById(R.id.text_grit);

        layoutBackpack = findViewById(R.id.layout_backpack);
        textBackpackEmpty = findViewById(R.id.text_backpack_empty);

        textThrallType = findViewById(R.id.text_thrall_type);
        textPerkType = findViewById(R.id.text_perk_type);
        textPerk1 = findViewById(R.id.text_perk_1);
        textPerk2 = findViewById(R.id.text_perk_2);
        textPerk3 = findViewById(R.id.text_perk_3);

        layoutEquipment = findViewById(R.id.layout_equipment);
        textEquipmentEmpty = findViewById(R.id.text_equipment_empty);

        btnTeleport.setOnClickListener(v -> loadOnlinePlayers());
        
        apiClient = ApiClient.getInstance(this);
        prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE);
        rconMode = prefs.getString("rcon_mode", "direct");
        itemConfigManager = ItemConfigManager.getInstance(this);
        itemConfigManager.updateServerUrl(prefs.getString("base_url", ""));
    }

    private void loadThrallData() {
        String thrallDataStr = getIntent().getStringExtra(EXTRA_THRALL_DATA);

        if (thrallDataStr != null) {
            try {
                thrallData = new JSONObject(thrallDataStr);
                if (itemConfigManager.isLoaded()) {
                    displayThrallData(thrallData);
                } else {
                    progressLoading.setVisibility(View.VISIBLE);
                    itemConfigManager.loadConfig(new ItemConfigManager.Callback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> displayThrallData(thrallData));
                        }
                        
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(ThrallDetailActivity.this, 
                                    "物品图标加载失败: " + error, Toast.LENGTH_SHORT).show();
                                displayThrallData(thrallData);
                            });
                        }
                    });
                }
            } catch (Exception e) {
                Toast.makeText(this, "解析奴隶数据失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            progressLoading.setVisibility(View.GONE);
            Toast.makeText(this, "无奴隶数据", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayThrallData(JSONObject thrall) {
        try {
            String name = getThrallName(thrall);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("👤 " + name);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            textThrallId.setText(String.valueOf(thrall.optLong("thrall_id", 0)));
            textThrallName.setText(name);
            
            int level = thrall.optInt("level", 0);
            if (level < 1) level = 1;
            textLevel.setText(String.valueOf(level));
            
            textHealth.setText(String.valueOf(thrall.optInt("health", 0)));
            
            thrallPosition = thrall.optString("position", "未知");
            textPosition.setText(thrallPosition);

            textOwnerName.setText(getSafeString(thrall, "owner_char_name", "未知"));
            textGuildName.setText(getSafeString(thrall, "owner_guild_name", "无部落"));
            textGuildId.setText(getSafeString(thrall, "owner_guild_id", "无"));

            JSONObject stats = thrall.optJSONObject("stats");
            if (stats != null) {
                textFood.setText(stats.optInt("food", 0) + "%");
                textStrength.setText(String.valueOf(stats.optInt("strength", 0)));
                textAgility.setText(String.valueOf(stats.optInt("agility", 0)));
                textVitality.setText(String.valueOf(stats.optInt("vitality", 0)));
                textGrit.setText(String.valueOf(stats.optInt("grit", 0)));
            } else {
                textFood.setText("0%");
                textStrength.setText("0");
                textAgility.setText("0");
                textVitality.setText("0");
                textGrit.setText("0");
            }

            renderBackpack(thrall);
            renderPerks(thrall);
            renderEquipment(thrall);

            progressLoading.setVisibility(View.GONE);
            layoutContent.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Toast.makeText(this, "显示数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
    
    private String getSafeString(JSONObject obj, String key, String defaultValue) {
        String value = obj.optString(key, defaultValue);
        if (value == null || value.isEmpty() || "null".equals(value)) {
            return defaultValue;
        }
        return value;
    }

    private void renderBackpack(JSONObject thrall) {
        JSONArray backpack = null;
        
        JSONObject inventory = thrall.optJSONObject("inventory");
        if (inventory != null) {
            backpack = inventory.optJSONArray("backpack");
        }
        if (backpack == null) {
            backpack = thrall.optJSONArray("backpack");
        }
        
        if (backpack == null || backpack.length() == 0) {
            textBackpackEmpty.setVisibility(View.VISIBLE);
            return;
        }

        int validCount = 0;
        for (int i = 0; i < backpack.length(); i++) {
            JSONObject item = backpack.optJSONObject(i);
            if (item != null) {
                validCount++;
            }
        }

        if (validCount == 0) {
            textBackpackEmpty.setVisibility(View.VISIBLE);
            return;
        }

        textBackpackEmpty.setVisibility(View.GONE);
        layoutBackpack.removeAllViews();

        TextView titleView = new TextView(this);
        titleView.setText("🎒 背包物品 (" + validCount + ")");
        titleView.setTextColor(getResources().getColor(R.color.pink, null));
        titleView.setTextSize(14);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 8);
        layoutBackpack.addView(titleView);

        int columns = 5;
        int rows = (int) Math.ceil(validCount / (double) columns);

        LinearLayout centerWrapper = new LinearLayout(this);
        centerWrapper.setOrientation(LinearLayout.HORIZONTAL);
        centerWrapper.setGravity(android.view.Gravity.CENTER);
        centerWrapper.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(columns);
        gridLayout.setRowCount(rows);
        gridLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < validCount; i++) {
            JSONObject item = null;
            if (i < backpack.length()) {
                item = backpack.optJSONObject(i);
            }
            
            if (item != null) {
                View itemSlot = createItemSlot(item, i);
                gridLayout.addView(itemSlot);
            }
        }

        centerWrapper.addView(gridLayout);
        layoutBackpack.addView(centerWrapper);
    }

    private View createItemSlot(JSONObject item, int index) {
        LinearLayout slot = new LinearLayout(this);
        slot.setOrientation(LinearLayout.VERTICAL);
        slot.setBackgroundResource(R.drawable.bg_card);
        slot.setPadding(4, 4, 4, 4);
        slot.setGravity(android.view.Gravity.CENTER);

        int size = (int) (55 * getResources().getDisplayMetrics().density);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = size;
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        slot.setLayoutParams(params);

        if (item != null) {
            int templateId = item.optInt("item_id", 0);
            int quantity = item.optInt("quantity", 1);
            
            String itemName = itemConfigManager.getItemName(templateId);
            String iconPath = itemConfigManager.getIconPath(templateId);

            FrameLayout container = new FrameLayout(this);
            container.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            int iconSize = size - 16;
            
            if (iconPath != null && !iconPath.isEmpty()) {
                ImageView iconView = new ImageView(this);
                Glide.with(this)
                    .load(iconPath)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .override(iconSize, iconSize)
                    .centerCrop()
                    .into(iconView);
                FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconSize, iconSize);
                iconParams.gravity = android.view.Gravity.CENTER;
                iconView.setLayoutParams(iconParams);
                container.addView(iconView);
            } else if (itemConfigManager.isLoaded()) {
                TextView iconView = new TextView(this);
                iconView.setText("📦");
                iconView.setTextSize(24);
                iconView.setGravity(android.view.Gravity.CENTER);
                FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                iconParams.gravity = android.view.Gravity.CENTER;
                iconView.setLayoutParams(iconParams);
                container.addView(iconView);
            } else {
                TextView iconView = new TextView(this);
                iconView.setText("⏳");
                iconView.setTextSize(24);
                iconView.setGravity(android.view.Gravity.CENTER);
                FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                iconParams.gravity = android.view.Gravity.CENTER;
                iconView.setLayoutParams(iconParams);
                container.addView(iconView);
            }

            TextView qtyView = new TextView(this);
            qtyView.setText("x" + quantity);
            qtyView.setTextColor(getResources().getColor(R.color.white, null));
            qtyView.setTextSize(9);
            qtyView.setGravity(android.view.Gravity.CENTER);
            qtyView.setBackgroundColor(getResources().getColor(R.color.black, null));
            qtyView.setPadding(2, 1, 2, 1);
            
            FrameLayout.LayoutParams qtyParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            qtyParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
            qtyView.setLayoutParams(qtyParams);

            container.addView(qtyView);
            slot.addView(container);

            String displayName = itemName != null && !itemName.isEmpty() 
                ? itemName : "物品 #" + templateId;
            slot.setContentDescription(displayName);
            
            final String finalDisplayName = displayName;
            final int finalTemplateId = templateId;
            final int finalQuantity = quantity;
            slot.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("物品信息")
                    .setMessage("名称: " + finalDisplayName + "\n模板ID: " + finalTemplateId + "\n数量: " + finalQuantity)
                    .setPositiveButton("确定", null)
                    .show();
            });
        }

        return slot;
    }

    private void renderPerks(JSONObject thrall) {
        String thrallType = thrall.optString("thrall_type", "未知");
        if (thrallType != null && thrallType.length() > 0 && !"null".equals(thrallType)) {
            thrallType = thrallType.replaceAll("([A-Z])", " $1").replaceAll("\\s+", " ").trim();
        } else {
            thrallType = "未知";
        }
        textThrallType.setText(thrallType);

        JSONObject perks = thrall.optJSONObject("perks");
        if (perks != null) {
            textPerkType.setText(getSafeString(perks, "perk_type", "未知"));
            textPerk1.setText(getSafeString(perks, "perk_1", "无"));
            textPerk2.setText(getSafeString(perks, "perk_2", "无"));
            textPerk3.setText(getSafeString(perks, "perk_3", "无"));
        } else {
            textPerkType.setText("未知");
            textPerk1.setText("无");
            textPerk2.setText("无");
            textPerk3.setText("无");
        }
    }

    private void renderEquipment(JSONObject thrall) {
        JSONObject equipment = null;
        
        JSONObject inventory = thrall.optJSONObject("inventory");
        if (inventory != null) {
            equipment = inventory.optJSONObject("equipment");
        }
        if (equipment == null) {
            equipment = thrall.optJSONObject("equipment");
        }
        
        if (equipment == null) {
            textEquipmentEmpty.setVisibility(View.VISIBLE);
            return;
        }

        String[][] slots = {
            {"head", "头盔", "🪖"},
            {"body", "胸甲", "👕"},
            {"hands", "手套", "🧤"},
            {"legs", "护腿", "👖"},
            {"feet", "靴子", "👢"},
            {"main_hand", "主手", "⚔️"},
            {"off_hand", "副手", "🛡️"}
        };

        boolean hasEquipment = false;
        for (String[] slot : slots) {
            JSONObject item = equipment.optJSONObject(slot[0]);
            if (item != null && item.optInt("item_id", 0) > 0) {
                hasEquipment = true;
                break;
            }
        }

        if (!hasEquipment) {
            textEquipmentEmpty.setVisibility(View.VISIBLE);
            return;
        }

        textEquipmentEmpty.setVisibility(View.GONE);
        layoutEquipment.removeAllViews();

        for (String[] slot : slots) {
            JSONObject item = equipment.optJSONObject(slot[0]);
            View slotView = createEquipmentSlot(slot[0], slot[1], slot[2], item);
            layoutEquipment.addView(slotView);
        }
    }

    private View createEquipmentSlot(String key, String name, String emoji, JSONObject item) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setBackgroundResource(R.drawable.bg_card);
        container.setPadding(8, 8, 8, 8);
        container.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        container.setLayoutParams(params);

        FrameLayout iconContainer = new FrameLayout(this);
        int iconSize = (int) (60 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconContainer.setLayoutParams(iconParams);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setPadding(16, 0, 0, 0);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        infoLayout.setLayoutParams(infoParams);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextColor(getResources().getColor(R.color.pink, null));
        nameView.setTextSize(12);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);

        infoLayout.addView(nameView);

        if (item != null) {
            int templateId = item.optInt("item_id", 0);
            String itemName = itemConfigManager.getItemName(templateId);
            String iconPath = itemConfigManager.getIconPath(templateId);
            
            if (iconPath != null && !iconPath.isEmpty()) {
                ImageView iconView = new ImageView(this);
                Glide.with(this)
                    .load(iconPath)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .override(iconSize, iconSize)
                    .centerCrop()
                    .into(iconView);
                FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(iconSize, iconSize);
                imgParams.gravity = android.view.Gravity.CENTER;
                iconView.setLayoutParams(imgParams);
                iconContainer.addView(iconView);
            } else if (itemConfigManager.isLoaded()) {
                TextView iconView = new TextView(this);
                iconView.setText(emoji);
                iconView.setTextSize(28);
                iconView.setGravity(android.view.Gravity.CENTER);
                FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                textParams.gravity = android.view.Gravity.CENTER;
                iconView.setLayoutParams(textParams);
                iconContainer.addView(iconView);
            } else {
                TextView iconView = new TextView(this);
                iconView.setText("⏳");
                iconView.setTextSize(28);
                iconView.setGravity(android.view.Gravity.CENTER);
                FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                textParams.gravity = android.view.Gravity.CENTER;
                iconView.setLayoutParams(textParams);
                iconContainer.addView(iconView);
            }
            
            TextView itemView = new TextView(this);
            if (itemName != null && !itemName.isEmpty()) {
                itemView.setText(itemName);
            } else {
                itemView.setText("物品 #" + templateId);
            }
            itemView.setTextColor(getResources().getColor(R.color.text_primary, null));
            itemView.setTextSize(12);

            TextView idView = new TextView(this);
            idView.setText("模板ID: " + templateId);
            idView.setTextColor(getResources().getColor(R.color.text_secondary, null));
            idView.setTextSize(10);

            infoLayout.addView(itemView);
            infoLayout.addView(idView);
        }

        container.addView(iconContainer);
        container.addView(infoLayout);

        return container;
    }

    private void loadOnlinePlayers() {
        btnTeleport.setEnabled(false);
        btnTeleport.setText("⏳ 获取在线玩家...");

        apiClient.sendCommand("listplayers", rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            String listResponse = json.optString("response", "");
                            List<String> playerIdxList = new ArrayList<>();
                            List<String> playerNameList = new ArrayList<>();
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

                            if (playerIdxList.isEmpty()) {
                                btnTeleport.setText("❌ 当前无在线玩家");
                                btnTeleport.setEnabled(true);
                                return;
                            }

                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                ThrallDetailActivity.this,
                                android.R.layout.simple_spinner_item,
                                playerNameList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerPlayers.setAdapter(adapter);

                            final String[] selectedIdx = {null};
                            spinnerPlayers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    if (position > 0 && position <= playerIdxList.size()) {
                                        selectedIdx[0] = playerIdxList.get(position - 1);
                                        executeTeleport(selectedIdx[0]);
                                    }
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });

                            layoutTeleportSelect.setVisibility(View.VISIBLE);
                            btnTeleport.setText("📍 传送指定玩家到该处");
                            btnTeleport.setEnabled(true);

                        } else {
                            btnTeleport.setText("❌ 获取失败");
                            btnTeleport.setEnabled(true);
                        }
                    } catch (Exception e) {
                        btnTeleport.setText("❌ 解析失败");
                        btnTeleport.setEnabled(true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnTeleport.setText("❌ 网络错误");
                    btnTeleport.setEnabled(true);
                });
            }
        });
    }

    private void executeTeleport(String playerIdx) {
        if (thrallPosition == null || thrallPosition.equals("未知")) {
            textTeleportResult.setText("奴隶坐标未知");
            textTeleportResult.setTextColor(getResources().getColor(R.color.error, null));
            textTeleportResult.setVisibility(View.VISIBLE);
            return;
        }

        String normalizedPosition = thrallPosition.replace(",", " ").replaceAll("\\s+", " ").trim();
        String[] coords = normalizedPosition.split(" ");
        if (coords.length < 3) {
            textTeleportResult.setText("坐标格式错误");
            textTeleportResult.setTextColor(getResources().getColor(R.color.error, null));
            textTeleportResult.setVisibility(View.VISIBLE);
            return;
        }

        try {
            double x = Double.parseDouble(coords[0]);
            double y = Double.parseDouble(coords[1]);
            double z = Double.parseDouble(coords[2]);

            textTeleportResult.setText("⏳ 正在传送玩家...");
            textTeleportResult.setTextColor(getResources().getColor(R.color.info, null));
            textTeleportResult.setVisibility(View.VISIBLE);

            String command = "con " + playerIdx + " TeleportPlayer " + (int)x + " " + (int)y + " " + (int)z;
            
            apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success")) {
                                textTeleportResult.setText("✓ 传送成功！");
                                textTeleportResult.setTextColor(getResources().getColor(R.color.success, null));
                            } else {
                                textTeleportResult.setText("✗ 传送失败: " + json.optString("message"));
                                textTeleportResult.setTextColor(getResources().getColor(R.color.error, null));
                            }
                        } catch (Exception e) {
                            textTeleportResult.setText("✗ 传送失败");
                            textTeleportResult.setTextColor(getResources().getColor(R.color.error, null));
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        textTeleportResult.setText("✗ 网络错误: " + error);
                        textTeleportResult.setTextColor(getResources().getColor(R.color.error, null));
                    });
                }
            });

        } catch (NumberFormatException e) {
            textTeleportResult.setText("坐标解析失败");
            textTeleportResult.setTextColor(getResources().getColor(R.color.error, null));
            textTeleportResult.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
