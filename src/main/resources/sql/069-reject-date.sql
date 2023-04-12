ALTER TABLE lystore.order_reject
ADD COLUMN reject_date date NOT NULL DEFAULT CURRENT_DATE;


ALTER TABLE lystore.order_client_equipment
ADD COLUMN done_date date;

ALTER TABLE lystore."order-region-equipment"
ADD COLUMN done_date date;


DROP VIEW lystore.alloperationorders;
DROP VIEW lystore.allOrders;

CREATE   View lystore.allOrders  as
                     (SELECT ore.id,
                            ore.price AS "price TTC",
                            -1 as priceHT,
                            -1 as tax_amount,
                            ore.amount,
                            ore.creation_date,
                            ore.modification_date,
                            ore.NAME,
                            ore.summary,
                            ore.description,
                            ore.image,
                            ore.status,
                            ore.id_contract,
                            ore.equipment_key,
                            ore.id_campaign,
                            ore.id_structure,
                            ore.cause_status,
                            ore.number_validation,
                            ore.id_order,
                            ore.comment,
                            ore.rank AS "prio",
                            NULL     AS price_proposal,
                            ore.id_project,
                            ore.id_order_client_equipment,
                            NULL AS program,
                            NULL AS action,
                            ore.id_operation ,
                            ore.id_type,
                            ore.done_date,
                            NULL AS override_region
                     FROM   lystore."order-region-equipment" ore )
       UNION
                  (
                         SELECT oce.id,
                                CASE
                                       WHEN price_proposal IS NULL THEN price + (price*tax_amount/100)
                                       ELSE price_proposal
                                END AS "price TTC",
                                price as priceHT,
                                tax_amount,
                                amount,
                                creation_date,
                                NULL AS modification_date,
                                NAME,
                                summary,
                                description,
                                image,
                                status,
                                id_contract,
                                equipment_key,
                                id_campaign,
                                id_structure,
                                cause_status,
                                number_validation,
                                id_order,
                                comment,
                                rank AS "prio",
                                price_proposal,
                                id_project,
                                NULL AS id_order_client_equipment,
                                program,
                                action,
                                id_operation,
                                id_type,
                                done_date,
                                override_region
                         FROM   lystore.order_client_equipment oce);

CREATE VIEW lystore.alloperationorders
 AS
 SELECT DISTINCT o.id,
    round(sum(((( SELECT
                CASE
                    WHEN orders.price_proposal IS NOT NULL THEN 0::numeric
                    WHEN orders.override_region IS NULL OR orders.override_region IS TRUE THEN 0::numeric
                    WHEN sum(oco.price + oco.price * oco.tax_amount / 100::numeric * oco.amount::numeric) IS NULL THEN 0::numeric
                    ELSE sum(oco.price + oco.price * oco.tax_amount / 100::numeric * oco.amount::numeric)
                END AS sum
           FROM lystore.order_client_options oco
          WHERE oco.id_order_client_equipment = orders.id AND o.id = orders.id_operation)) +
        CASE
            WHEN orders.override_region IS TRUE THEN 0::numeric
            WHEN orders.price_proposal IS NOT NULL THEN orders.price_proposal
            ELSE round(orders."price TTC", 2)
        END) * orders.amount::numeric), 2) AS amount
   FROM lystore.operation o
     JOIN lystore.allorders orders ON orders.id_operation = o.id
  GROUP BY o.id;

ALTER TABLE lystore.alloperationorders
    OWNER TO "web-education";