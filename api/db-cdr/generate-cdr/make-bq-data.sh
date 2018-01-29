#!/bin/bash

# This generates big query data that gets put in cloudsql
# Counts and teh public and cdr data four cloudsql
# note dev-up must be run to generate the schema
# note run-local-data-migrations must be run to generate hard coded data from liquibase
# note  the account must be authorized to perform gcloud and bq operations

set -xeuo pipefail
IFS=$'\n\t'


# get options
# --project=all-of-us-workbench-test *required

# --cdr=cdr_version ... *optional

USAGE="./generate-clousql-cdr/make-bq-data.sh --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT>"
USAGE="$USAGE --account <ACCOUNT> --cdr-version=YYYYMMDD"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${WORKBENCH_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${CDR_VERSION}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi


#Check cdr_version is of form YYYYMMDD
if [[ $CDR_VERSION =~ ^[0-9]{4}(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])$ ]]; then
    echo "New CDR VERSION will be $CDR_VERSION"
  else
    echo "CDR Version doesn't match required format YYYYMMDD"
    echo "Usage: $USAGE"
    exit 1
fi

NEW_BQ_CDR_DATASET=cdr$CDR_VERSION
schema_path=generate-cdr/bq-schemas

# Check that bq_dataset exists and exit if not
datasets=`bq --project=$BQ_PROJECT ls`
if [ -z "$datasets" ]
then
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
if [[ $datasets =~ .*$BQ_DATASET.* ]]; then
  echo "$BQ_PROJECT.$BQ_DATASET exists. Good. Carrying on."
else
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi


# Make dataset for cdr cloudsql tables
datasets=`bq --project=$WORKBENCH_PROJECT ls`
echo $datasets
if [[ $datasets =~ .*$NEW_BQ_CDR_DATASET.* ]]; then
  echo "$NEW_BQ_CDR_DATASET exists"
else
  echo "Creating $NEW_BQ_CDR_DATASET"
  bq --project=$WORKBENCH_PROJECT mk $NEW_BQ_CDR_DATASET
fi

# Copy tables we can that we need from cdr to our cloudsql cdr dataset
# todo uncomment temp commented out
copy_tables=(domain vocabulary criteria )
for t in "${copy_tables[@]}"
do
  bq --project=$WORKBENCH_PROJECT rm -f $NEW_BQ_CDR_DATASET.$t
  bq --quiet --project=$BQ_PROJECT cp $BQ_DATASET.$t $WORKBENCH_PROJECT:$NEW_BQ_CDR_DATASET.$t
done

# Create bq tables we have json schema for
create_tables=(achilles_analysis achilles_results achilles_results_dist achilles_results_concept concept concept_relationship )
for t in "${create_tables[@]}"
do
  # Make the concept_counts table from cdr
  bq --project=$WORKBENCH_PROJECT rm -f $NEW_BQ_CDR_DATASET.$t
  bq --quiet --project=$WORKBENCH_PROJECT mk --schema=$schema_path/$t.json $NEW_BQ_CDR_DATASET.$t
done


####################
# achilles queries #
####################
# Run achilles count queries to fill achilles_results
if ./generate-cdr/run-achilles-queries.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET --workbench-project $WORKBENCH_PROJECT --account $ACCOUNT --workbench-dataset $NEW_BQ_CDR_DATASET
then
    echo "Achilles queries ran"
else
    echo "FAILED To run achilles queries for CDR $CDR_VERSION"
    exit 1
fi


###########################
# concept with count cols #
###########################
# We can't just copy concept because the schema has a couple extra columns
# and dates need to be formatted for mysql
# Insert the data into it.
echo "Inserting concept table data ... "
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$WORKBENCH_PROJECT.$NEW_BQ_CDR_DATASET.concept\`
(concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept,
concept_code, valid_start_date, valid_end_date, invalid_reason, count_value, prevalence)
SELECT c.concept_id, c.concept_name, c.domain_id, c.vocabulary_id, c.concept_class_id, c.standard_concept, c.concept_code,
Concat(substr(c.valid_start_date, 1,4), '-',substr(c.valid_start_date,5,2),'-',substr(c.valid_start_date,7,2)) as valid_start_date,
Concat(substr(c.valid_end_date, 1,4), '-',substr(c.valid_end_date,5,2),'-',substr(c.valid_end_date,7,2)) as valid_end_date,
invalid_reason,
case when r.count_value is not null THEN r.count_value ELSE 0 end as count_value,
case when r.count_value is not null THEN (select round(r.count_value / a.count_value, 2)
  from \`$WORKBENCH_PROJECT.$NEW_BQ_CDR_DATASET.achilles_results\` a where a.analysis_id = 1)  ELSE 0.00 end as prevalence
FROM \`$BQ_PROJECT.$BQ_DATASET.concept\` c left outer join \`$WORKBENCH_PROJECT.$NEW_BQ_CDR_DATASET.achilles_results\` r
on cast(c.concept_id as string) = r.stratum_1 and r.analysis_id = 3000"


########################
# concept_relationship #
########################
echo "Inserting concept_relationship"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$WORKBENCH_PROJECT.$NEW_BQ_CDR_DATASET.concept_relationship\`
 (concept_id_1, concept_id_2, relationship_id, valid_start_date, valid_end_date, invalid_reason)
SELECT c.concept_id_1, c.concept_id_2, c.relationship_id,
Concat(substr(c.valid_start_date, 1,4), '-',substr(c.valid_start_date,5,2),'-',substr(c.valid_start_date,7,2)) as valid_start_date,
Concat(substr(c.valid_end_date, 1,4), '-',substr(c.valid_end_date,5,2),'-',substr(c.valid_end_date,7,2)) as valid_end_date,
c.invalid_reason
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` c"




# Convert the date columns into mysql dates where they are not null
#bq --quiet --project=$WORKBENCH_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$NEW_BQ_CDR_DATASET.concept_relationship\` \
#SET valid_start_date = Concat(substr(valid_start_date, 1,4), '-',substr(valid_start_date,5,2),'-',substr(valid_start_date,7,2)) \
#WHERE valid_start_date is not null"
#
#bq --quiet --project=$WORKBENCH_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$NEW_BQ_CDR_DATASET.concept_relationship\` \
#SET valid_end_date = Concat(substr(valid_end_date, 1,4), '-',substr(valid_end_date,5,2),'-',substr(valid_end_date,7,2)) \
#WHERE valid_end_date is not null"



