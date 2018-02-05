#!/bin/bash

# This copies the bq cdr counts dataset to public and runs some updates
# to sanitize the data for public consumption

set -xeuo pipefail
IFS=$'\n\t'

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-public-data.sh --workbench-project <PROJECT> --workbench-dataset <DATASET> --public-project <PROJECT> --public-dataset <DATASET> "

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --workbench-dataset) WORKBENCH_DATASET=$2; shift 2;;
    --public-project) PUBLIC_PROJECT=$2; shift 2;;
    --public-dataset) PUBLIC_DATASET=$2; shift 2;;
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

# Check that source dataset exists and exit if not
datasets=`bq --project=$WORKBENCH_PROJECT ls`
if [ -z "$datasets" ]
then
  echo "$WORKBENCH_PROJECT.$WORKBENCH_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
if [[ $datasets =~ .*$WORKBENCH_DATASET.* ]]; then
  echo "$WORKBENCH_PROJECT.$WORKBENCH_DATASET exists. Good. Carrying on."
else
  echo "$WORKBENCH_PROJECT.$WORKBENCH_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

# Check that public project exists
datasets=`bq --project=$PUBLIC_PROJECT ls`
if [ -z "$datasets" ]
then
  echo "$WORKBENCH_PROJECT.$WORKBENCH_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

# Make dataset for public cloudsql tables
datasets=`bq --project=$PUBLIC_PROJECT ls`
echo $datasets
if [[ $datasets =~ .*$PUBLIC_DATASET.* ]]; then
  echo "$PUBLIC_DATASET exists"
else
  echo "Creating $PUBLIC_DATASET"
  bq --project=$PUBLIC_PROJECT mk $PUBLIC_DATASET
fi

create_tables=(achilles_analysis achilles_results achilles_results_dist concept concept_relationship criteria domain vocabulary )
for t in "${create_tables[@]}"
do
  bq --project=$WORKBENCH_PROJECT rm -f $PUBLIC_PROJECT:$PUBLIC_DATASET.$t
  bq --nosync cp $WORKBENCH_PROJECT:$WORKBENCH_DATASET.$t $PUBLIC_PROJECT:$PUBLIC_DATASET.$t
done

# Todo, Run queries to make counts suitable for public

# Todo, fill achilles_results_concept table possibly if we need to.