CREATE TABLE trader_client_assignments (
    assignment_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    trader_id      BIGINT UNSIGNED NOT NULL,
    client_id      BIGINT UNSIGNED NOT NULL,
    assigned_by    BIGINT UNSIGNED NOT NULL,  -- must be an ADMIN user
    assigned_at    TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
    active         BOOLEAN NOT NULL DEFAULT TRUE,

    UNIQUE KEY uq_assignment (trader_id, client_id),

    FOREIGN KEY (trader_id)
        REFERENCES traders(user_id),

    FOREIGN KEY (client_id)
        REFERENCES clients(user_id),

    FOREIGN KEY (assigned_by)
        REFERENCES users(user_id)
) ENGINE = InnoDB;
