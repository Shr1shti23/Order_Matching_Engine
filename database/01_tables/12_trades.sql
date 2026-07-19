CREATE TABLE trades (
    trade_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    instrument_id  INT UNSIGNED    NOT NULL,
    buy_order_id   BIGINT UNSIGNED NOT NULL,
    sell_order_id  BIGINT UNSIGNED NOT NULL,
    price          DECIMAL(18,4)   NOT NULL,
    quantity       BIGINT UNSIGNED NOT NULL,
    executed_at    TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    FOREIGN KEY (buy_order_id)
        REFERENCES orders(order_id),

    FOREIGN KEY (sell_order_id)
        REFERENCES orders(order_id),

    -- Drives the "View All Trades" feed sorted by time per instrument.
    INDEX idx_trades_symbol_time (instrument_id, executed_at),

    CHECK (quantity > 0 AND price > 0)
) ENGINE = InnoDB;
