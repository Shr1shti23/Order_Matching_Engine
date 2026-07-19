CREATE TABLE order_events (
    event_id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT UNSIGNED NOT NULL,
    event_type        ENUM(
                          'CREATED', 'PARTIALLY_FILLED', 'FILLED',
                          'MODIFIED', 'CANCELLED', 'REJECTED', 'EXPIRED'
                      ) NOT NULL,
    previous_status   ENUM('PENDING', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED') NULL,
    new_status        ENUM('PENDING', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED') NOT NULL,
    quantity_changed  BIGINT        NULL,
    price             DECIMAL(18,4) NULL,
    actor_user_id     BIGINT UNSIGNED NULL,
    details           JSON NULL,
    created_at        TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE,

    FOREIGN KEY (actor_user_id)
        REFERENCES users(user_id),

    INDEX idx_order_events (order_id, created_at)
) ENGINE = InnoDB;
