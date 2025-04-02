package com.trys10studios.inventoryapp;

public class InventoryItem {
    private String itemName;
    private int id;
    private int quantity;
    private int price;
    private String category;
    private String description;

    // Constructor
    public InventoryItem(String itemName, int id, int quantity, String category, int price, String description) {
        this.itemName = itemName;
        this.id = id;
        this.quantity = quantity;
        this.category = category;
        this.price = price;
        this.description = description;
    }

    // Setters
    public void setItemName(String newItemName){
        itemName = newItemName;
    }

    public void setId(int newId) {
        id = newId;
    }

    public void setQuantity(int newQuantity){
        quantity = newQuantity;
    }
    public void setCategory(String newCategory) { category = newCategory; }
    public void setPrice(int newPrice){
        quantity = newPrice;
    }
    public void setDescription(String newDescription){
        description = newDescription;
    }

    // Getters
    public String getItemName() {
        return itemName;
    }
    public int getItemId(){ return id; }
    public int getItemQuantity() {
        return quantity;
    }
    public String getItemCategory() { return category; }
    public int getItemPrice() {return price; }
    public String getItemDescription() {
        return description;
    }
}
