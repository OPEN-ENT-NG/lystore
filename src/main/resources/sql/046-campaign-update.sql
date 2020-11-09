ALTER TABLE lystore.campaign
ADD COLUMN start_date DATE ,
ADD COLUMN  end_date DATE,
ADD COLUMN  automatic_close BOOLEAN DEFAULT false
;


CREATE OR REPLACE FUNCTION lystore.update_old_end_date() RETURNS TRIGGER AS $$
BEGIN
         UPDATE lystore.campaign
         SET end_date = NULL
		 WHERE end_date <  NOW() AND automatic_close = true AND accessible = true;
		 RETURN NEW ;
END;
$$ LANGUAGE 'plpgsql';


CREATE TRIGGER update_old_end_date_trigger AFTER
UPDATE ON lystore.campaign
FOR EACH ROW EXECUTE PROCEDURE lystore.update_old_end_date();