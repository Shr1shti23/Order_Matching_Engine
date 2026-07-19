DELIMITER $$

CREATE TRIGGER trg_orders_audit_insert
AFTER INSERT ON orders
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (actor_user_id, action_type, entity_type, entity_id, details)
    VALUES (
        NEW.trader_id,
        'ORDER_PLACED',
        'orders',
        NEW.order_id,
        JSON_OBJECT(
            'client_id', NEW.client_id,
            'side',      NEW.side,
            'qty',       NEW.original_qty,
            'price',     NEW.price
        )
    );
END$$

DELIMITER ;
