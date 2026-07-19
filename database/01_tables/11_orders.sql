CREATE TABLE orders (
    order_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    client_id      BIGINT UNSIGNED NOT NULL,  -- whose funds / holdings are affected
    trader_id      BIGINT UNSIGNED NOT NULL,  -- who placed the order on the client's behalf
    instrument_id  INT UNSIGNED    NOT NULL,

    side           ENUM('BUY', 'SELL') NOT NULL,
    order_type     ENUM('MARKET', 'LIMIT') NOT NULL,
    time_in_force  ENUM('DAY', 'GTC', 'IOC', 'FOK') NOT NULL DEFAULT 'DAY',

    price          DECIMAL(18,4) NULL,         -- NULL only for MARKET orders
    original_qty   BIGINT UNSIGNED NOT NULL,
    remaining_qty  BIGINT UNSIGNED NOT NULL,

    status         ENUM('PENDING', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED')
                   NOT NULL DEFAULT 'PENDING',

    created_at     TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (client_id)
        REFERENCES clients(user_id),

    FOREIGN KEY (trader_id)
        REFERENCES traders(user_id),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    CHECK (remaining_qty <= original_qty),
    CHECK (order_type <> 'LIMIT' OR price IS NOT NULL),  -- LIMIT orders must carry a price

    -- Used by the matching engine: best price first, earliest order first.
    INDEX idx_order_book    (instrument_id, side, status, price, created_at),
    INDEX idx_orders_client (client_id,  created_at),
    INDEX idx_orders_trader (trader_id,  created_at)
) ENGINE = InnoDB;
