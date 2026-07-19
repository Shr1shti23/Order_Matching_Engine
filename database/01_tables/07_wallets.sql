CREATE TABLE wallets (
    wallet_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    client_id     BIGINT UNSIGNED NOT NULL UNIQUE,
    cash_balance  DECIMAL(18,2)   NOT NULL DEFAULT 0.00,
    currency      CHAR(3)         NOT NULL DEFAULT 'INR',
    version       INT UNSIGNED    NOT NULL DEFAULT 0,
    updated_at    TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (client_id)
        REFERENCES clients(user_id)
        ON DELETE CASCADE,

    CHECK (cash_balance >= 0)
) ENGINE = InnoDB;
