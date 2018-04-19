-- This file is being used by ApplicationTest.java to setup tables that hold application configuration at startup.
create table config (
  config_id varchar(80) PRIMARY KEY,
  configuration CLOB NOT NULL);

create table cdr_version (
  cdr_version_id bigint PRIMARY KEY,
  name varchar(80) NOT NULL,
  data_access_level tinyint NOT NULL,
  release_number smallint NOT NULL,
  bigquery_project varchar(80) NOT NULL,
  bigquery_dataset varchar(80) NOT NULL,
  creation_time datetime NOT NULL,
  num_participants bigint NOT NULL,
  cdr_db_name varchar(20),
  public_db_name varchar(20)
);
