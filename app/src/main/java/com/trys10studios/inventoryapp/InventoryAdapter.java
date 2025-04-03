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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private List<InventoryItem> itemList;
    private final InventoryDatabase inventoryDatabase;
    private final Context context; // Store the context
    private static final String CHANNEL_ID = "inventory_notifications";
    private OnItemEditedListener editListener;

    public InventoryAdapter(List<InventoryItem> itemList, InventoryDatabase inventoryDatabase, Context context, OnItemEditedListener editListener) {
        this.itemList = itemList;
        this.inventoryDatabase = inventoryDatabase;
        this.context = context; // Initialize the context
        this.editListener = editListener;
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

        holder.editButton.setOnClickListener(v -> {
            // Open edit dialog (or edit logic)
            inventoryDatabase.updateInventoryItem(currentItem);

            // Notify Activity to refresh the data
            if (editListener != null) {
                editListener.onItemEdited();
            }
        });

        holder.itemName.setText(currentItem.getItemName());
        holder.itemQuantity.setText(String.valueOf(currentItem.getItemQuantity()));
        holder.itemDescription.setText(shortenText(currentItem.getItemDescription()));
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
        holder.fullViewButton.setOnClickListener(v -> showFullScreenDialog(currentItem));
    }

    private void showFullScreenDialog(InventoryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.full_view, null);

        // Initialize UI elements
        TextView itemName = view.findViewById(R.id.full_item_name);
        TextView id = view.findViewById(R.id.full_id_number);
        TextView sku = view.findViewById(R.id.full_sku);
        TextView quantity = view.findViewById(R.id.full_quantity);
        TextView category = view.findViewById(R.id.full_category);
        TextView price = view.findViewById(R.id.full_price);
        TextView description = view.findViewById(R.id.full_description);
        Button closeButton = view.findViewById(R.id.close_button);

        // Set data
        itemName.setText(context.getString(R.string.item) + " " + item.getItemName());
        id.setText(context.getString(R.string.id) + " " + String.valueOf(item.getItemId()));
        sku.setText(context.getString(R.string.sku) + " " + String.valueOf(item.getSku()));
        quantity.setText(context.getString(R.string.quantity) + " " + String.valueOf(item.getItemQuantity()));
        category.setText(context.getString(R.string.category) + " " + String.valueOf(item.getItemCategory()));
        price.setText(context.getString(R.string.price) + " " + String.valueOf(item.getItemPrice()));
        description.setText(context.getString(R.string.description) + " " + item.getItemDescription());

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Set dialog to full screen
        if(dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
        // Set the close button's action
        closeButton.setOnClickListener(v -> {
            // Dismiss the dialog when the close button is clicked
            dialog.dismiss();
        });
    }


    private void showEditDialog(InventoryItem item, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Item");

        // Inflate custom dialog layout
        View view = LayoutInflater.from(context).inflate(R.layout.add_item, null);
        builder.setView(view);

        // Get references to input fields
        EditText nameInput = view.findViewById(R.id.item_name_input);
        EditText sku = view.findViewById(R.id.sku_num);
        EditText quantity = view.findViewById(R.id.quantity_count);
        EditText category = view.findViewById(R.id.category_input);
        EditText price = view.findViewById(R.id.price_input);
        EditText descriptionInput = view.findViewById(R.id.item_description_input);

        // Populate fields with current item data
        nameInput.setText(item.getItemName());
        sku.setText(String.valueOf(item.getSku()));
        quantity.setText(String.valueOf(item.getItemQuantity()));
        category.setText(item.getItemCategory());
        price.setText(String.valueOf(item.getItemPrice()));
        descriptionInput.setText(item.getItemDescription());

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Get updated values
            String newName = nameInput.getText().toString();
            String newSKU = sku.getText().toString();
            String newQuantity = quantity.getText().toString();
            String newCategory = category.getText().toString();
            String newPrice = price.getText().toString();
            String newDescription = descriptionInput.getText().toString();

            // Fetch updated data from the database
            List<InventoryItem> updatedItems = inventoryDatabase.getAllInventoryItems();
            updateItemList(updatedItems);  // Update UI with fresh data

            // Validate quantity
            if (newQuantity.isEmpty()) {
                // Show Toast if quantity is empty
                Toast.makeText(context, "Quantity cannot be empty", Toast.LENGTH_SHORT).show();
                return;  // Exit if quantity is empty
            }

            try {
                Integer.parseInt(newQuantity);  // Try to parse the quantity as an integer
            } catch (NumberFormatException e) {
                // Show Toast if it's not a valid number
                Toast.makeText(context, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
                return;  // Exit if invalid quantity
            }

            // Update item
            item.setItemName(newName);
            item.setSKU(newSKU);
            item.setQuantity(Integer.parseInt(newQuantity));
            item.setDescription(newDescription);
            item.setCategory(newCategory);
            item.setPrice(Integer.parseInt(newPrice));

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

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView itemName, itemQuantity, itemSKU, itemCategory, itemPrice, itemDescription, itemID;
        public ImageButton increaseButton, decreaseButton, deleteButton;
        public Button fullViewButton, editButton;

        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemID = itemView.findViewById(R.id.id_number);
            itemQuantity = itemView.findViewById(R.id.quantity_count);
            itemSKU = itemView.findViewById(R.id.sku);
            itemCategory = itemView.findViewById(R.id.category_name);
            itemPrice = itemView.findViewById(R.id.price_num);
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
    private String shortenText(String string) {
        // Limit Description to 25 chars
        int maxLength = 25;

        if (string.length() > maxLength) {
            string = string.substring(0, maxLength);  // Shorten the text
        }

        return string;  // Return the TextView for chaining or further use
    }
    public void updateItemList(List<InventoryItem> newItemList) {
        this.itemList = newItemList;
        notifyDataSetChanged();  // Notify the adapter that the list has changed
    }
    public interface OnItemEditedListener {
        void onItemEdited();
    }
}
