package com.example.inventoryapp;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {
    private List<InventoryItem> inventoryList;

    // Required methods like onCreateViewHolder, onBindViewHolder, and getItemCount
    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        public TextView itemName, itemQuantity, itemDescription;
        public InventoryViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            itemQuantity = itemView.findViewById(R.id.quantity_count);
            itemDescription = itemView.findViewById(R.id.item_description);
        }
    }
}
