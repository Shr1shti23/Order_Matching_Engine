package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.OrderDao;
import com.bank.trading.model.Order;
import com.bank.trading.model.OrderStatus;
import com.bank.trading.model.OrderType;
import com.bank.trading.model.Side;
import com.bank.trading.model.TimeInForce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDaoImpl implements OrderDao {

    @Override
    public List<Order> findOpenOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, i.symbol FROM orders o " +
                     "JOIN instruments i ON o.instrument_id = i.instrument_id " +
                     "WHERE o.status IN ('PENDING', 'PARTIALLY_FILLED') " +
                     "ORDER BY o.created_at ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                orders.add(mapRow(rs, true));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching open orders", e);
        }
        return orders;
    }

    @Override
    public long insert(Order order, Connection conn) throws SQLException {
        String sql = "INSERT INTO orders (client_id, trader_id, instrument_id, side, order_type, time_in_force, price, original_qty, remaining_qty, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, order.getClientId());
            stmt.setLong(2, order.getTraderId());
            stmt.setInt(3, order.getInstrumentId());
            stmt.setString(4, order.getSide().name());
            stmt.setString(5, order.getOrderType().name());
            stmt.setString(6, order.getTimeInForce().name());
            stmt.setBigDecimal(7, order.getPrice());
            stmt.setLong(8, order.getOriginalQty());
            stmt.setLong(9, order.getRemainingQty());
            stmt.setString(10, order.getStatus().name());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    order.setOrderId(id);
                    return id;
                } else {
                    throw new SQLException("Creating order failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public void updateStatus(long orderId, OrderStatus newStatus, long remainingQty, Connection conn) throws SQLException {
        String sql = "UPDATE orders SET status = ?, remaining_qty = ?, updated_at = NOW() WHERE order_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setLong(2, remainingQty);
            stmt.setLong(3, orderId);
            stmt.executeUpdate();
        }
    }

    private Order mapRow(ResultSet rs, boolean withSymbol) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getLong("order_id"));
        order.setClientId(rs.getLong("client_id"));
        order.setTraderId(rs.getLong("trader_id"));
        order.setInstrumentId(rs.getInt("instrument_id"));
        if (withSymbol) {
            order.setSymbol(rs.getString("symbol"));
        }
        order.setSide(Side.valueOf(rs.getString("side")));
        order.setOrderType(OrderType.valueOf(rs.getString("order_type")));
        order.setTimeInForce(TimeInForce.valueOf(rs.getString("time_in_force")));
        order.setPrice(rs.getBigDecimal("price"));
        order.setOriginalQty(rs.getLong("original_qty"));
        order.setRemainingQty(rs.getLong("remaining_qty"));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return order;
    }
}
