package com.example.inventoryapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private InventoryDatabase inventoryDatabase;
    private RecyclerView recyclerView;
    private InventoryAdapter inventoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);

        // Initialize the database
        inventoryDatabase = new InventoryDatabase(this);

        // Initialize the RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch inventory items from the database
        List<InventoryItem> inventoryItems = inventoryDatabase.getAllInventoryItems();

        // Set up the adapter
        inventoryAdapter = new InventoryAdapter(inventoryItems);
        recyclerView.setAdapter(inventoryAdapter);
    }
}
