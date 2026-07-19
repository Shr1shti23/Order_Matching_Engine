-- =====================================================================
-- BANK TRADING PLATFORM — UNINSTALL SCRIPT
-- =====================================================================
-- Drops the trading platform database completely.
-- Usage: mysql -u username -p < uninstall.sql
-- =====================================================================

DROP DATABASE IF EXISTS trading_platform;

SELECT 'Database Uninstalled Successfully (trading_platform dropped).' AS status;
