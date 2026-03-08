package com.rconclient;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rconclient.network.ApiClient;
import com.rconclient.utils.ItemConfigManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayerItemsActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYER_NAME = "player_name";
    
    private TextView textPlayerName;
    private ProgressBar progressLoading;
    private ImageButton btnRefresh;
    private EditText editSearch;
    private RecyclerView recyclerBackpack;
    private LinearLayout layoutEquipment;
    private LinearLayout layoutQuickbar;
    
    private ApiClient apiClient;
    private SharedPreferences prefs;
    private String rconMode = "direct";
    private String playerName;
    
    private List<JSONObject> backpackItems = new ArrayList<>();
    private List<JSONObject> equipmentItems = new ArrayList<>();
    private List<JSONObject> quickbarItems = new ArrayList<>();
    private BackpackAdapter backpackAdapter;
    private ItemConfigManager itemConfigManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_items);
        
        playerName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
        if (playerName == null || playerName.isEmpty()) {
            finish();
            return;
        }
        
        apiClient = ApiClient.getInstance(this);
        prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE);
        rconMode = prefs.getString("rcon_mode", "direct");
        itemConfigManager = ItemConfigManager.getInstance(this);
        itemConfigManager.updateServerUrl(prefs.getString("base_url", ""));
        
        initViews();
        loadItemConfig();
    }
    
    private void loadItemConfig() {
        if (!itemConfigManager.isLoaded()) {
            itemConfigManager.loadConfig(new ItemConfigManager.Callback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        backpackAdapter.notifyDataSetChanged();
                        renderEquipment();
                        renderQuickbar();
                        loadInventory();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        android.util.Log.e("PlayerItemsActivity", "Failed to load item config: " + error);
                        loadInventory();
                    });
                }
            });
        } else {
            loadInventory();
        }
    }
    
    private void initViews() {
        textPlayerName = findViewById(R.id.text_player_name);
        progressLoading = findViewById(R.id.progress_loading);
        btnRefresh = findViewById(R.id.btn_refresh);
        editSearch = findViewById(R.id.edit_search);
        recyclerBackpack = findViewById(R.id.recycler_backpack);
        layoutEquipment = findViewById(R.id.layout_equipment);
        layoutQuickbar = findViewById(R.id.layout_quickbar);
        
        textPlayerName.setText(playerName);
        
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> loadInventory());
        
        recyclerBackpack.setLayoutManager(new GridLayoutManager(this, 5));
        backpackAdapter = new BackpackAdapter(this, 
            (item, slotIndex) -> showItemDetail(item, item.optInt("item_id", slotIndex), 0),
            null);
        recyclerBackpack.setAdapter(backpackAdapter);
        
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    
    private void loadInventory() {
        progressLoading.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);
        
        apiClient.getPlayerInventory(playerName, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                    try {
                        android.util.Log.d("PlayerItemsActivity", "Response: " + response);
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONObject inventory = json.optJSONObject("inventory");
                            if (inventory != null) {
                                parseInventory(inventory);
                            } else {
                                showError("暂无物品数据");
                            }
                        } else {
                            showError(json.optString("message", "获取物品数据失败"));
                        }
                    } catch (Exception e) {
                        android.util.Log.e("PlayerItemsActivity", "Error: " + e.getMessage(), e);
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
    
    private void parseInventory(JSONObject inventory) {
        backpackItems.clear();
        equipmentItems.clear();
        quickbarItems.clear();
        
        android.util.Log.d("PlayerItemsActivity", "parseInventory: " + inventory.toString());
        
        JSONObject backpackObj = inventory.optJSONObject("backpack");
        JSONObject equipmentObj = inventory.optJSONObject("equipment");
        JSONObject quickbarObj = inventory.optJSONObject("quickbar");
        
        if (backpackObj != null) {
            Iterator<String> keys = backpackObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    JSONObject item = backpackObj.getJSONObject(key);
                    android.util.Log.d("PlayerItemsActivity", "backpack item: key=" + key + ", item=" + item.toString());
                    backpackItems.add(item);
                } catch (Exception e) {
                    android.util.Log.e("PlayerItemsActivity", "Error parsing backpack item: " + e.getMessage());
                }
            }
        }
        
        if (equipmentObj != null) {
            Iterator<String> keys = equipmentObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    JSONObject item = equipmentObj.getJSONObject(key);
                    equipmentItems.add(item);
                } catch (Exception e) {
                    android.util.Log.e("PlayerItemsActivity", "Error parsing equipment item: " + e.getMessage());
                }
            }
        }
        
        if (quickbarObj != null) {
            Iterator<String> keys = quickbarObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    JSONObject item = quickbarObj.getJSONObject(key);
                    quickbarItems.add(item);
                } catch (Exception e) {
                    android.util.Log.e("PlayerItemsActivity", "Error parsing quickbar item: " + e.getMessage());
                }
            }
        }
        
        android.util.Log.d("PlayerItemsActivity", "Parsed counts - backpack: " + backpackItems.size() + 
            ", equipment: " + equipmentItems.size() + ", quickbar: " + quickbarItems.size());
        
        backpackAdapter.setItems(backpackItems);
        updateBackpackHeight();
        
        renderEquipment();
        renderQuickbar();
        
        if (backpackItems.isEmpty() && equipmentItems.isEmpty() && quickbarItems.isEmpty()) {
            Toast.makeText(this, "暂无物品数据，请确保桌面客户端正在运行", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateBackpackHeight() {
        int itemCount = backpackAdapter.getItemCount();
        int columns = 5;
        int rows = (int) Math.ceil((double) itemCount / columns);
        
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int outerPadding = (int) (12 * metrics.density);
        int containerPadding = (int) (8 * metrics.density);
        int availableWidth = metrics.widthPixels - (outerPadding * 2) - (containerPadding * 2);
        int spacing = (int) (4 * metrics.density);
        int slotSize = (availableWidth - spacing * 6) / 5;
        
        int totalHeight = rows * (slotSize + spacing * 2) + containerPadding * 2;
        
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) recyclerBackpack.getLayoutParams();
        params.height = totalHeight;
        recyclerBackpack.setLayoutParams(params);
        
        android.util.Log.d("PlayerItemsActivity", "updateBackpackHeight: itemCount=" + itemCount + ", rows=" + rows + ", height=" + totalHeight);
    }
    
    private void renderEquipment() {
        int[] slotIds = {R.id.slot_helmet, R.id.slot_chest, R.id.slot_gloves, R.id.slot_legs, R.id.slot_boots};
        int[] targetItemIds = {3, 4, 5, 6, 7};
        String[] defaultNames = {"头盔", "胸甲", "手套", "护腿", "靴子"};
        
        for (int i = 0; i < slotIds.length; i++) {
            LinearLayout slot = findViewById(slotIds[i]);
            slot.removeAllViews();
            
            JSONObject item = null;
            for (JSONObject eq : equipmentItems) {
                if (eq.optInt("item_id", -1) == targetItemIds[i]) {
                    item = eq;
                    break;
                }
            }
            
            if (item != null) {
                int templateId = item.optInt("template_id", item.optInt("item_id", 0));
                String iconPath = itemConfigManager.getIconPath(templateId);
                
                if (iconPath != null && !iconPath.isEmpty()) {
                    ImageView iconView = new ImageView(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    iconView.setLayoutParams(params);
                    iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    Glide.with(this).load(iconPath).into(iconView);
                    slot.addView(iconView);
                } else if (itemConfigManager.isLoaded()) {
                    TextView iconView = new TextView(this);
                    iconView.setText("🛡️");
                    iconView.setTextSize(20);
                    iconView.setGravity(Gravity.CENTER);
                    slot.addView(iconView);
                } else {
                    TextView iconView = new TextView(this);
                    iconView.setText("⏳");
                    iconView.setTextSize(20);
                    iconView.setGravity(Gravity.CENTER);
                    slot.addView(iconView);
                }
                
                int finalI = i;
                JSONObject finalItem = item;
                slot.setOnClickListener(v -> showItemDetail(finalItem, targetItemIds[finalI], 1));
            } else {
                TextView emptyView = new TextView(this);
                emptyView.setText(defaultNames[i]);
                emptyView.setTextColor(getResources().getColor(R.color.text_secondary, null));
                emptyView.setTextSize(9);
                emptyView.setGravity(Gravity.CENTER);
                slot.addView(emptyView);
            }
        }
    }
    
    private void renderQuickbar() {
        layoutQuickbar.removeAllViews();
        
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int outerPadding = (int) (12 * metrics.density);
        int containerPadding = (int) (4 * metrics.density);
        int availableWidth = metrics.widthPixels - (outerPadding * 2) - (containerPadding * 2);
        int slotSize = availableWidth / 8;
        
        for (int i = 0; i < 8; i++) {
            final int index = i;
            android.widget.FrameLayout slot = new android.widget.FrameLayout(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(slotSize, slotSize);
            params.setMargins(0, 0, 0, 0);
            params.gravity = Gravity.CENTER;
            slot.setLayoutParams(params);
            slot.setBackgroundResource(R.drawable.bg_item_slot);
            
            JSONObject item = null;
            for (JSONObject qb : quickbarItems) {
                if (qb.optInt("item_id", -1) == index) {
                    item = qb;
                    break;
                }
            }
            
            if (item != null) {
                int templateId = item.optInt("template_id", item.optInt("item_id", 0));
                String iconPath = itemConfigManager.getIconPath(templateId);
                
                if (iconPath != null && !iconPath.isEmpty()) {
                    ImageView iconView = new ImageView(this);
                    android.widget.FrameLayout.LayoutParams iconParams = new android.widget.FrameLayout.LayoutParams(
                        slotSize - 4, slotSize - 4);
                    iconParams.gravity = Gravity.CENTER;
                    iconView.setLayoutParams(iconParams);
                    iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    Glide.with(this).load(iconPath).into(iconView);
                    slot.addView(iconView);
                } else if (itemConfigManager.isLoaded()) {
                    TextView iconView = new TextView(this);
                    android.widget.FrameLayout.LayoutParams iconParams = new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT, 
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
                    iconParams.gravity = Gravity.CENTER;
                    iconView.setLayoutParams(iconParams);
                    iconView.setText("⚡");
                    iconView.setTextSize(18);
                    iconView.setGravity(Gravity.CENTER);
                    slot.addView(iconView);
                } else {
                    TextView iconView = new TextView(this);
                    android.widget.FrameLayout.LayoutParams iconParams = new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT, 
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
                    iconParams.gravity = Gravity.CENTER;
                    iconView.setLayoutParams(iconParams);
                    iconView.setText("⏳");
                    iconView.setTextSize(18);
                    iconView.setGravity(Gravity.CENTER);
                    slot.addView(iconView);
                }
                
                int quantity = item.optInt("quantity", 1);
                if (quantity > 1) {
                    TextView countView = new TextView(this);
                    android.widget.FrameLayout.LayoutParams countParams = new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, 
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
                    countParams.gravity = Gravity.BOTTOM | Gravity.END;
                    countParams.rightMargin = 2;
                    countParams.bottomMargin = 2;
                    countView.setLayoutParams(countParams);
                    countView.setText(String.valueOf(quantity));
                    countView.setTextColor(getResources().getColor(R.color.white, null));
                    countView.setTextSize(8);
                    countView.setBackgroundColor(0xCC000000);
                    countView.setPadding(2, 0, 2, 0);
                    countView.setMaxLines(1);
                    countView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    slot.addView(countView);
                }
                
                JSONObject finalItem = item;
                final int finalIndex = index;
                slot.setOnClickListener(v -> showItemDetail(finalItem, finalIndex, 2));
            }
            
            layoutQuickbar.addView(slot);
        }
    }
    
    private void showItemDetail(JSONObject item) {
        showItemDetail(item, -1, -1);
    }
    
    private void showItemDetail(JSONObject item, int positionId, int invType) {
        try {
            int templateId = item.optInt("template_id", item.optInt("item_id", 0));
            String configName = itemConfigManager.getItemName(templateId);
            String instanceName = item.optString("instance_name", "");
            String itemCategory = itemConfigManager.getItemCategory(templateId);
            
            String itemName = configName != null && !configName.isEmpty() ? configName : "物品详情";
            
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(48, 24, 48, 16);
            
            TextView detailText = new TextView(this);
            StringBuilder sb = new StringBuilder();
            
            if (configName != null && !configName.isEmpty()) {
                sb.append("名称: ").append(configName).append("\n");
            } else if (!instanceName.isEmpty()) {
                sb.append("名称: ").append(instanceName).append("\n");
            } else {
                sb.append("名称: 未知物品\n");
            }
            
            sb.append("模板ID: ").append(templateId).append("\n");
            sb.append("数量: ").append(item.optInt("quantity", 1)).append("\n");
            
            String invTypeStr = item.optString("inv_type", "");
            String typeStr = "背包";
            if ("1".equals(invTypeStr)) typeStr = "装备栏";
            else if ("2".equals(invTypeStr)) typeStr = "快捷栏";
            sb.append("位置: ").append(typeStr);
            
            detailText.setText(sb.toString());
            detailText.setTextSize(14);
            detailText.setPadding(0, 0, 0, 24);
            layout.addView(detailText);
            
            java.util.List<String> options = new java.util.ArrayList<>();
            final java.util.List<String> actions = new java.util.ArrayList<>();
            
            if ("Weapon".equals(itemCategory) || "Tools".equals(itemCategory)) {
                options.add("🗡️ 修改轻击伤害");
                actions.add("lightDamage");
                options.add("🗡️ 修改重击伤害");
                actions.add("heavyDamage");
                options.add("🎯 修改护甲穿透");
                actions.add("armorPen");
                options.add("💫 修改眩晕");
                actions.add("concussive");
            }
            
            if ("Armor".equals(itemCategory)) {
                options.add("🛡️ 修改护甲值");
                actions.add("armourValue");
            }
            
            if ("Weapon".equals(itemCategory) || "Armor".equals(itemCategory) || "Tools".equals(itemCategory)) {
                options.add("🔧 修改最大耐久");
                actions.add("maxDurability");
            }
            
            options.add("📦 允许堆叠数量");
            actions.add("maxStackSize");
            
            if (templateId != 0) {
                options.add("✨ 生成该物品");
                actions.add("spawnItem");
            }
            
            for (int i = 0; i < options.size(); i++) {
                TextView optionBtn = new TextView(this);
                optionBtn.setText(options.get(i));
                optionBtn.setTextSize(14);
                optionBtn.setTextColor(0xFFFF9800);
                optionBtn.setPadding(16, 16, 16, 16);
                
                android.util.TypedValue outValue = new android.util.TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                optionBtn.setBackgroundResource(outValue.resourceId);
                
                final String action = actions.get(i);
                final int finalPositionId = positionId >= 0 ? positionId : item.optInt("item_id", 0);
                final int finalInvType = invType >= 0 ? invType : getInvTypeFromItem(item);
                final JSONObject finalItem = item;
                
                optionBtn.setOnClickListener(v -> {
                    AlertDialog dialogToDismiss = (AlertDialog) layout.getTag();
                    if (dialogToDismiss != null) {
                        dialogToDismiss.dismiss();
                    }
                    openDamageSettingModal(finalItem, finalPositionId, finalInvType, action);
                });
                
                layout.addView(optionBtn);
            }
            
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(itemName)
                .setView(layout)
                .setNegativeButton("关闭", null)
                .create();
            layout.setTag(dialog);
            dialog.show();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                .setTitle("错误")
                .setMessage("显示详情失败")
                .setPositiveButton("确定", null)
                .show();
        }
    }
    
    private int getInvTypeFromItem(JSONObject item) {
        String invTypeStr = item.optString("inv_type", "");
        if ("1".equals(invTypeStr)) return 1;
        if ("2".equals(invTypeStr)) return 2;
        return 0;
    }
    
    private void openDamageSettingModal(JSONObject item, int positionId, int invType, String actionType) {
        int templateId = item.optInt("template_id", item.optInt("item_id", 0));
        String itemName = itemConfigManager.getItemName(templateId);
        if (itemName == null || itemName.isEmpty()) {
            itemName = "未知物品";
        }
        
        if ("spawnItem".equals(actionType)) {
            showSpawnItemDialog(templateId, itemName);
            return;
        }
        
        String title;
        String hint;
        int maxValue = 0;
        boolean isArmorPen = "armorPen".equals(actionType);
        
        switch (actionType) {
            case "lightDamage":
                title = "设置轻击伤害";
                hint = "伤害数值";
                break;
            case "heavyDamage":
                title = "设置重击伤害";
                hint = "伤害数值";
                break;
            case "armorPen":
                title = "设置护甲穿透";
                hint = "0-100 (百分比)";
                maxValue = 100;
                break;
            case "concussive":
                title = "设置眩晕";
                hint = "眩晕数值";
                break;
            case "armourValue":
                title = "设置护甲值";
                hint = "护甲数值";
                break;
            case "maxDurability":
                title = "设置最大耐久";
                hint = "耐久数值";
                break;
            case "maxStackSize":
                title = "设置允许堆叠数量";
                hint = "最大允许堆叠数量";
                break;
            default:
                return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);
        
        TextView infoText = new TextView(this);
        infoText.setText("物品: " + itemName + "\n玩家: " + playerName);
        infoText.setPadding(0, 0, 0, 20);
        layout.addView(infoText);
        
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint(hint);
        input.setText("0");
        layout.addView(input);
        
        builder.setView(layout);
        
        final boolean finalIsArmorPen = isArmorPen;
        final int finalMaxValue = maxValue;
        builder.setPositiveButton("保存", (dialog, which) -> {
            String valueStr = input.getText().toString().trim();
            if (valueStr.isEmpty()) {
                Toast.makeText(this, "请输入数值", Toast.LENGTH_SHORT).show();
                return;
            }
            
            double value;
            try {
                value = Double.parseDouble(valueStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效数值", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (finalIsArmorPen && (value < 0 || value > 100)) {
                Toast.makeText(this, "护甲穿透范围应为 0-100", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (value < 0) {
                Toast.makeText(this, "数值不能为负数", Toast.LENGTH_SHORT).show();
                return;
            }
            
            saveDamageSetting(positionId, invType, actionType, value, finalIsArmorPen);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void saveDamageSetting(int positionId, int invType, String actionType, double value, boolean isArmorPen) {
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
                                String command;
                                int damageStatId = "lightDamage".equals(actionType) ? 6 : 7;
                                
                                if ("concussive".equals(actionType)) {
                                    String command1 = "con " + playerIdx + " SetInventoryItemStat " + positionId + " DamageConcussiveLightOnHit " + (int)value + " " + invType;
                                    String command2 = "con " + playerIdx + " SetInventoryItemStat " + positionId + " DamageConcussiveHeavyOnHit " + (int)value + " " + invType;
                                    executeCommand(command1);
                                    executeCommand(command2);
                                    return;
                                }
                                
                                if ("armourValue".equals(actionType)) {
                                    command = "con " + playerIdx + " SetInventoryItemStat " + positionId + " ArmourValue " + (int)value + " " + invType;
                                } else if ("maxDurability".equals(actionType)) {
                                    command = "con " + playerIdx + " SetInventoryItemStat " + positionId + " MaxDurability " + (int)value + " " + invType;
                                } else if ("maxStackSize".equals(actionType)) {
                                    command = "con " + playerIdx + " SetInventoryItemStat " + positionId + " MaxStackSize " + (int)value + " " + invType;
                                } else if (isArmorPen) {
                                    double armorPenValue = value / 100.0;
                                    command = "con " + playerIdx + " SetInventoryItemStat " + positionId + " ArmorPen " + armorPenValue + " " + invType;
                                } else {
                                    command = "con " + playerIdx + " SetInventoryItemIntStat " + positionId + " " + damageStatId + " " + (int)value + " " + invType;
                                }
                                executeCommand(command);
                            } else {
                                Toast.makeText(PlayerItemsActivity.this, "玩家不在线或未找到", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerItemsActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerItemsActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void showSpawnItemDialog(int templateId, String itemName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("生成物品");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);
        
        TextView infoText = new TextView(this);
        infoText.setText("物品: " + itemName + "\n模板ID: " + templateId + "\n玩家: " + playerName);
        infoText.setPadding(0, 0, 0, 20);
        layout.addView(infoText);
        
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("生成数量");
        input.setText("1");
        layout.addView(input);
        
        builder.setView(layout);
        
        builder.setPositiveButton("生成", (dialog, which) -> {
            String valueStr = input.getText().toString().trim();
            if (valueStr.isEmpty()) {
                Toast.makeText(this, "请输入数量", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int amount;
            try {
                amount = Integer.parseInt(valueStr);
                if (amount < 1) {
                    Toast.makeText(this, "数量至少为1", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效数量", Toast.LENGTH_SHORT).show();
                return;
            }
            
            spawnItemToPlayer(templateId, amount);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void spawnItemToPlayer(int templateId, int amount) {
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
                                String command = "con " + playerIdx + " spawnitem " + templateId + " " + amount;
                                executeCommand(command);
                            } else {
                                Toast.makeText(PlayerItemsActivity.this, "玩家不在线或未找到", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerItemsActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerItemsActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void executeCommand(String command) {
        apiClient.sendCommand(command, rconMode, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            Toast.makeText(PlayerItemsActivity.this, "操作成功", Toast.LENGTH_SHORT).show();
                            loadInventory();
                        } else {
                            Toast.makeText(PlayerItemsActivity.this, "操作失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(PlayerItemsActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerItemsActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
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
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
