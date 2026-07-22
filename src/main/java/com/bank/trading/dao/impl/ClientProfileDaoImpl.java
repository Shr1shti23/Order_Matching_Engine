package com.bank.trading.dao.impl;

import com.bank.trading.dao.ClientProfileDao;
import com.bank.trading.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ClientProfileDaoImpl implements ClientProfileDao {

    @Override
    public void save(Client client, Connection conn) throws SQLException {
        String sql = "INSERT INTO clients (user_id, kyc_status, risk_profile) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, client.getUserId());
            stmt.setString(2, client.getKycStatus());
            stmt.setString(3, client.getRiskProfile());
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
}
