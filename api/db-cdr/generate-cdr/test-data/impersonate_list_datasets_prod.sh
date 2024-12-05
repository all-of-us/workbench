#!/bin/bash

# RT list - ./impersonate_list_datasets_prod.sh fc-aou-cdr-prod
# CT list - ./impersonate_list_datasets_prod.sh fc-aou-cdr-prod-ct

PROJECT_ID=$1 # project

gcloud config set auth/impersonate_service_account deploy@all-of-us-rw-prod.iam.gserviceaccount.com

bq ls --format=sparse --project_id "$PROJECT_ID" > "datasets/$PROJECT_ID.json"

gcloud config unset auth/impersonate_service_account