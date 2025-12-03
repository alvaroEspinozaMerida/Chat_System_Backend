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
                      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                      last_login_at TEXT,
                      email TEXT,
                      status TEXT DEFAULT 'active'
                    )
                """); /// Added last_login, email, and status

                st.execute("""
                    CREATE TABLE IF NOT EXISTS profile (
                    user_id       INTEGER PRIMARY KEY,
                    display_name  TEXT,
                    avatar_url    TEXT,
                    bio           TEXT,
                    privacy_level TEXT DEFAULT 'public',
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                """); //User profiles to display with frontend

                st.execute("""
                    CREATE TABLE IF NOT EXISTS user_settings (
                    user_id        INTEGER PRIMARY KEY,
                    theme          TEXT DEFAULT 'system',
                    language       TEXT DEFAULT 'en',
                    notify_email   INTEGER DEFAULT 1,
                    notify_push    INTEGER DEFAULT 1,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                """); // User settings -- not necessary rn, but I'll keep it created on db anyway
                
                st.execute("""
                    CREATE TABLE IF NOT EXISTS user_sessions (
                    session_id TEXT PRIMARY KEY,
                    user_id    INTEGER NOT NULL,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    expires_at TEXT,
                    revoked_at TEXT,
                    ip_address TEXT,
                    user_agent TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                """); // Meant for active/inactive status -- not necessary, but was in the erd diagram created

                st.execute("""
                    CREATE TABLE IF NOT EXISTS friendships (
                      user_id   INTEGER NOT NULL,
                      friend_id INTEGER NOT NULL,
                      since_at  TEXT DEFAULT CURRENT_TIMESTAMP,
                      status TEXT NOT NULL DEFAULT 'active',
                      PRIMARY KEY (user_id, friend_id),
                      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                      FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                """); //Added status, and foreign keys that reference friend_id and user_id (vice-versa)
                
                
                st.execute("""
                    CREATE TABLE IF NOT EXISTS friend_requests (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      sender_id   INTEGER NOT NULL,
                      receiver_id INTEGER NOT NULL,
                      status      TEXT NOT NULL DEFAULT 'pending',
                      created_at  TEXT DEFAULT CURRENT_TIMESTAMP,
                      responded_at TEXT,
                      FOREIGN KEY (sender_id)   REFERENCES users(id) ON DELETE CASCADE,
                      FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                """); //Added responded_at, and foreign keys like in friendships table

                st.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      sender_id   INTEGER NOT NULL,
                      receiver_id INTEGER,         
                      content     TEXT NOT NULL,
                      sent_at     TEXT DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (sender_id)   REFERENCES users(id) ON DELETE CASCADE,
                      FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                """); //Meant for chat history
        }
    }
}