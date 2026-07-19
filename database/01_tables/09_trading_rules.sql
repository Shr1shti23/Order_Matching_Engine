CREATE TABLE trading_rules (
    rule_id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    scope               ENUM('GLOBAL', 'CLIENT', 'INSTRUMENT') NOT NULL,
    client_id           BIGINT UNSIGNED NULL,     -- populated only when scope = 'CLIENT'
    instrument_id       INT UNSIGNED    NULL,     -- populated only when scope = 'INSTRUMENT'
    max_trade_size      BIGINT UNSIGNED NULL,
    max_position_limit  BIGINT UNSIGNED NULL,
    daily_loss_limit    DECIMAL(18,2)   NULL,
    created_by          BIGINT UNSIGNED NOT NULL, -- must be an ADMIN user

    created_at          TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (client_id)
        REFERENCES clients(user_id),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    FOREIGN KEY (created_by)
        REFERENCES users(user_id),

    CHECK (
        (scope = 'GLOBAL'      AND client_id IS NULL     AND instrument_id IS NULL) OR
        (scope = 'CLIENT'      AND client_id IS NOT NULL)                           OR
        (scope = 'INSTRUMENT'  AND instrument_id IS NOT NULL)
    )
) ENGINE = InnoDB;
