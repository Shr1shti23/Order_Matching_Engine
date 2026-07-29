-- =====================================================================
-- BANK TRADING PLATFORM — DATABASE REBUILD SCRIPT
-- =====================================================================
-- Drops the database and performs a fresh installation.
-- Usage: mysql -u username -p < rebuild.sql
-- =====================================================================

DROP DATABASE IF EXISTS order_matching_engine;

SOURCE install.sql;
