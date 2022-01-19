CREATE TABLE lystore.order_region_file (
    id character varying (36) NOT NULL,
    id_order_region_equipment bigint,
    filename character varying (255) NOT NULL,
    CONSTRAINT order_region_file_pkey PRIMARY KEY (id, id_order_region_equipment),
    CONSTRAINT fk_order_region_equipment_id FOREIGN KEY (id_order_region_equipment)
    REFERENCES lystore."order-region-equipment" (id) MATCH SIMPLE
    ON UPDATE NO ACTION
);