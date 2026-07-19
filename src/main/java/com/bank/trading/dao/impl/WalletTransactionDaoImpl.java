package com.bank.trading.dao.impl;

import com.bank.trading.dao.WalletTransactionDao;
import com.bank.trading.model.WalletTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

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
}
