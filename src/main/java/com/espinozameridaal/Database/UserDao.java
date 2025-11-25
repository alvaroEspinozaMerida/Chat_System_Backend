package com.espinozameridaal.Database;

import com.espinozameridaal.Models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    // Create a new user and also create profile and settings rows
    public User createUser(String username) throws SQLException {
        String sql = "INSERT INTO users (username) VALUES (?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create user");
                }

                long id = rs.getLong(1);

                // also create profile and settings for user
                createProfile(id);
                createSettings(id);

                User u = new User(id, username, "");
                u.friends = new ArrayList<>();
                return u;
            }
        }
    }

    //finds user by name or create if missing
    public User findOrCreateByUsername(String username) throws SQLException {
        User existing = findByUsername(username);
        if (existing != null) {
            return existing;
        }
        return createUser(username);
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                long id = rs.getLong("id");
                String uname = rs.getString("username");
                User u = new User(id, uname, "");
                u.friends = new ArrayList<>();
                return u;
            }
        }
    }

    // Creates profile row for a new user
    private void createProfile(long userId) throws SQLException {
        String sql = """
            INSERT INTO profile (user_id, display_name, avatar_url, bio, privacy_level)
            VALUES (?, ?, NULL, NULL, 'public')
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, "user_" + userId);
            ps.executeUpdate();
        }
    }

    //Creates default settings row for new users
    private void createSettings(long userId) throws SQLException {
        String sql = """
            INSERT INTO user_settings (user_id, theme, language, notify_email, notify_push)
            VALUES (?, 'system', 'en', 1, 1)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    // Returns a list of friends
    public List<User> getFriends(long userId) throws SQLException {
        String sql = """
            SELECT u.id, u.username
            FROM friendships f
            JOIN users u
              ON (
                    (f.user_id = ?  AND u.id = f.friend_id)
                 OR (f.friend_id = ? AND u.id = f.user_id)
              )
            """;

        List<User> friends = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String uname = rs.getString("username");

                    User f = new User(id, uname, "");
                    f.friends = new ArrayList<>();
                    friends.add(f);
                }
            }
        }

        return friends;
    }

    // Adds a friendship as a pair
    public void addFriendship(long userId, long friendId) throws SQLException {
        if (userId == friendId) return; // cannot friend yourself

        long a = Math.min(userId, friendId);
        long b = Math.max(userId, friendId);

        String sql = "INSERT OR IGNORE INTO friendships (user_id, friend_id) VALUES (?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, a);
            ps.setLong(2, b);
            ps.executeUpdate();
        }
    }
}