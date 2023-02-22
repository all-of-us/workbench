#!/usr/bin/env bash
# Vars for the process
export CDR_DB_NAME=$1
export PROJECT_PRE_PROD="all-of-us-rw-preprod"
export PROJECT_PROD="all-of-us-rw-prod"

function validate_cdr(){
 local db_prd=$(gcloud sql databases list --instance workbenchmaindb --project "$PROJECT_PROD" | grep "$CDR_DB_NAME" | cut -d" " -f1)
 if [[ "$db_prd" == "$CDR_DB_NAME" ]]; then
   echo ""
   echo -e "\033[0;31m******************************************************************************************************************\033[0m"
   echo -e "\033[0;31m**\t\t\t\t\t\t\tWARNING\t\t\t\t\t\t\t**\033[0m"
   echo -e "\033[0;31m**\t\t\t\033[43m\033[1;5;31m\tCloudSQL database $CDR_DB_NAME exists in Production\t\033[0;31m\t\t\t**\033[0m"
   echo -e "\033[0;31m**\tEnsure that the CDR you are importing is not LIVE in Production!\t\t\t\t\t**\033[0m"
   echo -e "\033[0;31m**\tNavigate to google cloud console and check!\t\t\t\t\t\t\t\t**\033[0m"
   echo -e "\033[0;31m**\thttps://console.cloud.google.com/sql/instances/workbenchmaindb/databases?project=$PROJECT_PROD\t**\033[0m"
   echo -e "\033[0;31m**\tIf you still want to OVERWRITE CloudSQL database $CDR_DB_NAME Production\t\t\t\t\t**\033[0m"
   echo -e "\033[0;31m**\t\tFirst manually delete the CloudSQL database $CDR_DB_NAME Production\t\t\t\t**\033[0m"
   echo -e "\033[0;31m**\t\tbefore running the ./project.rb command\t\t\t\t\t\t\t\t**\033[0m"
   echo -e "\033[0;31m******************************************************************************************************************\033[0m"
   echo ""
   exit 1
 fi
 local db_pre=$(gcloud sql databases list --instance workbenchmaindb --project "$PROJECT_PRE_PROD" | grep "$CDR_DB_NAME" | cut -d" " -f1)
 if [[ -z "$db_pre" ]]; then
   echo ""
   echo -e "\033[0;31m******************************************************************************************************************\033[0m"
   echo -e "\033[0;31m**\t\t\t\033[43m\033[1;5;31m\tCloudSQL database $CDR_DB_NAME does not exist in preprod\t\t\033[0;31m\t\t**\033[0m"
   echo -e "\033[0;31m**\tNavigate to google cloud console and check!\t\t\t\t\t\t\t\t**\033[0m"
   echo -e "\033[0;31m**\thttps://console.cloud.google.com/sql/instances/workbenchmaindb/databases?project=a$PROJECT_PRE_PROD\t**\033[0m"
   echo -e "\033[0;31m**\t\tbefore running the ./project.rb command\t\t\t\t\t\t\t\t**\033[0m"
   echo -e "\033[0;31m******************************************************************************************************************\033[0m"
      echo ""
   exit 1
 elif [[ "$db_pre" == "$CDR_DB_NAME" && -z "$db_prd" ]]; then
   echo "Exporting preprod:[$db_pre] for importing into prod"
 fi
}
# Validate cdr name for export-import operations
echo "1. Validating if database $CDR_DB_NAME can be exported from preprod to import into prod"
validate_cdr

# Export: cdr to preprod bucket"
echo "2. Exporting sqldump file for cdr: $CDR_DB_NAME to preprod bucket"
gcloud sql export sql workbenchmaindb \
       gs://"$PROJECT_PRE_PROD".appspot.com/"$CDR_DB_NAME"_export.sql \
       --database="$CDR_DB_NAME" --project="$PROJECT_PRE_PROD"

# Copy: Sqldump file from preprod bucket to prod bucket
echo "3. Copying sqldump file $CDR_DB_NAME_export.sql to prod bucket"
gsutil cp gs://"$PROJECT_PRE_PROD".appspot.com/"$CDR_DB_NAME"_export.sql \
       gs://"$PROJECT_PROD".appspot.com/"$CDR_DB_NAME"_export.sql

# Import: Create Production cloud SQL cdr from sqldump file
echo "4. Creating prod database $CDR_DB_NAME from $CDR_DB_NAME_export.sql"
# Print production Google Cloud Console to monitor import
echo -e "\tNavigate to the URL below to monitor progress..."
echo -e "\tURL: https://console.cloud.google.com/sql/instances/workbenchmaindb/databases?project=$PROJECT_PROD"
gcloud sql import sql workbenchmaindb \
       gs://"$PROJECT_PROD".appspot.com/"$CDR_DB_NAME"_export.sql \
       --quiet --project="$PROJECT_PROD"

# Verify:  Newly created database exists in production
echo "5. Verifying production cdr $CDR_DB_NAME exists"
db_prd=$(gcloud sql databases list --instance workbenchmaindb --project "$PROJECT_PROD" | grep "$CDR_DB_NAME" | cut -d" " -f1)
if [[ "$db_prd" == "$CDR_DB_NAME" ]]; then
  echo -e "\033[0;31m**\t\tProduction CloudSQL database $CDR_DB_NAME created successfully**\033[0m"
else
  echo -e "\033[0;31m**\t\033[43m\033[1;5;31m\tProduction CloudSQL database $CDR_DB_NAME creation failed\t\033[0;31m\t**\033[0m"
  exit 1
fi

# Remove: sqldump files from preprod and prod buckets
echo "6a. Removing sqldump file from preprod bucket"
gsutil rm gs://"$PROJECT_PRE_PROD".appspot.com/"$CDR_DB_NAME"_export.sql

echo "6b. Removing sqldump file from prod bucket"
gsutil rm gs://"$PROJECT_PROD".appspot.com/"$CDR_DB_NAME"_export.sql

# Verify: print out row counts from cdr from preprod and prod for manual verification
echo "7a. Querying row counts for tables in $CDR_DB_NAME for preprod and prod"
echo "$(./project.rb verify-cloud-cdr-counts --cdr-db-name "$CDR_DB_NAME")"
echo -e "7b. \033[0;31m**Manually check row counts for tables match between preprod and prod**\033[0m"
echo "done!"


