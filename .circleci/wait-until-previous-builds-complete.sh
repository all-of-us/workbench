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
# DISCLAIMER This max time may not be enough if there are 2 or more workflows in waiting before this workflow.
max_time=$((45 * 60))
sleep_time=30

#********************
# FUNCTIONS
# *******************

# CIRCLECI_TOKEN is a env variable whose value is personal token.
# See https://circleci.com/docs/2.0/managing-api-tokens/#creating-a-personal-api-token
check_circleci_token() {
  if [[ ! $CIRCLECI_TOKEN ]]; then
    printf '%s\n' "Required env variable \"CIRCLECI_TOKEN\" is not found.\n Create a personal token then create \"CIRCLECI_TOKEN\" env variable?\n" >&2
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
  curl -X GET -sS \
    -H "Content-Type: application/json" \
    -H "Circle-Token: ${CIRCLECI_TOKEN}" \
    "${url}"
}

# Get list of recently built pipelines. Returns json string.
fetch_pipeline_ids() {
  pipeline_json="/tmp/master_branch_pipelines.json"
  local get_path="pipeline?org-slug=${project_slug}"
  local get_result=$(circle_get "${get_path}")
  echo "${get_result}" | jq "[.items[] | select(.vcs.branch==\"master\")][] | {created_at: .created_at, id: .id, number: .number}" > "${pipeline_json}"
  printf "Found following pipelines on ${branch} branch:\n"
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

# https://circleci.com/docs/2.0/workflows/#states
# v2 workflow api tells status, but not the pipeline api.
fetch_workflow_status() {
  # Remove double or single quotes.
  local id=$(echo "${1}" | xargs echo)
  local get_path="pipeline/${id}/workflow"
  local get_result=$(circle_get "${get_path}")
  local workflow_summary=$(echo "${get_result}" | jq ".items[] | {name: .name, id: .id, status: .status, pipeline_number: .pipeline_number}")
  printf "${workflow_summary}\n"
  # Rerunning a failed workflow produces a nested datetime sorted array. Get the status of latest run (first array element).
  __=$(echo "${get_result}" | jq -r "first(.items[]) | select(.name==\"${workflow_name}\") | .status | @sh")
}

fetch_this_pipeline_id() {
  local get_path="workflow/${CIRCLE_WORKFLOW_ID}"
  local get_result=$(circle_get "${get_path}")
  __=$(echo "${get_result}" | jq -r '.pipeline_number')
}

# '/v2/pipeline' api retrieves all recent pipelines on all branches (PR, build-test-deploy and releases).
# We should consider blocking only if job is running on "master" branch.
# Release pipelines and PR pipelines are not considered for blocking.
should_skip() {
  if [[ "$CIRCLE_BRANCH" != "master" ]]; then
    printf "Not on master branch.\n"
    return 0
  fi

  # Get this job pipeline id. Don't poll and block itself.
  fetch_this_pipeline_id
  this_pipeline_id=$__
  if [[ "$this_pipeline_id" == $1 ]]; then
    printf '%s\n' "Not waiting on myself (pipeline id: \"${this_pipeline_id}\").\n" >&2
    return 0
  fi

  return 1
}

#********************
# RUNNING
# *******************

printf "\n"
check_circleci_token

# Get recent pipelines.
fetch_pipeline_ids
# Save returned json string to variable.
ids=$__
pipeline_ids=$(jq '. | .id' ${ids} | jq -r @sh)

# Check workflow status in each pipeline. Wait while workflow status is running or failing.
wait="30s"
IFS=$'\n'
for id in ${pipeline_ids}; do
  printf "***   \n"
  fetch_pipeline_number "${id}"
  pipeline_num=$__
  printf "Checking pipeline number \"${pipeline_num}\".\n"
  if should_skip "${pipeline_num}"; then continue; fi

  is_running=true
  waited_time=0
  printf "Polling workflow status in pipeline number \"${pipeline_num}\" while status is failing or running. Max wait time is ${max_time} seconds. Please wait...\n"
  while [[ "${is_running}" == "true" ]]; do
    # Getting pipeline's workflow status.
    fetch_workflow_status "${id}"
    status=$__

    if [[ -z "$status" ]]; then
      # $status is blank because this workflow name does not match $workflow_name variable.
      printf '%s\n' "Not \"${workflow_name}\".\n" >&2
      break # Break out while loop. check next pipeline.
    fi

    # An active workflow has status running or failing.
    if [[ ("${status}" == "'running'") || ("${status}" == "'failing'") ]]; then
      printf "sleeping ${wait} seconds (workflow status is ${status}).\n"
      sleep $sleep_time
      waited_time=$((sleep_time + waited_time))
    else
      printf "Finished waiting for workflow in pipeline_id: ${id}. It finished with status of ${status}!\n"
      is_running=false
    fi
    printf "Has been waiting for ${waited_time} seconds.\n\n"
    if [ $waited_time -gt $max_time ]; then
      printf "\n\n***** Max wait time (${max_time} seconds) exceeded. Stop checking this workflow. *****\n\n"
      is_running=false
    fi
  done
  printf "\n\n"
done
unset IFS
printf "finished checking all pipelines.\n\n"
