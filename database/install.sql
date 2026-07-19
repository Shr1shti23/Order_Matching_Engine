-- =====================================================================
-- BANK TRADING PLATFORM — MAIN INSTALLATION SCRIPT
-- =====================================================================
-- Runs all scripts in the correct dependency order.
-- Usage: mysql -u username -p < install.sql
-- =====================================================================

-- 1. Create Database
SOURCE 00_database/00_create_db.sql;

-- Disable FK checks during table creation for safety
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Create Tables
SOURCE 01_tables/01_roles.sql;
SOURCE 01_tables/02_permissions.sql;
SOURCE 01_tables/03_role_permissions.sql;
SOURCE 01_tables/04_users.sql;
SOURCE 01_tables/05_traders.sql;
SOURCE 01_tables/06_clients.sql;
SOURCE 01_tables/07_wallets.sql;
SOURCE 01_tables/08_instruments.sql;
SOURCE 01_tables/09_trading_rules.sql;
SOURCE 01_tables/10_holdings.sql;
SOURCE 01_tables/11_orders.sql;
SOURCE 01_tables/12_trades.sql;
SOURCE 01_tables/13_audit_log.sql;
SOURCE 01_tables/14_login_attempts.sql;
SOURCE 01_tables/15_order_events.sql;
SOURCE 01_tables/16_wallet_transactions.sql;
SOURCE 01_tables/17_holding_transactions.sql;
SOURCE 01_tables/18_trader_client_assignments.sql;
SOURCE 01_tables/19_market_calendar.sql;
SOURCE 01_tables/20_price_history.sql;

-- Re-enable FK checks
SET FOREIGN_KEY_CHECKS = 1;

-- 3. Create Triggers
SOURCE 03_triggers/trg_users_enforce_creator.sql;
SOURCE 03_triggers/trg_users_audit_insert.sql;
SOURCE 03_triggers/trg_users_audit_update.sql;
SOURCE 03_triggers/trg_orders_audit_insert.sql;
SOURCE 03_triggers/trg_orders_audit_cancel.sql;
SOURCE 03_triggers/trg_trades_after_insert.sql;

-- 4. Create Views
SOURCE 04_views/v_admin_dashboard.sql;
SOURCE 04_views/v_order_book.sql;
SOURCE 04_views/v_portfolio_value.sql;

-- 5. Seed Core/Master Data
SOURCE 07_seed/01_roles_permissions.sql;
SOURCE 07_seed/02_system_admin.sql;
SOURCE 07_seed/03_instruments.sql;

-- 6. Load Test Data
SOURCE 08_test_data/01_traders_clients.sql;
SOURCE 08_test_data/02_wallets_holdings_trades.sql;

SELECT 'Installation Completed Successfully!' AS status;
