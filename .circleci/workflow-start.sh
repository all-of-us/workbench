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
  curl -su ${circle_token}: \
       -H "Content-Type: application/json" \
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
fetch_pipelines() {
  get_path="${api_root}pipeline?org-slug=${project_slug}"
  printf "GET list of pipelines"
  get_result=$(get $get_path)
  echo $get_result | jq '[.items[] | select(.vcs.branch=="master")][]' > ${pipeline_json}
  printf "Saved ${pipeline_json}"
  cat ${pipeline_json}
}

get_workflow () {
  GET_PIPELINE_OUTPUT=$(curl --silent -X GET \
  "https://circleci.com/api/v2/pipeline/${PIPELINE_ID}?circle-token=${CIRCLECI_TOKEN}" \
  -H 'Accept: */*' \
  -H 'Content-Type: application/json')
  WORKFLOW_ID=$(echo $GET_PIPELINE_OUTPUT | jq -r .items[0].id)
  echo "The created worlkflow is ${WORKFLOW_ID}"
  echo "Link to workflow is"
  echo "https://circleci.com/workflow-run/${WORKFLOW_ID}"
}

fetch_pipelines
