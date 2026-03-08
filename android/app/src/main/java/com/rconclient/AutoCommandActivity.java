package com.rconclient;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rconclient.adapter.RuleAdapter;
import com.rconclient.model.AutoExecuteRule;
import com.rconclient.model.Category;
import com.rconclient.model.Command;
import com.rconclient.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AutoCommandActivity extends AppCompatActivity implements RuleAdapter.OnRuleActionListener {

    private RecyclerView recyclerRules;
    private TextView textEmpty;
    private EditText editSearch;
    private Button btnClearSearch;
    private TextView textSearchResult;

    private ApiClient apiClient;
    private List<AutoExecuteRule> rules = new ArrayList<>();
    private List<AutoExecuteRule> filteredRules = new ArrayList<>();
    private RuleAdapter adapter;
    
    private List<Category> categories = new ArrayList<>();
    private List<Command> commands = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_command);

        apiClient = ApiClient.getInstance(this);

        initViews();
        loadCategoriesAndCommands();
        loadRules();
    }

    private void initViews() {
        recyclerRules = findViewById(R.id.recycler_rules);
        textEmpty = findViewById(R.id.text_empty);
        editSearch = findViewById(R.id.edit_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        textSearchResult = findViewById(R.id.text_search_result);

        adapter = new RuleAdapter();
        adapter.setOnRuleActionListener(this);
        recyclerRules.setLayoutManager(new LinearLayoutManager(this));
        recyclerRules.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_rule).setOnClickListener(v -> showAddRuleDialog());
        
        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String query = s.toString().trim();
                filterRules(query);
            }
        });
        
        btnClearSearch.setOnClickListener(v -> {
            editSearch.setText("");
            filterRules("");
        });
    }
    
    private void filterRules(String query) {
        filteredRules.clear();
        
        if (query.isEmpty()) {
            filteredRules.addAll(rules);
            btnClearSearch.setVisibility(View.GONE);
            textSearchResult.setVisibility(View.GONE);
        } else {
            btnClearSearch.setVisibility(View.VISIBLE);
            String lowerQuery = query.toLowerCase();
            
            for (AutoExecuteRule rule : rules) {
                if (matchesQuery(rule, lowerQuery)) {
                    filteredRules.add(rule);
                }
            }
            
            textSearchResult.setVisibility(View.VISIBLE);
            textSearchResult.setText("搜索结果: 找到 " + filteredRules.size() + " 条规则");
        }
        
        adapter.setRules(filteredRules);
        updateEmptyView();
    }
    
    private boolean matchesQuery(AutoExecuteRule rule, String query) {
        if (rule.getRuleName() != null && rule.getRuleName().toLowerCase().contains(query)) {
            return true;
        }
        
        if (rule.getConditions() != null) {
            for (AutoExecuteRule.Condition condition : rule.getConditions()) {
                if (condition.getDisplayText().toLowerCase().contains(query)) {
                    return true;
                }
                if (condition.getType() != null && condition.getType().toLowerCase().contains(query)) {
                    return true;
                }
                if (condition.getValue() != null && String.valueOf(condition.getValue()).toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        
        if (rule.getSecondaryConditions() != null) {
            for (AutoExecuteRule.Condition condition : rule.getSecondaryConditions()) {
                if (condition.getDisplayText().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        
        if (rule.getExecuteType() != null && rule.getExecuteType().toLowerCase().contains(query)) {
            return true;
        }
        
        if (rule.getExecuteData() != null) {
            if (rule.getExecuteData().getCommand() != null && 
                rule.getExecuteData().getCommand().toLowerCase().contains(query)) {
                return true;
            }
            if (rule.getExecuteData().getCategory() != null && 
                rule.getExecuteData().getCategory().toLowerCase().contains(query)) {
                return true;
            }
        }
        
        if (rule.getAfterExecute() != null) {
            AutoExecuteRule.AfterExecute ae = rule.getAfterExecute();
            if (ae.getNotificationMessage() != null && 
                ae.getNotificationMessage().toLowerCase().contains(query)) {
                return true;
            }
            if (ae.getFailNotificationMessage() != null && 
                ae.getFailNotificationMessage().toLowerCase().contains(query)) {
                return true;
            }
            if (ae.getSetTag() != null && ae.getSetTag().toLowerCase().contains(query)) {
                return true;
            }
        }
        
        return false;
    }

    private void loadCategoriesAndCommands() {
        apiClient.getCategories(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.optBoolean("success")) {
                        JSONArray arr = json.getJSONArray("categories");
                        categories.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            Category cat = new Category();
                            cat.setName(arr.getJSONObject(i).optString("name"));
                            cat.setDescription(arr.getJSONObject(i).optString("description"));
                            categories.add(cat);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {}
        });

        apiClient.getCommands("all", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.optBoolean("success")) {
                        JSONArray arr = json.getJSONArray("commands");
                        commands.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject cmdJson = arr.getJSONObject(i);
                            Command cmd = new Command();
                            cmd.setName(cmdJson.optString("name"));
                            cmd.setDescription(cmdJson.optString("description"));
                            cmd.setCategory(cmdJson.optString("category"));
                            cmd.setExample(cmdJson.optString("example"));
                            commands.add(cmd);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void loadRules() {
        apiClient.getAutoExecuteRules(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            JSONArray arr = json.getJSONArray("rules");
                            rules.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                rules.add(AutoExecuteRule.fromJson(arr.getJSONObject(i)));
                            }
                            String currentQuery = editSearch.getText().toString().trim();
                            filterRules(currentQuery);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(AutoCommandActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(AutoCommandActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateEmptyView() {
        boolean isEmpty = filteredRules.isEmpty();
        textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerRules.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (isEmpty && !rules.isEmpty()) {
            textEmpty.setText("未找到匹配的规则\n请尝试其他搜索条件");
        } else if (isEmpty) {
            textEmpty.setText("暂无自动命令规则\n点击右上角添加新规则");
        }
    }

    private void showAddRuleDialog() {
        showRuleDialog(null);
    }

    private void showCommandSelectDialog(OnCommandSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_command_select, null);
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        
        EditText editSearch = view.findViewById(R.id.edit_search_command);
        RecyclerView recyclerCommands = view.findViewById(R.id.recycler_commands);
        TextView textNoCommands = view.findViewById(R.id.text_no_commands);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        
        recyclerCommands.setLayoutManager(new LinearLayoutManager(this));
        
        com.rconclient.adapter.CommandSelectAdapter adapter = new com.rconclient.adapter.CommandSelectAdapter();
        adapter.setCommands(commands);
        recyclerCommands.setAdapter(adapter);
        
        adapter.setOnCommandSelectedListener(command -> {
            listener.onCommandSelected(command);
            dialog.dismiss();
        });
        
        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String query = s.toString().trim();
                adapter.filter(query);
                
                if (query.isEmpty() && commands.isEmpty()) {
                    textNoCommands.setVisibility(View.VISIBLE);
                    recyclerCommands.setVisibility(View.GONE);
                } else {
                    textNoCommands.setVisibility(View.GONE);
                    recyclerCommands.setVisibility(View.VISIBLE);
                }
            }
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.8)
        );
    }

    interface OnCommandSelectedListener {
        void onCommandSelected(Command command);
    }

    private void showRuleDialog(AutoExecuteRule existingRule) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_rule_edit, null);
        builder.setView(view);
        builder.setTitle(existingRule == null ? "添加规则" : "编辑规则");
        
        AlertDialog dialog = builder.create();
        
        EditText editRuleName = view.findViewById(R.id.edit_rule_name);
        LinearLayout containerAddedConditions = view.findViewById(R.id.container_added_conditions);
        TextView textNoConditions = view.findViewById(R.id.text_no_conditions);
        Spinner spinnerExecuteType = view.findViewById(R.id.spinner_execute_type);
        LinearLayout containerSingleCommand = view.findViewById(R.id.container_single_command);
        LinearLayout containerCategoryCommand = view.findViewById(R.id.container_category_command);
        Button btnSelectCommand = view.findViewById(R.id.btn_select_command);
        TextView textSelectedCommand = view.findViewById(R.id.text_selected_command);
        Spinner spinnerCategory = view.findViewById(R.id.spinner_category);
        Spinner spinnerAmountOperation = view.findViewById(R.id.spinner_amount_operation);
        EditText editAmountValue = view.findViewById(R.id.edit_amount_value);
        EditText editSetTag = view.findViewById(R.id.edit_set_tag);
        EditText editFailNotification = view.findViewById(R.id.edit_fail_notification);
        EditText editNotification = view.findViewById(R.id.edit_notification);
        Button btnSaveRule = view.findViewById(R.id.btn_save_rule);
        
        List<AutoExecuteRule.Condition> addedConditions = new ArrayList<>();
        List<AutoExecuteRule.Condition> addedSecondaryConditions = new ArrayList<>();
        
        String[] executeTypes = {"单个命令", "命令分类", "不操作"};
        ArrayAdapter<String> executeTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, executeTypes);
        executeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExecuteType.setAdapter(executeTypeAdapter);
        
        String[] amountOperations = {"不操作", "增加", "扣除", "强行指定"};
        ArrayAdapter<String> amountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, amountOperations);
        amountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAmountOperation.setAdapter(amountAdapter);
        
        List<String> categoryNames = new ArrayList<>();
        for (Category cat : categories) {
            categoryNames.add(cat.getName());
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        
        String[] selectedCommand = {""};
        
        btnSelectCommand.setOnClickListener(v -> showCommandSelectDialog(command -> {
            selectedCommand[0] = command.getExample();
            btnSelectCommand.setText(command.getName() + " (" + command.getCategory() + ")");
            textSelectedCommand.setText("命令: " + command.getExample());
            textSelectedCommand.setVisibility(View.VISIBLE);
        }));
        
        spinnerExecuteType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position == 0) {
                    containerSingleCommand.setVisibility(View.VISIBLE);
                    containerCategoryCommand.setVisibility(View.GONE);
                } else if (position == 1) {
                    containerSingleCommand.setVisibility(View.GONE);
                    containerCategoryCommand.setVisibility(View.VISIBLE);
                } else {
                    containerSingleCommand.setVisibility(View.GONE);
                    containerCategoryCommand.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        if (existingRule != null) {
            editRuleName.setText(existingRule.getRuleName());
            addedConditions.addAll(existingRule.getConditions());
            if (existingRule.getSecondaryConditions() != null) {
                addedSecondaryConditions.addAll(existingRule.getSecondaryConditions());
            }
            
            if ("single".equals(existingRule.getExecuteType())) {
                spinnerExecuteType.setSelection(0);
                if (existingRule.getExecuteData() != null && existingRule.getExecuteData().getCommand() != null) {
                    selectedCommand[0] = existingRule.getExecuteData().getCommand();
                    for (int i = 0; i < commands.size(); i++) {
                        if (commands.get(i).getExample().equals(selectedCommand[0])) {
                            Command cmd = commands.get(i);
                            btnSelectCommand.setText(cmd.getName() + " (" + cmd.getCategory() + ")");
                            textSelectedCommand.setText("命令: " + cmd.getExample());
                            textSelectedCommand.setVisibility(View.VISIBLE);
                            break;
                        }
                    }
                }
            } else if ("category".equals(existingRule.getExecuteType())) {
                spinnerExecuteType.setSelection(1);
                if (existingRule.getExecuteData() != null && existingRule.getExecuteData().getCategory() != null) {
                    for (int i = 0; i < categoryNames.size(); i++) {
                        if (categoryNames.get(i).equals(existingRule.getExecuteData().getCategory())) {
                            spinnerCategory.setSelection(i);
                            break;
                        }
                    }
                }
            } else {
                spinnerExecuteType.setSelection(2);
            }
            
            if (existingRule.getAfterExecute() != null) {
                AutoExecuteRule.AfterExecute ae = existingRule.getAfterExecute();
                if (ae.getAmountOperation() != null) {
                    switch (ae.getAmountOperation()) {
                        case "add": spinnerAmountOperation.setSelection(1); break;
                        case "deduct": spinnerAmountOperation.setSelection(2); break;
                        case "set": spinnerAmountOperation.setSelection(3); break;
                    }
                    editAmountValue.setText(String.valueOf((int) ae.getAmountValue()));
                }
                editSetTag.setText(ae.getSetTag());
                editFailNotification.setText(ae.getFailNotificationMessage());
                editNotification.setText(ae.getNotificationMessage());
            }
            
            btnSaveRule.setText("💾 更新自动触发规则");
        }
        
        updateConditionsView(containerAddedConditions, textNoConditions, addedConditions, addedSecondaryConditions);
        
        View.OnClickListener conditionClickListener = v -> {
            String conditionType = null;
            int id = v.getId();
            if (id == R.id.btn_condition_keyword) conditionType = "keyword";
            else if (id == R.id.btn_condition_amount) conditionType = "amount";
            else if (id == R.id.btn_condition_tag) conditionType = "tag";
            else if (id == R.id.btn_condition_level) conditionType = "level";
            else if (id == R.id.btn_condition_playtime) conditionType = "playtime";
            else if (id == R.id.btn_condition_new_player) conditionType = "new_player";
            else if (id == R.id.btn_condition_server_time) conditionType = "server_time";
            else if (id == R.id.btn_condition_item) conditionType = "item";
            else if (id == R.id.btn_condition_vip) conditionType = "vip";
            
            if (conditionType != null) {
                boolean isSecondaryCondition = "amount".equals(conditionType) || "tag".equals(conditionType) 
                        || "server_time".equals(conditionType) || "item".equals(conditionType) || "vip".equals(conditionType);
                
                if (isSecondaryCondition && addedConditions.isEmpty()) {
                    Toast.makeText(this, "该触发条件必须先有其它触发条件作为前置才能使用，请先添加其它条件。", Toast.LENGTH_LONG).show();
                    return;
                }
                
                if (isSecondaryCondition) {
                    showSecondaryConditionDialog(conditionType, addedSecondaryConditions, () -> {
                        updateConditionsView(containerAddedConditions, textNoConditions, addedConditions, addedSecondaryConditions);
                    });
                } else {
                    showConditionDialog(conditionType, addedConditions, addedSecondaryConditions, () -> {
                        updateConditionsView(containerAddedConditions, textNoConditions, addedConditions, addedSecondaryConditions);
                    });
                }
            }
        };
        
        view.findViewById(R.id.btn_condition_keyword).setOnClickListener(conditionClickListener);
        view.findViewById(R.id.btn_condition_amount).setOnClickListener(conditionClickListener);
        view.findViewById(R.id.btn_condition_tag).setOnClickListener(conditionClickListener);
        view.findViewById(R.id.btn_condition_level).setOnClickListener(conditionClickListener);
        view.findViewById(R.id.btn_condition_playtime).setOnClickListener(conditionClickListener);
        view.findViewById(R.id.btn_condition_new_player).setOnClickListener(conditionClickListener);
        view.findViewById(R.id.btn_condition_server_time).setOnClickListener(conditionClickListener);
        view.findViewById(R.id.btn_condition_item).setOnClickListener(conditionClickListener);
        view.findViewById(R.id.btn_condition_vip).setOnClickListener(conditionClickListener);
        
        LinearLayout containerVariables = view.findViewById(R.id.container_variables);
        LinearLayout containerFailVariables = view.findViewById(R.id.container_fail_variables);
        
        view.findViewById(R.id.btn_insert_variable).setOnClickListener(v -> {
            if (containerVariables.getVisibility() == View.GONE) {
                containerVariables.setVisibility(View.VISIBLE);
            } else {
                containerVariables.setVisibility(View.GONE);
            }
        });
        
        view.findViewById(R.id.btn_insert_fail_variable).setOnClickListener(v -> {
            if (containerFailVariables.getVisibility() == View.GONE) {
                containerFailVariables.setVisibility(View.VISIBLE);
            } else {
                containerFailVariables.setVisibility(View.GONE);
            }
        });
        
        View.OnClickListener varClickListener = v -> {
            String variable = null;
            int id = v.getId();
            if (id == R.id.btn_var_char_name) variable = "@角色名";
            else if (id == R.id.btn_var_clan_name) variable = "@部落名";
            else if (id == R.id.btn_var_amount) variable = "@金额";
            else if (id == R.id.btn_var_level) variable = "@等级";
            else if (id == R.id.btn_var_playtime) variable = "@在线时间";
            else if (id == R.id.btn_var_tag) variable = "@权限标签";
            else if (id == R.id.btn_var_respawn) variable = "@复活点";
            else if (id == R.id.btn_var_vip_time) variable = "@会员剩余时间";
            
            if (variable != null) {
                int cursorPos = editNotification.getSelectionStart();
                String currentText = editNotification.getText().toString();
                String newText = currentText.substring(0, cursorPos) + variable + currentText.substring(cursorPos);
                editNotification.setText(newText);
                editNotification.setSelection(cursorPos + variable.length());
                containerVariables.setVisibility(View.GONE);
            }
        };
        
        view.findViewById(R.id.btn_var_char_name).setOnClickListener(varClickListener);
        view.findViewById(R.id.btn_var_clan_name).setOnClickListener(varClickListener);
        view.findViewById(R.id.btn_var_amount).setOnClickListener(varClickListener);
        view.findViewById(R.id.btn_var_level).setOnClickListener(varClickListener);
        view.findViewById(R.id.btn_var_playtime).setOnClickListener(varClickListener);
        view.findViewById(R.id.btn_var_tag).setOnClickListener(varClickListener);
        view.findViewById(R.id.btn_var_respawn).setOnClickListener(varClickListener);
        view.findViewById(R.id.btn_var_vip_time).setOnClickListener(varClickListener);
        
        View.OnClickListener failVarClickListener = v -> {
            String variable = null;
            int id = v.getId();
            if (id == R.id.btn_var_fail_char_name) variable = "@角色名";
            else if (id == R.id.btn_var_fail_clan_name) variable = "@部落名";
            else if (id == R.id.btn_var_fail_amount) variable = "@金额";
            else if (id == R.id.btn_var_fail_level) variable = "@等级";
            else if (id == R.id.btn_var_fail_playtime) variable = "@在线时间";
            else if (id == R.id.btn_var_fail_tag) variable = "@权限标签";
            else if (id == R.id.btn_var_fail_respawn) variable = "@复活点";
            else if (id == R.id.btn_var_fail_vip_time) variable = "@会员剩余时间";
            
            if (variable != null) {
                int cursorPos = editFailNotification.getSelectionStart();
                String currentText = editFailNotification.getText().toString();
                String newText = currentText.substring(0, cursorPos) + variable + currentText.substring(cursorPos);
                editFailNotification.setText(newText);
                editFailNotification.setSelection(cursorPos + variable.length());
                containerFailVariables.setVisibility(View.GONE);
            }
        };
        
        view.findViewById(R.id.btn_var_fail_char_name).setOnClickListener(failVarClickListener);
        view.findViewById(R.id.btn_var_fail_clan_name).setOnClickListener(failVarClickListener);
        view.findViewById(R.id.btn_var_fail_amount).setOnClickListener(failVarClickListener);
        view.findViewById(R.id.btn_var_fail_level).setOnClickListener(failVarClickListener);
        view.findViewById(R.id.btn_var_fail_playtime).setOnClickListener(failVarClickListener);
        view.findViewById(R.id.btn_var_fail_tag).setOnClickListener(failVarClickListener);
        view.findViewById(R.id.btn_var_fail_respawn).setOnClickListener(failVarClickListener);
        view.findViewById(R.id.btn_var_fail_vip_time).setOnClickListener(failVarClickListener);
        
        btnSaveRule.setOnClickListener(v -> {
            if (addedConditions.isEmpty()) {
                Toast.makeText(this, "请至少添加一个触发条件", Toast.LENGTH_SHORT).show();
                return;
            }
            
            AutoExecuteRule rule = existingRule != null ? existingRule : new AutoExecuteRule();
            rule.setRuleName(editRuleName.getText().toString().trim());
            if (rule.getRuleName().isEmpty()) {
                rule.setRuleName("自动触发规则 " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
            }
            
            rule.setConditions(new ArrayList<>(addedConditions));
            if (!addedSecondaryConditions.isEmpty()) {
                rule.setSecondaryConditions(new ArrayList<>(addedSecondaryConditions));
            } else {
                rule.setSecondaryConditions(null);
            }
            
            int executeTypePos = spinnerExecuteType.getSelectedItemPosition();
            if (executeTypePos == 0) {
                rule.setExecuteType("single");
                AutoExecuteRule.ExecuteData data = new AutoExecuteRule.ExecuteData();
                data.setCommand(selectedCommand[0]);
                rule.setExecuteData(data);
            } else if (executeTypePos == 1) {
                rule.setExecuteType("category");
                AutoExecuteRule.ExecuteData data = new AutoExecuteRule.ExecuteData();
                data.setCategory(categoryNames.get(spinnerCategory.getSelectedItemPosition()));
                rule.setExecuteData(data);
            } else {
                rule.setExecuteType("no_operation");
                rule.setExecuteData(null);
            }
            
            AutoExecuteRule.AfterExecute afterExecute = null;
            int amountOpPos = spinnerAmountOperation.getSelectedItemPosition();
            if (amountOpPos > 0 || !editSetTag.getText().toString().trim().isEmpty() 
                    || !editNotification.getText().toString().trim().isEmpty()
                    || !editFailNotification.getText().toString().trim().isEmpty()) {
                afterExecute = new AutoExecuteRule.AfterExecute();
                if (amountOpPos > 0) {
                    String[] ops = {"", "add", "deduct", "set"};
                    afterExecute.setAmountOperation(ops[amountOpPos]);
                    try {
                        afterExecute.setAmountValue(Double.parseDouble(editAmountValue.getText().toString()));
                    } catch (NumberFormatException e) {
                        afterExecute.setAmountValue(0);
                    }
                }
                afterExecute.setSetTag(editSetTag.getText().toString().trim());
                afterExecute.setNotificationMessage(editNotification.getText().toString().trim());
                afterExecute.setFailNotificationMessage(editFailNotification.getText().toString().trim());
            }
            rule.setAfterExecute(afterExecute);
            
            if (existingRule == null) {
                rule.setEnabled(true);
            }
            
            saveRule(rule, existingRule != null, dialog);
        });
        
        dialog.show();
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.85)
        );
    }

    private void updateConditionsView(LinearLayout container, TextView textNoConditions, 
                                       List<AutoExecuteRule.Condition> conditions, 
                                       List<AutoExecuteRule.Condition> secondaryConditions) {
        container.removeAllViews();
        
        if (conditions.isEmpty() && secondaryConditions.isEmpty()) {
            container.addView(textNoConditions);
            textNoConditions.setVisibility(View.VISIBLE);
            return;
        }
        
        textNoConditions.setVisibility(View.GONE);
        
        for (int i = 0; i < conditions.size(); i++) {
            AutoExecuteRule.Condition c = conditions.get(i);
            View itemView = createConditionItemView(c, i, true, conditions, secondaryConditions, container, textNoConditions);
            container.addView(itemView);
        }
        
        if (!secondaryConditions.isEmpty()) {
            TextView secondaryLabel = new TextView(this);
            secondaryLabel.setText("二级条件：");
            secondaryLabel.setTextColor(getResources().getColor(R.color.warning));
            secondaryLabel.setTextSize(12);
            secondaryLabel.setPadding(0, 16, 0, 8);
            container.addView(secondaryLabel);
            
            for (int i = 0; i < secondaryConditions.size(); i++) {
                AutoExecuteRule.Condition c = secondaryConditions.get(i);
                View itemView = createConditionItemView(c, i, false, conditions, secondaryConditions, container, textNoConditions);
                container.addView(itemView);
            }
        }
    }

    private View createConditionItemView(AutoExecuteRule.Condition condition, int index, boolean isPrimary,
                                          List<AutoExecuteRule.Condition> conditions,
                                          List<AutoExecuteRule.Condition> secondaryConditions,
                                          LinearLayout container, TextView textNoConditions) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(8, 8, 8, 8);
        
        TextView textView = new TextView(this);
        textView.setText("• " + condition.getDisplayText());
        textView.setTextColor(getResources().getColor(R.color.text_secondary));
        textView.setTextSize(12);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textView.setLayoutParams(textParams);
        
        Button deleteBtn = new Button(this);
        deleteBtn.setText("删除");
        deleteBtn.setTextSize(10);
        deleteBtn.setTextColor(getResources().getColor(R.color.white));
        deleteBtn.setBackgroundResource(R.drawable.bg_button_danger);
        deleteBtn.setMinHeight(32);
        deleteBtn.setPadding(8, 0, 8, 0);
        
        deleteBtn.setOnClickListener(v -> {
            if (isPrimary) {
                conditions.remove(index);
            } else {
                secondaryConditions.remove(index);
            }
            updateConditionsView(container, textNoConditions, conditions, secondaryConditions);
        });
        
        itemLayout.addView(textView);
        itemLayout.addView(deleteBtn);
        
        return itemLayout;
    }

    private void showConditionDialog(String conditionType, List<AutoExecuteRule.Condition> conditions,
                                      List<AutoExecuteRule.Condition> secondaryConditions, Runnable onUpdate) {
        if ("new_player".equals(conditionType)) {
            conditions.add(new AutoExecuteRule.Condition("new_player", "eq", "yes"));
            onUpdate.run();
            return;
        }
        
        if ("server_time".equals(conditionType)) {
            showServerTimeDialog(conditions, onUpdate);
            return;
        }
        
        if ("item".equals(conditionType)) {
            showItemConditionDialog(secondaryConditions, onUpdate);
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_condition_setting, null);
        builder.setView(view);
        builder.setTitle("设置" + getConditionTypeName(conditionType) + "条件");
        
        AlertDialog dialog = builder.create();
        
        LinearLayout containerOperator = view.findViewById(R.id.container_operator);
        Spinner spinnerOperator = view.findViewById(R.id.spinner_operator);
        LinearLayout containerValue = view.findViewById(R.id.container_value);
        TextView textValueLabel = view.findViewById(R.id.text_value_label);
        EditText editValue = view.findViewById(R.id.edit_condition_value);
        LinearLayout containerItem = view.findViewById(R.id.container_item);
        LinearLayout containerServerTime = view.findViewById(R.id.container_server_time);
        Button btnConfirm = view.findViewById(R.id.btn_confirm_condition);
        
        containerItem.setVisibility(View.GONE);
        containerServerTime.setVisibility(View.GONE);
        
        List<String> operators = new ArrayList<>();
        if ("keyword".equals(conditionType)) {
            operators.add("等于");
            operators.add("开头等于");
            operators.add("包含");
            operators.add("不包含");
            operators.add("结尾等于");
            editValue.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        } else if ("playtime".equals(conditionType)) {
            operators.add("每隔");
            operators.add("等于");
            operators.add("大于");
            operators.add("小于");
            operators.add("等于或者大于");
            operators.add("等于或者小于");
            editValue.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            textValueLabel.setText(getConditionTypeName(conditionType) + "值（分钟）：");
        } else {
            operators.add("等于或者大于");
            operators.add("等于");
            operators.add("大于");
            operators.add("小于");
            operators.add("等于或者小于");
            editValue.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            textValueLabel.setText(getConditionTypeName(conditionType) + "值：");
        }
        
        ArrayAdapter<String> operatorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, operators);
        operatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOperator.setAdapter(operatorAdapter);
        
        btnConfirm.setOnClickListener(v -> {
            String value = editValue.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(this, "请输入" + getConditionTypeName(conditionType) + "值", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String operator;
            int opIndex = spinnerOperator.getSelectedItemPosition();
            
            if ("keyword".equals(conditionType)) {
                String[] keywordOps = {"eq", "startsWith", "contains", "notContains", "endsWith"};
                operator = keywordOps[opIndex < keywordOps.length ? opIndex : 0];
            } else if ("playtime".equals(conditionType)) {
                String[] playtimeOps = {"interval", "eq", "gt", "lt", "gte", "lte"};
                operator = playtimeOps[opIndex < playtimeOps.length ? opIndex : 0];
            } else {
                String[] otherOps = {"gte", "eq", "gt", "lt", "lte"};
                operator = otherOps[opIndex < otherOps.length ? opIndex : 0];
            }
            
            Object valueObj = "keyword".equals(conditionType) ? value : Double.parseDouble(value);
            conditions.add(new AutoExecuteRule.Condition(conditionType, operator, valueObj));
            
            dialog.dismiss();
            onUpdate.run();
        });
        
        dialog.show();
    }

    private void showServerTimeDialog(List<AutoExecuteRule.Condition> conditions, Runnable onUpdate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_condition_setting, null);
        builder.setView(view);
        builder.setTitle("设置时间条件");
        
        AlertDialog dialog = builder.create();
        
        view.findViewById(R.id.container_operator).setVisibility(View.GONE);
        view.findViewById(R.id.container_value).setVisibility(View.GONE);
        view.findViewById(R.id.container_item).setVisibility(View.GONE);
        LinearLayout containerServerTime = view.findViewById(R.id.container_server_time);
        containerServerTime.setVisibility(View.VISIBLE);
        
        Spinner spinnerTimeType = view.findViewById(R.id.spinner_time_type);
        LinearLayout containerDateRange = view.findViewById(R.id.container_date_range);
        LinearLayout containerWeekday = view.findViewById(R.id.container_weekday);
        Button btnStartDate = view.findViewById(R.id.btn_start_date);
        Button btnEndDate = view.findViewById(R.id.btn_end_date);
        Spinner spinnerStartHour = view.findViewById(R.id.spinner_start_hour);
        Spinner spinnerStartMinute = view.findViewById(R.id.spinner_start_minute);
        Spinner spinnerEndHour = view.findViewById(R.id.spinner_end_hour);
        Spinner spinnerEndMinute = view.findViewById(R.id.spinner_end_minute);
        CheckBox checkMon = view.findViewById(R.id.check_mon);
        CheckBox checkTue = view.findViewById(R.id.check_tue);
        CheckBox checkWed = view.findViewById(R.id.check_wed);
        CheckBox checkThu = view.findViewById(R.id.check_thu);
        CheckBox checkFri = view.findViewById(R.id.check_fri);
        CheckBox checkSat = view.findViewById(R.id.check_sat);
        CheckBox checkSun = view.findViewById(R.id.check_sun);
        Spinner spinnerWeekdayStartHour = view.findViewById(R.id.spinner_weekday_start_hour);
        Spinner spinnerWeekdayStartMinute = view.findViewById(R.id.spinner_weekday_start_minute);
        Spinner spinnerWeekdayEndHour = view.findViewById(R.id.spinner_weekday_end_hour);
        Spinner spinnerWeekdayEndMinute = view.findViewById(R.id.spinner_weekday_end_minute);
        Button btnConfirm = view.findViewById(R.id.btn_confirm_condition);
        
        String[] timeTypes = {"指定日期时间", "每周固定星期"};
        ArrayAdapter<String> timeTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeTypes);
        timeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeType.setAdapter(timeTypeAdapter);
        
        List<String> hours = new ArrayList<>();
        List<String> minutes = new ArrayList<>();
        for (int i = 0; i < 24; i++) hours.add(String.format("%02d", i));
        for (int i = 0; i < 60; i++) minutes.add(String.format("%02d", i));
        
        ArrayAdapter<String> hourAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hours);
        ArrayAdapter<String> minuteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, minutes);
        
        spinnerStartHour.setAdapter(hourAdapter);
        spinnerStartMinute.setAdapter(minuteAdapter);
        spinnerEndHour.setAdapter(hourAdapter);
        spinnerEndMinute.setAdapter(minuteAdapter);
        spinnerWeekdayStartHour.setAdapter(hourAdapter);
        spinnerWeekdayStartMinute.setAdapter(minuteAdapter);
        spinnerWeekdayEndHour.setAdapter(hourAdapter);
        spinnerWeekdayEndMinute.setAdapter(minuteAdapter);
        
        spinnerEndHour.setSelection(23);
        spinnerEndMinute.setSelection(59);
        
        final Calendar[] startCalendar = {Calendar.getInstance()};
        final Calendar[] endCalendar = {Calendar.getInstance()};
        endCalendar[0].add(Calendar.DAY_OF_MONTH, 1);
        
        btnStartDate.setText(formatDate(startCalendar[0]));
        btnEndDate.setText(formatDate(endCalendar[0]));
        
        btnStartDate.setOnClickListener(v -> showDatePicker(startCalendar[0], btnStartDate));
        btnEndDate.setOnClickListener(v -> showDatePicker(endCalendar[0], btnEndDate));
        
        spinnerTimeType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                containerDateRange.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                containerWeekday.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        btnConfirm.setOnClickListener(v -> {
            int timeTypePos = spinnerTimeType.getSelectedItemPosition();
            
            if (timeTypePos == 0) {
                String startDateTime = formatDate(startCalendar[0]) + " " + hours.get(spinnerStartHour.getSelectedItemPosition()) + ":" + minutes.get(spinnerStartMinute.getSelectedItemPosition());
                String endDateTime = formatDate(endCalendar[0]) + " " + hours.get(spinnerEndHour.getSelectedItemPosition()) + ":" + minutes.get(spinnerEndMinute.getSelectedItemPosition());
                
                conditions.add(new AutoExecuteRule.Condition("server_time", "date_range", startDateTime + "|" + endDateTime));
            } else {
                List<String> selectedDays = new ArrayList<>();
                if (checkMon.isChecked()) selectedDays.add("1");
                if (checkTue.isChecked()) selectedDays.add("2");
                if (checkWed.isChecked()) selectedDays.add("3");
                if (checkThu.isChecked()) selectedDays.add("4");
                if (checkFri.isChecked()) selectedDays.add("5");
                if (checkSat.isChecked()) selectedDays.add("6");
                if (checkSun.isChecked()) selectedDays.add("0");
                
                if (selectedDays.isEmpty()) {
                    Toast.makeText(this, "请至少选择一个星期", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String timeRange = hours.get(spinnerWeekdayStartHour.getSelectedItemPosition()) + ":" + minutes.get(spinnerWeekdayStartMinute.getSelectedItemPosition())
                        + "-" + hours.get(spinnerWeekdayEndHour.getSelectedItemPosition()) + ":" + minutes.get(spinnerWeekdayEndMinute.getSelectedItemPosition());
                
                conditions.add(new AutoExecuteRule.Condition("server_time", "weekday", String.join(",", selectedDays) + "|" + timeRange));
            }
            
            dialog.dismiss();
            onUpdate.run();
        });
        
        dialog.show();
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private void showItemConditionDialog(List<AutoExecuteRule.Condition> secondaryConditions, Runnable onUpdate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_condition_setting, null);
        builder.setView(view);
        builder.setTitle("设置背包物品条件");
        
        AlertDialog dialog = builder.create();
        
        view.findViewById(R.id.container_operator).setVisibility(View.GONE);
        view.findViewById(R.id.container_value).setVisibility(View.GONE);
        view.findViewById(R.id.container_server_time).setVisibility(View.GONE);
        LinearLayout containerItem = view.findViewById(R.id.container_item);
        containerItem.setVisibility(View.VISIBLE);
        
        EditText editItemTemplateId = view.findViewById(R.id.edit_item_template_id);
        Spinner spinnerItemOperator = view.findViewById(R.id.spinner_item_operator);
        EditText editItemQuantity = view.findViewById(R.id.edit_item_quantity);
        Button btnAddItem = view.findViewById(R.id.btn_add_item);
        LinearLayout containerAddedItems = view.findViewById(R.id.container_added_items);
        TextView textNoItems = view.findViewById(R.id.text_no_items);
        Button btnConfirm = view.findViewById(R.id.btn_confirm_condition);
        
        String[] itemOperators = {"大于等于", "等于", "大于", "小于", "小于等于"};
        ArrayAdapter<String> itemOperatorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemOperators);
        itemOperatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerItemOperator.setAdapter(itemOperatorAdapter);
        
        List<String> addedItems = new ArrayList<>();
        
        btnAddItem.setOnClickListener(v -> {
            String templateId = editItemTemplateId.getText().toString().trim();
            String quantity = editItemQuantity.getText().toString().trim();
            
            if (templateId.isEmpty()) {
                Toast.makeText(this, "请输入物品ID", Toast.LENGTH_SHORT).show();
                return;
            }
            if (quantity.isEmpty() || Integer.parseInt(quantity) < 1) {
                Toast.makeText(this, "请输入有效的物品数量（至少为1）", Toast.LENGTH_SHORT).show();
                return;
            }
            
            addedItems.add(templateId + ":" + quantity);
            editItemTemplateId.setText("");
            editItemQuantity.setText("1");
            
            updateAddedItemsView(containerAddedItems, textNoItems, addedItems);
        });
        
        btnConfirm.setOnClickListener(v -> {
            if (addedItems.isEmpty()) {
                Toast.makeText(this, "请至少添加一个物品", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String[] operatorKeys = {"gte", "eq", "gt", "lt", "lte"};
            String operator = operatorKeys[spinnerItemOperator.getSelectedItemPosition()];
            String value = String.join(";", addedItems);
            
            secondaryConditions.add(new AutoExecuteRule.Condition("item", operator, value));
            
            dialog.dismiss();
            onUpdate.run();
        });
        
        dialog.show();
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private void updateAddedItemsView(LinearLayout container, TextView textNoItems, List<String> items) {
        container.removeAllViews();
        
        if (items.isEmpty()) {
            container.addView(textNoItems);
            textNoItems.setVisibility(View.VISIBLE);
            return;
        }
        
        textNoItems.setVisibility(View.GONE);
        
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(8, 8, 8, 8);
            
            TextView textView = new TextView(this);
            textView.setText("物品ID: " + item.replace(":", " 数量 "));
            textView.setTextColor(getResources().getColor(R.color.text_secondary));
            textView.setTextSize(12);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            textView.setLayoutParams(textParams);
            
            Button deleteBtn = new Button(this);
            deleteBtn.setText("删除");
            deleteBtn.setTextSize(10);
            deleteBtn.setTextColor(getResources().getColor(R.color.white));
            deleteBtn.setBackgroundResource(R.drawable.bg_button_danger);
            deleteBtn.setMinHeight(32);
            deleteBtn.setPadding(8, 0, 8, 0);
            
            final int index = i;
            deleteBtn.setOnClickListener(v -> {
                items.remove(index);
                updateAddedItemsView(container, textNoItems, items);
            });
            
            itemLayout.addView(textView);
            itemLayout.addView(deleteBtn);
            container.addView(itemLayout);
        }
    }

    private void showDatePicker(Calendar calendar, Button button) {
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    button.setText(formatDate(calendar));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private String formatDate(Calendar calendar) {
        return calendar.get(Calendar.YEAR) + "-" + 
                String.format("%02d", calendar.get(Calendar.MONTH) + 1) + "-" + 
                String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
    }

    private String getConditionTypeName(String type) {
        switch (type) {
            case "keyword": return "关键词";
            case "amount": return "金额";
            case "tag": return "权限标签";
            case "level": return "等级";
            case "playtime": return "在线时间";
            case "new_player": return "新玩家";
            case "server_time": return "时间条件";
            case "item": return "背包物品";
            case "vip": return "会员";
            default: return type;
        }
    }

    private void showSecondaryConditionDialog(String conditionType, List<AutoExecuteRule.Condition> secondaryConditions, Runnable onUpdate) {
        if ("server_time".equals(conditionType)) {
            showServerTimeDialog(secondaryConditions, onUpdate);
            return;
        }
        
        if ("item".equals(conditionType)) {
            showItemConditionDialog(secondaryConditions, onUpdate);
            return;
        }
        
        if ("vip".equals(conditionType)) {
            String[] options = {"是会员", "不是会员"};
            new AlertDialog.Builder(this)
                    .setTitle("设置会员条件")
                    .setItems(options, (dialog, which) -> {
                        String value = which == 0 ? "yes" : "no";
                        secondaryConditions.add(new AutoExecuteRule.Condition("vip", "eq", value));
                        onUpdate.run();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_condition_setting, null);
        builder.setView(view);
        builder.setTitle("设置" + getConditionTypeName(conditionType) + "条件");
        
        AlertDialog dialog = builder.create();
        
        LinearLayout containerOperator = view.findViewById(R.id.container_operator);
        Spinner spinnerOperator = view.findViewById(R.id.spinner_operator);
        LinearLayout containerValue = view.findViewById(R.id.container_value);
        TextView textValueLabel = view.findViewById(R.id.text_value_label);
        EditText editValue = view.findViewById(R.id.edit_condition_value);
        LinearLayout containerItem = view.findViewById(R.id.container_item);
        LinearLayout containerServerTime = view.findViewById(R.id.container_server_time);
        Button btnConfirm = view.findViewById(R.id.btn_confirm_condition);
        
        containerItem.setVisibility(View.GONE);
        containerServerTime.setVisibility(View.GONE);
        
        List<String> operators = new ArrayList<>();
        operators.add("等于或者大于");
        operators.add("等于");
        operators.add("大于");
        operators.add("小于");
        operators.add("等于或者小于");
        editValue.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        textValueLabel.setText(getConditionTypeName(conditionType) + "值：");
        
        ArrayAdapter<String> operatorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, operators);
        operatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOperator.setAdapter(operatorAdapter);
        
        btnConfirm.setOnClickListener(v -> {
            String value = editValue.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(this, "请输入" + getConditionTypeName(conditionType) + "值", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String[] otherOps = {"gte", "eq", "gt", "lt", "lte"};
            String operator = otherOps[spinnerOperator.getSelectedItemPosition()];
            
            secondaryConditions.add(new AutoExecuteRule.Condition(conditionType, operator, Double.parseDouble(value)));
            
            dialog.dismiss();
            onUpdate.run();
        });
        
        dialog.show();
    }

    private void saveRule(AutoExecuteRule rule, boolean isUpdate, AlertDialog dialog) {
        ApiClient.ApiCallback callback = new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            Toast.makeText(AutoCommandActivity.this, isUpdate ? "更新成功" : "保存成功", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadRules();
                        } else {
                            Toast.makeText(AutoCommandActivity.this, (isUpdate ? "更新" : "保存") + "失败: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(AutoCommandActivity.this, (isUpdate ? "更新" : "保存") + "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(AutoCommandActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        };
        
        if (isUpdate) {
            apiClient.updateAutoExecuteRule(rule, callback);
        } else {
            try {
                apiClient.createAutoExecuteRule(rule.toJson(), callback);
            } catch (Exception e) {
                Toast.makeText(this, "创建失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onEdit(AutoExecuteRule rule, int position) {
        showRuleDialog(rule);
    }

    @Override
    public void onDelete(AutoExecuteRule rule, int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除规则")
                .setMessage("确定要删除此规则吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteRule(rule, position))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteRule(AutoExecuteRule rule, int position) {
        apiClient.deleteAutoExecuteRule(rule.getId(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success")) {
                            rules.remove(position);
                            adapter.setRules(rules);
                            updateEmptyView();
                            Toast.makeText(AutoCommandActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AutoCommandActivity.this, json.optString("message", "删除失败"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(AutoCommandActivity.this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(AutoCommandActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onToggleEnabled(AutoExecuteRule rule, int position, boolean enabled) {
        rule.setEnabled(enabled);
        
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId() == rule.getId()) {
                rules.get(i).setEnabled(enabled);
                break;
            }
        }
        
        apiClient.updateAutoExecuteRule(rule, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (!json.optBoolean("success")) {
                            rule.setEnabled(!enabled);
                            for (int i = 0; i < rules.size(); i++) {
                                if (rules.get(i).getId() == rule.getId()) {
                                    rules.get(i).setEnabled(!enabled);
                                    break;
                                }
                            }
                            adapter.notifyItemChanged(position);
                            Toast.makeText(AutoCommandActivity.this, json.optString("message", "更新失败"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        rule.setEnabled(!enabled);
                        for (int i = 0; i < rules.size(); i++) {
                            if (rules.get(i).getId() == rule.getId()) {
                                rules.get(i).setEnabled(!enabled);
                                break;
                            }
                        }
                        adapter.notifyItemChanged(position);
                        Toast.makeText(AutoCommandActivity.this, "更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    rule.setEnabled(!enabled);
                    for (int i = 0; i < rules.size(); i++) {
                        if (rules.get(i).getId() == rule.getId()) {
                            rules.get(i).setEnabled(!enabled);
                            break;
                        }
                    }
                    adapter.notifyItemChanged(position);
                    Toast.makeText(AutoCommandActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
