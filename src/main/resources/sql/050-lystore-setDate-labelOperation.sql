UPDATE lystore.label_operation
SET start_date = '2017-01-01', end_date = '2099-12-31'
WHERE start_date IS null
AND end_date IS null;