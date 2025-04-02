package com.trys10studios.inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// InventoryDatabase.java
public class InventoryDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventory.db";
    private static final int VERSION = 1;

    // Constructor
    public InventoryDatabase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the "inventory" table
        db.execSQL("CREATE TABLE inventory (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, quantity INTEGER, sku TEXT, category TEXT, price INTEGER, description TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop the table if it exists and recreate it
        db.execSQL("DROP TABLE IF EXISTS inventory");
        onCreate(db);
    }

    // Method to add a new inventory item
    public void insertInventoryItem(InventoryItem newItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newItem.getItemName());
        values.put("sku", newItem.getSku());
        values.put("quantity", newItem.getItemQuantity());
        values.put("category", newItem.getItemCategory());
        values.put("price", newItem.getItemPrice());
        values.put("description", newItem.getItemDescription());

        // Insert into the database and get the new row's id
        long newId = db.insert("inventory", null, values);

        // Set the auto-generated id back to the InventoryItem object
        newItem.setId((int) newId);
    }

    // Method to retrieve all inventory items
    public List<InventoryItem> getAllInventoryItems() {
        List<InventoryItem> itemList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM inventory", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                String sku = cursor.getString(cursor.getColumnIndexOrThrow("sku"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                int price = cursor.getInt(cursor.getColumnIndexOrThrow("price"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

                InventoryItem item = new InventoryItem(name, id, quantity, sku, category, price, description);
                itemList.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return itemList;
    }

    // Method to delete an inventory item by ID
    public void deleteInventoryItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("inventory", "_id = ?", new String[]{String.valueOf(id)});
    }

    // Method to update an inventory item
    public void updateInventoryItem(InventoryItem updatedItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", updatedItem.getItemName());
        values.put("sku", updatedItem.getSku());
        values.put("quantity", updatedItem.getItemQuantity());
        values.put("category", updatedItem.getItemCategory());
        values.put("price", updatedItem.getItemPrice());
        values.put("description", updatedItem.getItemDescription());

        db.update("inventory", values, "_id = ?", new String[]{String.valueOf(updatedItem.getItemId())});
    }

    // Method to check inventory and notify if any item is zero
    public void checkInventoryAndNotify(NotificationHandler notificationHandler) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM inventory WHERE quantity = 0", null);

        if (cursor.moveToFirst()) {
            // If there's at least one item with zero quantity
            String message = "Alert: Some items in your inventory are at zero stock!";
            notificationHandler.sendSms(message); // Use notification handler to send notification
        }

        cursor.close();
    }
    public List<InventoryItem> searchInventoryItems(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<InventoryItem> resultList = new ArrayList<>();

        // Simple query to search in name or description
        String selection = "name LIKE ? OR description LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};

        Cursor cursor = db.query(DATABASE_NAME, null, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String sku = cursor.getString(cursor.getColumnIndexOrThrow("sku"));
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                int price = cursor.getInt(cursor.getColumnIndexOrThrow("price"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

                // Add matching item to result list
                resultList.add(new InventoryItem(name, 0, quantity, sku, category, price, description)); // Assuming you have constructor
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return resultList;
    }

}
