-- =====================================================================
-- BANK TRADING PLATFORM — UNINSTALL SCRIPT
-- =====================================================================
-- Drops the trading platform database completely.
-- Usage: mysql -u username -p < uninstall.sql
-- =====================================================================

DROP DATABASE IF EXISTS order_matching_engine;

SELECT 'Database Uninstalled Successfully (order_matching_engine dropped).' AS status;
