ALTER TABLE lystore."order-region-equipment"
DROP CONSTRAINT fk_order_id ,
ADD CONSTRAINT fk_order_id FOREIGN KEY (id_order)
      REFERENCES lystore.order (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;