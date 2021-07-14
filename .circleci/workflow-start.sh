#!/bin/bash -x

# set -eo pipefail
set -xv

# CIRCLECI_TOKEN is a personal token.
# https://circleci.com/docs/2.0/managing-api-tokens/#creating-a-personal-api-token
# token = process.env.CIRCLECI_TOKEN
branch='master';
slug="gh/all-of-us"

# v2 workflow api provides status, but not the pipeline api.
# https://circleci.com/docs/2.0/workflows/#states
workflow_active_status=("running" "failing")

base_url="https://circleci.com/api/v2"
curl_headers="-H \"Accept: application/json\" -H \"Content-Type: application/json\" -H \"Circle-Token: ${CIRCLECI_TOKEN}\""

# Get list of recently built pipelines. Save results to json file.
pipeline_json="/tmp/master_branch_pipelines.json"
fetch_pipelines() {
  curl -X GET -f --url "${base_url}/pipeline?org-slug=${slug} ${curl_headers}" | jq '[.items[] | select(.vcs.branch=="master")][]' > ${pipeline_json}
  echo "Saved ${pipeline_json}"
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
