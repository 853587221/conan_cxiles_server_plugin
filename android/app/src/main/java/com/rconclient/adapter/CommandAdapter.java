package com.rconclient.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rconclient.R;
import com.rconclient.model.Command;

import java.util.ArrayList;
import java.util.List;

public class CommandAdapter extends RecyclerView.Adapter<CommandAdapter.ViewHolder> {
    private List<Command> commands = new ArrayList<>();
    private OnCommandClickListener listener;
    
    public interface OnCommandClickListener {
        void onCommandClick(Command command);
    }
    
    public void setCommands(List<Command> commands) {
        this.commands = commands;
        notifyDataSetChanged();
    }
    
    public void setOnCommandClickListener(OnCommandClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_command, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Command command = commands.get(position);
        holder.nameText.setText(command.getName());
        holder.descText.setText(command.getDescription());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommandClick(command);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return commands.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView descText;
        
        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_command_name);
            descText = itemView.findViewById(R.id.text_command_desc);
        }
    }
}
