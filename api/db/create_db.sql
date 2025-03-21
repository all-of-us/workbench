CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE USER IF NOT EXISTS '${DEV_READONLY_DB_USER}'@'%' IDENTIFIED BY '${DEV_READONLY_DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${WORKBENCH_DB_USER}'@'%' IDENTIFIED BY '${WORKBENCH_DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${LIQUIBASE_DB_USER}'@'%' IDENTIFIED BY '${LIQUIBASE_DB_PASSWORD}';
-- In case the users already exist, change their passwords.
SET PASSWORD FOR '${DEV_READONLY_DB_USER}'@'%' = PASSWORD('${DEV_READONLY_DB_PASSWORD}');
SET PASSWORD FOR '${WORKBENCH_DB_USER}'@'%' = PASSWORD('${WORKBENCH_DB_PASSWORD}');
SET PASSWORD FOR '${LIQUIBASE_DB_USER}'@'%' = PASSWORD('${LIQUIBASE_DB_PASSWORD}');

-- Grant readonly access to all tables, captures Workbench, CDR, and Liquibase.
GRANT SELECT, CREATE TEMPORARY TABLES ON *.* TO '${DEV_READONLY_DB_USER}'@'%';

-- Give main db access and wildcard permission to cdr databases for workbench
-- cdr* is the older and/or local naming convention, synth_r_*, r_* is registered tier and c_* is controlled tier
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE TEMPORARY TABLES ON ${DB_NAME}.* TO '${WORKBENCH_DB_USER}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE TEMPORARY TABLES ON `cdr%`.* TO '${WORKBENCH_DB_USER}'@'%';

-- Liquibase needs to perform schema changes on the main database.
GRANT SELECT, INSERT, UPDATE, DELETE, DROP, ALTER, CREATE, INDEX, REFERENCES, CREATE TEMPORARY TABLES, CREATE VIEW ON ${DB_NAME}.* TO '${LIQUIBASE_DB_USER}'@'%';

-- workbench user needs privileges to perform schema changes on tanagra_db
GRANT SELECT, INSERT, UPDATE, DELETE, DROP, ALTER, CREATE, INDEX, REFERENCES, CREATE TEMPORARY TABLES, CREATE VIEW ON `tanagra_db`.* TO '${WORKBENCH_DB_USER}'@'%';
