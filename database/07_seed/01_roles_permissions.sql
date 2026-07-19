USE trading_platform;

-- =====================================================================
-- ROLES
-- =====================================================================
INSERT INTO roles (role_name) VALUES
    ('ADMIN'),    -- role_id = 1
    ('TRADER'),   -- role_id = 2
    ('CLIENT');   -- role_id = 3

-- =====================================================================
-- PERMISSIONS
-- =====================================================================
INSERT INTO permissions (permission_name, description) VALUES
    ('MANAGE_USERS',       'Create, update, and suspend trader and client accounts'),
    ('MANAGE_INSTRUMENTS', 'Add, edit, and delist tradable instruments'),
    ('MANAGE_RULES',       'Configure trading limits and risk parameters'),
    ('VIEW_ALL_TRADES',    'View every trade executed on the platform'),
    ('PLACE_ORDER',        'Submit buy/sell orders on behalf of a client'),
    ('CANCEL_ORDER',       'Cancel a pending or partially-filled order'),
    ('VIEW_OWN_PORTFOLIO', 'View own holdings, wallet balance, and trade history');

-- =====================================================================
-- ROLE → PERMISSION MAPPING
-- =====================================================================
-- ADMIN: platform management (no trading permissions by design)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, permission_id
FROM   permissions
WHERE  permission_name IN (
    'MANAGE_USERS',
    'MANAGE_INSTRUMENTS',
    'MANAGE_RULES',
    'VIEW_ALL_TRADES'
);

-- TRADER: order execution only
INSERT INTO role_permissions (role_id, permission_id)
SELECT 2, permission_id
FROM   permissions
WHERE  permission_name IN (
    'PLACE_ORDER',
    'CANCEL_ORDER'
);

-- CLIENT: read-only self-service view
INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, permission_id
FROM   permissions
WHERE  permission_name = 'VIEW_OWN_PORTFOLIO';
