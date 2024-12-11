gcloud config set auth/impersonate_service_account deploy@all-of-us-rw-prod.iam.gserviceaccount.com

# RT

bq rm -r -f -d fc-aou-cdr-prod:<datasetName>

# CT

bq rm -r -f -d fc-aou-cdr-prod-ct:<datasetName>

gcloud config unset auth/impersonate_service_account