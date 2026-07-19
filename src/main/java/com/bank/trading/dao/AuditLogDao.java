package com.bank.trading.dao;

import com.bank.trading.model.AuditLog;
import java.sql.Connection;
import java.sql.SQLException;

public interface AuditLogDao {
    void insert(AuditLog auditLog, Connection conn) throws SQLException;
}
