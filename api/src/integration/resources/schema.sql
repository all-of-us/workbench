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

create table concept (
  concept_id int PRIMARY KEY,
  concept_name varchar(255) NOT NULL,
  domain_id varchar(20) NULL,
  vocabulary_id varchar(20) NULL,
  concept_class_id varchar(20) NULL,
  standard_concept varchar(1) NULL,
  concept_code varchar(50) NULL,
  valid_start_date date NULL,
  valid_end_date date NULL,
  invalid_reason varchar(1),
  count_value bigint NOT NULL,
  prevalence DECIMAL NOT NULL
);
