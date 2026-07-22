package com.bank.trading.dao;

import com.bank.trading.model.AuditLog;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface AuditLogDao {
    void insert(AuditLog auditLog, Connection conn) throws SQLException;
    List<AuditLog> findRecent(int limit);
}
