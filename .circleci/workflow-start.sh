#! /bin/bash

set -e
set -o pipefail
set -o functrace
set -xv

#********************
# VARIABLES
# *******************
api_root="https://circleci.com/api/v2/"
branch='master';
project_slug="gh/all-of-us"

# CIRCLECI_TOKEN is a personal token.
# https://circleci.com/docs/2.0/managing-api-tokens/#creating-a-personal-api-token


# v2 workflow api provides status, but not the pipeline api.
# https://circleci.com/docs/2.0/workflows/#states
workflow_active_status=("running" "failing")


#********************
# FUNCTIONS
# *******************

post () {
  local url="${api_root}${1}"
  printf "HTTP POST ${url}\n\n" > /dev/tty

  curl -su ${circle_token}: -X POST \
       -H "Content-Type: application/json" \
       -H "Circle-Token: ${CIRCLECI_TOKEN}" \
       -d "$2" \
       "${url}"
}

get () {
  local url="${api_root}${1}"
  printf "HTTP GET ${url}\n\n" > /dev/tty
  curl -H "Content-Type: application/json" \
       -H "Circle-Token: ${CIRCLECI_TOKEN}" \
       "${url}"
}

pretty_json () {
  # jq . <<< "${result}" | sed 's/^/  /'
  echo "\`\`\`"
  jq . <<< "${result}"
  echo "\`\`\`"
}


# Get list of recently built pipelines. Save results to json file.
pipeline_json="/tmp/master_branch_pipelines.json"
fetch_pipeline_ids() {
  local get_path="pipeline?org-slug=${project_slug}"
  printf "GET list of pipelines"
  local get_result=$(get $get_path)
  echo $get_result | jq '[.items[] | select(.vcs.branch=="master")][] | {created_at: .created_at, id: .id, number: .number}' > ${pipeline_json}
  cat ${pipeline_json}
}

fetch_pipeline_workflow () {
  local get_path="pipeline/{${1}}/workflow"
  printf "GET workflow in pipeline id '${$1}'"
  local get_result=$(get $get_path)
  workflow_id=$(echo $get_result | jq .items[].id | @sh)
  printf workflow_id
}

fetch_pipeline_ids
pipeline_ids=$(jq - .id $pipeline_json | @sh)
printf pipeline_ids

# Get the workflow for each pipeline.
IFS=$'\n'
for id in ${pipeline_ids}; do
  # Remove double-quotes
  id=$(echo $id | xargs echo)
  printf $id
  fetch_pipeline_workflow($id)
done
unset IFS
