ALTER TABLE lystore."order-region-equipment"
  ADD COLUMN id_type bigint;

  Update lystore."order-region-equipment"
set id_type = 1;

Update lystore."order-region-equipment" ore
set id_type= equipment.id_type
FROM
  (
    select
      distinct  id_type,
      id
    from
      lystore.equipment
  ) as equipment
where ore.equipment_key = equipment.id ;


DROP View lystore.allOperationOrders;
DROP View lystore.allOrders ;

CREATE View lystore.allOrders  as
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
                                override_region
                         FROM   lystore.order_client_equipment oce);



Create View lystore.allOperationOrders  as
               SELECT DISTINCT o.id,
               Round(( (SELECT CASE
                                  WHEN orders.price_proposal IS NOT NULL THEN 0
                                  WHEN orders.override_region IS NULL THEN 0
                                  WHEN Sum(oco.price + ( (
                                           oco.price * oco.tax_amount ) / 100 )
                                                       *
                                                       oco.amount) IS
                                       NULL THEN 0
                                  ELSE Sum(oco.price + ( (
                                           oco.price * oco.tax_amount ) / 100 )
                                                       *
                                                       oco.amount)
                                END
                         FROM   lystore.order_client_options oco
                         WHERE  oco.id_order_client_equipment = orders.id AND o.id = orders.id_operation)
                        + orders."price TTC" ) * orders.amount, 2) AS amount
FROM   lystore.operation AS o
       INNER JOIN lystore.allorders orders
               ON orders.id_operation = o.id;


CREATE INDEX operation_idx ON lystore.operation USING btree (id);
CREATE INDEX oce_operation_idx ON lystore.order_client_equipment USING btree (id_operation);
CREATE INDEX ore_operation_idx ON lystore."order-region-equipment" USING btree (id_operation);


