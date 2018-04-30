#!/bin/bash

# This copies the bq cdr counts dataset to public and runs some updates
# to sanitize the data for public consumption

set -xeuo pipefail
IFS=$'\n\t'

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-public-data.sh --workbench-project <PROJECT> --workbench-dataset <DATASET> --public-project <PROJECT> --public-dataset <DATASET> --bin-size <INT>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --workbench-dataset) WORKBENCH_DATASET=$2; shift 2;;
    --public-project) PUBLIC_PROJECT=$2; shift 2;;
    --public-dataset) PUBLIC_DATASET=$2; shift 2;;
    --bin-size) BIN_SIZE=$2; shift 2;;
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

if [ -z "${BIN_SIZE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

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

copy_tables=(achilles_analysis achilles_results achilles_results_concept concept concept_relationship criteria db_domain domain vocabulary )
for t in "${copy_tables[@]}"
do
  bq --project=$WORKBENCH_PROJECT rm -f $PUBLIC_PROJECT:$PUBLIC_DATASET.$t
  bq --nosync cp $WORKBENCH_PROJECT:$WORKBENCH_DATASET.$t $PUBLIC_PROJECT:$PUBLIC_DATASET.$t
done

# Round counts for public dataset
# 1. Set any count > 0 and < BIN_SIZE  to BIN_SIZE,
# 2. Round any above BIN_SIZE to multiple of BIN_SIZE

# Get person count to set prevalence
q="select count_value from \`${PUBLIC_PROJECT}.${PUBLIC_DATASET}.achilles_results\` a where a.analysis_id = 1"
person_count=$(bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql "$q" |  tr -dc '0-9')

# achilles_results
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.achilles_results\`
set count_value =
    case when count_value < ${BIN_SIZE}
        then ${BIN_SIZE}
    else
        cast(ROUND(count_value / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end
where count_value > 0"

# achilles_results_concept
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.achilles_results_concept\`
set count_value =
    case when count_value < ${BIN_SIZE}
        then ${BIN_SIZE}
    else
        cast(ROUND(count_value / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end
where count_value > 0"

# concept
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.concept\`
set count_value =
    case when count_value < ${BIN_SIZE}
        then ${BIN_SIZE}
    else
        cast(ROUND(count_value / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end,
    prevalence =
    case when count_value < ${BIN_SIZE}
        then ROUND(${BIN_SIZE} / ${person_count},2)
    else
        ROUND(ROUND(count_value / ${BIN_SIZE}) * ${BIN_SIZE}/ ${person_count}, 2)
    end
where count_value > 0"

# criteria
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.criteria\`
set est_count =
    case when est_count < ${BIN_SIZE}
        then ${BIN_SIZE}
    else
        cast(ROUND(est_count / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end
where est_count > 0"
