#!/bin/bash

# This copies the bq cdr counts dataset to public and runs some updates
# to sanitize the data for public consumption

set -xeuo pipefail
IFS=$'\n\t'

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-public-data.sh --workbench-project <PROJECT> --workbench-dataset <DATASET> --public-project <PROJECT> --public-dataset <DATASET> --min-count <INT>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --workbench-dataset) WORKBENCH_DATASET=$2; shift 2;;
    --public-project) PUBLIC_PROJECT=$2; shift 2;;
    --public-dataset) PUBLIC_DATASET=$2; shift 2;;
    --min-count) MIN_COUNT=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${WORKBENCH_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${WORKBENCH_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${PUBLIC_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${PUBLIC_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${MIN_COUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

# Get person count to set prevalence
q="select count_value from \`${PUBLIC_PROJECT}.${PUBLIC_DATASET}.achilles_results\` a where a.analysis_id = 1"
person_count=`bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql "$q" |  tr -dc '0-9'`

# Sent any count below min count to min count
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.achilles_results\`
set count_value = ${MIN_COUNT}
WHERE count_value < ${MIN_COUNT} and count_value > 0;"

# Round all to nearest multiple of min count
#update `all-of-us-workbench-test.public20180305.achilles_results`
#set count_value = case when Mod(count_value, 20) >= 10 then count_value + 20 - Mod(count_value, 20)
#when Mod(count_value, 20) < 10 then count_value -  Mod(count_value, 20)
#end
#where count_value > 20 and Mod(count_value, 20) > 0
exit 0;
##### End debugging



# Check that source dataset exists and exit if not
datasets=`bq --project=$WORKBENCH_PROJECT ls`
if [ -z "$datasets" ]
then
  echo "$WORKBENCH_PROJECT.$WORKBENCH_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
re=\\b$WORKBENCH_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$WORKBENCH_PROJECT.$WORKBENCH_DATASET exists. Good. Carrying on."
else
  echo "$WORKBENCH_PROJECT.$WORKBENCH_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

# Check that public project exists
datasets=`bq --project=$PUBLIC_PROJECT ls`
if [ -z "$datasets" ]
then
  echo "$PUBLIC_PROJECT does not exist. Please specify a valid PUBLIC project and dataset."
  exit 1
fi

# Make dataset for public cloudsql tables
datasets=`bq --project=$PUBLIC_PROJECT ls`
re=\\b$PUBLIC_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$PUBLIC_DATASET exists"
else
  echo "Creating $PUBLIC_DATASET"
  bq --project=$PUBLIC_PROJECT mk $PUBLIC_DATASET
fi

copy_tables=(achilles_analysis achilles_results achilles_results_concept achilles_results_dist concept concept_relationship criteria db_domain domain vocabulary )
for t in "${copy_tables[@]}"
do
  bq --project=$WORKBENCH_PROJECT rm -f $PUBLIC_PROJECT:$PUBLIC_DATASET.$t
  bq --nosync cp $WORKBENCH_PROJECT:$WORKBENCH_DATASET.$t $PUBLIC_PROJECT:$PUBLIC_DATASET.$t
done

# Todo, Run queries to make counts suitable for public

# Business logic
# Aggregate bin size will be set at 20 or MIN_COUNT. Counts lower than 20 will be displayed as 20; Counts higher than 20 will
# be rounded up or down to the closest multiple of 20. Eg: A count of 1245 will be displayed as 1240 .

# Peter Debugging this . Move to bottom later
# Get person count to set prevalence
q="select count_value from \`${PUBLIC_PROJECT}.${PUBLIC_DATASET}.achilles_results\` a where a.analysis_id = 1"
person_count=`bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql "$q" |  tr -dc '0-9'`

# UPDATE COUNT > 20 TO NEAREST MULTIPLE OF 20
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.concept\`
set count_value = ${MIN_COUNT},  prevalence = round(${MIN_COUNT}/${person_count}, 2)
WHERE count_value < 20 and count_value > 0;"

exit 0;
##### End debugging



