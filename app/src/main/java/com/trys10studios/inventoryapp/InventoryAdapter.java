package com.trys10studios.inventoryapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

        // Edit Button functionality
        holder.editButton.setOnClickListener(v -> showEditDialog(currentItem, position));
    }

    private void showEditDialog(InventoryItem item, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Item");

        // Inflate custom dialog layout
        View view = LayoutInflater.from(context).inflate(R.layout.add_item, null);
        builder.setView(view);

        // Get references to input fields
        EditText nameInput = view.findViewById(R.id.item_name_input);
        EditText descriptionInput = view.findViewById(R.id.item_description_input);

        // Populate fields with current item data
        nameInput.setText(item.getItemName());
        descriptionInput.setText(item.getItemDescription());

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Get updated values
            String newName = nameInput.getText().toString();
            String newDescription = descriptionInput.getText().toString();

            // Update item
            item.setItemName(newName);
            item.setDescription(newDescription);

            // Update database
            inventoryDatabase.updateInventoryItem(item);

            // Notify adapter that item has changed
            notifyItemChanged(position);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.create().show();
    }


    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView itemName, itemQuantity, itemDescription, itemID;
        public ImageButton increaseButton, decreaseButton, deleteButton;
        public Button fullViewButton, editButton;

        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemID = itemView.findViewById(R.id.id_number);
            itemQuantity = itemView.findViewById(R.id.quantity_count);
            itemDescription = itemView.findViewById(R.id.item_description);
            increaseButton = itemView.findViewById(R.id.increase_button);
            decreaseButton = itemView.findViewById(R.id.decrease_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            fullViewButton = itemView.findViewById(R.id.full_view_button);
            editButton = itemView.findViewById(R.id.edit_button);
        }
    }

    // Method to retrieve the user's phone number
    public String getPhoneNumber() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("default_phone_number", null); // Returns null if not found
    }
}
