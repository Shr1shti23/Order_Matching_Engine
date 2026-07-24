package com.bank.trading.dao;

import com.bank.trading.model.Trader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface TraderProfileDao {
    void save(Trader trader, Connection conn) throws SQLException;
    Optional<Long> findTraderIdByNameAndAadhaar(String username, String aadhaarLast4);
}
