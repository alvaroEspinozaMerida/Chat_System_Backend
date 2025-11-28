package com.espinozameridaal;

@FunctionalInterface
public interface MessageListener {
    void onMessageReceived(String message);
}

