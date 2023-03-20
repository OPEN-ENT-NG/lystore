CREATE OR REPLACE FUNCTION lystore.order_total(oce_id BIGINT ,region BOOLEAN) RETURNS NUMERIC AS $$
BEGIN
IF region is true THEN
RETURN (SELECT Round(Round( ore.price,2) *ore.amount ,2) as total from lystore."order-region-equipment" as ore where ore.id = oce_id);
ELSE
RETURN(
SELECT Round( Round(CASE
			   WHEN oce.price_proposal IS NOT NULL THEN oce.price_proposal * oce.amount
               WHEN oco.total IS NULL THEN (oce.price + (
                                           oce.price * oce.tax_amount ) /
                                                       100 )

               ELSE Sum(oco.total + oce.price + ( oce.price * oce.tax_amount ) /
                                                100)
             END, 2)* oce.amount,2) AS total
FROM   lystore.order_client_equipment oce
       LEFT JOIN (SELECT Round( Sum (Round(ocoo.price,2) * 1.2 * ocoo.amount),2) AS total,
                         ocee.id                              AS id_oce
                  FROM   lystore.order_client_equipment ocee
                         INNER JOIN lystore.order_client_options ocoo
                                 ON ocoo.id_order_client_equipment = ocee.id
                  GROUP  BY ocee.id) AS oco
              ON oco.id_oce = oce.id
WHERE  oce.id = oce_id
GROUP  BY oce.amount,
          oco.total,
          oce.price,
          oce.id
);
END IF;
END;
$$ LANGUAGE plpgsql;