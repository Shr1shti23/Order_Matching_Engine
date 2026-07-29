package com.bank.trading.service;

import com.bank.trading.dao.UserDao;
import com.bank.trading.dao.impl.UserDaoImpl;
import com.bank.trading.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles role-based access control permission checks and authentication.
 */
public class RBACService {

    private final UserDao userDao;
    private final Map<Integer, List<String>> rolePermissionsCache = new HashMap<>();

    public RBACService() {
        this.userDao = new UserDaoImpl();
        preloadPermissions();
    }

    /** Preloads role permissions from the database. */
    public void preloadPermissions() {
        rolePermissionsCache.clear();
        for (int roleId = 1; roleId <= 3; roleId++) {
            rolePermissionsCache.put(roleId, userDao.findPermissionsByRoleId(roleId));
        }
    }

    /**
     * Validates user credentials and returns the authenticated User.
     *
     * @param username login username
     * @param password plain-text password
     * @return the authenticated User
     * @throws IllegalArgumentException if credentials are invalid
     * @throws IllegalStateException    if the account is suspended
     */
    public User authenticate(String username, String password) {
        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("User account is suspended.");
        }
        if ("DELETED".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        if (!PasswordService.verify(user.getPasswordHash(), password)) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        // Transparent rehashing: Upgrade hash if parameters changed or if legacy format
        if (PasswordService.needsRehash(user.getPasswordHash())) {
            try (java.sql.Connection conn = com.bank.trading.config.DatabaseConfig.getConnection()) {
                String newHash = PasswordService.hash(password);
                userDao.updatePassword(user.getUserId(), newHash, user.isForcePasswordReset(), conn);
                user.setPasswordHash(newHash);
                user.setPasswordAlgo("argon2id");
            } catch (Exception ignore) {
                // Log/ignore background rehash failure to not block login
            }
        }

        return user;
    }

    /**
     * Returns true if the given role has the specified permission.
     */
    public boolean hasPermission(int roleId, String permissionName) {
        List<String> perms = rolePermissionsCache.computeIfAbsent(
                roleId, k -> userDao.findPermissionsByRoleId(k));
        return perms != null && perms.contains(permissionName);
    }

    /** Returns all permission names for a given role. */
    public List<String> getPermissions(int roleId) {
        return rolePermissionsCache.computeIfAbsent(
                roleId, k -> userDao.findPermissionsByRoleId(k));
    }
}
