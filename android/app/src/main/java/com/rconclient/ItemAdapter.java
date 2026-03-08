package com.rconclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rconclient.utils.ItemConfigManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    
    private Context context;
    private List<JSONObject> allItems;
    private List<JSONObject> displayItems;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private ItemConfigManager itemConfigManager;
    private String currentFilter = "";
    
    public interface OnItemClickListener {
        void onItemClick(JSONObject item);
    }
    
    public interface OnItemLongClickListener {
        void onItemLongClick(JSONObject item, int positionId, int invType);
    }
    
    public ItemAdapter(Context context, List<JSONObject> items, 
                       OnItemClickListener clickListener, 
                       OnItemLongClickListener longClickListener) {
        this.context = context;
        this.allItems = new ArrayList<>(items);
        this.displayItems = items;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.itemConfigManager = ItemConfigManager.getInstance(context);
    }
    
    public void updateItems(List<JSONObject> newItems) {
        this.allItems = new ArrayList<>(newItems);
        applyFilter(currentFilter);
    }
    
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_inventory_slot, parent, false);
        return new ItemViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        JSONObject item = displayItems.get(position);
        
        try {
            int templateId = item.optInt("template_id", item.optInt("item_id", 0));
            int quantity = item.optInt("quantity", 1);
            String instanceName = item.optString("instance_name", "");
            
            String iconPath = itemConfigManager.getIconPath(templateId);
            String itemName = itemConfigManager.getItemName(templateId);
            
            if (iconPath != null && !iconPath.isEmpty()) {
                holder.textIcon.setVisibility(View.GONE);
                holder.imageIcon.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(iconPath)
                        .placeholder(R.drawable.bg_item_slot)
                        .error(R.drawable.bg_item_slot)
                        .into(holder.imageIcon);
            } else if (itemConfigManager.isLoaded()) {
                holder.imageIcon.setVisibility(View.GONE);
                holder.textIcon.setVisibility(View.VISIBLE);
                holder.textIcon.setText(getItemEmoji(templateId));
            } else {
                holder.imageIcon.setVisibility(View.GONE);
                holder.textIcon.setVisibility(View.VISIBLE);
                holder.textIcon.setText("⏳");
            }
            
            holder.textCount.setText(String.valueOf(quantity));
            holder.textCount.setVisibility(quantity > 1 ? View.VISIBLE : View.GONE);
            
            if (itemName != null && !itemName.isEmpty()) {
                holder.itemView.setContentDescription(itemName);
            }
            
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item);
                }
            });
            
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    int itemId = item.optInt("item_id", holder.getAdapterPosition());
                    int invType = item.optInt("inv_type", 0);
                    longClickListener.onItemLongClick(item, itemId, invType);
                    return true;
                }
                return false;
            });
            
        } catch (Exception e) {
            holder.textIcon.setVisibility(View.VISIBLE);
            holder.imageIcon.setVisibility(View.GONE);
            holder.textIcon.setText("📦");
            holder.textCount.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return displayItems.size();
    }
    
    public void filter(String query) {
        currentFilter = query;
        applyFilter(query);
    }
    
    private void applyFilter(String query) {
        displayItems.clear();
        if (query.isEmpty()) {
            displayItems.addAll(allItems);
        } else {
            String lowerQuery = query.toLowerCase();
            for (JSONObject item : allItems) {
                String name = item.optString("instance_name", "").toLowerCase();
                String templateId = String.valueOf(item.optInt("template_id", item.optInt("item_id", 0)));
                String configName = itemConfigManager.getItemName(templateId);
                if (configName != null) configName = configName.toLowerCase();
                
                if (name.contains(lowerQuery) || templateId.contains(query) || 
                    (configName != null && configName.contains(lowerQuery))) {
                    displayItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
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
    
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView textIcon;
        ImageView imageIcon;
        TextView textCount;
        
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textIcon = itemView.findViewById(R.id.text_icon);
            imageIcon = itemView.findViewById(R.id.image_icon);
            textCount = itemView.findViewById(R.id.text_count);
        }
    }
}
