package com.rconclient.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AutoExecuteRule {
    private int id;
    private String ruleName;
    private List<Condition> conditions;
    private List<Condition> secondaryConditions;
    private String executeType;
    private ExecuteData executeData;
    private AfterExecute afterExecute;
    private boolean enabled;
    
    public AutoExecuteRule() {
        conditions = new ArrayList<>();
        secondaryConditions = new ArrayList<>();
    }
    
    public static AutoExecuteRule fromJson(JSONObject json) {
        AutoExecuteRule rule = new AutoExecuteRule();
        rule.id = json.optInt("id");
        rule.ruleName = json.optString("rule_name");
        rule.enabled = json.optBoolean("enabled", true);
        
        JSONArray conditionsArr = json.optJSONArray("conditions");
        if (conditionsArr != null) {
            for (int i = 0; i < conditionsArr.length(); i++) {
                rule.conditions.add(Condition.fromJson(conditionsArr.optJSONObject(i)));
            }
        }
        
        JSONArray secondaryArr = json.optJSONArray("secondary_conditions");
        if (secondaryArr != null) {
            for (int i = 0; i < secondaryArr.length(); i++) {
                rule.secondaryConditions.add(Condition.fromJson(secondaryArr.optJSONObject(i)));
            }
        }
        
        rule.executeType = json.optString("execute_type");
        
        JSONObject executeDataObj = json.optJSONObject("execute_data");
        if (executeDataObj != null) {
            rule.executeData = ExecuteData.fromJson(executeDataObj);
        }
        
        JSONObject afterExecuteObj = json.optJSONObject("after_execute");
        if (afterExecuteObj != null) {
            rule.afterExecute = AfterExecute.fromJson(afterExecuteObj);
        }
        
        return rule;
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("rule_name", ruleName);
            json.put("enabled", enabled);
            json.put("execute_type", executeType);
            
            if (conditions != null && !conditions.isEmpty()) {
                JSONArray conditionsArr = new JSONArray();
                for (Condition c : conditions) {
                    conditionsArr.put(c.toJson());
                }
                json.put("conditions", conditionsArr);
            }
            
            if (secondaryConditions != null && !secondaryConditions.isEmpty()) {
                JSONArray secondaryArr = new JSONArray();
                for (Condition c : secondaryConditions) {
                    secondaryArr.put(c.toJson());
                }
                json.put("secondary_conditions", secondaryArr);
            }
            
            if (executeData != null) {
                json.put("execute_data", executeData.toJson());
            }
            
            if (afterExecute != null) {
                json.put("after_execute", afterExecute.toJson());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public List<Condition> getConditions() { return conditions; }
    public void setConditions(List<Condition> conditions) { this.conditions = conditions; }
    public List<Condition> getSecondaryConditions() { return secondaryConditions; }
    public void setSecondaryConditions(List<Condition> secondaryConditions) { this.secondaryConditions = secondaryConditions; }
    public String getExecuteType() { return executeType; }
    public void setExecuteType(String executeType) { this.executeType = executeType; }
    public ExecuteData getExecuteData() { return executeData; }
    public void setExecuteData(ExecuteData executeData) { this.executeData = executeData; }
    public AfterExecute getAfterExecute() { return afterExecute; }
    public void setAfterExecute(AfterExecute afterExecute) { this.afterExecute = afterExecute; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public static class Condition {
        private String type;
        private String operator;
        private Object value;
        
        public Condition() {}
        
        public Condition(String type, String operator, Object value) {
            this.type = type;
            this.operator = operator;
            this.value = value;
        }
        
        public static Condition fromJson(JSONObject json) {
            Condition c = new Condition();
            c.type = json.optString("type");
            c.operator = json.optString("operator");
            Object val = json.opt("value");
            if (val instanceof Number) {
                c.value = ((Number) val).doubleValue();
            } else {
                c.value = json.optString("value");
            }
            return c;
        }
        
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("type", type);
                json.put("operator", operator);
                json.put("value", value);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return json;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        
        public String getDisplayText() {
            String typeName = getTypeName(type);
            String operatorName = getOperatorName(operator);
            
            if ("new_player".equals(type)) {
                return typeName;
            }
            
            if ("vip".equals(type)) {
                String val = String.valueOf(value);
                if ("yes".equals(val)) {
                    return typeName + ": 是会员";
                } else {
                    return typeName + ": 不是会员";
                }
            }
            
            if ("server_time".equals(type)) {
                if ("weekday".equals(operator)) {
                    String[] weekdayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                    String val = String.valueOf(value);
                    String[] parts = val.split("\\|");
                    if (parts.length > 0) {
                        String[] days = parts[0].split(",");
                        StringBuilder sb = new StringBuilder();
                        for (String d : days) {
                            if (sb.length() > 0) sb.append("、");
                            try {
                                sb.append(weekdayNames[Integer.parseInt(d.trim())]);
                            } catch (Exception e) {
                                sb.append(d);
                            }
                        }
                        return typeName + ": 每周的 " + sb.toString();
                    }
                }
                return typeName + ": " + value;
            }
            
            if ("item".equals(type)) {
                String val = String.valueOf(value);
                String[] items = val.split(";");
                StringBuilder sb = new StringBuilder();
                for (String item : items) {
                    if (sb.length() > 0) sb.append(", ");
                    String[] parts = item.split(":");
                    if (parts.length == 2) {
                        sb.append("物品ID ").append(parts[0]).append(" 数量 ").append(parts[1]);
                    } else {
                        sb.append(item);
                    }
                }
                return typeName + ": " + operatorName + " " + sb.toString();
            }
            
            return typeName + ": " + operatorName + " " + value;
        }
        
        private String getTypeName(String type) {
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
        
        private String getOperatorName(String op) {
            switch (op) {
                case "eq": return "等于";
                case "startsWith": return "开头等于";
                case "contains": return "包含";
                case "notContains": return "不包含";
                case "endsWith": return "结尾等于";
                case "gt": return "大于";
                case "lt": return "小于";
                case "gte": return "等于或者大于";
                case "lte": return "等于或者小于";
                case "interval": return "每隔";
                case "date_range": return "日期范围";
                case "weekday": return "星期";
                default: return op;
            }
        }
    }
    
    public static class ExecuteData {
        private String type;
        private String command;
        private String category;
        
        public static ExecuteData fromJson(JSONObject json) {
            ExecuteData data = new ExecuteData();
            data.type = json.optString("type");
            data.command = json.optString("command");
            data.category = json.optString("category");
            return data;
        }
        
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("type", type);
                json.put("command", command);
                json.put("category", category);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return json;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
    
    public static class AfterExecute {
        private String amountOperation;
        private double amountValue;
        private String setTag;
        private String notificationMessage;
        private String failNotificationMessage;
        
        public static AfterExecute fromJson(JSONObject json) {
            AfterExecute data = new AfterExecute();
            data.amountOperation = json.optString("amountOperation");
            data.amountValue = json.optDouble("amountValue");
            data.setTag = json.optString("setTag");
            data.notificationMessage = json.optString("notificationMessage");
            data.failNotificationMessage = json.optString("failNotificationMessage");
            return data;
        }
        
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                if (amountOperation != null && !amountOperation.isEmpty()) {
                    json.put("amountOperation", amountOperation);
                    json.put("amountValue", amountValue);
                }
                if (setTag != null && !setTag.isEmpty()) {
                    json.put("setTag", setTag);
                }
                if (notificationMessage != null && !notificationMessage.isEmpty()) {
                    json.put("notificationMessage", notificationMessage);
                }
                if (failNotificationMessage != null && !failNotificationMessage.isEmpty()) {
                    json.put("failNotificationMessage", failNotificationMessage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return json;
        }
        
        public String getAmountOperation() { return amountOperation; }
        public void setAmountOperation(String amountOperation) { this.amountOperation = amountOperation; }
        public double getAmountValue() { return amountValue; }
        public void setAmountValue(double amountValue) { this.amountValue = amountValue; }
        public String getSetTag() { return setTag; }
        public void setSetTag(String setTag) { this.setTag = setTag; }
        public String getNotificationMessage() { return notificationMessage; }
        public void setNotificationMessage(String notificationMessage) { this.notificationMessage = notificationMessage; }
        public String getFailNotificationMessage() { return failNotificationMessage; }
        public void setFailNotificationMessage(String failNotificationMessage) { this.failNotificationMessage = failNotificationMessage; }
        
        public String getAmountOperationText() {
            if (amountOperation == null || amountOperation.isEmpty()) return "";
            switch (amountOperation) {
                case "add": return "增加";
                case "deduct": return "扣除";
                case "set": return "强行指定";
                default: return amountOperation;
            }
        }
    }
}
