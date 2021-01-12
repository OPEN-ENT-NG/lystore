ALTER TABLE lystore.instruction
ADD COLUMN cp_adopted BOOLEAN DEFAULT FALSE
;


ALTER TABLE lystore.order_client_equipment
  DROP  CONSTRAINT valid_status_order,
   ADD CONSTRAINT valid_status_order CHECK ((id_operation is not null AND status = 'IN PROGRESS' OR STATUS='VALID' OR status = 'SENT') OR (id_operation is null)) ;

