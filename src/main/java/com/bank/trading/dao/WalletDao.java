package com.bank.trading.dao;

import com.bank.trading.model.Wallet;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface WalletDao {
    List<Wallet> findAll();
    Optional<Wallet> findByClientId(long clientId);
    void updateBalance(long walletId, BigDecimal newBalance, int currentVersion, Connection conn) throws SQLException;
    void save(Wallet wallet, Connection conn) throws SQLException;
}
