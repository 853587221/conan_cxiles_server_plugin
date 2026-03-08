package com.rconclient.model;

import org.json.JSONObject;

public class Category {
    private int id;
    private String name;
    private String description;
    private String icon;
    private String key;
    private int sortOrder;
    private int productCount;
    
    public Category() {}
    
    public static Category fromJson(JSONObject json) {
        Category category = new Category();
        category.id = json.optInt("id");
        category.name = json.optString("name");
        category.description = json.optString("description");
        category.icon = json.optString("icon");
        category.key = json.optString("key");
        category.sortOrder = json.optInt("sort_order");
        category.productCount = json.optInt("product_count");
        return category;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }
}
