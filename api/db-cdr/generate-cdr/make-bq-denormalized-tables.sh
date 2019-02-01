#!/bin/bash

# This generates big query denormalized tables for search and review.

set -xeuo pipefail
IFS=$'\n\t'


USAGE="./generate-cdr/make-bq-denormalized-tables --bq-project <PROJECT> --bq-dataset <DATASET>"

BQ_PROJECT=""
BQ_DATASET=""

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    -- ) shift; echo -e "Usage: $USAGE"; break ;;
    * ) break ;;
  esac
done

if [ -z "${BQ_PROJECT}" ]
then
  echo -e "Usage: $USAGE"
  echo -e "Missing bq project name"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo -e "Usage: $USAGE"
  echo -e "Missing bq_dataset name"
  exit 1
fi

echo "Making denormalized review tables"
if ./generate-cdr/make-bq-denormalized-review.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET
then
    echo "Denormalized review tables generated"
else
    echo "FAILED To generate denormalized review tables"
    exit 1
fi

echo "Making denormalized search tables"
if ./generate-cdr/make-bq-denormalized-search.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET
then
    echo "Denormalized search tables generated"
else
    echo "FAILED To generate denormalized search tables"
    exit 1
fi

echo " Finished make-bq-denormalized-tables"

