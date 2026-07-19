DELIMITER $$

CREATE TRIGGER trg_users_enforce_creator
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    DECLARE creator_role VARCHAR(20);

    IF NEW.created_by IS NULL THEN
        -- Only the initial seed admin may omit a creator.
        IF (SELECT COUNT(*) FROM users) > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Only the initial system admin may have created_by = NULL';
        END IF;
    ELSE
        SELECT r.role_name INTO creator_role
        FROM   users u
        JOIN   roles r ON u.role_id = r.role_id
        WHERE  u.user_id = NEW.created_by;

        IF creator_role IS NULL OR creator_role <> 'ADMIN' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'New users can only be created by an ADMIN';
        END IF;
    END IF;
END$$

DELIMITER ;
