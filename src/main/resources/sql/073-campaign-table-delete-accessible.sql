DROP TRIGGER update_old_end_date_trigger ON lystore.campaign;
ALTER TABLE lystore.campaign
DROP COLUMN accessible;


CREATE OR REPLACE FUNCTION lystore.campaign_is_Open(start_date date, end_date date, automatic bool)
RETURNS boolean AS
$$
BEGIN
    IF automatic AND DATE( NOW() ) BETWEEN start_date AND end_date THEN
        RETURN true;
    ELSIF NOT automatic AND start_date <= DATE( NOW() )AND end_date IS NULL THEN
        RETURN true;
    ELSE
        RETURN false;
    END IF;
END;
$$
LANGUAGE plpgsql;

CREATE OR REPLACE VIEW lystore.campaign_status AS
SELECT id, lystore.campaign_is_Open(start_date, end_date, automatic_close) AS is_open
FROM lystore.campaign;