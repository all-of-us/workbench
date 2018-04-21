-- This file is being used by ApplicationTest.java to load application configuration at initialization time.
insert into config (config_id, configuration) values
  ('main', '{"firecloud":{"billingAccountId":"014D91-FCB792-33D2C0","billingProjectPrefix":"aou-test-f1-","debugEndpoints":false,"enforceRegistered":false,"jupyterUserScriptUri":"gs://all-of-us-workbench-test-scripts/setup_notebook_cluster.sh","registeredDomainName":"all-of-us-registered-test"},"auth":{"serviceAccountApiUsers":["all-of-us-workbench-test@appspot.gserviceaccount.com"]},"cdr":{"defaultCdrVersion":"Test Registered CDR"},"googleCloudStorageService":{"credentialsBucketName":"all-of-us-workbench-test-credentials"},"googleDirectoryService":{"gSuiteDomain":"fake-research-aou.org"},"server":{"stackdriverApiKey":"AIzaSyDPoX4Eg7-_FVKi7JFzEKaJpZ4IMRLaER4","projectId":"all-of-us-workbench-test"}}');

insert into config (config_id, configuration) values
  ('cdrBigQuerySchema', '{"description":"Configuration specifying the schema for tables in the CDR, including foreign key relationships. Adapted from BigQuery schema files"}');

insert into cdr_version(cdr_version_id, name, data_access_level, release_number, bigquery_project, bigquery_dataset, creation_time, num_participants, cdr_db_name, public_db_name) VALUES
  (1, 'Test Registered CDR', 1, 1, 'all-of-us-ehr-dev', 'test_merge_dec26', SYSDATE(), 942637, 'cdr20180223', 'public20180223');
