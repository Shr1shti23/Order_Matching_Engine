CREATE TABLE audit_log (
    audit_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    actor_user_id  BIGINT UNSIGNED NULL,   -- NULL = system event
    action_type    ENUM(
                       'LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'DELETE',
                       'SUSPEND', 'ORDER_PLACED', 'ORDER_CANCELLED', 'TRADE_EXECUTED'
                   ) NOT NULL,
    entity_type    VARCHAR(30) NOT NULL,   -- table name: 'users', 'orders', 'trades', …
    entity_id      BIGINT UNSIGNED NULL,
    details        JSON NULL,              -- flexible payload (old/new values, IP address, etc.)
    created_at     TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (actor_user_id)
        REFERENCES users(user_id),

    INDEX idx_audit_entity     (entity_type, entity_id),
    INDEX idx_audit_actor_time (actor_user_id, created_at)
) ENGINE = InnoDB;
