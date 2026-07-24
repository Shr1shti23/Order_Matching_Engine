CREATE TABLE clients (
    user_id       BIGINT UNSIGNED PRIMARY KEY,
    kyc_status    ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    risk_profile  ENUM('LOW', 'MEDIUM', 'HIGH')           NOT NULL DEFAULT 'MEDIUM',
    aadhaar_last4 VARCHAR(4),

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;
