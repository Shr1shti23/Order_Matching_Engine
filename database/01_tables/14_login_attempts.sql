CREATE TABLE login_attempts (
    attempt_id   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50) NOT NULL,
    success      BOOLEAN     NOT NULL,
    ip_address   VARCHAR(45),                           -- IPv6-safe (max 45 chars)
    attempted_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_login_username_time (username, attempted_at)
) ENGINE = InnoDB;
