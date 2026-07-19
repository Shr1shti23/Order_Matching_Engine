package com.bank.trading.dao;

import com.bank.trading.model.OrderEvent;
import java.sql.Connection;
import java.sql.SQLException;

public interface OrderEventDao {
    void insert(OrderEvent event, Connection conn) throws SQLException;
}
