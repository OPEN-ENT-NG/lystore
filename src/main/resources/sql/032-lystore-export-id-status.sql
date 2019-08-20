ALTER TABLE lystore.export
ADD COLUMN id BIGSERIAL NOT NULL,
ADD COLUMN status CHARACTER varying(10) COLLATE pg_catalog."default" NOT NULL DEFAULT 'WAITING'
CONSTRAINT status_values CHECK (status::text = ANY (ARRAY['WAITING'::CHARACTER varying, 'SUCCESS'::CHARACTER varying, 'ERROR'::CHARACTER varying]))