package com.example.inventoryapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private List<InventoryItem> itemList;

    public InventoryAdapter(List<InventoryItem> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = itemList.get(position);
        holder.itemName.setText(item.getItemName());
        holder.itemID.setText(String.valueOf(item.getItemId()));
        holder.itemQuantity.setText(String.valueOf(item.getItemQuantity()));
        holder.itemDescription.setText(item.getItemDescription());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView itemName;
        public TextView itemID;
        public TextView itemQuantity;
        public TextView itemDescription;

        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemID = itemView.findViewById(R.id.id_number);
            itemQuantity = itemView.findViewById(R.id.quantity_count);
            itemDescription = itemView.findViewById(R.id.item_description);
        }
    }
}
