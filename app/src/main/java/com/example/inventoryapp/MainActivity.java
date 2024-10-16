package com.example.inventoryapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SEND_SMS = 101;
    private EditText usernameInput, passwordInput;
    private UserDatabase userDatabase;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI elements
        usernameInput = findViewById(R.id.enterUserName);
        passwordInput = findViewById(R.id.enterPassword);
        Button loginButton = findViewById(R.id.loginButton);
        Button signUpButton = findViewById(R.id.createAccount);

        // Initialize the database and session manager
        userDatabase = new UserDatabase(this);
        sessionManager = new SessionManager(this);

        // Request SMS permissions
        requestPermissions();

        // Handle login button click
        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (userDatabase.checkUser(username, password)) {
                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                sessionManager.createLoginSession(username);
                Intent intent = new Intent(MainActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle sign-up button click
        signUpButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (userDatabase.addUser(username, password)) {
                Toast.makeText(MainActivity.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                sessionManager.createLoginSession(username);
                Intent intent = new Intent(MainActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Sign up failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.SEND_SMS
        };

        // Check which permissions are already granted
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        // Request the permissions that are not granted
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_CODE_SEND_SMS);
        } else {
            // All permissions are already granted
            Toast.makeText(this, "All permissions are already granted.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted! You can now send SMS.", Toast.LENGTH_SHORT).show();
                String phoneNumber = getPhoneNumber(); // Retrieve the phone number after permission is granted
            } else {
                Toast.makeText(this, "Permission denied! SMS functionality will be disabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getPhoneNumber() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("default_phone_number", null); // Returns null if not found
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sessionManager.logoutUser();
    }

    public void sendSms(String message) {
        String phoneNumber = getPhoneNumber(); // Retrieve the saved phone number
        if (phoneNumber != null) {
            SmsManager smsManager = SmsManager.getDefault();
            PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE);
            smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, null);

            // Register a BroadcastReceiver to listen for the SMS sent status
            registerReceiver(new SmsBroadcastReceiver(), new IntentFilter("SMS_SENT"));
        } else {
            Toast.makeText(this, "Phone number not available!", Toast.LENGTH_SHORT).show();
        }
    }
}
