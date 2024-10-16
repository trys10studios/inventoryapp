package com.example.inventoryapp;

public class InventoryItem {
    private String itemName;
    private int id;
    private int quantity;
    private String description;

    // Constructor
    public InventoryItem(String itemName, int id, int quantity, String description) {
        this.itemName = itemName;
        this.id = id;
        this.quantity = quantity;
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
    public String getItemDescription() {
        return description;
    }
}
