package com.bank.trading.dao;

import com.bank.trading.model.WalletTransaction;
import java.sql.Connection;
import java.sql.SQLException;

public interface WalletTransactionDao {
    void insert(WalletTransaction txn, Connection conn) throws SQLException;
}
