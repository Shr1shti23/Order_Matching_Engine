package com.bank.trading.dao;

import com.bank.trading.model.Order;
import com.bank.trading.model.OrderStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface OrderDao {
    List<Order> findOpenOrders();
    long insert(Order order, Connection conn) throws SQLException;
    void updateStatus(long orderId, OrderStatus newStatus, long remainingQty, Connection conn) throws SQLException;
    List<Order> findAll();
    List<Order> findByClientId(long clientId);
}
