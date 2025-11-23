package com.espinozameridaal.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = "jdbc:sqlite:chat.db";  

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // calls once at server startup
    public static void init() throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

                st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      username TEXT NOT NULL UNIQUE,
                      password TEXT,
                      created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                
                st.execute("""
                    CREATE TABLE IF NOT EXISTS friendships (
                      user_id   INTEGER NOT NULL,
                      friend_id INTEGER NOT NULL,
                      since_at  TEXT DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (user_id, friend_id)
                    )
                """);
                
                st.execute("""
                    CREATE TABLE IF NOT EXISTS friend_requests (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      sender_id   INTEGER NOT NULL,
                      receiver_id INTEGER NOT NULL,
                      status      TEXT NOT NULL DEFAULT 'pending',
                      created_at  TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                """);
        }
    }
}