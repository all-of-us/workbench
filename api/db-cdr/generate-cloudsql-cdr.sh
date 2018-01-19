#!/bin/bash

# This generates new cloudsql database for a cdr
# note dev-up must be run to generate the schema

set -xeuo pipefail
IFS=$'\n\t'

# Make the vocabulary table from cdr with no changes
#bq --project=all-of-us-ehr-dev cp test_merge_dec26.vocabulary test_vocabulary_ppi.vocabulary
project="all-of-us-ehr-dev"
bqcdr="test_merge_dec26"
cdr_schema_file="cdr_schema.sql"
public_schema_file="tmp/public_schema.sql"
cloudsql_instance=workbenchtest
cdr_db_name=cdr
gcs_bucket=gs://all-of-us-workbench-cloudsql-create

echo "Dumping cdr schema "
mysqldump -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} \
    --no-data --add-drop-table --ignore-table=$cdr_db_name.DATABASECHANGELOG \
    --ignore-table=$cdr_db_name.DATABASECHANGELOGLOCK $cdr_db_name > $cdr_schema_file



ls -la /

# Grant access to buckets for service account
SQL_SERVICE_ACCOUNT=`gcloud sql instances describe --project $project \
    --account peter.speltz@pmi-ops.org $cloudsql_instance | grep serviceAccountEmailAddress \
    | cut -d: -f2`

echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to bucket $gcs_bucket..."
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:W $gcs_bucket

# Copy dump to cloud storage for importing
gsutil cp $cdr_schema_file $gcs_bucket/$cdr_schema_file

# Import the schema into the cloud sql instance
gcloud beta sql import sql $cloudsql_instance $gcs_bucket/$cdr_schema_file
