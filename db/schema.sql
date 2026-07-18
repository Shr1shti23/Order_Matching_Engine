-- =====================================================================
-- BANK TRADING PLATFORM — MYSQL SCHEMA
-- =====================================================================
-- Roles     : ADMIN (system-seeded) | TRADER | CLIENT
-- Passwords : Hashed with Argon2id in the application layer.
--             MySQL stores only the encoded hash string that Java
--             produces — it never computes Argon2id itself.
-- Run order : 1) schema.sql   2) seed.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS trading_platform
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE trading_platform;

-- Disable FK checks so tables can be created in any order.
SET FOREIGN_KEY_CHECKS = 0;


-- =====================================================================
-- SECTION 1 : RBAC — Roles & Permissions
-- =====================================================================
-- Using a proper many-to-many design (roles → role_permissions ← permissions)
-- so permissions can be added or revoked without altering table columns.

CREATE TABLE roles (
    role_id    TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    role_name  VARCHAR(20) NOT NULL UNIQUE   -- ADMIN | TRADER | CLIENT
) ENGINE = InnoDB;


CREATE TABLE permissions (
    permission_id    SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    permission_name  VARCHAR(60)  NOT NULL UNIQUE,  -- e.g. 'PLACE_ORDER'
    description      VARCHAR(255)
) ENGINE = InnoDB;


CREATE TABLE role_permissions (
    role_id        TINYINT UNSIGNED  NOT NULL,
    permission_id  SMALLINT UNSIGNED NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    FOREIGN KEY (role_id)
        REFERENCES roles(role_id)
        ON DELETE CASCADE,

    FOREIGN KEY (permission_id)
        REFERENCES permissions(permission_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;


-- =====================================================================
-- SECTION 2 : USERS — Base identity table for all roles
-- =====================================================================
-- Role-specific attributes live in thin sub-type tables (traders, clients)
-- to avoid nullable columns that belong to only one role.

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


-- Trader-specific attributes (one row per trader user).
CREATE TABLE traders (
    user_id        BIGINT UNSIGNED PRIMARY KEY,
    employee_code  VARCHAR(20)  NOT NULL UNIQUE,
    department     VARCHAR(50),

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;


-- Client-specific attributes (one row per client user).
CREATE TABLE clients (
    user_id       BIGINT UNSIGNED PRIMARY KEY,
    kyc_status    ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    risk_profile  ENUM('LOW', 'MEDIUM', 'HIGH')           NOT NULL DEFAULT 'MEDIUM',

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;


-- =====================================================================
-- SECTION 3 : WALLETS — Cash balance (clients only)
-- =====================================================================
-- Traders execute trades using the CLIENT's wallet, not their own.
-- The version column implements optimistic locking for concurrent trades.

CREATE TABLE wallets (
    wallet_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    client_id     BIGINT UNSIGNED NOT NULL UNIQUE,
    cash_balance  DECIMAL(18,2)   NOT NULL DEFAULT 0.00,
    currency      CHAR(3)         NOT NULL DEFAULT 'INR',
    version       INT UNSIGNED    NOT NULL DEFAULT 0,
    updated_at    TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (client_id)
        REFERENCES clients(user_id)
        ON DELETE CASCADE,

    CHECK (cash_balance >= 0)
) ENGINE = InnoDB;


-- =====================================================================
-- SECTION 4 : INSTRUMENTS — Admin-managed master data
-- =====================================================================

CREATE TABLE instruments (
    instrument_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    symbol             VARCHAR(20)  NOT NULL UNIQUE,
    name               VARCHAR(100) NOT NULL,
    instrument_type    ENUM('STOCK', 'BOND', 'ETF', 'FOREX') NOT NULL,
    tick_size          DECIMAL(10,4) NOT NULL DEFAULT 0.01,  -- minimum price increment
    lot_size           INT UNSIGNED  NOT NULL DEFAULT 1,     -- minimum quantity increment
    status             ENUM('ACTIVE', 'SUSPENDED', 'DELISTED') NOT NULL DEFAULT 'ACTIVE',
    last_traded_price  DECIMAL(18,4) NULL,                   -- updated after each trade via trigger
    created_by         BIGINT UNSIGNED NOT NULL,             -- must be an ADMIN user

    created_at         TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (created_by)
        REFERENCES users(user_id)
) ENGINE = InnoDB;


-- =====================================================================
-- SECTION 5 : TRADING RULES — Admin-configured limits
-- =====================================================================
-- A single table covers global, per-client, and per-instrument rules.
-- The CHECK constraint ensures the scope and FK columns stay consistent.

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


-- =====================================================================
-- SECTION 6 : HOLDINGS — Consolidated portfolio snapshot
-- =====================================================================
-- One row per (client, instrument) pair.  avg_buy_price is recalculated
-- on each BUY using a weighted-average formula in the application layer.

CREATE TABLE holdings (
    holding_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    client_id      BIGINT UNSIGNED NOT NULL,
    instrument_id  INT UNSIGNED    NOT NULL,
    quantity       BIGINT          NOT NULL DEFAULT 0,
    avg_buy_price  DECIMAL(18,4)   NOT NULL DEFAULT 0,
    updated_at     TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_client_instrument (client_id, instrument_id),

    FOREIGN KEY (client_id)
        REFERENCES clients(user_id),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    CHECK (quantity >= 0)
) ENGINE = InnoDB;


-- =====================================================================
-- SECTION 7 : ORDERS — Mutable order state
-- =====================================================================
-- Stores only the latest state of each order.
-- Full lifecycle history is kept in order_events (Section 9A).

CREATE TABLE orders (
    order_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    client_id      BIGINT UNSIGNED NOT NULL,  -- whose funds / holdings are affected
    trader_id      BIGINT UNSIGNED NOT NULL,  -- who placed the order on the client's behalf
    instrument_id  INT UNSIGNED    NOT NULL,

    side           ENUM('BUY', 'SELL') NOT NULL,
    order_type     ENUM('MARKET', 'LIMIT') NOT NULL,
    time_in_force  ENUM('DAY', 'GTC', 'IOC', 'FOK') NOT NULL DEFAULT 'DAY',

    price          DECIMAL(18,4) NULL,         -- NULL only for MARKET orders
    original_qty   BIGINT UNSIGNED NOT NULL,
    remaining_qty  BIGINT UNSIGNED NOT NULL,

    status         ENUM('PENDING', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED')
                   NOT NULL DEFAULT 'PENDING',

    created_at     TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (client_id)
        REFERENCES clients(user_id),

    FOREIGN KEY (trader_id)
        REFERENCES traders(user_id),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    CHECK (remaining_qty <= original_qty),
    CHECK (order_type <> 'LIMIT' OR price IS NOT NULL),  -- LIMIT orders must carry a price

    -- Used by the matching engine: best price first, earliest order first.
    INDEX idx_order_book    (instrument_id, side, status, price, created_at),
    INDEX idx_orders_client (client_id,  created_at),
    INDEX idx_orders_trader (trader_id,  created_at)
) ENGINE = InnoDB;


-- =====================================================================
-- SECTION 8 : TRADES — Immutable execution log
-- =====================================================================
-- Rows are never updated after insert.  The trigger on this table
-- updates last_traded_price in instruments.

CREATE TABLE trades (
    trade_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    instrument_id  INT UNSIGNED    NOT NULL,
    buy_order_id   BIGINT UNSIGNED NOT NULL,
    sell_order_id  BIGINT UNSIGNED NOT NULL,
    price          DECIMAL(18,4)   NOT NULL,
    quantity       BIGINT UNSIGNED NOT NULL,
    executed_at    TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    FOREIGN KEY (buy_order_id)
        REFERENCES orders(order_id),

    FOREIGN KEY (sell_order_id)
        REFERENCES orders(order_id),

    -- Drives the "View All Trades" feed sorted by time per instrument.
    INDEX idx_trades_symbol_time (instrument_id, executed_at),

    CHECK (quantity > 0 AND price > 0)
) ENGINE = InnoDB;


-- =====================================================================
-- SECTION 9 : AUDIT LOG — Append-only event log
-- =====================================================================
-- Every login, CRUD operation, and trade execution lands here.
-- actor_user_id is NULL for system-generated events.

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


-- Login attempts are stored separately because a failed login attempt
-- does not mutate any application row that a trigger could hook into.
-- The application writes here directly and may also mirror to audit_log.
CREATE TABLE login_attempts (
    attempt_id   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50) NOT NULL,
    success      BOOLEAN     NOT NULL,
    ip_address   VARCHAR(45),                           -- IPv6-safe (max 45 chars)
    attempted_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_login_username_time (username, attempted_at)
) ENGINE = InnoDB;


-- =====================================================================
-- SECTION 9A : ORDER EVENTS — Immutable order lifecycle log
-- =====================================================================
-- Every state transition on an order is recorded here.
-- The orders table holds only the current state.

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


-- =====================================================================
-- SECTION 9B : WALLET TRANSACTIONS — Immutable cash ledger
-- =====================================================================
-- Every cash movement is recorded here.
-- The wallets table holds only the current balance snapshot.

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


-- =====================================================================
-- SECTION 9C : HOLDING TRANSACTIONS — Immutable portfolio ledger
-- =====================================================================
-- Complete history of every portfolio movement per client.

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


-- =====================================================================
-- SECTION 9D : TRADER ↔ CLIENT ASSIGNMENTS
-- =====================================================================
-- Controls which traders are authorised to place orders for which clients.
-- Only an ADMIN can create an assignment (enforced in the application layer).

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


-- =====================================================================
-- SECTION 9E : MARKET CALENDAR
-- =====================================================================
-- Each row represents one trading day.
-- NULL open/close times indicate a non-trading day (HOLIDAY).

CREATE TABLE market_calendar (
    market_date   DATE PRIMARY KEY,
    session_type  ENUM('REGULAR', 'HOLIDAY', 'HALF_DAY', 'SPECIAL') NOT NULL,
    market_open   TIME NULL,
    market_close  TIME NULL,
    remarks       VARCHAR(255)
) ENGINE = InnoDB;


-- =====================================================================
-- SECTION 9F : PRICE HISTORY
-- =====================================================================
-- Tick-by-tick price records used for charts and analytics.

CREATE TABLE price_history (
    history_id      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    instrument_id   INT UNSIGNED    NOT NULL,
    trade_price     DECIMAL(18,4)   NOT NULL,
    traded_volume   BIGINT UNSIGNED NOT NULL,
    trade_time      TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (instrument_id)
        REFERENCES instruments(instrument_id),

    INDEX idx_price_history (instrument_id, trade_time)
) ENGINE = InnoDB;


SET FOREIGN_KEY_CHECKS = 1;


-- =====================================================================
-- SECTION 11 : TRIGGERS
-- =====================================================================

DELIMITER $$

-- Enforce creator rules:
--   • NULL created_by is allowed only for the very first row (seed admin).
--   • Every subsequent user must be created by an ADMIN.
CREATE TRIGGER trg_users_enforce_creator
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    DECLARE creator_role VARCHAR(20);

    IF NEW.created_by IS NULL THEN
        -- Only the initial seed admin may omit a creator.
        IF (SELECT COUNT(*) FROM users) > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Only the initial system admin may have created_by = NULL';
        END IF;
    ELSE
        SELECT r.role_name INTO creator_role
        FROM   users u
        JOIN   roles r ON u.role_id = r.role_id
        WHERE  u.user_id = NEW.created_by;

        IF creator_role IS NULL OR creator_role <> 'ADMIN' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'New users can only be created by an ADMIN';
        END IF;
    END IF;
END$$


-- Audit: record every new user creation.
CREATE TRIGGER trg_users_audit_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
    VALUES (
        NEW.created_by,
        'CREATE',
        'users',
        NEW.user_id,
        JSON_OBJECT('username', NEW.username, 'role_id', NEW.role_id)
    );
END$$


-- Audit: record status changes (SUSPEND, ACTIVATE, etc.) on a user.
CREATE TRIGGER trg_users_audit_update
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status THEN
        INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
        VALUES (
            NEW.user_id,
            IF(NEW.status = 'SUSPENDED', 'SUSPEND', 'UPDATE'),
            'users',
            NEW.user_id,
            JSON_OBJECT('old_status', OLD.status, 'new_status', NEW.status)
        );
    END IF;
END$$


-- Audit: record every new order.
CREATE TRIGGER trg_orders_audit_insert
AFTER INSERT ON orders
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
    VALUES (
        NEW.trader_id,
        'ORDER_PLACED',
        'orders',
        NEW.order_id,
        JSON_OBJECT(
            'client_id', NEW.client_id,
            'side',      NEW.side,
            'qty',       NEW.original_qty,
            'price',     NEW.price
        )
    );
END$$


-- Audit: record order cancellations.
CREATE TRIGGER trg_orders_audit_cancel
AFTER UPDATE ON orders
FOR EACH ROW
BEGIN
    IF NEW.status = 'CANCELLED' AND OLD.status <> 'CANCELLED' THEN
        INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
        VALUES (NEW.trader_id, 'ORDER_CANCELLED', 'orders', NEW.order_id, NULL);
    END IF;
END$$


-- On every trade:
--   1. Update the instrument's last traded price.
--   2. Write a TRADE_EXECUTED entry to the audit log.
-- NOTE: Holdings and wallet updates belong in the application's settlement
--       transaction (see Section 13), because they span two parties and must
--       commit atomically with the trade insert.
CREATE TRIGGER trg_trades_after_insert
AFTER INSERT ON trades
FOR EACH ROW
BEGIN
    UPDATE instruments
    SET    last_traded_price = NEW.price
    WHERE  instrument_id = NEW.instrument_id;

    INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
    VALUES (
        NULL,
        'TRADE_EXECUTED',
        'trades',
        NEW.trade_id,
        JSON_OBJECT(
            'instrument_id', NEW.instrument_id,
            'price',         NEW.price,
            'quantity',      NEW.quantity
        )
    );
END$$

DELIMITER ;


-- =====================================================================
-- SECTION 12 : VIEWS
-- =====================================================================

-- Admin dashboard: platform-wide counts and today's turnover.
CREATE VIEW v_admin_dashboard AS
SELECT
    (SELECT COUNT(*) FROM users)                                             AS total_users,
    (SELECT COUNT(*) FROM traders)                                           AS total_traders,
    (SELECT COUNT(*) FROM clients)                                           AS total_clients,
    (SELECT COUNT(*) FROM users  WHERE role_id = 2 AND status = 'ACTIVE')   AS active_traders,
    (SELECT COUNT(*) FROM trades WHERE DATE(executed_at) = CURDATE())        AS trades_today,
    (SELECT COALESCE(SUM(price * quantity), 0)
       FROM trades WHERE DATE(executed_at) = CURDATE())                      AS turnover_today;


-- Live order book per instrument — feeds the trader's order-book view.
CREATE VIEW v_order_book AS
SELECT
    instrument_id,
    side,
    price,
    SUM(remaining_qty)  AS qty_at_price,
    MIN(created_at)     AS earliest_order
FROM orders
WHERE status IN ('PENDING', 'PARTIALLY_FILLED')
GROUP BY instrument_id, side, price;


-- Client portfolio: quantity, average cost, current market value, unrealised P&L.
CREATE VIEW v_portfolio_value AS
SELECT
    h.client_id,
    h.instrument_id,
    i.symbol,
    h.quantity,
    h.avg_buy_price,
    i.last_traded_price,
    (h.quantity * i.last_traded_price)                        AS current_value,
    (h.quantity * (i.last_traded_price - h.avg_buy_price))    AS unrealized_pnl
FROM holdings   h
JOIN instruments i ON h.instrument_id = i.instrument_id;


-- =====================================================================
-- SECTION 13 : EXAMPLE — ATOMIC TRADE SETTLEMENT
-- =====================================================================
-- Run this block inside your Java service layer as a single transaction.
-- A trade must NEVER be recorded without the matching balance and
-- holdings update committing at the same time.

-- START TRANSACTION;
--
--   -- Debit buyer's wallet (optimistic lock via version check)
--   UPDATE wallets
--   SET    cash_balance = cash_balance - (:qty * :price),
--          version      = version + 1
--   WHERE  client_id = :buyer_id
--     AND  version   = :buyer_version;
--
--   -- Credit seller's wallet
--   UPDATE wallets
--   SET    cash_balance = cash_balance + (:qty * :price),
--          version      = version + 1
--   WHERE  client_id = :seller_id
--     AND  version   = :seller_version;
--
--   -- Add shares to buyer's holdings (weighted-average cost on duplicate)
--   INSERT INTO holdings (client_id, instrument_id, quantity, avg_buy_price)
--   VALUES (:buyer_id, :instrument_id, :qty, :price)
--   ON DUPLICATE KEY UPDATE
--       avg_buy_price = ((avg_buy_price * quantity) + (:price * :qty)) / (quantity + :qty),
--       quantity      = quantity + :qty;
--
--   -- Remove shares from seller's holdings
--   UPDATE holdings
--   SET    quantity = quantity - :qty
--   WHERE  client_id     = :seller_id
--     AND  instrument_id = :instrument_id;
--
--   -- Record the execution (triggers fire here)
--   INSERT INTO trades (instrument_id, buy_order_id, sell_order_id, price, quantity)
--   VALUES (:instrument_id, :buy_order_id, :sell_order_id, :price, :qty);
--
--   -- Advance the order state machine for both sides
--   UPDATE orders
--   SET    remaining_qty = :new_remaining,
--          status        = :new_status
--   WHERE  order_id IN (:buy_order_id, :sell_order_id);
--
-- COMMIT;
