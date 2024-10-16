package com.example.inventoryapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryActivity extends AppCompatActivity implements NotificationHandler {
    private InventoryDatabase inventoryDatabase;
    private InventoryAdapter inventoryAdapter;
    private List<InventoryItem> itemList;
    private SessionManager sessionManager; // Add this to manage session
    private static final String CHANNEL_ID = "inventory_notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);

        // Initialize AppController with this activity as the notification handler
        AppController appController = new AppController(this, this);

        // Example usage: checking inventory and notifying
        appController.checkInventoryAndNotify();

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

        // For top of screen notifications
        createNotificationChannel();

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
        // Implement the SMS sending logic here
        String phoneNumber = getPhoneNumber(); // Implement this method to retrieve the phone number
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
    private void createNotificationChannel() {
        CharSequence name = "Inventory Notifications";
        String description = "Channel for inventory notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
    @Override
    public void sendNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ensure the icon exists in the res/drawable directory
                .setContentTitle("Inventory Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true); // Automatically remove the notification when clicked

        // Intent for opening the app when the notification is clicked
        Intent intent = new Intent(this, InventoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        // Get the NotificationManager service
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Send the notification
        notificationManager.notify(1, builder.build()); // Notification ID can be any unique number
    }
    private void checkNotificationPermission() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && !notificationManager.areNotificationsEnabled()) {
            // Show a Toast or a splash image prompting the user to enable notifications
            Toast.makeText(this, "Please enable notifications to receive alerts!", Toast.LENGTH_LONG).show();

            // Optionally, redirect the user to app settings
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, check for notifications
                checkNotificationPermission();
            } else {
                Toast.makeText(this, "SMS permission is required!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
