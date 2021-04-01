#!/bin/bash

# This validates that prep tables, cohort builder menu, data dictionary, cope survey versions and whole genome variant tables exist.
set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export CDR_VERSION=$3       # CDR version

PREP_TABLE_RUN="!_prep_tables_!"
BUCKET="all-of-us-workbench-private-cloudsql"
TEMP_FILE_DIR="csv"
CSV_HOME_DIR="cdr_csv_files"
CRITERIA_MENU="cb_criteria_menu.csv"
DS_DATA_DICTIONARY="ds_data_dictionary.csv"
CB_SURVEY_VERSION="cb_survey_version.csv"
PREP_CDR_DATE="prep_cdr_date.csv"
PREP_CRITERIA="prep_criteria.csv"
PREP_CRITERIA_ANCESTOR="prep_criteria_ancestor.csv"
PREP_CLINICAL_TERMS="prep_clinical_terms_nc.csv"
PREP_CONCEPT="prep_concept.csv"
PREP_CONCEPT_RELATIONSHIP="prep_concept_relationship.csv"
All_FILES=($CRITERIA_MENU
           $DS_DATA_DICTIONARY
           $PREP_CDR_DATE
           $PREP_CRITERIA
           $PREP_CRITERIA_ANCESTOR
           $PREP_CLINICAL_TERMS
           $PREP_CONCEPT
           $PREP_CONCEPT_RELATIONSHIP)
INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4")

if [[ ${INCOMPATIBLE_DATASETS[@]} =~ $BQ_DATASET ]];
  then
  echo "Can't run CDR build indices against "$BQ_DATASET"!"
  exit 1
fi

rm -rf $TEMP_FILE_DIR
mkdir $TEMP_FILE_DIR

if gsutil -m cp gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/*.csv $TEMP_FILE_DIR
 then
  timestamp=$(date +%s)
  for file in ${All_FILES[@]}; do
    read -r header < $TEMP_FILE_DIR/$file
    IFS=',' read -r -a columns <<< $header
    firstColumn=$(echo $columns | cut -d' ' -f 1)
    case $file in
      $CRITERIA_MENU|$DS_DATA_DICTIONARY)
        if [[ $CDR_VERSION != $PREP_TABLE_RUN ]];
        then
          echo "Processing $file"
          if [[ $firstColumn == id ]];
          then
            echo "Removing $file header"
            # Remove the first line of file
            sed 1d $TEMP_FILE_DIR/$file > $TEMP_FILE_DIR/temp_$file
            # Zip the file
            gzip -cvf $TEMP_FILE_DIR/temp_$file > $TEMP_FILE_DIR/$file.gz
          else
            # Zip the file
            gzip $TEMP_FILE_DIR/$file
          fi
          # Copy it back to bucket
          gsutil cp $TEMP_FILE_DIR/$file.gz gs://$BUCKET/$BQ_DATASET/$CDR_VERSION/
          # Backup the csv file
          echo "Backing up $file"
          gsutil cp $TEMP_FILE_DIR/$file.gz gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup/"$timestamp"_"$file".gz
        fi
      ;;
    $PREP_CDR_DATE|$PREP_CRITERIA|$PREP_CRITERIA_ANCESTOR|$PREP_CLINICAL_TERMS|$PREP_CONCEPT|$PREP_CONCEPT_RELATIONSHIP)
      tableName=${file%.*}
      if [[ $firstColumn == id || \
            $firstColumn == ancestor_id || \
            $firstColumn == parent || \
            $firstColumn == concept_id || \
            $firstColumn == concept_id_1 || \
            $firstColumn == bq_dataset ]];
      then
        echo "Removing $file header"
        # Remove the first line of file
        sed 1d $TEMP_FILE_DIR/$file > $TEMP_FILE_DIR/temp_$file
        # Rename file
        mv $TEMP_FILE_DIR/temp_$file $TEMP_FILE_DIR/$file
        # Copy it back to bucket
        echo "Copying $file"
        gsutil cp $TEMP_FILE_DIR/$file gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/
      fi

      if [[ $CDR_VERSION != $PREP_TABLE_RUN ]];
      then
        # Check to see if table exists
        tables=$(bq ls --max_results 1000 "$BQ_PROJECT:$BQ_DATASET" | awk '{print $1}' | tail +3)
        for table in ${tables[@]};
        do
          if [[ $table == $tableName ]];
          then
            echo "Backing up $file"
            bq extract --project_id=$BQ_PROJECT --compression GZIP --print_header=false $BQ_DATASET.$tableName gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup/"$timestamp"_"$file".gz
          fi
        done
      fi

      # Load the csv file into table
      echo "Starting load of $file"
      schema_path=generate-cdr/bq-schemas
      bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$tableName
      bq load --project_id=$BQ_PROJECT --source_format=CSV $BQ_DATASET.$tableName gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/$file $schema_path/$tableName.json
      echo "Finished loading $file"
    ;;
    esac
  done
fi

rm -rf $TEMP_FILE_DIR

# Validate that a cdr cutoff date exists
echo "Validating that a CDR cutoff date exists..."
q="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.prep_cdr_date\` where bq_dataset = '$BQ_DATASET'"
cdrDate=$(bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql "$q" | tr -dc '0-9')
if [[ $cdrDate != 1 ]];
then
  echo "CDR cutoff date doesn't exist in $BQ_PROJECT.$BQ_DATASET.prep_cdr_date!"
  exit 1
fi

# Purge all backup csv files except for the last 10 versions
if [[ $CDR_VERSION != $PREP_TABLE_RUN ]];
then
  fileCount=$(gsutil ls gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup | wc -l)
  allFilesCount=${#All_FILES[@]}
  numberToDelete=$(($((fileCount - 1)) - $((allFilesCount * 10))))
  if [[ $numberToDelete > 0 ]];
  then
    echo "Purging $numberToDelete backup files"
    while IFS= read -r line; do
      echo "Removing $line"
      gsutil rm gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup/$line
    # This lists all the files in the backup bucket sorted by timestamp and gets only the number to delete
    done < <(gsutil ls gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup | rev | cut -d/ -f1 | rev | sort | awk 'NF' | head -$numberToDelete)
  fi
fi