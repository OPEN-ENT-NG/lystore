ALTER TABLE lystore.instruction
ALTER COLUMN cp_adopted type VARCHAR(255);

UPDATE Lystore.instruction
SET cp_adopted = null
WHERE cp_adopted = 'false' AND submitted_to_cp is false;

UPDATE Lystore.instruction
SET cp_adopted = 'WAITING'
WHERE cp_adopted = 'false' AND submitted_to_cp is true;

UPDATE Lystore.instruction
SET cp_adopted = 'ADOPTED'
WHERE cp_adopted = 'true' AND submitted_to_cp is true;

ALTER TABLE Lystore.instruction
    ADD CONSTRAINT "cp_adopted" CHECK (cp_adopted IN ('WAITING', 'ADOPTED', 'REJECTED',null) );