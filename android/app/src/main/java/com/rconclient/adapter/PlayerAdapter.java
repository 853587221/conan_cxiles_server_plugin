package com.rconclient.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rconclient.R;
import com.rconclient.model.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.ViewHolder> {
    private List<Player> players = new ArrayList<>();
    private OnPlayerClickListener listener;
    
    public interface OnPlayerClickListener {
        void onPlayerClick(Player player);
    }
    
    public void setPlayers(List<Player> players) {
        this.players = players;
        notifyDataSetChanged();
    }
    
    public void setOnPlayerClickListener(OnPlayerClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Player player = players.get(position);
        holder.nameText.setText(player.getCharName());
        holder.levelText.setText("Lv." + player.getLevel());
        holder.goldText.setText(String.format("%.0f", player.getGold()));
        
        holder.permissionText.setText(String.valueOf(player.getPermissionLevel()));
        
        if (player.isOnline()) {
            holder.onlineStatus.setText("在线");
            holder.onlineStatus.setVisibility(View.VISIBLE);
        } else {
            holder.onlineStatus.setVisibility(View.GONE);
        }
        
        long vipExpiry = player.getMonthlyCardExpiry();
        long now = System.currentTimeMillis() / 1000;
        if (vipExpiry > 0 && vipExpiry > now) {
            int remainingDays = (int) Math.ceil((vipExpiry - now) / 86400.0);
            holder.vipStatus.setText("👑" + remainingDays + "天");
            holder.vipStatus.setVisibility(View.VISIBLE);
        } else {
            holder.vipStatus.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayerClick(player);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return players.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView levelText;
        TextView goldText;
        TextView permissionText;
        TextView onlineStatus;
        TextView vipStatus;
        
        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_player_name);
            levelText = itemView.findViewById(R.id.text_player_level);
            goldText = itemView.findViewById(R.id.text_player_gold);
            permissionText = itemView.findViewById(R.id.text_permission);
            onlineStatus = itemView.findViewById(R.id.text_online_status);
            vipStatus = itemView.findViewById(R.id.text_vip_status);
        }
    }
}
