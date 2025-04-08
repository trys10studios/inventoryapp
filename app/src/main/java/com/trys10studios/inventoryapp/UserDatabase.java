package com.trys10studios.inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class UserDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "users.db";
    private static final int VERSION = 1;
    private Context context; // Add context variable

    public UserDatabase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        this.context = context; // Initialize context
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the "users" table
        db.execSQL("CREATE TABLE users (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT, password TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop the table if it exists and recreate it
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // Method to add a new user
    public boolean addUser(String username, String password) {
        // Check if the user already exists
        if (isUserExists(username)) {
            return false; // User already exists, return false
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        long result = db.insert("users", null, values);
        return result != -1; // Returns true if the user was successfully added
    }

    // Method to check if the user exists and perform login
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username = ? AND password = ?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();

        if (exists) {
            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(context, "Username and/or password not valid", Toast.LENGTH_SHORT).show();
        }

        return exists; // Return whether the user exists or not
    }

    // Method to check if a user exists by username
    public boolean isUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0; // Returns true if there is at least one user with the provided username
        cursor.close();
        return exists; // Return whether the user exists or not
    }
}
