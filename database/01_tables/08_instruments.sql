CREATE TABLE instruments (
    instrument_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    symbol             VARCHAR(20)  NOT NULL UNIQUE,
    name               VARCHAR(100) NOT NULL,
    instrument_type    ENUM('STOCK', 'BOND', 'ETF', 'FOREX') NOT NULL,
    tick_size          DECIMAL(10,4) NOT NULL DEFAULT 0.01,  -- minimum price increment
    lot_size           INT UNSIGNED  NOT NULL DEFAULT 1,     -- minimum quantity increment
    status             ENUM('ACTIVE', 'SUSPENDED', 'DELISTED') NOT NULL DEFAULT 'ACTIVE',
    last_traded_price  DECIMAL(18,4) NULL,                   -- updated after each trade via trigger
    created_by         BIGINT UNSIGNED NOT NULL,             -- must be an ADMIN user

    created_at         TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (created_by)
        REFERENCES users(user_id)
) ENGINE = InnoDB;
