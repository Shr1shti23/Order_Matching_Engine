CREATE TABLE users (
    user_id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    email          VARCHAR(120) NOT NULL UNIQUE,

    -- Argon2id encoded string from Java; embeds algorithm + params + salt + hash.
    -- No separate salt column needed — it is baked into the encoded string.
    password_hash  VARCHAR(255) NOT NULL,
    password_algo  VARCHAR(20)  NOT NULL DEFAULT 'argon2id',  -- kept for algorithm agility

    role_id        TINYINT UNSIGNED NOT NULL,
    status         ENUM('ACTIVE', 'SUSPENDED', 'DELETED') NOT NULL DEFAULT 'ACTIVE',

    -- Who created this account.  NULL is allowed only for the seed admin (see trigger).
    created_by     BIGINT UNSIGNED NULL,

    created_at     TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    last_login_at  TIMESTAMP(6) NULL,

    FOREIGN KEY (role_id)
        REFERENCES roles(role_id),

    FOREIGN KEY (created_by)
        REFERENCES users(user_id),

    -- Speeds up "list all active traders / suspended clients" queries.
    INDEX idx_users_role_status (role_id, status)
) ENGINE = InnoDB;
