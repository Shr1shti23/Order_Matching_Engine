package com.bank.trading.dao.impl;

import com.bank.trading.dao.HoldingTransactionDao;
import com.bank.trading.model.HoldingTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class HoldingTransactionDaoImpl implements HoldingTransactionDao {

    @Override
    public void insert(HoldingTransaction txn, Connection conn) throws SQLException {
        String sql = "INSERT INTO holding_transactions (client_id, instrument_id, trade_id, transaction_type, quantity, price, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, txn.getClientId());
            stmt.setInt(2, txn.getInstrumentId());
            
            if (txn.getTradeId() != null) {
                stmt.setLong(3, txn.getTradeId());
            } else {
                stmt.setNull(3, Types.BIGINT);
            }
            
            stmt.setString(4, txn.getTransactionType().name());
            stmt.setLong(5, txn.getQuantity());
            stmt.setBigDecimal(6, txn.getPrice());
            
            stmt.executeUpdate();
        }
    }
}
