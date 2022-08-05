#!/bin/bash

# This generates the cb menu for cohort builder.

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

echo "Insert cb_criteria_menu"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
    (id,parent_id,category,domain_id,type,name,is_group,sort_order)
VALUES
(20,2,'Program Data','SURVEY','PPI','All Surveys',0,1)"

echo "Adding surveys"
query="select name from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'SURVEY'
and parent_id = 0
order by id"
surveyNames=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql --format csv "$query")

echo "Getting max id"
query="select max(id) as id from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`"
maxId=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

sortOrder=1

while IFS= read -r line
do
  if [[ "$line" != "name" ]]; then
    maxId=$((maxId+1))
    sortOrder=$((sortOrder+1))
    echo "$maxId $line"
    bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
      "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
          (id,parent_id,category,domain_id,type,name,is_group,sort_order)
      VALUES
      ($maxId,2,'Program Data','SURVEY','PPI','$line',0,$sortOrder)"
  fi
done <<< "$surveyNames"