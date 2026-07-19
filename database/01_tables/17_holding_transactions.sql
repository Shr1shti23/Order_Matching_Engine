CREATE TABLE holding_transactions (
    transaction_id    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    client_id         BIGINT UNSIGNED NOT NULL,
    instrument_id     INT UNSIGNED    NOT NULL,
    trade_id          BIGINT UNSIGNED NULL,
    transaction_type  ENUM(
                          'BUY', 'SELL',
                          'DIVIDEND', 'BONUS', 'SPLIT',
                          'TRANSFER_IN', 'TRANSFER_OUT', 'ADJUSTMENT'
                      ) NOT NULL,
    quantity          BIGINT        NOT NULL,
    price             DECIMAL(18,4) NULL,

    created_at        TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (client_id)
        REFERENCES clients(user_id),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    FOREIGN KEY (trade_id)
        REFERENCES trades(trade_id),

    INDEX idx_portfolio_history (client_id, instrument_id, created_at)
) ENGINE = InnoDB;
