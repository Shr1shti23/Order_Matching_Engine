package com.bank.trading.dao;

import com.bank.trading.model.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(long userId);
    Optional<User> findByUsername(String username);
    void save(User user, Connection conn) throws SQLException;
    void updateStatus(long userId, String status, Connection conn) throws SQLException;
    void updatePassword(long userId, String newPasswordHash, boolean forcePasswordReset, Connection conn) throws SQLException;
    List<User> findByRoleId(int roleId);
    List<String> findPermissionsByRoleId(int roleId);
}
