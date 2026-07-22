package com.bank.trading.dao;

import com.bank.trading.model.Client;
import java.sql.Connection;
import java.sql.SQLException;

public interface ClientProfileDao {
    void save(Client client, Connection conn) throws SQLException;
    void update(Client client, Connection conn) throws SQLException;
}
