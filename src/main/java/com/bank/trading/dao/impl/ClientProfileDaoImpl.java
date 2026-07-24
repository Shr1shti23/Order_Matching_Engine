package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.ClientProfileDao;
import com.bank.trading.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ClientProfileDaoImpl implements ClientProfileDao {

    @Override
    public void save(Client client, Connection conn) throws SQLException {
        String sql = "INSERT INTO clients (user_id, kyc_status, risk_profile, aadhaar_last4) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, client.getUserId());
            stmt.setString(2, client.getKycStatus());
            stmt.setString(3, client.getRiskProfile());
            stmt.setString(4, client.getAadhaarLast4());
            stmt.executeUpdate();
        }
    }

    @Override
    public void update(Client client, Connection conn) throws SQLException {
        String sql = "UPDATE clients SET kyc_status = ?, risk_profile = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, client.getKycStatus());
            stmt.setString(2, client.getRiskProfile());
            stmt.setLong(3, client.getUserId());
            stmt.executeUpdate();
        }
    }

    @Override
    public Optional<Long> findClientIdByNameAndAadhaar(String username, String aadhaarLast4) {
        String sql = "SELECT u.user_id FROM users u JOIN clients c ON u.user_id = c.user_id " +
                     "WHERE u.username = ? AND (c.aadhaar_last4 = ? OR (c.aadhaar_last4 IS NULL AND ? IS NULL))";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, aadhaarLast4);
            stmt.setString(3, aadhaarLast4);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getLong("user_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding client ID by name and aadhaar", e);
        }
        return Optional.empty();
    }
}
