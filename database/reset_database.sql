-- =====================================================================
-- BANK TRADING PLATFORM — DATABASE RESET SCRIPT
-- =====================================================================
-- Clears all transactional and activity log tables.
-- Retains master configuration data (roles, permissions, admin users, instruments).
-- Usage: mysql -u username -p < reset_database.sql
-- =====================================================================

USE trading_platform;

SET FOREIGN_KEY_CHECKS = 0;

-- Clear transaction history and audit tables
TRUNCATE TABLE price_history;
TRUNCATE TABLE holding_transactions;
TRUNCATE TABLE wallet_transactions;
TRUNCATE TABLE order_events;
TRUNCATE TABLE audit_log;
TRUNCATE TABLE login_attempts;
TRUNCATE TABLE trades;
TRUNCATE TABLE orders;
TRUNCATE TABLE holdings;

-- Reset wallet cash balances back to 0.00
UPDATE wallets SET cash_balance = 0.00;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Database Reset Completed successfully (Transactional data cleared).' AS status;
