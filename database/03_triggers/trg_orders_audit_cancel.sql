DELIMITER $$

CREATE TRIGGER trg_orders_audit_cancel
AFTER UPDATE ON orders
FOR EACH ROW
BEGIN
    IF NEW.status = 'CANCELLED' AND OLD.status <> 'CANCELLED' THEN
        INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
        VALUES (NEW.trader_id, 'ORDER_CANCELLED', 'orders', NEW.order_id, NULL);
    END IF;
END$$

DELIMITER ;
