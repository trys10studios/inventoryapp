package com.trys10studios.inventoryapp;

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
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity implements NotificationHandler {
    private InventoryDatabase inventoryDatabase;
    private InventoryAdapter inventoryAdapter;
    private List<InventoryItem> itemList;
    private SessionManager sessionManager; // Add this to manage session
    private static final String CHANNEL_ID = "inventory_notifications";
    private static final int REQUEST_CODE_PHONE_STATE = 1;
    private static final int REQUEST_CODE_SMS = 2; // Add this constant for SMS permission
    private List<InventoryItem> filteredItemList;  // Add a filtered list for search results

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);

        // Initialize AppController with this activity as the notification handler
        AppController appController = new AppController(this, this);

        // Check inventory and notify
        appController.checkInventoryAndNotify();

        // Initialize the database and session manager
        inventoryDatabase = new InventoryDatabase(this);
        sessionManager = new SessionManager(this);

        checkPhoneStatePermission();

        // Initialize the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch inventory items from the database
        itemList = inventoryDatabase.getAllInventoryItems(); // Store it in the member variable
        inventoryAdapter = new InventoryAdapter(itemList, inventoryDatabase, this);
        recyclerView.setAdapter(inventoryAdapter);
        filteredItemList = new ArrayList<>(itemList);  // Initialize the filtered list

        // Handle the Add Button click
        Button addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> showAddItemDialog());

        // Initialize the logout button and its click listener
        Button logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> logoutUser());

        checkNotificationPermission();
        checkSmsPermission();

        // For top of screen notifications
        createNotificationChannel();

        // Check if a phone number is already saved
        String savedPhoneNumber = getPhoneNumber();
        if (savedPhoneNumber == null) {
            // Try to automatically retrieve the phone number
            retrievePhoneNumber();
        }

        // Set up search functionality
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Optional: You can handle query submission here if needed
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter the inventory items based on the search query
                filterItems(newText);
                return true;
            }
        });

    }

    // This method filters the items based on the query text
    private void filterItems(String query) {
        // Clear the filtered list and add the matching items
        filteredItemList.clear();
        if (query.isEmpty()) {
            filteredItemList.addAll(itemList);  // If the query is empty, show all items
        } else {
            for (InventoryItem item : itemList) {
                if (item.getItemName().toLowerCase().contains(query.toLowerCase())) {
                    filteredItemList.add(item);  // Add the items that match the query
                }
            }
        }
        // Update the adapter's list and notify the RecyclerView
        inventoryAdapter.updateItemList(filteredItemList);
        // Notify the adapter that the dataset has changed
        inventoryAdapter.notifyDataSetChanged();
    }

    private void promptForPhoneNumber() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Phone Number");

        // Input field for phone number
        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String phoneNumber = input.getText().toString().trim(); // Trim spaces
            if (!phoneNumber.isEmpty()) {
                savePhoneNumber(phoneNumber);  // Save phone number in SharedPreferences
                Toast.makeText(this, "Phone number saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a valid phone number!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void logoutUser() {
        sessionManager.logoutUser();
        Intent intent = new Intent(this, MainActivity.class); // Redirect to MainActivity after logging out
        startActivity(intent);
        finish(); // Finish the current activity, built-in Android method for closing the activity
    }

    private void showAddItemDialog() {
        // Create an alert dialog to input new item details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Item");

        // Set the dialog view with custom layout
        final View customLayout = getLayoutInflater().inflate(R.layout.add_item, null);
        builder.setView(customLayout);

        // Set up the buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            // Get user input
            EditText itemNameInput = customLayout.findViewById(R.id.item_name_input);
            EditText itemQuantityInput = customLayout.findViewById(R.id.quantity_count);
            EditText itemDescriptionInput = customLayout.findViewById(R.id.item_description_input);
            EditText itemCategoryInput = customLayout.findViewById(R.id.category_input);
            EditText itemPrice = customLayout.findViewById(R.id.price_input);

            String name = itemNameInput.getText().toString();
            String quantityString = itemQuantityInput.getText().toString();
            String description = itemDescriptionInput.getText().toString();
            String category = itemDescriptionInput.getText().toString();
            String priceString = itemDescriptionInput.getText().toString();

            try {
                int quantity = Integer.parseInt(quantityString);
                int price = Integer.parseInt(priceString);

                // Create new InventoryItem and insert it into the database
                InventoryItem newItem = new InventoryItem(name, 0, quantity, category, price, description); // id zero due to auto-increment
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE_SMS);
        }
    }

    @Override
    public void sendSms(String message) {
        // Implement the SMS sending logic here
        String phoneNumber = getPhoneNumber(); // Retrieve the phone number from SharedPreferences
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
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
        return sharedPreferences.getString("phoneNumber", null); // Retrieve the phone number
    }

    private void savePhoneNumber(String phoneNumber) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("phoneNumber", phoneNumber); // Save the phone number with this key
        editor.apply();
    }

    private void retrievePhoneNumber() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            String phoneNumber = telephonyManager.getLine1Number();  // Try to retrieve the phone number

            Log.d("InventoryActivity", "Retrieved phone number: " + phoneNumber); // Add this line

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                savePhoneNumber(phoneNumber);  // Save phone number if available
                Toast.makeText(this, "Phone number retrieved: " + phoneNumber, Toast.LENGTH_SHORT).show();
            } else {
                // Phone number not available, fall back to manual input
                promptForPhoneNumber();
            }
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PHONE_STATE);
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
        if (requestCode == REQUEST_CODE_PHONE_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try retrieving phone number again
                retrievePhoneNumber();
            } else {
                // Permission denied, fall back to manual input
                Toast.makeText(this, "Permission denied. Please enter your phone number manually.", Toast.LENGTH_SHORT).show();
                promptForPhoneNumber();
            }
        }
    }

    private void checkPhoneStatePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PHONE_STATE);
        } else {
            // Permission granted
            retrievePhoneNumber();
        }
    }
    public void sendNotification(String itemName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "inventory_notifications"; // Ensure this matches channel ID

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification) // Notification Icon
                .setContentTitle("Inventory Alert")
                .setContentText(itemName + " is at zero stock!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true); // Dismiss the notification when tapped

        // Show the notification
        notificationManager.notify(1, builder.build()); // Unique ID for each notification
    }
}
