#!/bin/bash

# This generates big query denormalized tables for search, review and datasets.

set -ex

export BQ_PROJECT=$1          # project
export BQ_DATASET=$2          # dataset
export CDR_DATE=$3            # cdr date
export DATA_BROWSER_FLAG=$4   # data browser flag
export DRY_RUN=$5             # dry run

echo "Making denormalized search tables"
if ./generate-cdr/make-bq-denormalized-search.sh $BQ_PROJECT $BQ_DATASET $CDR_DATE $DATA_BROWSER_FLAG $DRY_RUN
then
    echo "Denormalized search tables generated"
else
    echo "FAILED To generate denormalized search tables"
    exit 1
fi

echo "Making criteria tables"
if ./generate-cdr/generate-cb-criteria-tables.sh $BQ_PROJECT $BQ_DATASET $DATA_BROWSER_FLAG $DRY_RUN
then
    echo "criteria tables generated"
else
    echo "FAILED To generate criteria tables"
    exit 1
fi

echo "Making denormalized review tables"
if ./generate-cdr/make-bq-denormalized-review.sh $BQ_PROJECT $BQ_DATASET $DRY_RUN
then
    echo "Denormalized review tables generated"
else
    echo "FAILED To generate denormalized review tables"
    exit 1
fi

echo "Making denormalized dataset tables"
if ./generate-cdr/make-bq-denormalized-dataset.sh $BQ_PROJECT $BQ_DATASET $DRY_RUN
then
    echo "Denormalized dataset tables generated"
else
    echo "FAILED To generate denormalized dataset tables"
    exit 1
fi

echo "Making dataset linking tables"
if ./generate-cdr/make-bq-dataset-linking.sh $BQ_PROJECT $BQ_DATASET $DRY_RUN
then
    echo "dataset linking tables generated"
else
    echo "FAILED To generate dataset linking tables"
    exit 1
fi

echo " Finished make-bq-denormalized-tables"

