CREATE OR REPLACE FUNCTION lystore.order_total(oce_id BIGINT) RETURNS NUMERIC AS $$

BEGIN
RETURN(
SELECT Round(CASE
			   WHEN oce.price_proposal IS NOT NULL THEN oce.price_proposal * oce.amount
               WHEN oco.total IS NULL THEN oce.price + (
                                           oce.price * oce.tax_amount ) /
                                                       100 *
                                                       oce.amount

               ELSE Sum(oco.total + oce.price + ( oce.price * oce.tax_amount ) /
                                                100) *
                    oce.amount
             END, 2) AS total
FROM   lystore.order_client_equipment oce
       LEFT JOIN (SELECT Sum (ocoo.price * 1.2 * ocoo.amount) AS total,
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
END;
$$ LANGUAGE plpgsql;