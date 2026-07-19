package com.bank.trading.dao.impl;

import com.bank.trading.dao.OrderEventDao;
import com.bank.trading.model.OrderEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class OrderEventDaoImpl implements OrderEventDao {

    @Override
    public void insert(OrderEvent event, Connection conn) throws SQLException {
        String sql = "INSERT INTO order_events (order_id, event_type, previous_status, new_status, quantity_changed, price, actor_user_id, details, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, event.getOrderId());
            stmt.setString(2, event.getEventType().name());
            
            if (event.getPreviousStatus() != null) {
                stmt.setString(3, event.getPreviousStatus().name());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }
            
            stmt.setString(4, event.getNewStatus().name());
            
            if (event.getQuantityChanged() != null) {
                stmt.setLong(5, event.getQuantityChanged());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }
            
            stmt.setBigDecimal(6, event.getPrice());
            
            if (event.getActorUserId() != null) {
                stmt.setLong(7, event.getActorUserId());
            } else {
                stmt.setNull(7, Types.BIGINT);
            }
            
            stmt.setString(8, event.getDetails());
            
            stmt.executeUpdate();
        }
    }
}
