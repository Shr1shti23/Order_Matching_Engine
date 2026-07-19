# Bank Trading Platform Database Repository

This repository represents the production-quality relational schema and seeding structures for an enterprise bank trading platform.

---

## 1. Directory Structure

```text
database/
│
├── install.sql                     # Main installation script
├── rebuild.sql                     # Drop and reinstall script
├── reset_database.sql              # Transactional data wiper
├── uninstall.sql                   # Drop database script
│
├── 00_database/
│   └── 00_create_db.sql            # Database and encoding configuration
│
├── 01_tables/                      # Table definitions (ordered by FK dependencies)
│   ├── 01_roles.sql
│   ├── 02_permissions.sql
│   ├── 03_role_permissions.sql
│   ├── 04_users.sql
│   ├── 05_traders.sql
│   ├── 06_clients.sql
│   ├── 07_wallets.sql
│   ├── 08_instruments.sql
│   ├── 09_trading_rules.sql
│   ├── 10_holdings.sql
│   ├── 11_orders.sql
│   ├── 12_trades.sql
│   ├── 13_audit_log.sql
│   ├── 14_login_attempts.sql
│   ├── 15_order_events.sql
│   ├── 16_wallet_transactions.sql
│   ├── 17_holding_transactions.sql
│   ├── 18_trader_client_assignments.sql
│   ├── 19_market_calendar.sql
│   └── 20_price_history.sql
│
├── 02_indexes/
│   └── placeholder.sql             # Inline indexes exist directly in 01_tables/
│
├── 03_triggers/                    # System audit and business rule triggers
│   ├── trg_users_enforce_creator.sql
│   ├── trg_users_audit_insert.sql
│   ├── trg_users_audit_update.sql
│   ├── trg_orders_audit_insert.sql
│   ├── trg_orders_audit_cancel.sql
│   └── trg_trades_after_insert.sql
│
├── 04_views/                       # Aggregation & operational views
│   ├── v_admin_dashboard.sql
│   ├── v_order_book.sql
│   └── v_portfolio_value.sql
│
├── 05_procedures/
│   └── placeholder.sql             # Empty placeholder for stored procedures
│
├── 06_functions/
│   └── placeholder.sql             # Empty placeholder for functions
│
├── 07_seed/                        # Core system config and master seed data
│   ├── 01_roles_permissions.sql
│   ├── 02_system_admin.sql
│   └── 03_instruments.sql
│
├── 08_test_data/                   # Development/Testing environment mock datasets
│   ├── 01_traders_clients.sql
│   └── 02_wallets_holdings_trades.sql
│
├── 09_scripts/                     # Replica/backup installation scripts
│   ├── install.sql
│   ├── rebuild.sql
│   ├── reset_database.sql
│   └── uninstall.sql
│
└── docs/                           # Schema documentation
    └── Schema.md
```

---

## 2. Installation and Execution Instructions

### Installation

To run a complete installation of the schema, triggers, views, core seed configurations, and development test data, execute `install.sql` from within the `database/` folder using the MySQL command-line utility:

```bash
cd database
mysql -u <username> -p < install.sql
```

### Rebuilding the Schema

To drop the existing database completely and execute a fresh install:

```bash
cd database
mysql -u <username> -p < rebuild.sql
```

### Resetting Transactional Data

To clear out transactional tables (trades, orders, holdings, transactions, and audit logs) while maintaining the core metadata (roles, permissions, users, instruments):

```bash
cd database
mysql -u <username> -p < reset_database.sql
```

### Uninstalling the Database

To remove the database and all its contents:

```bash
cd database
mysql -u <username> -p < uninstall.sql
```

---

## 3. Database Architecture & Execution Order

The execution sequence must be strictly maintained due to foreign key relationships:

1. **Database Initialization**: `00_database/00_create_db.sql` creates the database.
2. **Table Schema creation**: `01_tables/` directory is run from `01` to `20` sequentially.
3. **Triggers creation**: Triggers in `03_triggers/` are bound to tables.
4. **Views creation**: Views in `04_views/` are compiled.
5. **Seeding core configuration**: Roles, permissions, bootstrap admin, and tradeable instruments are loaded via `07_seed/`.
6. **Seeding mock test data**: Traders, clients, active wallets, assignments, holdings, transactions, and trade executions are loaded via `08_test_data/`.
