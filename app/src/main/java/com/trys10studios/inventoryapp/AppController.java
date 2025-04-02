package com.trys10studios.inventoryapp;

import android.content.Context;
import java.util.List;

public class AppController {

    private final NotificationHandler notificationHandler;
    private UserDatabase userDatabase;
    private InventoryDatabase inventoryDatabase;
    private Context context;
    private InventoryActivity inventoryActivity; // Hold a reference to InventoryActivity

    public AppController(Context context, NotificationHandler notificationHandler) {
        this.context = context;
        this.notificationHandler = notificationHandler; // Store the reference
        userDatabase = new UserDatabase(context); // Initialize user database
        inventoryDatabase = new InventoryDatabase((Context) context); // Initialize inventory database
    }

    /* User Management */

    // Method to sign up a new user
    public boolean signUp(String username, String password) {
        // Check if user already exists
        if (userDatabase.isUserExists(username)) {
            return false; // User already exists
        } else {
            // Insert new user into the database
            userDatabase.addUser(username, password);
            return true; // User created successfully
        }
    }

    // Method to log in a user
    public boolean login(String username, String password) {
        return userDatabase.checkUser(username, password); // Validate user credentials
    }

    /* Inventory Management */

    // Method to add a new inventory item
    public void addInventoryItem(String itemName, int quantity,String category, int price, String description) {
        InventoryItem newItem = new InventoryItem(itemName, 0, quantity, category, price, description); // ID is 0 because it's auto-incremented
        inventoryDatabase.insertInventoryItem(newItem);
    }

    // Method to fetch all inventory items
    public List<InventoryItem> getAllInventoryItems() {
        return inventoryDatabase.getAllInventoryItems(); // Retrieve all inventory items
    }

    // Method to delete an inventory item by id
    public void deleteInventoryItem(int id) {
        inventoryDatabase.deleteInventoryItem(id);
    }

    // Method to update an inventory item
    public void updateInventoryItem(String itemName, int id, int quantity, String category, int price, String description) {
        InventoryItem updatedItem = new InventoryItem(itemName, id, quantity, category, price, description);
        updatedItem.setId(id);
        inventoryDatabase.updateInventoryItem(updatedItem);
    }
    // Method to check inventory and notify
    public void checkInventoryAndNotify() {
        inventoryDatabase.checkInventoryAndNotify(notificationHandler); // Pass notificationHandler
    }
}
