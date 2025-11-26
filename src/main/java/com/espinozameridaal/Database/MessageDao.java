package com.espinozameridaal.Database;

import com.espinozameridaal.Models.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {

    // Save a message into the database
    public void saveMessage(long senderId, long receiverId, String content) throws SQLException {
        String sql = """
            INSERT INTO messages (sender_id, receiver_id, content)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, senderId);
            ps.setLong(2, receiverId);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    // Loas all direct messages between two users
    public List<Message> getConversation(long userA, long userB) throws SQLException {
        String sql = """
            SELECT id, sender_id, receiver_id, content, sent_at
            FROM messages
            WHERE (sender_id = ? AND receiver_id = ?)
               OR (sender_id = ? AND receiver_id = ?)
            ORDER BY sent_at ASC
        """;

        List<Message> list = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userA);
            ps.setLong(2, userB);
            ps.setLong(3, userB);
            ps.setLong(4, userA);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Message(
                        rs.getLong("id"),
                        rs.getLong("sender_id"),
                        rs.getLong("receiver_id"),
                        rs.getString("content"),
                        rs.getString("sent_at") // already timestamped by DB
                ));
            }
        }
        return list;
    }

    // Load message history for a single user
    public List<Message> getMessagesForUser(long userId) throws SQLException {
        String sql = """
            SELECT id, sender_id, receiver_id, content, sent_at
            FROM messages
            WHERE sender_id = ? OR receiver_id = ?
            ORDER BY sent_at DESC
        """;

        List<Message> list = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Message(
                        rs.getLong("id"),
                        rs.getLong("sender_id"),
                        rs.getLong("receiver_id"),
                        rs.getString("content"),
                        rs.getString("sent_at")
                ));
            }
        }
        return list;
    }
}