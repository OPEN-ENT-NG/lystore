CREATE INDEX order_client_status_index ON lystore.order_client_equipment USING btree (status);
CREATE INDEX order_client_options_idC_index ON lystore.order_client_options USING btree (id_order_client_equipment);
DROP View lystore.allOperationOrders;


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
               ON orders.id_operation = o.id