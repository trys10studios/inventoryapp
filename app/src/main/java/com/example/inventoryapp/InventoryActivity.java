package com.example.inventoryapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryActivity extends AppCompatActivity implements SmsSender {
    private InventoryDatabase inventoryDatabase;
    private InventoryAdapter inventoryAdapter;
    private List<InventoryItem> itemList;
    private SessionManager sessionManager; // Add this to manage session

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);

        // Initialize the database and session manager
        inventoryDatabase = new InventoryDatabase(this);
        sessionManager = new SessionManager(this);

        checkSmsPermission(); // Check SMS permission

        // Initialize the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch inventory items from the database
        itemList = inventoryDatabase.getAllInventoryItems(); // Store it in the member variable
        inventoryAdapter = new InventoryAdapter(itemList, inventoryDatabase, this, this);
        recyclerView.setAdapter(inventoryAdapter);

        // Handle the Add Button click
        Button addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> showAddItemDialog());

        // Initialize the logout button and its click listener
        Button logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {
        sessionManager.logoutUser();
        Intent intent = new Intent(this, MainActivity.class); // Redirect to MainActivity after logging out
        startActivity(intent);
        finish(); // Finish the current activity
    }

    private void showAddItemDialog() {
        // Create an alert dialog to input new item details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Item");

        // Set the dialog view with custom layout (you'll create this layout)
        final View customLayout = getLayoutInflater().inflate(R.layout.add_item, null);
        builder.setView(customLayout);

        // Set up the buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            // Get user input
            EditText itemNameInput = customLayout.findViewById(R.id.item_name_input);
            EditText itemQuantityInput = customLayout.findViewById(R.id.quantity_count);
            EditText itemDescriptionInput = customLayout.findViewById(R.id.item_description_input);

            String name = itemNameInput.getText().toString();
            String quantityString = itemQuantityInput.getText().toString();
            String description = itemDescriptionInput.getText().toString();

            try {
                int quantity = Integer.parseInt(quantityString);

                // Create new InventoryItem and insert it into the database
                InventoryItem newItem = new InventoryItem(name, 0, quantity, description); // id zero due to auto-increment
                inventoryDatabase.insertInventoryItem(newItem);

                // Refresh the list in the RecyclerView
                itemList.add(newItem);
                inventoryAdapter.notifyItemInserted(itemList.size() - 1);
            } catch (NumberFormatException e) {
                Toast.makeText(InventoryActivity.this, "Please enter valid numeric values for ID and Quantity.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void checkSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        } else {
            retrievePhoneNumber(); // Call the method to retrieve phone number
        }
    }

    @Override
    public void sendSms(String message) {
        String phoneNumber = getPhoneNumber(); // Implement this method to retrieve the phone number.
        if (phoneNumber != null) {
            SmsManager smsManager = SmsManager.getDefault();
            PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE);
            smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, null);
            Toast.makeText(this, "SMS sent: " + message, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Phone number not available!", Toast.LENGTH_SHORT).show();
        }
    }

    private String getPhoneNumber() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("phoneNumber", null); // Assuming "phoneNumber" is the key
    }
    private void savePhoneNumber(String phoneNumber) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("phoneNumber", phoneNumber); // Save the phone number with this key
        editor.apply();
    }
    private void retrievePhoneNumber() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            String phoneNumber = telephonyManager.getLine1Number();
            Log.d("InventoryActivity", "Retrieved phone number: " + phoneNumber);
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                savePhoneNumber(phoneNumber); // Save the phone number for later use
                Toast.makeText(this, "Phone number retrieved: " + phoneNumber, Toast.LENGTH_SHORT).show();
            } else {
                Log.d("InventoryActivity", "Phone number is null or empty");
                Toast.makeText(this, "Unable to retrieve phone number", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d("InventoryActivity", "Permission to read phone state is denied");
            Toast.makeText(this, "Permission to read phone state is required", Toast.LENGTH_SHORT).show();
        }
    }
}
