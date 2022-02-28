function createTmpTable(){
  local tmpTbl="prep_temp_"$1"_"$ID_PREFIX
  res=$(bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
    "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.$tmpTbl\` AS
      SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.$1\` LIMIT 0")
  echo $res >&2
  echo "$tmpTbl"
}
function cpToMainThenRmTmpTableThenRmTmpTable(){
  local tbl_to=$(echo "$1" | sed -e 's/prep_temp_\(.*\)_[0-9]*/\1/')
  bq cp --append_table=true --quiet --project_id=$BQ_PROJECT \
     "$BQ_DATASET.$1" "$BQ_DATASET.$tbl_to"
  echo "Deleting temp table $1"
  bq rm --quiet --project_id=$BQ_PROJECT "$BQ_DATASET.$1"
}
