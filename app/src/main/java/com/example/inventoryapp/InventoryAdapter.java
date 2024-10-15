package com.example.inventoryapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {
    private List<InventoryItem> inventoryList;

    // Constructor to initialize the inventory list
    public InventoryAdapter(List<InventoryItem> inventoryItems) {
        this.inventoryList = inventoryItems;
    }

    // Inflate the layout for individual items in the RecyclerView
    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_data_display, parent, false); // Ensure this layout exists
        return new InventoryViewHolder(view);
    }

    // Bind the data to the UI elements in the ViewHolder
    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem currentItem = inventoryList.get(position);
        holder.itemName.setText(currentItem.getItemName());
        holder.itemQuantity.setText(String.valueOf(currentItem.getItemQuantity()));
        holder.itemDescription.setText(currentItem.getItemDescription());
    }

    // Return the total number of items
    @Override
    public int getItemCount() {
        return inventoryList.size();
    }

    // ViewHolder class for managing individual item views
    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        public TextView itemName, itemQuantity, itemDescription;

        public InventoryViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name); // Match the IDs in your XML layout
            itemQuantity = itemView.findViewById(R.id.quantity_count);
            itemDescription = itemView.findViewById(R.id.item_description);
        }
    }
}
