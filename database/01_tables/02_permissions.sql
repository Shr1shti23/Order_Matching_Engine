CREATE TABLE permissions (
    permission_id    SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    permission_name  VARCHAR(60)  NOT NULL UNIQUE,  -- e.g. 'PLACE_ORDER'
    description      VARCHAR(255)
) ENGINE = InnoDB;
