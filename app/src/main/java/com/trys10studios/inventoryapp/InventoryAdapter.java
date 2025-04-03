package com.trys10studios.inventoryapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
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
        holder.editButton.setOnClickListener(v -> showEditDialog(currentItem, position, holder));
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

    private void showEditDialog(InventoryItem item, int position, ViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Item");

        View view = LayoutInflater.from(context).inflate(R.layout.add_item, null);
        builder.setView(view);

        TextView nameInput = view.findViewById(R.id.item_name_input);
        TextView sku = view.findViewById(R.id.sku_num);
        TextView quantity = view.findViewById(R.id.quantity_count);
        TextView category = view.findViewById(R.id.category_input);
        TextView price = view.findViewById(R.id.price_input);
        TextView descriptionInput = view.findViewById(R.id.item_description_input);

        // Populate initial values
        nameInput.setText(item.getItemName());
        sku.setText(item.getSku());
        quantity.setText(String.valueOf(item.getItemQuantity()));
        category.setText(item.getItemCategory());
        price.setText(String.valueOf(item.getItemPrice()));
        descriptionInput.setText(item.getItemDescription());

        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newItemName = nameInput.getText().toString().trim();
            String newSku = sku.getText().toString().trim();
            String newCategory = category.getText().toString().trim();
            String newDescription = descriptionInput.getText().toString().trim();
            String quantityStr = quantity.getText().toString().trim();
            String priceStr = price.getText().toString().trim();

            if (quantityStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(context, "Quantity and Price cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            int newQuantity, newPrice;
            try {
                newQuantity = Integer.parseInt(quantityStr);
                if (newQuantity < 0) {
                    Toast.makeText(context, "Quantity cannot be negative", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                newPrice = Integer.parseInt(priceStr);
                if (newPrice < 0) {
                    Toast.makeText(context, "Price cannot be negative", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update the item object
            item.setItemName(newItemName);
            item.setSKU(newSku);
            item.setQuantity(newQuantity);
            item.setPrice(newPrice);
            item.setCategory(newCategory);
            item.setDescription(newDescription);

            // Update database
            inventoryDatabase.updateInventoryItem(item);

            // Refresh full inventory list from database
            List<InventoryItem> updatedList = inventoryDatabase.getAllInventoryItems();

            // Get the updated item from the refreshed list
            InventoryItem updatedItem = updatedList.get(position);

            // Update UI in the main list
            holder.itemName.setText(updatedItem.getItemName());
            holder.itemQuantity.setText(String.valueOf(updatedItem.getItemQuantity()));
            holder.itemPrice.setText(String.valueOf(updatedItem.getItemPrice()));
            holder.itemCategory.setText(updatedItem.getItemCategory());
            holder.itemSKU.setText(updatedItem.getSku());
            holder.itemDescription.setText(updatedItem.getItemDescription());

            // Notify adapter of the change
            updateItemList(updatedList);
            notifyItemChanged(position);

            Log.d("EditDialog", "Updated Item: " + updatedItem);
            dialog.dismiss();
        });
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
            itemSKU = itemView.findViewById(R.id.sku_num);
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
        itemList.clear();  // Keep the same reference
        itemList.addAll(newItemList);
        notifyDataSetChanged();  // Notify the adapter that the list has changed
    }
    public interface OnItemEditedListener {
        void onItemEdited();
    }
}
