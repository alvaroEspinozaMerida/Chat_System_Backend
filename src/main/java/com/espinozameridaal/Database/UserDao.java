package com.espinozameridaal.Database;

import com.espinozameridaal.Models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User findOrCreateByUsername(String username) throws SQLException {
        User existing = findByUsername(username);
        if (existing != null) return existing;

        String sql = "INSERT INTO users (username) VALUES (?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Failed to create user");
                long id = rs.getLong(1);

                User u = new User(id, username, "");
                u.friends = new ArrayList<>();
                return u;
            }
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                long id = rs.getLong("id");
                String uname = rs.getString("username");
                User u = new User(id, uname, "");
                u.friends = new ArrayList<>();
                return u;
            }
        }
    }

    public List<User> getFriends(long userId) throws SQLException {
        String sql = """
            SELECT u.id, u.username
            FROM friendships f
            JOIN users u ON u.id = f.friend_id
            WHERE f.user_id = ?
            """;

        List<User> friends = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
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

    public void addFriendship(long userId, long friendId) throws SQLException {
        String sql = "INSERT INTO friendships (user_id, friend_id) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, friendId);
            ps.executeUpdate();
        }
    }
}