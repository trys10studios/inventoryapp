package com.example.inventoryapp;

public interface NotificationHandler {
    void sendSms(String message);

    void sendNotification(String message);
}
