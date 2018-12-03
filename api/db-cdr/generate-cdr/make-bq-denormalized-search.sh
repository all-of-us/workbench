#!/bin/bash

# This generates big query denormalized tables.

set -xeuo pipefail
IFS=$'\n\t'


# get options

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-denormalized-search.sh --bq-project <PROJECT> --bq-dataset <DATASET>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done


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


# Check that bq_dataset exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls)
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

# Check that bq_dataset exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls)
if [ -z "$datasets" ]
then
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
re=\\b$BQ_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$BQ_PROJECT.$BQ_DATASET exists. Good. Carrying on."
else
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas
create_tables=(icd9)
for t in "${create_tables[@]}"
do
    bq --project=$BQ_PROJECT rm -f $BQ_DATASET.$t
    bq --quiet --project=$BQ_PROJECT mk --schema=$schema_path/$t.json --time_partitioning_type=DAY --clustering_fields source_concept_id $BQ_DATASET.$t
done

# Populate some tables from cdr data

################################################
#      insert condition data into icd9         #
################################################
echo "Inserting conditions data into icd9"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.icd9\`
 (person_id, entry_date, source_concept_id, age_at_event, visit_concept_id)
select co.person_id, co.condition_start_date as entry_date, co.condition_source_concept_id as source_concept_id,
cast(floor(date_diff(co.condition_start_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
vo.visit_concept_id
from `all-of-us-ehr-dev.synthetic_cdr20180606.condition_occurrence` co
join `all-of-us-ehr-dev.synthetic_cdr20180606.criteria` c on (c.concept_id = co.condition_source_concept_id and c.is_selectable = 1 and c.type = 'ICD9')
join `all-of-us-ehr-dev.synthetic_cdr20180606.person` p on (p.person_id = co.person_id)
left join `all-of-us-ehr-dev.synthetic_cdr20180606.visit_occurrence` vo on (vo.visit_occurrence_id = co.visit_occurrence_id)"