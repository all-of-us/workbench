#! /bin/bash

set -e
set -o pipefail
set -o functrace

#********************
# VARIABLES
# *******************
api_root="https://circleci.com/api/v2/"
# polling branch name. Other branches (PRs) are excluded from consideration.
branch="master";
# Polling workflow name. Other workflow names (nightly-tests and deploy-staging) are excluded from consideration.
workflow_name="build-test-deploy"
project_slug="gh/all-of-us"

#********************
# FUNCTIONS
# *******************

# CIRCLECI_TOKEN is a env variable whose value is personal token.
# See https://circleci.com/docs/2.0/managing-api-tokens/#creating-a-personal-api-token
check_circleci_token() {
  if [[ ! $CIRCLECI_TOKEN ]]; then
    printf '%s\n' "Required env variable \"CIRCLECI_TOKEN\" not found.\n Create a personal token then create \"CIRCLECI_TOKEN\" env variable?\n" >&2
    exit 1
  fi
}

post () {
  local url="${api_root}${1}"
  printf "curl POST ${url}\n" > /dev/tty
  curl -X POST \
       -H "Content-Type: application/json" \
       -H "Circle-Token: ${CIRCLECI_TOKEN}" \
       -d "$2" \
       "${url}"
}

get () {
  local url="${api_root}${1}"
  printf "curl GET ${url}\n" > /dev/tty
  curl -X GET -sS \
      -H "Content-Type: application/json" \
      -H "Circle-Token: ${CIRCLECI_TOKEN}" \
      "${url}"
}

# Get list of recently built pipelines. Save results to json file.
pipeline_json="/tmp/master_branch_pipelines.json"
fetch_pipeline_ids() {
  local get_path="pipeline?org-slug=${project_slug}"
  local get_result=$(get "${get_path}")
  # Debug echo ${get_result} | jq .
  echo ${get_result} | jq '[.items[] | select(.vcs.branch=="master")][] | {created_at: .created_at, id: .id, number: .number}' > ${pipeline_json}
  printf "Found following pipelines on ${branch} branch:\n"
  cat ${pipeline_json}
  printf "\n"
}

fetch_pipeline_number() {
  # Remove double or single quotes.
  local id=$(echo $1 | xargs echo)
  local get_path="pipeline/${id}"
  local get_result=$(get "${get_path}")
  # Debug echo ${get_result} | jq .
  __=$(echo "${get_result}" | jq -r .number)
}

# https://circleci.com/docs/2.0/workflows/#states
# v2 workflow api tells status, but not the pipeline api.
fetch_workflow_status() {
  # Remove double or single quotes.
  local id=$(echo $1 | xargs echo)
  local get_path="pipeline/${id}/workflow"
  local get_result=$(get ${get_path})
  # Debug echo $get_result | jq .
  local workflow_summary=$(echo ${get_result} | jq '.items[] | {name: .name, id: .id, status: .status, pipeline_number: .pipeline_number}')
  printf "${workflow_summary}\n"
  # workflow branch name is bound by $workflow_name variable.
  # Rerunning a failed workflow produces an array. Get the status in the first array element.
  __=$(echo ${get_result} | jq -r 'first(.items[]) | select(.name=='\"$workflow_name\"') | .status | @sh' )
}

fetch_this_pipeline_id() {
  # Debug printf "CIRCLE_WORKFLOW_ID: ${CIRCLE_WORKFLOW_ID}\n"
  local get_path="workflow/${CIRCLE_WORKFLOW_ID}"
  local get_result=$(get ${get_path})
  # Debug printf "this pipeline id: $(echo ${get_result} | jq .)\n"
  __=$(echo "${get_result}" | jq -r '.pipeline_number')
}

#********************
# ACTIONS
# *******************

printf "\n"
check_circleci_token

# Get self pipeline id for comparison later.
fetch_this_pipeline_id
this_pipeline_id=$__
printf "This pipeline id is ${this_pipeline_id}. This workflow id is ${CIRCLE_WORKFLOW_ID}\n\n"

# Get recent pipelines.
fetch_pipeline_ids
pipeline_ids=$(jq '. | .id' ${pipeline_json} | jq -r @sh)

# Check workflow status in each pipeline. Wait while status is running or failing. Max wait time for all active pipelines is 30 minutes.
wait="30s"
IFS=$'\n'
for id in ${pipeline_ids}; do
  printf "***   \n"
  fetch_pipeline_number "${id}"
  pipeline_num=$__

  if [[ $this_pipeline_id == $pipeline_num ]]; then
    # Don't check this pipeline if this pipeline is itself.
    printf '%s\n' "Not waiting on myself (pipeline id: ${this_pipeline_id}).\n" >&2
    continue
  fi

  is_running=true
  # Max wait time is 40 minutes because e2e tests may take a long time to finish.
  # DISCLAIMER This max time may not be enough if there are 2 or more workflows in waiting before this workflow.
  max_time_seconds=$(( 40 * 60 ))
  waited_time=0
  sleep_time=30 # seconds
  printf "Polling workflow status in pipeline number \"${pipeline_num}\" while status is failing or running. Max wait time is ${max_time_seconds} seconds. Please wait...\n"
  while [[ "${is_running}" == "true" ]]; do
    # Getting pipeline's workflow status.
    fetch_workflow_status "${id}"
    status=$__
    # Debug printf "workflow_status: ${status}\n"

    if [[ -z $status ]]; then
      # $status is blank because this workflow name does not match $workflow_name variable.
      printf '%s\n' "Skip querying this workflow because it is not \"${workflow_name}\"\n" >&2
      break # Break out while loop. check next pipeline.
    fi

    # An active workflow has status running or failing.
    if [[ ( $status == "'running'" ) || ( $status == "'failing'" ) ]]; then
      printf "sleeping ${wait} seconds (workflow status is ${status}).\n"
      sleep $sleep_time
      waited_time=$(( sleep_time + waited_time ))
    else
      is_running=false
      printf "Finished waiting for workflow in its pipeline_id: ${id}. It finished with status of ${status}!\n"
    fi
    printf "waited time is ${waited_time} seconds.\n"
    if [ $waited_time -ge $max_time_seconds ]; then
      printf "Max wait time (${max_time_seconds} seconds) exceeded. Stopping querying for this workflow, unblock and letting this job continue.\n"
      exit 0
    fi
  done
  printf "***   \n\n"
done

unset IFS
printf "finished checking all pipelines.\n"
