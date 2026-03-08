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

public class CommandSelectAdapter extends RecyclerView.Adapter<CommandSelectAdapter.ViewHolder> {

    private List<Command> commands = new ArrayList<>();
    private List<Command> filteredCommands = new ArrayList<>();
    private OnCommandSelectedListener listener;

    public interface OnCommandSelectedListener {
        void onCommandSelected(Command command);
    }

    public void setOnCommandSelectedListener(OnCommandSelectedListener listener) {
        this.listener = listener;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
        this.filteredCommands = new ArrayList<>(commands);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredCommands.clear();
        if (query.isEmpty()) {
            filteredCommands.addAll(commands);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Command cmd : commands) {
                if (cmd.getName().toLowerCase().contains(lowerQuery) ||
                    cmd.getCategory().toLowerCase().contains(lowerQuery) ||
                    (cmd.getDescription() != null && cmd.getDescription().toLowerCase().contains(lowerQuery))) {
                    filteredCommands.add(cmd);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_command_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Command command = filteredCommands.get(position);
        holder.bind(command);
    }

    @Override
    public int getItemCount() {
        return filteredCommands.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textCommandName;
        private TextView textCommandCategory;
        private TextView textCommandDescription;
        private TextView textCommandExample;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textCommandName = itemView.findViewById(R.id.text_command_name);
            textCommandCategory = itemView.findViewById(R.id.text_command_category);
            textCommandDescription = itemView.findViewById(R.id.text_command_description);
            textCommandExample = itemView.findViewById(R.id.text_command_example);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCommandSelected(filteredCommands.get(pos));
                }
            });
        }

        public void bind(Command command) {
            textCommandName.setText(command.getName());
            textCommandCategory.setText("(" + command.getCategory() + ")");
            textCommandDescription.setText(command.getDescription() != null ? command.getDescription() : "");
            textCommandExample.setText(command.getExample() != null ? command.getExample() : "");
        }
    }
}
