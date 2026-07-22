package com.bank.trading.dao;

import com.bank.trading.model.WalletTransaction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface WalletTransactionDao {
    void insert(WalletTransaction txn, Connection conn) throws SQLException;
    List<WalletTransaction> findByWalletId(long walletId);
}
