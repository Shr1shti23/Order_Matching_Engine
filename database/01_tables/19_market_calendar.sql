CREATE TABLE market_calendar (
    market_date   DATE PRIMARY KEY,
    session_type  ENUM('REGULAR', 'HOLIDAY', 'HALF_DAY', 'SPECIAL') NOT NULL,
    market_open   TIME NULL,
    market_close  TIME NULL,
    remarks       VARCHAR(255)
) ENGINE = InnoDB;
