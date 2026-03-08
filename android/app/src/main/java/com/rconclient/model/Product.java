package com.rconclient.model;

import org.json.JSONObject;

public class Product {
    private int id;
    private String name;
    private String description;
    private String image;
    private String categoryKey;
    private double price;
    private int sortOrder;
    
    public Product() {}
    
    public static Product fromJson(JSONObject json) {
        Product product = new Product();
        product.id = json.optInt("id");
        product.name = json.optString("name");
        product.description = json.optString("description");
        product.image = json.optString("image");
        product.categoryKey = json.optString("category_key");
        product.price = json.optDouble("price");
        product.sortOrder = json.optInt("sort_order");
        return product;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getCategoryKey() { return categoryKey; }
    public void setCategoryKey(String categoryKey) { this.categoryKey = categoryKey; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
