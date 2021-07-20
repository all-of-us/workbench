#! /bin/bash

set -e
set -o pipefail
set -o functrace

#********************
# VARIABLES
# *******************
api_root="https://circleci.com/api/v2/"
# polling branch name. Other branches (PRs) are excluded from consideration.
branch="master"
# Polling workflow name. Other workflow names (nightly-tests and deploy-staging) are excluded from consideration.
workflow_name="build-test-deploy"
project_slug="gh/all-of-us"

# Max check time on a workflow is 45 minutes because e2e tests may take a long time to finish.
# DISCLAIMER This max time may not be enough.
max_time=$((45 * 60))
sleep_time=30

#********************
# FUNCTIONS
# *******************

# CIRCLECI_TOKEN is a env variable whose value is personal token.
# See https://circleci.com/docs/2.0/managing-api-tokens/#creating-a-personal-api-token
check_circleci_token() {
  if [[ ! $CIRCLECI_TOKEN ]]; then
    printf '%s\n' "Required env variable \"CIRCLECI_TOKEN\" is not found.\n Create a personal token then create \"CIRCLECI_TOKEN\" env variable?" >&2
    exit 1
  fi
}

check_circleci_workflow_id() {
  if [[ ! $CIRCLE_WORKFLOW_ID ]]; then
    printf '%s\n' "Required env variable \"CIRCLE_WORKFLOW_ID\" is not found." >&2
    exit 1
  fi
}

circle_post() {
  local url="${api_root}${1}"
  printf "curl POST ${url}\n" >/dev/tty
  curl -X POST \
    -H "Content-Type: application/json" \
    -H "Circle-Token: ${CIRCLECI_TOKEN}" \
    -d "$2" \
    "${url}"
}

circle_get() {
  local url="${api_root}${1}"
  printf "curl GET ${url}\n" >/dev/tty
  curl -X GET -s -S -f \
    -H "Content-Type: application/json" \
    -H "Circle-Token: ${CIRCLECI_TOKEN}" \
    "${url}"
}

# Get pipeline that is doing the poll.
# Returns json object.
fetch_current_pipeline() {
  local get_path="workflow/${CIRCLE_WORKFLOW_ID}"
  local get_result=$(circle_get "${get_path}")
  __=$(echo "$get_result" | jq .)
}

# 'pipeline' api returns list of recent pipelines on all branches (PR, build-test-deploy and releases).
# We should consider blocking only if job is running on "master" branch.
# Release pipelines and PR pipelines are not considered for blocking.
# Function returns json object.
fetch_pipeline_ids() {
  printf '%s\n' "Fetch recently submitted pipelines on \"${branch}\" branch:"
  pipeline_json="/tmp/master_branch_pipelines.json"
  local get_path="pipeline?org-slug=${project_slug}"
  local get_result=$(circle_get "${get_path}")
  if [[ ! "${get_result}" ]]; then
    printf "curl failed."
    exit 1
  fi

  # jq filter out pipelines that were submitted after the current pipeline and not on master branch.
  # Don’t count the current pipeline that is doing the poll.
  jq_filter="[.items[] | select(.number < ${1} and .vcs.branch==\"${branch}\")]"
  length=$(echo "${get_result}" | jq "${jq_filter} | length")
  printf "%s\n\n" "pipelines size: ${length}"

  echo "${get_result}" | jq "${jq_filter}[] | {created_at: .created_at, id: .id, number: .number}" >"${pipeline_json}"
  cat ${pipeline_json}
  printf "\n"
  __=$(echo "${pipeline_json}")
}

fetch_pipeline_number() {
  # Remove double or single quotes.
  local id=$(echo "${1}" | xargs echo)
  local get_path="pipeline/${id}"
  local get_result=$(circle_get "${get_path}")
  __=$(echo "${get_result}" | jq -r .number)
}


#********************
# RUNNING
# *******************

check_circleci_token
check_circleci_workflow_id
printf "\n"

# Get pipeline that is doing the polling.
printf "%s\n" "Current pipeline:"
fetch_current_pipeline
current_pipeline_json=$__
current_pipeline_num=$(echo "${current_pipeline_json}" | jq -r '.pipeline_number')
current_created_time=$(echo "${current_pipeline_json}" | jq -r '.created_at')
printf "%s\n\n" "${current_pipeline_json}"

# Get all recently submitted pipelines.
fetch_pipeline_ids "${current_pipeline_num}"
pipelines=$__
# Parse out IDs.
pipeline_ids=$(jq '. | .id' "${pipelines}" | jq -r @sh)

# Check workflow status in each pipeline. Wait while workflow status is running or failing.
wait="30s"
IFS=$'\n'
for id in ${pipeline_ids}; do
  printf "********\n"
  id=$(echo "${id}" | xargs echo)
  fetch_pipeline_number "${id}"
  pipeline_num=$__

  is_running=true
  waited_time=0

  printf "%s\n" "Polling pipeline \"${pipeline_num}\" while status is failing or running. Max wait time is ${max_time} seconds. Please wait..."
  while [[ "${is_running}" == "true" ]]; do
    get_path="pipeline/${id}/workflow"
    request_result=$(circle_get "${get_path}")
    workflow_summary=$(echo "${request_result}" | jq '.items[]')
    printf "%s\n" "${workflow_summary}"

    status=$(echo "${request_result}" | jq -r "first(.items[]) | select(.name==\"${workflow_name}\") | .status | @sh")
    created_time=$(echo "${request_result}" | jq -r "first(.items[]) | select(.name==\"${workflow_name}\") | .created_at | @sh")

    if [[ -z "$status" ]]; then
      # $status is blank because this workflow name does not match $workflow_name variable.
      printf "%s\n" "Not workflow \"${workflow_name}\"." >&2
      break # Break out while loop. check next pipeline.
    fi

    # Additional filter by "create_at" time: Include only pipelines that were submitted before the current pipeline that is doing the polling.
    if [[ "$created_time" < "$current_created_time" ]]; then
      # An active workflow has running or failing status.
      if [[ ("${status}" == "'running'") || ("${status}" == "'failing'") ]]; then
        printf "%s\n" "sleeping ${wait} seconds (workflow status is ${status})."
        sleep $sleep_time
        waited_time=$((sleep_time + waited_time))
      else
        printf "%s\n\n" "Finished polling. workflow status is ${status}!"
        is_running=false
      fi
    else
      # Break out while loop if time of compare to workflow is not earlier than the workflow that is polling.
      printf "%s\n" "pipeline \"${pipeline_num}\" is not a workflow which was submitted before this polling pipeline \"${current_pipeline_num}\"."
      printf '%s\n\n' "Continue to check next pipeline."
      break # Break out while loop. check next pipeline.
    fi

    printf "%s\n" "Has been waiting for ${waited_time} seconds."
    if [ $waited_time -gt $max_time ]; then
      printf "\n\n%s\n\n" "***** Max wait time (${max_time} seconds) exceeded. Stop checking this workflow. *****"
      is_running=false
    fi
  done
  printf "%s\n" "End of while loop."
done
unset IFS
printf "%s\n" "finished checking all pipelines."
