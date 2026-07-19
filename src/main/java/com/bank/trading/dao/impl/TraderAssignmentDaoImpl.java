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
    public List<TraderClientAssignment> findAllActive() {
        List<TraderClientAssignment> assignments = new ArrayList<>();
        String sql = "SELECT * FROM trader_client_assignments WHERE active = true";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                assignments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching active trader assignments", e);
        }
        return assignments;
    }

    private TraderClientAssignment mapRow(ResultSet rs) throws SQLException {
        TraderClientAssignment assignment = new TraderClientAssignment();
        assignment.setAssignmentId(rs.getLong("assignment_id"));
        assignment.setTraderId(rs.getLong("trader_id"));
        assignment.setClientId(rs.getLong("client_id"));
        assignment.setAssignedBy(rs.getLong("assigned_by"));
        assignment.setActive(rs.getBoolean("active"));
        return assignment;
    }
}
