package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.UserDao;
import com.bank.trading.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    @Override
    public Optional<User> findById(long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by id: " + userId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username: " + username, e);
        }
        return Optional.empty();
    }

    @Override
    public void save(User user, Connection conn) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, password_algo, role_id, status, created_by, force_password_reset) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getPasswordAlgo() != null ? user.getPasswordAlgo() : "argon2id");
            stmt.setInt(5, user.getRoleId());
            stmt.setString(6, user.getStatus());
            if (user.getCreatedBy() != null) {
                stmt.setLong(7, user.getCreatedBy());
            } else {
                stmt.setNull(7, java.sql.Types.BIGINT);
            }
            stmt.setBoolean(8, user.isForcePasswordReset());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setUserId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public void updateStatus(long userId, String status, Connection conn) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void updatePassword(long userId, String newPasswordHash, boolean forcePasswordReset, Connection conn) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, password_algo = 'argon2id', force_password_reset = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setBoolean(2, forcePasswordReset);
            stmt.setLong(3, userId);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<User> findByRoleId(int roleId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role_id = ? AND status != 'DELETED' ORDER BY username";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching users by role: " + roleId, e);
        }
        return users;
    }

    @Override
    public List<String> findPermissionsByRoleId(int roleId) {
        List<String> permissions = new ArrayList<>();
        String sql = "SELECT p.permission_name FROM permissions p " +
                     "JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
                     "WHERE rp.role_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) permissions.add(rs.getString("permission_name"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching permissions for role: " + roleId, e);
        }
        return permissions;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPasswordAlgo(rs.getString("password_algo"));
        user.setRoleId(rs.getInt("role_id"));
        user.setStatus(rs.getString("status"));
        long createdBy = rs.getLong("created_by");
        if (!rs.wasNull()) user.setCreatedBy(createdBy);
        try {
            user.setForcePasswordReset(rs.getBoolean("force_password_reset"));
        } catch (SQLException ignore) {
            // column might not exist in older table versions before migration
        }
        return user;
    }
}
