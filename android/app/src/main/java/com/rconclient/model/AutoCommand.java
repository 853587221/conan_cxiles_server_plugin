package com.rconclient.model;

import java.util.List;

public class AutoCommand {
    private int id;
    private String name;
    private List<Condition> conditions;
    private String executeType;
    private String command;
    private String commandCategory;
    private String amountOperation;
    private double amountValue;
    private String setTag;
    private String notificationMessage;
    private boolean enabled;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public String getExecuteType() {
        return executeType;
    }

    public void setExecuteType(String executeType) {
        this.executeType = executeType;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommandCategory() {
        return commandCategory;
    }

    public void setCommandCategory(String commandCategory) {
        this.commandCategory = commandCategory;
    }

    public String getAmountOperation() {
        return amountOperation;
    }

    public void setAmountOperation(String amountOperation) {
        this.amountOperation = amountOperation;
    }

    public double getAmountValue() {
        return amountValue;
    }

    public void setAmountValue(double amountValue) {
        this.amountValue = amountValue;
    }

    public String getSetTag() {
        return setTag;
    }

    public void setSetTag(String setTag) {
        this.setTag = setTag;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static class Condition {
        private String type;
        private String operator;
        private String value;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
