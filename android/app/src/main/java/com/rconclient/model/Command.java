package com.rconclient.model;

import org.json.JSONObject;

public class Command {
    private int id;
    private String name;
    private String description;
    private String category;
    private String example;
    
    public Command() {}
    
    public static Command fromJson(JSONObject json) {
        Command command = new Command();
        command.id = json.optInt("id");
        command.name = json.optString("name");
        command.description = json.optString("description");
        command.category = json.optString("category");
        command.example = json.optString("example");
        return command;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }
}
