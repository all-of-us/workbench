#!/bin/bash

# RT list - ./impersonate_list_datasets_preprod.sh fc-aou-cdr-preprod
# CT list - ./impersonate_list_datasets_preprod.sh fc-aou-cdr-preprod-ct

PROJECT_ID=$1 # project

gcloud config set auth/impersonate_service_account deploy@all-of-us-rw-preprod.iam.gserviceaccount.com

bq ls --format=sparse --project_id "$PROJECT_ID" > "datasets/$PROJECT_ID.json"

gcloud config unset auth/impersonate_service_account