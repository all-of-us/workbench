#!/bin/bash

# This generates new cloudsql database for a cdr
# note dev-up must be run to generate the schema

set -xeuo pipefail
IFS=$'\n\t'

# Make the vocabulary table from cdr with no changes
#bq --project=all-of-us-ehr-dev cp test_merge_dec26.vocabulary test_vocabulary_ppi.vocabulary
project="all-of-us-workbench-test"
bqcdr="test_merge_dec26"
cdr_schema_file="cdr_schema.sql"
public_schema_file="tmp/public_schema.sql"
cloudsql_instance=workbenchtest
cdr_db_name=cdr
gcs_bucket=gs://all-of-us-workbench-cloudsql-create

echo "Dumping cdr schema "
mysqldump -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} \
    --no-data --add-drop-table --ignore-table=$cdr_db_name.DATABASECHANGELOG \
    --ignore-table=$cdr_db_name.DATABASECHANGELOGLOCK $cdr_db_name --database cdr \
    --add-drop-database > $cdr_schema_file

# Copy dump to cloud storage for importing
gsutil cp $cdr_schema_file $gcs_bucket/$cdr_schema_file

# Todo Maybe will need these for a service account
#SERVICE_ACCOUNT=all-of-us-workbench-test@appspot.gserviceaccount.com
#CREDS_FILE=all-of-us-workbench-test-f8d191035ffe.json
#gcloud auth activate-service-account $SERVICE_ACCOUNT --key-file=$CREDS_FILE

# Grant access to buckets for service account for cloudsql
SQL_SERVICE_ACCOUNT=`gcloud sql instances describe --project $project \
    --account $SERVICE_ACCOUNT $cloudsql_instance | grep serviceAccountEmailAddress \
    | cut -d: -f2`

echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to bucket $gcs_bucket..."
# Trim leading whitespace from sql service account
SQL_SERVICE_ACCOUNT=${SQL_SERVICE_ACCOUNT//[[:blank:]]/}
# Note, this only applies to files already existing in the bucket . So must rerun after adding files
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:W $gcs_bucket
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:R $gcs_bucket/*.sql

# Import the schema into the cloud sql instance
# Todo install gcloud beta
# gcloud beta sql import sql $cloudsql_instance $gcs_bucket/$cdr_schema_file

gcloud sql instances import --quiet --project $project  $cloudsql_instance $gcs_bucket/$cdr_schema_file
