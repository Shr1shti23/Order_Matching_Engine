CREATE TABLE traders (
    user_id        BIGINT UNSIGNED PRIMARY KEY,
    employee_code  VARCHAR(20)  NOT NULL UNIQUE,
    department     VARCHAR(50),

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;
