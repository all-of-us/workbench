table=$1
bq rm -f --project_id=$BQ_PROJECT $BQ_DATASET.$table
bq mk --project_id=$BQ_PROJECT $BQ_DATASET.$table ./$table.json
