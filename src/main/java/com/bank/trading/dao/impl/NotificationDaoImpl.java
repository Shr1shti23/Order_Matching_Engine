package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.NotificationDao;
import com.bank.trading.model.ClientNotification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotificationDaoImpl implements NotificationDao {

    @Override
    public void sendNotification(long clientId, String message) {
        String sql = "INSERT INTO client_notifications (client_id, message, created_at) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, clientId);
            stmt.setString(2, message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Log error silently or rethrow
            System.err.println("  [WARN] Failed to log notification to database: " + e.getMessage());
        }
    }

    @Override
    public List<ClientNotification> findByClientId(long clientId) {
        List<ClientNotification> list = new ArrayList<>();
        String sql = "SELECT * FROM client_notifications WHERE client_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ClientNotification n = new ClientNotification();
                    n.setNotificationId(rs.getLong("notification_id"));
                    n.setClientId(rs.getLong("client_id"));
                    n.setMessage(rs.getString("message"));
                    n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(n);
                }
            }
        } catch (SQLException e) {
            System.err.println("  [WARN] Failed to fetch notifications for client " + clientId + ": " + e.getMessage());
        }
        return list;
    }
}
