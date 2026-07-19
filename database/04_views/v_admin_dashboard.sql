CREATE VIEW v_admin_dashboard AS
SELECT
    (SELECT COUNT(*) FROM users)                                             AS total_users,
    (SELECT COUNT(*) FROM traders)                                           AS total_traders,
    (SELECT COUNT(*) FROM clients)                                           AS total_clients,
    (SELECT COUNT(*) FROM users  WHERE role_id = 2 AND status = 'ACTIVE')   AS active_traders,
    (SELECT COUNT(*) FROM trades WHERE DATE(executed_at) = CURDATE())        AS trades_today,
    (SELECT COALESCE(SUM(price * quantity), 0)
       FROM trades WHERE DATE(executed_at) = CURDATE())                      AS turnover_today;
