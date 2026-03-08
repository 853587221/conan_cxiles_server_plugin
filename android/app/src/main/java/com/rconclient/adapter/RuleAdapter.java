package com.rconclient.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rconclient.R;
import com.rconclient.model.AutoExecuteRule;

import java.util.ArrayList;
import java.util.List;

public class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.ViewHolder> {

    private List<AutoExecuteRule> rules = new ArrayList<>();
    private OnRuleActionListener listener;

    public interface OnRuleActionListener {
        void onEdit(AutoExecuteRule rule, int position);
        void onDelete(AutoExecuteRule rule, int position);
        void onToggleEnabled(AutoExecuteRule rule, int position, boolean enabled);
    }

    public void setOnRuleActionListener(OnRuleActionListener listener) {
        this.listener = listener;
    }

    public void setRules(List<AutoExecuteRule> rules) {
        this.rules = rules;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AutoExecuteRule rule = rules.get(position);
        holder.bind(rule, position);
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textRuleName;
        private TextView textConditions;
        private TextView textSecondaryConditions;
        private TextView textAction;
        private TextView textAfterExecute;
        private SwitchCompat switchEnabled;
        private Button btnEdit;
        private Button btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textRuleName = itemView.findViewById(R.id.text_rule_name);
            textConditions = itemView.findViewById(R.id.text_conditions);
            textSecondaryConditions = itemView.findViewById(R.id.text_secondary_conditions);
            textAction = itemView.findViewById(R.id.text_action);
            textAfterExecute = itemView.findViewById(R.id.text_after_execute);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(AutoExecuteRule rule, int position) {
            textRuleName.setText(rule.getRuleName() != null && !rule.getRuleName().isEmpty() 
                    ? rule.getRuleName() : "规则 #" + (position + 1));

            StringBuilder conditionsText = new StringBuilder();
            if (rule.getConditions() != null && !rule.getConditions().isEmpty()) {
                for (AutoExecuteRule.Condition c : rule.getConditions()) {
                    if (conditionsText.length() > 0) conditionsText.append("\n");
                    conditionsText.append("• ").append(c.getDisplayText());
                }
            }
            textConditions.setText(conditionsText.length() > 0 ? conditionsText.toString() : "无条件");
            textConditions.setVisibility(conditionsText.length() > 0 ? View.VISIBLE : View.GONE);

            if (textSecondaryConditions != null) {
                StringBuilder secondaryText = new StringBuilder();
                if (rule.getSecondaryConditions() != null && !rule.getSecondaryConditions().isEmpty()) {
                    for (AutoExecuteRule.Condition c : rule.getSecondaryConditions()) {
                        if (secondaryText.length() > 0) secondaryText.append("\n");
                        secondaryText.append("• ").append(c.getDisplayText());
                    }
                }
                if (secondaryText.length() > 0) {
                    textSecondaryConditions.setText("二级条件:\n" + secondaryText.toString());
                    textSecondaryConditions.setVisibility(View.VISIBLE);
                } else {
                    textSecondaryConditions.setVisibility(View.GONE);
                }
            }

            StringBuilder action = new StringBuilder();
            if (rule.getExecuteType() != null) {
                switch (rule.getExecuteType()) {
                    case "single":
                        action.append("执行命令: ");
                        if (rule.getExecuteData() != null && rule.getExecuteData().getCommand() != null) {
                            action.append(rule.getExecuteData().getCommand());
                        } else {
                            action.append("未设置");
                        }
                        break;
                    case "category":
                        action.append("执行分类: ");
                        if (rule.getExecuteData() != null && rule.getExecuteData().getCategory() != null) {
                            action.append(rule.getExecuteData().getCategory());
                        } else {
                            action.append("未设置");
                        }
                        break;
                    case "no_operation":
                        action.append("不执行命令");
                        break;
                }
            }
            textAction.setText(action.length() > 0 ? action.toString() : "无操作");

            if (textAfterExecute != null) {
                StringBuilder afterExecute = new StringBuilder();
                if (rule.getAfterExecute() != null) {
                    AutoExecuteRule.AfterExecute ae = rule.getAfterExecute();
                    if (ae.getAmountOperation() != null && !ae.getAmountOperation().isEmpty()) {
                        afterExecute.append("金额: ").append(ae.getAmountOperationText())
                                .append(" ").append((int) ae.getAmountValue());
                    }
                    if (ae.getSetTag() != null && !ae.getSetTag().isEmpty()) {
                        if (afterExecute.length() > 0) afterExecute.append(" | ");
                        afterExecute.append("设置标签: ").append(ae.getSetTag());
                    }
                    if (ae.getNotificationMessage() != null && !ae.getNotificationMessage().isEmpty()) {
                        if (afterExecute.length() > 0) afterExecute.append("\n");
                        afterExecute.append("通知: ").append(ae.getNotificationMessage());
                    }
                    if (ae.getFailNotificationMessage() != null && !ae.getFailNotificationMessage().isEmpty()) {
                        if (afterExecute.length() > 0) afterExecute.append("\n");
                        afterExecute.append("失败通知: ").append(ae.getFailNotificationMessage());
                    }
                }
                if (afterExecute.length() > 0) {
                    textAfterExecute.setText(afterExecute.toString());
                    textAfterExecute.setVisibility(View.VISIBLE);
                } else {
                    textAfterExecute.setVisibility(View.GONE);
                }
            }

            switchEnabled.setOnCheckedChangeListener(null);
            switchEnabled.setChecked(rule.isEnabled());
            switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleEnabled(rule, position, isChecked);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(rule, position);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(rule, position);
                }
            });
        }
    }
}
