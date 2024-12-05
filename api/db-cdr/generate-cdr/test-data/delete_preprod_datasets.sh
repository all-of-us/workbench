gcloud config set auth/impersonate_service_account deploy@all-of-us-rw-preprod.iam.gserviceaccount.com

# RT

bq rm -r -f -d fc-aou-cdr-preprod:R2022Q4R10
bq rm -r -f -d fc-aou-cdr-preprod:R2022Q4R10_base
bq rm -r -f -d fc-aou-cdr-preprod:R2022Q4R9
bq rm -r -f -d fc-aou-cdr-preprod:R2022Q4R9_base
bq rm -r -f -d fc-aou-cdr-preprod:R2024Q3R3
bq rm -r -f -d fc-aou-cdr-preprod:R2024Q3R3_base

# CT

bq rm -r -f -d fc-aou-cdr-preprod-ct:C2022Q4R11
bq rm -r -f -d fc-aou-cdr-preprod-ct:C2022Q4R11_base
bq rm -r -f -d fc-aou-cdr-preprod-ct:C2022Q4R12
bq rm -r -f -d fc-aou-cdr-preprod-ct:C2022Q4R12_base
bq rm -r -f -d fc-aou-cdr-preprod-ct:C2024Q3R3
bq rm -r -f -d fc-aou-cdr-preprod-ct:C2024Q3R3_base

gcloud config unset auth/impersonate_service_account