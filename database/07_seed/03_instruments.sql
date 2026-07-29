-- =====================================================================
-- SEED DATA 03: TRADABLE INSTRUMENTS
-- For execution instructions, see database/07_seed/00_SEED_DATA_INSTRUCTIONS.md
-- =====================================================================
USE order_matching_engine;

-- =====================================================================
-- INSTRUMENTS (created by sysadmin, user_id = 1)
-- Expanded to 8 tradable assets across equities and one ETF
-- =====================================================================
INSERT INTO instruments (symbol, name, instrument_type, tick_size, lot_size, status, last_traded_price, created_by)
VALUES
    -- US Equities
    ('AAPL',      'Apple Inc.',               'STOCK', 0.0100, 1, 'ACTIVE', 175.5000, 1),
    ('MSFT',      'Microsoft Corporation',    'STOCK', 0.0100, 1, 'ACTIVE', 420.2000, 1),
    ('TSLA',      'Tesla Inc.',               'STOCK', 0.0100, 1, 'ACTIVE', 180.0000, 1),
    ('GOOGL',     'Alphabet Inc.',            'STOCK', 0.0100, 1, 'ACTIVE', 150.7500, 1),
    ('NVDA',      'NVIDIA Corporation',       'STOCK', 0.0100, 1, 'ACTIVE', 485.3000, 1),
    ('AMZN',      'Amazon.com Inc.',          'STOCK', 0.0100, 1, 'ACTIVE', 175.2000, 1),
    -- Indian Equities
    ('INFY',      'Infosys Ltd.',             'STOCK', 0.0500, 1, 'ACTIVE', 1850.0000, 1),
    ('RELIANCE',  'Reliance Industries Ltd.', 'STOCK', 0.0500, 1, 'ACTIVE', 2950.0000, 1);
