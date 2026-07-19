# Bank Trading Platform Database Schema Documentation

This document describes the structure, relationships, indexes, and purposes of every table in the database schema.

---

## 1. roles

- **Purpose**: Defines system roles to control access and permissions.
- **Primary Key**: `role_id` (TINYINT UNSIGNED)
- **Foreign Keys**: None
- **Indexes**: Unique index on `role_name`.
- **Relationships**: One-to-many with `role_permissions` and `users`.
- **Used by**: RBAC / User Authentication Module.

---

## 2. permissions

- **Purpose**: Individual granular permissions that can be granted to roles.
- **Primary Key**: `permission_id` (SMALLINT UNSIGNED)
- **Foreign Keys**: None
- **Indexes**: Unique index on `permission_name`.
- **Relationships**: One-to-many with `role_permissions`.
- **Used by**: RBAC / User Authorization Module.

---

## 3. role_permissions

- **Purpose**: Mapping table linking roles with permissions (Many-to-Many).
- **Primary Key**: Composite `(role_id, permission_id)`
- **Foreign Keys**: 
  - `role_id` referencing `roles(role_id)` ON DELETE CASCADE
  - `permission_id` referencing `permissions(permission_id)` ON DELETE CASCADE
- **Indexes**: Covered by Primary Key.
- **Relationships**: Joins `roles` and `permissions`.
- **Used by**: RBAC Module.

---

## 4. users

- **Purpose**: Store basic user identity credentials, role assignment, status, and audit data.
- **Primary Key**: `user_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `role_id` referencing `roles(role_id)`
  - `created_by` referencing `users(user_id)`
- **Indexes**: 
  - Unique index on `username` and `email`
  - Index `idx_users_role_status` on `(role_id, status)`
- **Relationships**: Supertype table for `traders` and `clients`.
- **Used by**: User Management & Authentication Module.

---

## 5. traders

- **Purpose**: Extension of the `users` table storing trader-specific employee attributes.
- **Primary Key**: `user_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `user_id` referencing `users(user_id)` ON DELETE CASCADE
- **Indexes**: Unique index on `employee_code`.
- **Relationships**: Subtype of `users`. One-to-many with `orders` and `trader_client_assignments`.
- **Used by**: Trader Management & Execution Module.

---

## 6. clients

- **Purpose**: Extension of the `users` table storing client-specific KYC and risk metadata.
- **Primary Key**: `user_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `user_id` referencing `users(user_id)` ON DELETE CASCADE
- **Indexes**: None
- **Relationships**: Subtype of `users`. One-to-one with `wallets`. One-to-many with `orders`, `holdings`, `trader_client_assignments`, and `holding_transactions`.
- **Used by**: Client Onboarding & Portfolio Module.

---

## 7. wallets

- **Purpose**: Track the current fiat currency cash balances and optimistic locking version for clients.
- **Primary Key**: `wallet_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `client_id` referencing `clients(user_id)` ON DELETE CASCADE
- **Indexes**: Unique index on `client_id`.
- **Relationships**: Linked to `clients`. One-to-many with `wallet_transactions`.
- **Used by**: Treasury & Wallet Service Module.

---

## 8. instruments

- **Purpose**: Contains the master list of tradeable instruments (stocks, bonds, ETFs, etc.) managed by administrators.
- **Primary Key**: `instrument_id` (INT UNSIGNED)
- **Foreign Keys**: 
  - `created_by` referencing `users(user_id)`
- **Indexes**: Unique index on `symbol`.
- **Relationships**: Linked to `holdings`, `orders`, `trades`, `trading_rules`, `holding_transactions`, and `price_history`.
- **Used by**: Market Data & Instrument Management Module.

---

## 9. trading_rules

- **Purpose**: Defines risk management rules and bounds at the global, client, or instrument levels.
- **Primary Key**: `rule_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `client_id` referencing `clients(user_id)`
  - `instrument_id` referencing `instruments(instrument_id)`
  - `created_by` referencing `users(user_id)`
- **Indexes**: Checked constraints validate logical bounds based on scope.
- **Relationships**: Associated with `clients` and `instruments`.
- **Used by**: Pre-Trade Risk Engine Module.

---

## 10. holdings

- **Purpose**: Stores the consolidated quantity and average buy price of instruments owned by clients.
- **Primary Key**: `holding_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `client_id` referencing `clients(user_id)`
  - `instrument_id` referencing `instruments(instrument_id)`
- **Indexes**: Unique key `uq_client_instrument` on `(client_id, instrument_id)`.
- **Relationships**: Linked to `clients` and `instruments`.
- **Used by**: Portfolio & Position Management Module.

---

## 11. orders

- **Purpose**: Tracks order lifecycle, state changes, prices, and quantities submitted for execution.
- **Primary Key**: `order_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `client_id` referencing `clients(user_id)`
  - `trader_id` referencing `traders(user_id)`
  - `instrument_id` referencing `instruments(instrument_id)`
- **Indexes**: 
  - `idx_order_book` on `(instrument_id, side, status, price, created_at)`
  - `idx_orders_client` on `(client_id, created_at)`
  - `idx_orders_trader` on `(trader_id, created_at)`
- **Relationships**: One-to-many with `trades` and `order_events`.
- **Used by**: Order Management & Matching Engine Module.

---

## 12. trades

- **Purpose**: Immutable execution logs of matched buy and sell orders.
- **Primary Key**: `trade_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `instrument_id` referencing `instruments(instrument_id)`
  - `buy_order_id` referencing `orders(order_id)`
  - `sell_order_id` referencing `orders(order_id)`
- **Indexes**: 
  - `idx_trades_symbol_time` on `(instrument_id, executed_at)`
- **Relationships**: Feeds trigger updates to `instruments`, logs in `holding_transactions`, `wallet_transactions`, and `price_history`.
- **Used by**: Execution & Clearing Module.

---

## 13. audit_log

- **Purpose**: Centralized log of all system changes, status updates, order submissions, and events.
- **Primary Key**: `audit_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `actor_user_id` referencing `users(user_id)`
- **Indexes**: 
  - `idx_audit_entity` on `(entity_type, entity_id)`
  - `idx_audit_actor_time` on `(actor_user_id, created_at)`
- **Used by**: Security & Auditing Module.

---

## 14. login_attempts

- **Purpose**: Independent logging of login attempts for security and intrusion detection.
- **Primary Key**: `attempt_id` (BIGINT UNSIGNED)
- **Foreign Keys**: None
- **Indexes**: 
  - `idx_login_username_time` on `(username, attempted_at)`
- **Used by**: Authentication & Security Module.

---

## 15. order_events

- **Purpose**: Complete historical event audit log of order lifecycle state changes.
- **Primary Key**: `event_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `order_id` referencing `orders(order_id)` ON DELETE CASCADE
  - `actor_user_id` referencing `users(user_id)`
- **Indexes**: 
  - `idx_order_events` on `(order_id, created_at)`
- **Used by**: Order Auditing & History Module.

---

## 16. wallet_transactions

- **Purpose**: Cash ledger tracking every credit and debit to user wallets.
- **Primary Key**: `transaction_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `wallet_id` referencing `wallets(wallet_id)`
  - `trade_id` referencing `trades(trade_id)`
- **Indexes**: 
  - `idx_wallet_history` on `(wallet_id, created_at)`
- **Used by**: Cash Ledger & Accounting Module.

---

## 17. holding_transactions

- **Purpose**: Portfolio ledger recording every addition or subtraction of securities.
- **Primary Key**: `transaction_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `client_id` referencing `clients(user_id)`
  - `instrument_id` referencing `instruments(instrument_id)`
  - `trade_id` referencing `trades(trade_id)`
- **Indexes**: 
  - `idx_portfolio_history` on `(client_id, instrument_id, created_at)`
- **Used by**: Settlement & Custody Ledger Module.

---

## 18. trader_client_assignments

- **Purpose**: Defines relationship-manager pairings authorizing traders to place orders on behalf of clients.
- **Primary Key**: `assignment_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `trader_id` referencing `traders(user_id)`
  - `client_id` referencing `clients(user_id)`
  - `assigned_by` referencing `users(user_id)`
- **Indexes**: Unique key `uq_assignment` on `(trader_id, client_id)`.
- **Used by**: Trader Authorization & Client Services Module.

---

## 19. market_calendar

- **Purpose**: Holds information regarding session types, holiday closures, and market hours.
- **Primary Key**: `market_date` (DATE)
- **Foreign Keys**: None
- **Indexes**: None
- **Used by**: Market Data & Schedule Service Module.

---

## 20. price_history

- **Purpose**: Records historical tick-by-tick prices of instruments for graphing and analytics.
- **Primary Key**: `history_id` (BIGINT UNSIGNED)
- **Foreign Keys**: 
  - `instrument_id` referencing `instruments(instrument_id)`
- **Indexes**: 
  - `idx_price_history` on `(instrument_id, trade_time)`
- **Used by**: Analytics & Market Feeds Module.
