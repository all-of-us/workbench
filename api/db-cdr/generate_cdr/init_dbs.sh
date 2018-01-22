#!/bin/bash

# This generates new cloudsql database for a cdr
# note dev-up must be run to generate the schema
# note run-local-data-migrations must be run to generate hard coded data from liquibase

set -xeuo pipefail
IFS=$'\n\t'

# Make the vocabulary table from cdr with no changes
#bq --project=all-of-us-ehr-dev cp test_merge_dec26.vocabulary test_vocabulary_ppi.vocabulary
project="all-of-us-workbench-test"
bqcdr="test_merge_dec26"
cloudsql_instance=workbenchtest
cdr_db_name=cdr
gcs_bucket=gs://all-of-us-workbench-cloudsql-create

# output files
tmp_dir="/tmp"
cdr_schema_file="cdr_schema.sql"
public_schema_file="public_schema.sql"
hard_data_file="hard_data.sql"

# Dump cdr schema and copy to gcs
echo "Dumping cdr schema "
mysqldump -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} \
    --no-data --add-drop-table --ignore-table=$cdr_db_name.DATABASECHANGELOG \
    --ignore-table=$cdr_db_name.DATABASECHANGELOGLOCK $cdr_db_name --database cdr \
    --add-drop-database > $tmp_dir/$cdr_schema_file
gsutil cp $tmp_dir/$cdr_schema_file $gcs_bucket/$cdr_schema_file
rm $tmp_dir/$cdr_schema_file

# Dump the data from the hardcoded data tables -- criteria, db_domain... and copy to gcs
echo "Dumping Hard coded data "
mysqldump -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} \
     --add-drop-table --disable-keys cdr db_domain criteria > $tmp_dir/$hard_data_file
gsutil cp $tmp_dir/$hard_data_file $gcs_bucket/$hard_data_file
rm $tmp_dir/$hard_data_file

# Todo  maybe not hardcode service account name ?
SERVICE_ACCOUNT=all-of-us-workbench-test@appspot.gserviceaccount.com
gcloud auth activate-service-account $SERVICE_ACCOUNT --key-file=$GOOGLE_APPLICATION_CREDENTIALS

# Grant access to buckets for service account for cloudsql
SQL_SERVICE_ACCOUNT=`gcloud sql instances describe --project $project \
    --account $SERVICE_ACCOUNT $cloudsql_instance | grep serviceAccountEmailAddress \
    | cut -d: -f2`
# Trim leading whitespace from sql service account
SQL_SERVICE_ACCOUNT=${SQL_SERVICE_ACCOUNT//[[:blank:]]/}

echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to bucket $gcs_bucket..."
# Note, this only applies to files already existing in the bucket . So must rerun after adding files
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:W $gcs_bucket
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:R $gcs_bucket/*.sql

# Import the schema into the cloud sql instance
# Todo install gcloud beta for importing csv and sql . It is intended to replace sql instances import
# gcloud beta sql import sql $cloudsql_instance $gcs_bucket/$cdr_schema_file

gcloud sql instances import --quiet --project $project  $cloudsql_instance $gcs_bucket/$cdr_schema_file

# Import hard data
gcloud sql instances import --quiet --project $project  --database cdr $cloudsql_instance $gcs_bucket/$hard_data_file

