package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.WalletTransactionDao;
import com.bank.trading.model.WalletTransaction;
import com.bank.trading.model.WalletTxType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class WalletTransactionDaoImpl implements WalletTransactionDao {

    @Override
    public void insert(WalletTransaction txn, Connection conn) throws SQLException {
        String sql = "INSERT INTO wallet_transactions (wallet_id, transaction_type, amount, balance_after, trade_id, reference, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, txn.getWalletId());
            stmt.setString(2, txn.getTransactionType().name());
            stmt.setBigDecimal(3, txn.getAmount());
            stmt.setBigDecimal(4, txn.getBalanceAfter());
            if (txn.getTradeId() != null) {
                stmt.setLong(5, txn.getTradeId());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }
            stmt.setString(6, txn.getReference());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<WalletTransaction> findByWalletId(long walletId) {
        List<WalletTransaction> txns = new ArrayList<>();
        String sql = "SELECT * FROM wallet_transactions WHERE wallet_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, walletId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WalletTransaction t = new WalletTransaction();
                    t.setTransactionId(rs.getLong("transaction_id"));
                    t.setWalletId(rs.getLong("wallet_id"));
                    t.setTransactionType(WalletTxType.valueOf(rs.getString("transaction_type")));
                    t.setAmount(rs.getBigDecimal("amount"));
                    t.setBalanceAfter(rs.getBigDecimal("balance_after"));
                    long tradeId = rs.getLong("trade_id");
                    if (!rs.wasNull()) t.setTradeId(tradeId);
                    t.setReference(rs.getString("reference"));
                    txns.add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching wallet transactions for wallet " + walletId, e);
        }
        return txns;
    }
}
