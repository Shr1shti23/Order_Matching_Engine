CREATE TABLE holdings (
    holding_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    client_id      BIGINT UNSIGNED NOT NULL,
    instrument_id  INT UNSIGNED    NOT NULL,
    quantity       BIGINT          NOT NULL DEFAULT 0,
    avg_buy_price  DECIMAL(18,4)   NOT NULL DEFAULT 0,
    updated_at     TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_client_instrument (client_id, instrument_id),

    FOREIGN KEY (client_id)
        REFERENCES clients(user_id),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    CHECK (quantity >= 0)
) ENGINE = InnoDB;
