package com.example.inventoryapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

// MainActivity.java
public class MainActivity extends AppCompatActivity {
    private EditText usernameInput, passwordInput;
    private Button loginButton, signUpButton;
    private UserDatabase userDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI elements
        usernameInput = findViewById(R.id.enterUserName);
        passwordInput = findViewById(R.id.enterPassword);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.createAccount);

        // Initialize the database
        userDatabase = new UserDatabase(this);

        // Handle login button click
        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (userDatabase.checkUser(username, password)) {
                // User exists, proceed with login
                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                // Start the InventoryActivity after successful login
                Intent intent = new Intent(MainActivity.this, InventoryActivity.class);
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
            } else {
                Toast.makeText(MainActivity.this, "Sign up failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}