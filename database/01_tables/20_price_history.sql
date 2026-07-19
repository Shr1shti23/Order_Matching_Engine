CREATE TABLE price_history (
    history_id      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    instrument_id   INT UNSIGNED    NOT NULL,
    trade_price     DECIMAL(18,4)   NOT NULL,
    traded_volume   BIGINT UNSIGNED NOT NULL,
    trade_time      TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    INDEX idx_price_history (instrument_id, trade_time)
) ENGINE = InnoDB;
