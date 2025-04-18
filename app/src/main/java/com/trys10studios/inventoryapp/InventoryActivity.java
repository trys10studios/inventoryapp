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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

public class InventoryActivity extends AppCompatActivity implements NotificationHandler, InventoryAdapter.OnItemEditedListener {
    private InventoryDatabase inventoryDatabase;
    private InventoryAdapter inventoryAdapter;
    private List<InventoryItem> itemList;
    private SessionManager sessionManager; // Add this to manage session
    private static final String CHANNEL_ID = "inventory_notifications";
    private static final int REQUEST_CODE_PHONE_STATE = 1;
    private static final int REQUEST_CODE_SMS = 2; // Add this constant for SMS permission
    private List<InventoryItem> filteredItemList;  // Add a filtered list for search results
    ArrayAdapter<String> adapter;

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
        inventoryAdapter = new InventoryAdapter(itemList, inventoryDatabase, this, this);
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

        // Declare necessary variables
        Spinner spinnerSearchField = findViewById(R.id.spinnerSearchField);
        // Set up search functionality
        SearchView searchView = findViewById(R.id.search_view);

        // Set up the Spinner (for Category, Price, etc.)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"All", "Category", "Price", "Name", "Quantity", "Description"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchField.setAdapter(adapter);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Optional: Handle query submission here if needed
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Get the selected filter type from the Spinner
                String selectedFilter = spinnerSearchField.getSelectedItem().toString();

                // If the query is empty and "All" is selected, show all items
                if (newText.isEmpty() && selectedFilter.equals("All")) {
                    showAllItems(); // Show all items
                } else {
                    // Filter the inventory items based on the search query and selected filter type
                    filterItems(newText, selectedFilter);
                }
                return true;
            }
        });
        spinnerSearchField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFilter = spinnerSearchField.getSelectedItem().toString();
                String currentQuery = searchView.getQuery().toString().trim(); // Get the current text

                if (currentQuery.isEmpty() && selectedFilter.equals("All")) {
                    showAllItems();
                } else {
                    filterItems(currentQuery, selectedFilter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void showAllItems() {
        // Assuming 'itemList' contains all inventory items
        itemList = inventoryDatabase.getAllInventoryItems();
        inventoryAdapter.updateItemList(itemList);  // Update the RecyclerView or list adapter with all items
    }

    // The method to filter items based on both the query and filter type
    private void filterItems(String query, String filterType) {
        // List of inventory items to be filtered
        List<InventoryItem> filteredItems = new ArrayList<>();

        for (InventoryItem item : itemList) {
            boolean matches = false;

            // Apply filtering logic based on the filter type
            switch (filterType) {
                case "Category":
                    if (item.getItemCategory().toLowerCase().contains(query.toLowerCase())) {
                        matches = true;
                    }
                    break;
                case "Price":
                    try {
                        double price = Double.parseDouble(query);
                        if (item.getItemPrice() == price) {
                            matches = true;
                        }
                    } catch (NumberFormatException e) {
                        // Handle invalid price format
                    }
                    break;
                case "Name":
                    if (item.getItemName().toLowerCase().contains(query.toLowerCase())) {
                        matches = true;
                    }
                    break;
                case "Quantity":
                    try {
                        int quantity = Integer.parseInt(query);
                        if (item.getItemQuantity() == quantity) {
                            matches = true;
                        }
                    } catch (NumberFormatException e) {
                        // Handle invalid quantity format
                    }
                    break;
                case "Description":
                    if (item.getItemDescription().toLowerCase().contains(query.toLowerCase())) {
                        matches = true;
                    }
                    break;
                case "All":
                default:
                    // If "All" is selected, check all fields
                    if (item.getItemCategory().toLowerCase().contains(query.toLowerCase()) ||
                            item.getItemName().toLowerCase().contains(query.toLowerCase()) ||
                            item.getItemDescription().toLowerCase().contains(query.toLowerCase())) {
                        matches = true;
                    }
                    break;
            }

            if (matches) {
                filteredItems.add(item);
            }
        }

        // Update the displayed items and notify adapter
        inventoryAdapter.updateItemList(filteredItems);
    }

    // This method filters the items based on the category
    private void filterInventory(String category) {
        List<InventoryItem> filteredList = new ArrayList<>();

        for (InventoryItem item : itemList) {
            if (category.equals("All") || item.getItemCategory().equals(category)) {
                filteredList.add(item);
            }
        }

        // Update RecyclerView Adapter
        inventoryAdapter.updateItemList(filteredList);
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
            EditText itemSKUInput = customLayout.findViewById(R.id.sku_input);
            EditText itemQuantityInput = customLayout.findViewById(R.id.quantity_count);
            EditText itemCategoryInput = customLayout.findViewById(R.id.category_input);
            EditText itemPriceInput = customLayout.findViewById(R.id.price_input);
            EditText itemDescriptionInput = customLayout.findViewById(R.id.item_description_input);

            String nameString = itemNameInput.getText().toString();
            String skuString = itemSKUInput.getText().toString();
            String quantityString = itemQuantityInput.getText().toString();
            String categoryString = itemCategoryInput.getText().toString();
            String priceString = itemPriceInput.getText().toString();
            String descriptionString = itemDescriptionInput.getText().toString();

            try {
                int quantity = Integer.parseInt(quantityString);
                int price = Integer.parseInt(priceString);

                // Create new InventoryItem and insert it into the database
                InventoryItem newItem = new InventoryItem(nameString, 0, quantity, skuString, categoryString, price, descriptionString); // id zero due to auto-increment
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
    @Override
    public void onItemEdited() {
        refreshData();  // Reload UI when an item is edited
    }

    public void refreshData() {
        List<InventoryItem> newItemList = inventoryDatabase.getAllInventoryItems();
        inventoryAdapter.updateItemList(newItemList);
        adapter.notifyDataSetChanged();
    }
}
