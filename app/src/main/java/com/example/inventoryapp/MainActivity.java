package com.example.inventoryapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import android.telephony.SmsManager;

import androidx.annotation.NonNull;

import android.content.pm.PackageManager;

public class MainActivity extends AppCompatActivity {
    private EditText usernameInput, passwordInput;
    private UserDatabase userDatabase;
    private SessionManager sessionManager; // Add SessionManager
    private static final int REQUEST_CODE_SEND_SMS = 101;
    private Button requestPermissionButton;
    private EditText phoneNumberInput, messageInput;
    private TextView notificationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI elements
        usernameInput = findViewById(R.id.enterUserName);
        passwordInput = findViewById(R.id.enterPassword);
        Button loginButton = findViewById(R.id.loginButton);
        Button signUpButton = findViewById(R.id.createAccount);

        requestPermissionButton = findViewById(R.id.requestPermissionButton);
        // Set up button click listener to request SMS permission
        requestPermissionButton.setOnClickListener(v -> requestSmsPermission());
        notificationStatus = findViewById(R.id.notificationStatus);

        // Initialize the database and session manager
        userDatabase = new UserDatabase(this);
        sessionManager = new SessionManager(this); // Initialize SessionManager

        // Handle login button click
        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (userDatabase.checkUser(username, password)) {
                // User exists, proceed with login
                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                // Create a session for the logged-in user
                sessionManager.createLoginSession(username); // Save username in session

                // Start the InventoryActivity after successful login
                Intent intent = new Intent(MainActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish(); // Close MainActivity
            } else {
                // User not found, show error
                Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle sign-up button click
        signUpButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (userDatabase.addUser(username, password)) {
                Toast.makeText(MainActivity.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                // Optionally, you can log in the user immediately after sign-up
                sessionManager.createLoginSession(username); // Save username in session
                Intent intent = new Intent(MainActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish(); // Close MainActivity
            } else {
                Toast.makeText(MainActivity.this, "Sign up failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // This method will be called when the user chooses to log out
    public void logoutUser() {
        sessionManager.logoutUser();
        // Optionally, navigate back to login screen or show a message
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sessionManager.logoutUser();
    }

    private void checkSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE_SEND_SMS);
        }
    }

    private void sendSms(String phoneNumber, String message) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "SMS sent successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission not granted to send SMS", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestSmsPermission() {
        // Check if the SMS permission is already granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // Request SMS permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE_SEND_SMS);
        } else {
            // Permission already granted
            Toast.makeText(this, "SMS permission already granted.", Toast.LENGTH_SHORT).show();
            notificationStatus.setText("SMS permission already granted.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted! You can now send SMS.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied! SMS functionality will be disabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
