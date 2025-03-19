package com.trys10studios.inventoryapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private final List<InventoryItem> itemList;
    private final InventoryDatabase inventoryDatabase;
    private final Context context; // Store the context
    private static final String CHANNEL_ID = "inventory_notifications";

    public InventoryAdapter(List<InventoryItem> itemList, InventoryDatabase inventoryDatabase, Context context) {
        this.itemList = itemList;
        this.inventoryDatabase = inventoryDatabase;
        this.context = context; // Initialize the context
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        InventoryItem currentItem = itemList.get(position);

        holder.itemName.setText(currentItem.getItemName());
        holder.itemQuantity.setText(String.valueOf(currentItem.getItemQuantity()));
        holder.itemDescription.setText(currentItem.getItemDescription());
        holder.itemID.setText(String.valueOf(currentItem.getItemId()));

        // Increase button functionality
        holder.increaseButton.setOnClickListener(v -> {
            int newQuantity = currentItem.getItemQuantity() + 1;
            currentItem.setQuantity(newQuantity);

            // Update the database
            inventoryDatabase.updateInventoryItem(currentItem);

            // Notify that the item has changed
            notifyItemChanged(position);
        });

        // Decrease button functionality
        holder.decreaseButton.setOnClickListener(v -> {
            int newQuantity = currentItem.getItemQuantity() - 1;
            if (newQuantity >= 0) {  // Ensure quantity does not go negative
                currentItem.setQuantity(newQuantity);

                // Update the database
                inventoryDatabase.updateInventoryItem(currentItem);

                // Notify that the item has changed
                notifyItemChanged(position);

                // If quantity is zero, send SMS notification and Notification
                if (newQuantity == 0) {
                    ((InventoryActivity) context).sendSms(currentItem.getItemName() + " is at zero stock!");
                    ((InventoryActivity) context).sendNotification(currentItem.getItemName());
                }
            }
        });

        // Delete button functionality
        holder.deleteButton.setOnClickListener(v -> {
            // Remove the item from the list
            itemList.remove(position);

            // Delete the item from the database
            inventoryDatabase.deleteInventoryItem(currentItem.getItemId());

            // Notify that the item is removed
            notifyItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView itemName, itemQuantity, itemDescription, itemID;
        public ImageButton increaseButton, decreaseButton, deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemID = itemView.findViewById(R.id.id_number);
            itemQuantity = itemView.findViewById(R.id.quantity_count);
            itemDescription = itemView.findViewById(R.id.item_description);
            increaseButton = itemView.findViewById(R.id.increaseButton);
            decreaseButton = itemView.findViewById(R.id.decreaseButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    // Method to retrieve the user's phone number
    public String getPhoneNumber() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("default_phone_number", null); // Returns null if not found
    }
}
