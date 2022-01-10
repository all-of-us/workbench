#!/bin/bash

# This generates big query denormalized tables for search, review and datasets.

set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export WGV_PROJECT=$3       # whole genome variant project
export WGV_DATASET=$4       # whole genome variant dataset
export WGV_TABLE=$5         # whole genome variant table
export CDR_VERSION=$6       # CDR version
export DATA_BROWSER=$7      # data browser flag
export ARRAY_TABLE=$8       # array data table


echo ""
echo "Creating static prep tables"
if ./generate-cdr/make-bq-static-prep-tables.sh "$BQ_PROJECT" "$BQ_DATASET"
then
    echo "Prep table creation is complete"
else
    echo "Prep table creation failed!"
    exit 1
fi

echo ""
echo "Creating cb_survey_version table"
if ./generate-cdr/make-bq-cb-survey-version.sh "$BQ_PROJECT" "$BQ_DATASET"
then
    echo "cb_survey_version table creation is complete"
else
    echo "cb_survey_version table creation failed!"
    exit 1
fi

echo ""
echo "Making denormalized search events table"
if ./generate-cdr/make-bq-denormalized-search-events.sh "$BQ_PROJECT" "$BQ_DATASET" "$DATA_BROWSER"
then
    echo "Making denormalized search table complete"
else
    echo "Making denormalized search table failed!"
    exit 1
fi

echo ""
echo "Making criteria tables"
if ./generate-cdr/make-bq-criteria-tables.sh "$BQ_PROJECT" "$BQ_DATASET" "$DATA_BROWSER"
then
    echo "Making criteria tables complete"
else
    echo "Making criteria tables failed!"
    exit 1
fi

if [ "$DATA_BROWSER" == false ]
then

  echo ""
  echo "Making denormalized search person table"
  if ./generate-cdr/make-bq-denormalized-search-person.sh "$BQ_PROJECT" "$BQ_DATASET" "$WGV_PROJECT" "$WGV_DATASET" "$WGV_TABLE" "$ARRAY_TABLE"
  then
      echo "Making denormalized search person table complete"
  else
      echo "Making denormalized search person table failed!"
      exit 1
  fi

  echo ""
  echo "Making menu table"
  if ./generate-cdr/make-bq-cb-menu.sh "$BQ_PROJECT" "$BQ_DATASET"
  then
      echo "Making menu table complete"
  else
      echo "Making menu table failed!"
      exit 1
  fi

  echo ""
  echo "Making denormalized review tables"
  if ./generate-cdr/make-bq-denormalized-review.sh "$BQ_PROJECT" "$BQ_DATASET"
  then
      echo "Making denormalized review tables complete"
  else
      echo "Making denormalized review tables failed!"
      exit 1
  fi

  echo ""
  echo "Making denormalized dataset tables"
  if ./generate-cdr/make-bq-denormalized-dataset.sh "$BQ_PROJECT" "$BQ_DATASET"
  then
      echo "Making denormalized dataset tables complete"
  else
      echo "Making denormalized dataset tables failed"
      exit 1
  fi

  echo ""
  echo "Making dataset linking tables"
  if ./generate-cdr/make-bq-dataset-linking.sh "$BQ_PROJECT" "$BQ_DATASET"
  then
      echo "Making dataset linking tables complete"
  else
      echo "Making dataset linking tables failed!"
      exit 1
  fi
fi
