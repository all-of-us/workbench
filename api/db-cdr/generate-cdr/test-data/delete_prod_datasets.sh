gcloud config set auth/impersonate_service_account deploy@all-of-us-rw-prod.iam.gserviceaccount.com

# RT

bq rm -r -f -d fc-aou-cdr-prod:R2019Q4R6                
bq rm -r -f -d fc-aou-cdr-prod:R2019Q4R6_base        
bq rm -r -f -d fc-aou-cdr-prod:R2020Q4R5                
bq rm -r -f -d fc-aou-cdr-prod:R2020Q4R5_base     
bq rm -r -f -d fc-aou-cdr-prod:R2021Q3R8                
bq rm -r -f -d fc-aou-cdr-prod:R2021Q3R8_base  
bq rm -r -f -d fc-aou-cdr-prod:R2022Q2R6                
bq rm -r -f -d fc-aou-cdr-prod:R2022Q2R6_base    
bq rm -r -f -d fc-aou-cdr-prod:R2022Q4R9                
bq rm -r -f -d fc-aou-cdr-prod:R2022Q4R9_base

# CT

bq rm -r -f -d fc-aou-cdr-prod-ct:C2021Q3R8                   
bq rm -r -f -d fc-aou-cdr-prod-ct:C2021Q3R8_base
bq rm -r -f -d fc-aou-cdr-prod-ct:C2022Q2R6                   
bq rm -r -f -d fc-aou-cdr-prod-ct:C2022Q2R6_base              
bq rm -r -f -d fc-aou-cdr-prod-ct:C2022Q2R7                   
bq rm -r -f -d fc-aou-cdr-prod-ct:C2022Q2R7_base   
bq rm -r -f -d fc-aou-cdr-prod-ct:C2022Q4R11                  
bq rm -r -f -d fc-aou-cdr-prod-ct:C2022Q4R11_base
bq rm -r -f -d fc-aou-cdr-prod-ct:C2022Q4R9                   
bq rm -r -f -d fc-aou-cdr-prod-ct:C2022Q4R9_base

gcloud config unset auth/impersonate_service_account