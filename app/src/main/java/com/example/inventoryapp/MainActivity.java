package com.example.inventoryapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private EditText usernameInput, passwordInput;
    private Button loginButton, signUpButton;
    private UserDatabase userDatabase;
    private SessionManager sessionManager; // Add SessionManager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI elements
        usernameInput = findViewById(R.id.enterUserName);
        passwordInput = findViewById(R.id.enterPassword);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.createAccount);

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
        // sessionManager.logoutUser();
    }
}
