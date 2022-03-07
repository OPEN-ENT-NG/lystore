CREATE OR REPLACE VIEW lystore.alloperationorders
 AS
SELECT DISTINCT o.id,
                Round(SUM (( ((SELECT CASE
                                WHEN orders.price_proposal IS NOT NULL
                                    THEN 0 :: NUMERIC
                                WHEN orders.override_region IS NULL
                                    THEN 0 :: NUMERIC
                                WHEN SUM(oco.price + oco.price * oco.tax_amount / 100 :: NUMERIC * oco.amount :: NUMERIC) IS NULL
                                    THEN 0 :: NUMERIC
                           ELSE SUM(oco.price + ( ( oco.price * oco.tax_amount ) / 100 ) * oco.amount)
                           END AS SUM
                                FROM   lystore.order_client_options oco
                                WHERE  oco.id_order_client_equipment = orders.id
                                AND o.id = orders.id_operation)) +
                                ( CASE
                                    WHEN orders.override_region IS TRUE
                                        THEN 0
                                    WHEN orders.price_proposal IS NOT NULL
                                        THEN ( orders.price_proposal )
                                ELSE ( Round(orders."price TTC" , 2) )
                            END ) ) * orders.amount),2) AS amount
FROM lystore.operation o
JOIN lystore.allorders orders ON orders.id_operation = o.id
GROUP  BY o.id ;
ALTER TABLE lystore.alloperationorders
OWNER TO "web-education";

