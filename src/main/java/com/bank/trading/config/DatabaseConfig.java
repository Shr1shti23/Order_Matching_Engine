package com.bank.trading.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {

    private static HikariDataSource dataSource;

    private DatabaseConfig() {
        // Prevent instantiation
    }

    public static void init() {
        if (dataSource == null) {
            try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
                Properties prop = new Properties();
                if (input == null) {
                    throw new RuntimeException("Sorry, unable to find application.properties");
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
                throw new RuntimeException("Failed to initialize database connection pool", ex);
            }
        }
    }

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
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS client_notifications (" +
                                   "notification_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, " +
                                   "client_id BIGINT UNSIGNED NOT NULL, " +
                                   "message TEXT NOT NULL, " +
                                   "created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)) ENGINE = InnoDB");
            } catch (SQLException ignore) {}
        } catch (Exception ignore) {}
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized. Call init() first.");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
