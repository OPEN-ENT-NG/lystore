ALTER TABLE lystore.order_client_equipment
  DROP  CONSTRAINT valid_status_order,
   ADD CONSTRAINT valid_status_order CHECK (
   (id_operation is not null  AND status = 'IN PROGRESS' OR STATUS='VALID' OR status = 'SENT' OR status = 'WAITING_FOR_ACCEPTANCE')
    OR (id_operation is null)) ;


CREATE OR REPLACE FUNCTION order_without_ref()
RETURNS trigger AS  $$
BEGIN
DELETE FROM Lystore.order
WHERE id NOT IN (
    SELECT DISTINCT id_order FROM Lystore.allOrders
     WHERE id_order IS NOT NULL
     ORDER BY id_order );
    RETURN NULL;
END;
 $$  LANGUAGE plpgsql;
