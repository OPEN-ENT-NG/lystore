CREATE TABLE lystore.order_reject
(
    id BIGSERIAL,
    id_order BIGINT UNIQUE NOT NULL,
    comment text,
    PRIMARY KEY(id),
    CONSTRAINT fk_id_order_client_equipment FOREIGN KEY (id_order)
        REFERENCES lystore.order_client_equipment(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);