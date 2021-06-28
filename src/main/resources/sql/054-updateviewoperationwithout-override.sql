DROP VIEW
lystore.allOperationOrders;
CREATE VIEW
lystore.allOperationOrders  as
               SELECT DISTINCT o.id,
                 SUM(Round(( (SELECT CASE
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
                         WHERE  oco.id_order_client_equipment = orders.id)
                        + orders."price TTC" ) * orders.amount, 2) )AS amount

FROM   lystore.operation AS o
       INNER JOIN lystore.allorders orders
               ON orders.id_operation = o.id
			   WHERE orders.override_region is not true
			   	group by o.id ;