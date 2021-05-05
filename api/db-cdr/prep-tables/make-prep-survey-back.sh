#!/bin/bash

# Explanation here

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

BUCKET="all-of-us-workbench-private-cloudsql"
TEMP_FILE_DIR="csv"
CSV_HOME_DIR="cb_prep_tables/redcap"
CSV_OUTPUT_DIR="output"
ALL_CSV="_updated_all.csv"
REGISTERED_CSV="_updated_registered.csv"
CONTROLLED_CSV="_updated_controlled.csv"
# we will increment this as the row number as we write each row in each output file
idNumControlled = 1
idNumRegistered = 1
idNumAll = 1

# created an array of Field Names to skip and can add to this if needed
skipFieldNames = ('record_id')

# these are the column names that we are writing in the controlled and registered output files
    headers = ('id', 'parent_id', 'code', 'name', 'type', 'min', 'max',
               'answers_bucketed')

    # these are the headers for outputAll file
    headersAll = ('id', 'parent_id', 'code', 'name', 'type', 'min', 'max'
        , 'registered_topic_suppressed', 'registered_question_suppressed',
                  'registered_answer_bucketed'
        , 'controlled_topic_suppressed', 'controlled_question_suppressed',
                  'controlled_answer_bucketed')

    # these are the flags that we will look for in the original file
    possibleFlags = ('REGISTERED_QUESTION_SUPPRESSED',
                     'REGISTERED_TOPIC_SUPPRESSED',
                     'REGISTERED_ANSWERS_BUCKETED'
        , 'CONTROLLED_QUESTION_SUPPRESSED', 'CONTROLLED_TOPIC_SUPPRESSED',
                     'CONTROLLED_ANSWERS_BUCKETED')
    # initializing these variables in the case that the topic is null
        topicIdAll = 0
        topicIdControlled = 0
        topicIdRegister = 0

        # we will use this when the rowNumber = 1 to add in as the first row
        rowNumber = 0

function main() {
  rm -rf $TEMP_FILE_DIR
  mkdir $TEMP_FILE_DIR

  if gsutil -m cp gs://$BUCKET/$CSV_HOME_DIR/*.csv $TEMP_FILE_DIR
  then

    FILE_PREFIX=()
    # Collect each type of redcap survey csv file
    for file in "$TEMP_FILE_DIR"/*; do
      substring=$(echo $file | cut -d'_' -f 1)
      substring=$(echo $substring | cut -d'/' -f 2)
      if [[ " ${FILE_PREFIX[@]} " =~ " ${substring} " ]];
      then
        continue
      else
        FILE_PREFIX+=($substring)
      fi
    done

    MOST_RECENT_REDCAP_CSVS=()
    # Collect the most recent version of each redcap survey csv type.
    # Files that use the YYYY-MM-DD format are sorted* in alphabetical order.
    # They are also sorted* in chronological order, so the last file will be the most recent
    for prefix in "${FILE_PREFIX[@]}"; do
      for file in "$TEMP_FILE_DIR"/"$prefix"*; do
        most_recent_file=$file
      done
      MOST_RECENT_REDCAP_CSVS+=($most_recent_file)
    done

    # Iterate the most recent redcap survey csv files and generate a csv file for
    # 1) all 2) registered 3) controlled
    for file in "${MOST_RECENT_REDCAP_CSVS[@]}"; do

      echo "Processing survey: "$file

      initalizeHeaderRows $file

      while IFS="," read -r rec_column1 rec_column2 rec_column3 rec_column4 rec_column5 rec_column6 rec_column7 rec_column8 rec_column9 rec_column10 rec_column11 rec_column12 rec_column13 rec_column14 rec_column15 rec_column16 rec_column17 rec_column18
      #Variable / Field Name Form Name Section Header Field Type Field Label Choices,Calculations, OR Slider Labels Field Note Text Validation Type OR Show Slider Number Text Validation Min Text Validation Max Identifier? Branching Logic Required Field? Custom Alignment Question Number Matrix Group Name Matrix Ranking? Field Annotation
      do
        if [ ! "${skipFieldNames[@]}" =~ $rec_column1 ] && [ "${rec_column4,,}" != "descriptive" ] && ["${rec_column5,,}" != *"please specify"*]; then
          if [ ${rec_column18[@]} -eq 0 ]; then
            IFS=',' read -ra fieldAnnotationArray <<< "$rec_column18"
            for i in "${fieldAnnotationArray[@]}"; do
            done
            questionLabel = tr '\n' ' ' < rec_column5  # replace all '\n' with empty string
            questionCode = rec_column1
            textValidation = rec_column8
            choices = rec_column6
            ((rowNumber+=1))

            declare -A itemFlags
            for item in "${!itemFlags[@]}"; do
            if [[$item == "$rec_column18" ]]; then
                  itemFlags[item] = 1
              else:
                  itemFlags[item] = 0

            # in the case that fieldAnnotationsArray contain long code = short code
            declare -A fieldNames
            for item in " ${fieldAnnotationArray[@]} "; do
              if [[ ! " ${possibleFlags[@]} " =~ $item ]]; then
                IFS='=' read -ra splitCodes <<< "$item"
                  # in the case that it doesn't have a code ex: Launched 5/30/2017 (PTSC) & 12/10/2019 (CE).
                  if [ "${#splitCodes[@]}" -gt 1]; then
                    fieldNames["${splitCodes[0],,}"] = "${splitCodes[1],,}"

             # for the first row of survey
             if["$rowNumber" == 1]; then
            surveryIdAll = $idNumAll
            buildRowAll(allFile, 0, questionCode, questionLabel,
                        'SURVEY',
                        None, None, None, None)

            #continue


        exit 1







        # All of your work goes here. Below some code to query the BQ
        # database and write some csv files for All, Controlled and Registered

        # Query proper BQ dataset
        sqlParam="Respiratory Conditions"
        #sql="SELECT concept_code FROM \`$BQ_PROJECT.$BQ_DATASET.concept\` WHERE concept_name = '$sqlParam' and vocabulary_id = 'PPI' and concept_class_id = 'Topic'"
        #concept_code=$(echo $(echo -e $sql | bq query --quiet --nouse_legacy_sql --format=json) | jq '.[0]' | jq '.concept_code')
        #echo $concept_code

        # Write rows in new csv files
        writeAllRow $file
        writeRegisteredRow $file
        writeControlledRow $file

      done < $file
    done

    # Copy the generated csv files back to proper bucket
    for file in "${MOST_RECENT_REDCAP_CSVS[@]}"; do
      filePrefix $file
      echo "Copying "$prefix$REGISTERED_CSV" to bucket"
      gsutil cp $TEMP_FILE_DIR/$prefix$REGISTERED_CSV gs://$BUCKET/$CSV_HOME_DIR/$CSV_OUTPUT_DIR/
      echo "Copying "$prefix$CONTROLLED_CSV" to bucket"
      gsutil cp $TEMP_FILE_DIR/$prefix$CONTROLLED_CSV gs://$BUCKET/$CSV_HOME_DIR/$CSV_OUTPUT_DIR/
      echo "Copying "$prefix$ALL_CSV" to bucket"
      gsutil cp $TEMP_FILE_DIR/$prefix$ALL_CSV gs://$BUCKET/$CSV_HOME_DIR/$CSV_OUTPUT_DIR/
    done

  fi

  rm -rf $TEMP_FILE_DIR
}

function initalizeHeaderRows() {
  # Initialize output files with header rows
  filePrefix $1
  echo "id,parent_id,code,name,type,min,max,answers_bucketed" >> "$TEMP_FILE_DIR/$prefix$REGISTERED_CSV"
  echo "id,parent_id,code name,type,min,max,answers_bucketed" >> "$TEMP_FILE_DIR/$prefix$CONTROLLED_CSV"
  echo "id,parent_id,code,name,type,min,max,registered_topic_suppressed,registered_question_suppressed,registered_answer_bucketed,
  controlled_topic_suppressed,controlled_question_suppressed,controlled_answer_bucketed" >> "$TEMP_FILE_DIR/$prefix$ALL_CSV"
}

function writeAllRow() {
  # Write a row to All file
  filePrefix $1
  parentId $2
  code $3
  name $4
  itemType $5
  minValue $6
  maxValue, $7
  itemFlags $8
  fieldNames $9
  declare -A dictRow=( ["id"]= "$idNumAll" ["parent_id"]="$parent_id" ["code"]="$code" ["name"]="$name" ["type"]="$itemType" ["min"]="$minValue" ["max"]="$maxValue" )
 if [[ "$itemType" == "TOPIC"]]; then
dictRow['registered_topic_suppressed'] = itemFlags[
            "REGISTERED_TOPIC_SUPPRESSED"]
        dictRow['registered_question_suppressed'] = '0'
        dictRow['registered_answer_bucketed'] = '0'
        dictRow['controlled_topic_suppressed'] = itemFlags[
            "CONTROLLED_TOPIC_SUPPRESSED"]
        dictRow['controlled_question_suppressed'] = '0'
        dictRow['controlled_answer_bucketed'] = '0'
elif [[ "$itemType" == "QUESTION"]]; then
#  questionShortCode = getShortCode(code, fieldNames)
#        dictRow['code'] = questionShortCode
        dictRow['registered_topic_suppressed'] = '0'
        dictRow['registered_question_suppressed'] = itemFlags[
            "REGISTERED_QUESTION_SUPPRESSED"]
        dictRow['registered_answer_bucketed'] = itemFlags[
            "REGISTERED_ANSWERS_BUCKETED"]
        dictRow['controlled_topic_suppressed'] = '0'
        dictRow['controlled_question_suppressed'] = itemFlags[
            "CONTROLLED_QUESTION_SUPPRESSED"]
        dictRow['controlled_answer_bucketed'] = itemFlags[
            "CONTROLLED_ANSWERS_BUCKETED"]
  else
        dictRow['registered_topic_suppressed'] = '0'
        dictRow['registered_question_suppressed'] = '0'
        dictRow['registered_answer_bucketed'] = '0'
        dictRow['controlled_topic_suppressed'] = '0'
        dictRow['controlled_question_suppressed'] = '0'
        dictRow['controlled_answer_bucketed'] = '0'

  echo "0,0,001,name,type,0,0,0,0,0,0,0,0" >> "$TEMP_FILE_DIR/$prefix$ALL_CSV"
}

function writeControlledRow() {
  # Write a row to Controlled file
  filePrefix $1
  echo "0,0,001,name,type,0,0,0" >> "$TEMP_FILE_DIR/$prefix$CONTROLLED_CSV"
}

function writeRegisteredRow() {
  # Write a row to Registered file
  filePrefix $1
  echo "0,0,001,name,type,0,0,0" >> "$TEMP_FILE_DIR/$prefix$REGISTERED_CSV"
}

function filePrefix() {
  local file=$1
  prefix=$(echo $file | cut -d'_' -f 1)
  prefix=$(echo $prefix | cut -d'/' -f 2)
  prefix=${prefix:7}
}

main