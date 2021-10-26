ALTER TABLE lystore.order_client_equipment
  DROP  CONSTRAINT valid_status_order,
   ADD CONSTRAINT valid_status_order CHECK (
   (id_operation is not null  AND (status = 'IN PROGRESS' OR STATUS='VALID' OR status = 'SENT' OR status = 'WAITING_FOR_ACCEPTANCE'  OR status = 'DONE'))
    OR (id_operation is null)) ;

