package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.TraderAssignmentDao;
import com.bank.trading.model.TraderClientAssignment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TraderAssignmentDaoImpl implements TraderAssignmentDao {

    @Override
    public void assign(long traderId, long clientId, long assignedBy, Connection conn) throws SQLException {
        String sql = "INSERT INTO trader_client_assignments (trader_id, client_id, assigned_by, active) " +
                     "VALUES (?, ?, ?, TRUE) " +
                     "ON DUPLICATE KEY UPDATE active = TRUE, assigned_by = ?, assigned_at = NOW()";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, traderId);
            stmt.setLong(2, clientId);
            stmt.setLong(3, assignedBy);
            stmt.setLong(4, assignedBy);
            stmt.executeUpdate();
        }
    }

    @Override
    public void deassign(long traderId, long clientId, Connection conn) throws SQLException {
        String sql = "UPDATE trader_client_assignments SET active = FALSE " +
                     "WHERE trader_id = ? AND client_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, traderId);
            stmt.setLong(2, clientId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void reassign(long oldTraderId, long newTraderId, long clientId, long assignedBy, Connection conn) throws SQLException {
        deassign(oldTraderId, clientId, conn);
        assign(newTraderId, clientId, assignedBy, conn);
    }

    @Override
    public List<TraderClientAssignment> findAll() {
        List<TraderClientAssignment> assignments = new ArrayList<>();
        String sql = "SELECT * FROM trader_client_assignments ORDER BY assigned_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                assignments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching assignments", e);
        }
        return assignments;
    }

    @Override
    public List<TraderClientAssignment> findAllActive() {
        List<TraderClientAssignment> assignments = new ArrayList<>();
        String sql = "SELECT * FROM trader_client_assignments WHERE active = TRUE ORDER BY assigned_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                assignments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching active assignments", e);
        }
        return assignments;
    }

    @Override
    public List<TraderClientAssignment> findByTraderId(long traderId) {
        List<TraderClientAssignment> assignments = new ArrayList<>();
        String sql = "SELECT * FROM trader_client_assignments WHERE trader_id = ? AND active = TRUE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, traderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) assignments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching assignments for trader: " + traderId, e);
        }
        return assignments;
    }

    private TraderClientAssignment mapRow(ResultSet rs) throws SQLException {
        TraderClientAssignment a = new TraderClientAssignment();
        a.setAssignmentId(rs.getLong("assignment_id"));
        a.setTraderId(rs.getLong("trader_id"));
        a.setClientId(rs.getLong("client_id"));
        a.setActive(rs.getBoolean("active"));
        return a;
    }
}
