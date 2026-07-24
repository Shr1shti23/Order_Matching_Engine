package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.WalletDao;
import com.bank.trading.model.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WalletDaoImpl implements WalletDao {

    @Override
    public List<Wallet> findAll() {
        List<Wallet> wallets = new ArrayList<>();
        String sql = "SELECT * FROM wallets";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                wallets.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching wallets", e);
        }
        return wallets;
    }

    @Override
    public Optional<Wallet> findByClientId(long clientId) {
        String sql = "SELECT * FROM wallets WHERE client_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding wallet by clientId: " + clientId, e);
        }
        return Optional.empty();
    }

    @Override
    public void updateBalance(long walletId, BigDecimal newBalance, int currentVersion, Connection conn) throws SQLException {
        String sql = "UPDATE wallets SET cash_balance = ?, version = version + 1 WHERE wallet_id = ? AND version = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setLong(2, walletId);
            stmt.setInt(3, currentVersion);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Optimistic lock failure on wallet " + walletId);
            }
        }
    }

    @Override
    public void save(Wallet wallet, Connection conn) throws SQLException {
        String sql = "INSERT INTO wallets (client_id, cash_balance, currency, version) " +
                     "VALUES (?, ?, ?, 0)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, wallet.getClientId());
            stmt.setBigDecimal(2, wallet.getCashBalance());
            stmt.setString(3, wallet.getCurrency());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    wallet.setWalletId(rs.getLong(1));
                }
            }
        }
    }

    private Wallet mapRow(ResultSet rs) throws SQLException {
        Wallet wallet = new Wallet();
        wallet.setWalletId(rs.getLong("wallet_id"));
        wallet.setClientId(rs.getLong("client_id"));
        wallet.setCashBalance(rs.getBigDecimal("cash_balance"));
        wallet.setCurrency(rs.getString("currency"));
        wallet.setVersion(rs.getInt("version"));
        return wallet;
    }
}
