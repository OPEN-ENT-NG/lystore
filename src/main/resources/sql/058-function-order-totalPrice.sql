CREATE OR REPLACE FUNCTION lystore.order_total(oce_id BIGINT) RETURNS NUMERIC AS $$

BEGIN
RETURN( SELECT ROUND(( (SELECT CASE WHEN order_client_equipment.price_proposal IS NOT NULL THEN 0
                                    WHEN order_client_equipment.override_region IS NULL THEN 0
                                    WHEN SUM(order_client_options.price + (
                                              ( order_client_options.price * order_client_options.tax_amount )
                                              / 100 ) * order_client_options.amount) IS NULL THEN 0
                                    ELSE SUM(order_client_options.price + (
                                              ( order_client_options.price * order_client_options.tax_amount )
                                              / 100 ) * order_client_options.amount ) END
                        FROM lystore.order_client_options
                        WHERE order_client_options.id_order_client_equipment = order_client_equipment.id) +
                       ( CASE WHEN order_client_equipment.price_proposal IS NOT NULL
                                  THEN order_client_equipment.price_proposal
                              ELSE order_client_equipment.price + order_client_equipment.price * order_client_equipment.tax_amount / 100
                       END)
                        * order_client_equipment.amount ), 2)
        FROM lystore.order_client_equipment
        WHERE oce_id = order_client_equipment.id);
END;

$$ LANGUAGE plpgsql;