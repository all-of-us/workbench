gcloud config set auth/impersonate_service_account deploy@all-of-us-rw-preprod.iam.gserviceaccount.com

# RT

bq rm -r -f -d fc-aou-cdr-preprod:<datasetName>

# CT

bq rm -r -f -d fc-aou-cdr-preprod-ct:<datasetName>

gcloud config unset auth/impersonate_service_account