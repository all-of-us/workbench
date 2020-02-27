#!/bin/bash

# This generates big query denormalized tables for search, review and datasets.

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset
export CDR_DATE=$3 # cdr date

echo "Making denormalized search tables"
if ./generate-cdr/make-bq-denormalized-search.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET --cdr-date $CDR_DATE
then
    echo "Denormalized search tables generated"
else
    echo "FAILED To generate denormalized search tables"
    exit 1
fi

echo "Making criteria tables"
if ./generate-cdr/generate-cb-criteria-tables.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET
then
    echo "criteria tables generated"
else
    echo "FAILED To generate criteria tables"
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

echo "Making denormalized dataset tables"
if ./generate-cdr/make-bq-denormalized-dataset.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET
then
    echo "Denormalized dataset tables generated"
else
    echo "FAILED To generate denormalized dataset tables"
    exit 1
fi

echo "Making dataset linking tables"
if ./generate-cdr/make-bq-dataset-linking.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET
then
    echo "dataset linking tables generated"
else
    echo "FAILED To generate dataset linking tables"
    exit 1
fi

echo " Finished make-bq-denormalized-tables"

