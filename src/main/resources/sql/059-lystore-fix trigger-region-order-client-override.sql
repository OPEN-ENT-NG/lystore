DROP TRIGGER IF EXISTS check_order_no_ref ON lystore.order_client_equipment;
CREATE TRIGGER check_order_no_ref AFTER UPDATE
    ON Lystore.order_client_equipment
    FOR EACH STATEMENT
    EXECUTE PROCEDURE order_without_ref();


DROP TRIGGER IF EXISTS region_override_client_order_trigger ON lystore."order-region-equipment";
CREATE TRIGGER region_override_client_order_trigger AFTER
    INSERT
    ON lystore."order-region-equipment"
    FOR EACH ROW WHEN (NEW.id_order_client_equipment IS NOT NULL) EXECUTE PROCEDURE lystore.region_override_client_order();