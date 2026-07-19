package com.bank.trading.dao;

import com.bank.trading.model.HoldingTransaction;
import java.sql.Connection;
import java.sql.SQLException;

public interface HoldingTransactionDao {
    void insert(HoldingTransaction txn, Connection conn) throws SQLException;
}
