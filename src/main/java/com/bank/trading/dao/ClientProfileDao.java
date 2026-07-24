package com.bank.trading.dao;

import com.bank.trading.model.Client;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface ClientProfileDao {
    void save(Client client, Connection conn) throws SQLException;
    void update(Client client, Connection conn) throws SQLException;
    Optional<Long> findClientIdByNameAndAadhaar(String username, String aadhaarLast4);
}
