
CREATE OR REPLACE FUNCTION lystore.order_without_ref()
RETURNS trigger AS  $$
BEGIN
DELETE FROM Lystore.order
WHERE id NOT IN (
    SELECT DISTINCT id_order FROM Lystore.allorders
     WHERE id_order IS NOT NULL
     ORDER BY id_order );
    RETURN NULL;
END;
 $$  LANGUAGE plpgsql;
