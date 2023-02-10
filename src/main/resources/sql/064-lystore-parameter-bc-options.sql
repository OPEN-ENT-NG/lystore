
CREATE TABLE lystore.parameter_bc_options (
  name VARCHAR NOT NULL DEFAULT '',
  address VARCHAR NOT NULL DEFAULT '',
  signature VARCHAR NOT NULL DEFAULT '',
  image character varying(255) NOT NULL DEFAULT '',
  lock char(1) not null,
  constraint PK_parameter_bc_options PRIMARY KEY (lock),
  constraint CK_parameter_bc_options CHECK (lock='X')
);

INSERT INTO lystore.parameter_bc_options (name, address, signature, lock) VALUES('','','','X');