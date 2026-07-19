USE trading_platform;

-- =====================================================================
-- INSTRUMENTS (created by sysadmin with user_id = 1)
-- =====================================================================
INSERT INTO instruments (symbol, name, instrument_type, tick_size, lot_size, status, last_traded_price, created_by)
VALUES
    ('AAPL', 'Apple Inc.', 'STOCK', 0.0100, 1, 'ACTIVE', 175.5000, 1),
    ('MSFT', 'Microsoft Corporation', 'STOCK', 0.0100, 1, 'ACTIVE', 420.2000, 1),
    ('TSLA', 'Tesla Inc.', 'STOCK', 0.0100, 1, 'ACTIVE', 180.0000, 1),
    ('GOOGL', 'Alphabet Inc.', 'STOCK', 0.0100, 1, 'ACTIVE', 150.7500, 1);
