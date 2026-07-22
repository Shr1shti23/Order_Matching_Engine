package com.bank.trading.dao;

import com.bank.trading.model.TraderClientAssignment;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface TraderAssignmentDao {
    void assign(long traderId, long clientId, long assignedBy, Connection conn) throws SQLException;
    void deassign(long traderId, long clientId, Connection conn) throws SQLException;
    void reassign(long oldTraderId, long newTraderId, long clientId, long assignedBy, Connection conn) throws SQLException;
    List<TraderClientAssignment> findAll();
    List<TraderClientAssignment> findAllActive();
    List<TraderClientAssignment> findByTraderId(long traderId);
}
