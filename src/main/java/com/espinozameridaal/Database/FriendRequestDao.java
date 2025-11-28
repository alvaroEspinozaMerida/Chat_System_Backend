package com.espinozameridaal.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.espinozameridaal.Models.FriendRequest;

public class FriendRequestDao {

    // Sends a new friend request
    public boolean createRequest(long senderId, long receiverId) throws SQLException {

        if (senderId == receiverId) return false;

        String checkSql = """
            SELECT COUNT(*) FROM friend_requests
            WHERE sender_id = ? AND receiver_id = ? AND status = 'pending'
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {

            ps.setLong(1, senderId);
            ps.setLong(2, receiverId);

            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return false; // duplicate pending request
            }
        }

        String sql = """
            INSERT INTO friend_requests (sender_id, receiver_id, status)
            VALUES (?, ?, 'pending')
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, senderId);
            ps.setLong(2, receiverId);
            ps.executeUpdate();
            return true;
        }
    }

    // Gets all pending incoming friend requests
    public List<FriendRequest> getIncomingPending(long userId) throws SQLException {
        String sql = """
            SELECT * FROM friend_requests
            WHERE receiver_id = ? AND status = 'pending'
        """;

        List<FriendRequest> list = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FriendRequest fr = new FriendRequest(
                        rs.getLong("id"),
                        rs.getLong("sender_id"),
                        rs.getLong("receiver_id"),
                        rs.getString("status"),
                        rs.getString("created_at"),
                        rs.getString("responded_at")
                );
                list.add(fr);
            }
        }

        return list;
    }

    // Accept request
    public boolean accept(long requestId) throws SQLException {
      String sql = """
          UPDATE friend_requests
          SET status = 'accepted',
              responded_at = CURRENT_TIMESTAMP
          WHERE id = ?
      """;

      try (Connection conn = Database.getConnection();
          PreparedStatement ps = conn.prepareStatement(sql)) {

          ps.setLong(1, requestId);
          return ps.executeUpdate() > 0;
      }
    }

    // Decline request
    public boolean decline(long requestId) throws SQLException {
        String sql = """
            UPDATE friend_requests
            SET status = 'declined', responded_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, requestId);
            return ps.executeUpdate() > 0;
        }
    }

    // OPtional but finds a pending request between two users
    public FriendRequest findPending(long senderId, long receiverId) throws SQLException {
        String sql = """
            SELECT * FROM friend_requests
            WHERE sender_id = ? AND receiver_id = ? AND status = 'pending'
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, senderId);
            ps.setLong(2, receiverId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new FriendRequest(
                        rs.getLong("id"),
                        rs.getLong("sender_id"),
                        rs.getLong("receiver_id"),
                        rs.getString("status"),
                        rs.getString("created_at"),
                        rs.getString("responded_at")
                );
            }
        }

        return null;
    }
}


