package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.AuditLogDao;
import com.bank.trading.model.ActionType;
import com.bank.trading.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDaoImpl implements AuditLogDao {

    @Override
    public void insert(AuditLog auditLog, Connection conn) throws SQLException {
        String sql = "INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (auditLog.getActorUserId() != null) {
                stmt.setLong(1, auditLog.getActorUserId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            stmt.setString(2, auditLog.getActionType().name());
            stmt.setString(3, auditLog.getEntityType());
            if (auditLog.getEntityId() != null) {
                stmt.setLong(4, auditLog.getEntityId());
            } else {
                stmt.setNull(4, Types.BIGINT);
            }
            stmt.setString(5, auditLog.getDetails());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setAuditId(rs.getLong("audit_id"));
                    long actorId = rs.getLong("actor_user_id");
                    if (!rs.wasNull()) log.setActorUserId(actorId);
                    log.setActionType(ActionType.valueOf(rs.getString("action_type")));
                    log.setEntityType(rs.getString("entity_type"));
                    long entityId = rs.getLong("entity_id");
                    if (!rs.wasNull()) log.setEntityId(entityId);
                    log.setDetails(rs.getString("details"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching audit logs", e);
        }
        return logs;
    }
}
