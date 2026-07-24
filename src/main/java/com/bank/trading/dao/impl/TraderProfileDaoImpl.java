package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.TraderProfileDao;
import com.bank.trading.model.Trader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class TraderProfileDaoImpl implements TraderProfileDao {

    @Override
    public void save(Trader trader, Connection conn) throws SQLException {
        String sql = "INSERT INTO traders (user_id, employee_code, department, aadhaar_last4) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, trader.getUserId());
            stmt.setString(2, trader.getEmployeeCode());
            stmt.setString(3, trader.getDepartment());
            stmt.setString(4, trader.getAadhaarLast4());
            stmt.executeUpdate();
        }
    }

    @Override
    public Optional<Long> findTraderIdByNameAndAadhaar(String username, String aadhaarLast4) {
        String sql = "SELECT u.user_id FROM users u JOIN traders t ON u.user_id = t.user_id " +
                     "WHERE u.username = ? AND (t.aadhaar_last4 = ? OR (t.aadhaar_last4 IS NULL AND ? IS NULL))";
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
            throw new RuntimeException("Error finding trader ID by name and aadhaar", e);
        }
        return Optional.empty();
    }
}
