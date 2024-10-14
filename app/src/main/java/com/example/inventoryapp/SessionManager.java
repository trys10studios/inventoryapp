package com.example.inventoryapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createLoginSession(String username) {
        editor.putString("username", username);
        editor.commit();
    }

    public String getLoggedInUser() {
        return prefs.getString("username", null);
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}
