package com.bank.trading.dao.impl;

import com.bank.trading.dao.AuditLogDao;
import com.bank.trading.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

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
}
