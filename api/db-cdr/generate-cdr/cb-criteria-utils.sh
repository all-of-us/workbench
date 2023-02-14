function createTmpTable(){
  local tmpTbl="prep_temp_$1_$CB_CRITERIA_START_ID"
  res=$(bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
    "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.$tmpTbl\` AS
      SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.$1\` LIMIT 0")
  echo $res >&2
  echo "$tmpTbl"
}

function cpToMain(){
  local tbl_to=$(echo "$1" | sed -e 's/prep_temp_\(.*\)_[0-9]*/\1/')
  bq cp --append_table=true --quiet --project_id="$BQ_PROJECT" \
     "$BQ_DATASET.$1" "$BQ_DATASET.$tbl_to"
}

function rmTmpTable(){
  echo "Deleting temp table $1"
  bq rm --force --project_id="$BQ_PROJECT" "$BQ_DATASET.$1"
}

function cpToMainAndDeleteTmp(){
  for tmpTbl in "$@"
  do
    ## copy to main
    cpToMain "$tmpTbl" &
  done
  ## wait for copy to complete
  wait
  for tmpTbl in "$@"
  do
    ## delete tmp table
    rmTmpTable "$tmpTbl" &
  done
  ## wait for delete to complete before exiting
  wait
}
