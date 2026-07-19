CREATE TABLE wallet_transactions (
    transaction_id    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    wallet_id         BIGINT UNSIGNED NOT NULL,
    transaction_type  ENUM(
                          'DEPOSIT', 'WITHDRAWAL',
                          'TRADE_DEBIT', 'TRADE_CREDIT',
                          'REVERSAL', 'ADJUSTMENT'
                      ) NOT NULL,
    amount            DECIMAL(18,2) NOT NULL,
    balance_after     DECIMAL(18,2) NOT NULL,  -- snapshot for easy reconciliation
    trade_id          BIGINT UNSIGNED NULL,    -- linked trade, if applicable
    reference         VARCHAR(100),

    created_at        TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (wallet_id)
        REFERENCES wallets(wallet_id),

    FOREIGN KEY (trade_id)
        REFERENCES trades(trade_id),

    INDEX idx_wallet_history (wallet_id, created_at)
) ENGINE = InnoDB;
