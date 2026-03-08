package com.rconclient;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rconclient.utils.ItemConfigManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BackpackAdapter extends RecyclerView.Adapter<BackpackAdapter.SlotViewHolder> {
    
    private Context context;
    private int maxSlots = 50;
    private Map<Integer, JSONObject> itemMap = new HashMap<>();
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private ItemConfigManager itemConfigManager;
    private int slotSize;
    
    public interface OnItemClickListener {
        void onItemClick(JSONObject item, int slotIndex);
    }
    
    public interface OnItemLongClickListener {
        void onItemLongClick(JSONObject item, int slotIndex, int itemId);
    }
    
    public BackpackAdapter(Context context, OnItemClickListener clickListener, 
                           OnItemLongClickListener longClickListener) {
        this.context = context;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.itemConfigManager = ItemConfigManager.getInstance(context);
        
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        int outerPadding = (int) (12 * metrics.density);
        int containerPadding = (int) (8 * metrics.density);
        int availableWidth = metrics.widthPixels - (outerPadding * 2) - (containerPadding * 2);
        int spacing = (int) (4 * metrics.density);
        this.slotSize = (availableWidth - spacing * 6) / 5;
    }
    
    public void setItems(java.util.List<JSONObject> items) {
        itemMap.clear();
        maxSlots = 50;
        
        for (JSONObject item : items) {
            int itemId = item.optInt("item_id", -1);
            if (itemId >= 0) {
                itemMap.put(itemId, item);
                if (itemId >= maxSlots) {
                    maxSlots = itemId + 1;
                }
            }
        }
        
        if (itemMap.size() > 0 && maxSlots < itemMap.size()) {
            maxSlots = Math.max(maxSlots, itemMap.size());
        }
        
        android.util.Log.d("BackpackAdapter", "setItems: items=" + items.size() + ", maxSlots=" + maxSlots + ", itemMap.size=" + itemMap.size());
        
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemCount() {
        return maxSlots;
    }
    
    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout slot = new FrameLayout(context);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(slotSize, slotSize);
        int margin = (int) (2 * context.getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        slot.setLayoutParams(params);
        slot.setBackgroundResource(R.drawable.bg_item_slot);
        return new SlotViewHolder(slot);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        FrameLayout slot = holder.slot;
        slot.removeAllViews();
        slot.setClipChildren(false);
        
        JSONObject item = itemMap.get(position);
        
        if (item != null) {
            int templateId = item.optInt("template_id", item.optInt("item_id", 0));
            int quantity = item.optInt("quantity", 1);
            
            String iconPath = itemConfigManager.getIconPath(templateId);
            
            if (iconPath != null && !iconPath.isEmpty()) {
                ImageView iconView = new ImageView(context);
                FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                    slotSize - 12, slotSize - 12);
                iconParams.gravity = Gravity.CENTER;
                iconView.setLayoutParams(iconParams);
                iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                Glide.with(context).load(iconPath).into(iconView);
                slot.addView(iconView);
            } else if (itemConfigManager.isLoaded()) {
                TextView iconView = new TextView(context);
                FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                iconParams.gravity = Gravity.CENTER;
                iconView.setLayoutParams(iconParams);
                iconView.setText(getItemEmoji(templateId));
                iconView.setTextSize(20);
                iconView.setGravity(Gravity.CENTER);
                slot.addView(iconView);
            } else {
                TextView iconView = new TextView(context);
                FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                iconParams.gravity = Gravity.CENTER;
                iconView.setLayoutParams(iconParams);
                iconView.setText("⏳");
                iconView.setTextSize(20);
                iconView.setGravity(Gravity.CENTER);
                slot.addView(iconView);
            }
            
            if (quantity > 1) {
                TextView countView = new TextView(context);
                FrameLayout.LayoutParams countParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                countParams.gravity = Gravity.BOTTOM | Gravity.END;
                countParams.rightMargin = 4;
                countParams.bottomMargin = 4;
                countView.setLayoutParams(countParams);
                countView.setText(String.valueOf(quantity));
                countView.setTextColor(context.getResources().getColor(R.color.white, null));
                countView.setTextSize(8);
                countView.setBackgroundColor(0xCC000000);
                countView.setPadding(2, 0, 2, 0);
                countView.setMaxLines(1);
                countView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                slot.addView(countView);
            }
            
            String itemName = itemConfigManager.getItemName(templateId);
            slot.setContentDescription(itemName != null ? itemName : "物品");
            
            slot.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item, position);
                }
            });
        } else {
            slot.setContentDescription("空格子 " + position);
            slot.setOnClickListener(null);
        }
    }
    
    private String getItemEmoji(int templateId) {
        if (templateId >= 52000 && templateId <= 53000) {
            return "⚔️";
        } else if (templateId >= 53000 && templateId <= 54000) {
            return "🛡️";
        } else if (templateId >= 50000 && templateId <= 52000) {
            return "🍖";
        } else if (templateId >= 100000) {
            return "🏗️";
        } else {
            return "📦";
        }
    }
    
    static class SlotViewHolder extends RecyclerView.ViewHolder {
        FrameLayout slot;
        
        public SlotViewHolder(@NonNull FrameLayout itemView) {
            super(itemView);
            slot = itemView;
        }
    }
}
