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
  curl -X GET -s -S -f \
    -H "Content-Type: application/json" \
    -H "Circle-Token: ${CIRCLECI_TOKEN}" \
    "${url}"
}

# Get list of recently built pipelines. Returns a json object.
fetch_pipeline_ids() {
  pipeline_json="/tmp/master_branch_pipelines.json"
  local get_path="pipeline?org-slug=${project_slug}"
  local get_result=$(circle_get "${get_path}")
  if [[ ! "${get_result}" ]]; then
    printf "curl failed."
    exit 1
  fi
  echo "${get_result}" | jq "[.items[] | select(.vcs.branch==\"${branch}\")][] | {created_at: .created_at, id: .id, number: .number}" > "${pipeline_json}"
  printf "Found following pipelines on \"${branch}\" branch:\n"
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

# '/v2/pipeline' api retrieves all recent pipelines on all branches (PR, build-test-deploy and releases).
# We should consider blocking only if job is running on "master" branch.
# Release pipelines and PR pipelines are not considered for blocking.
should_skip_pipeline() {
  # Pipelines on non-master branches are not considered.
  if [[ $CIRCLE_BRANCH && "$CIRCLE_BRANCH" != "master" ]]; then
    printf "Not on master branch.\n"
    return 0
  fi

  # Ensure we don’t count the current pipeline that is doing the poll.
  # Compare pipeline number to ensure two pipelines are different.
  # The current workflow id is available as built-in environment variable CIRCLE_WORKFLOW_ID. It's used to get pipeline number.
  local get_path="workflow/${CIRCLE_WORKFLOW_ID}"
  local get_result=$(circle_get "${get_path}")
  this_pipeline_num=$(echo "${get_result}" | jq -r '.pipeline_number')
  if [[ "$this_pipeline_num" == "$1" ]]; then
    printf '%s\n' "Not waiting on own pipeline (pipeline id: \"${this_pipeline_num}\").\n" >&2
    return 0
  fi
  return 1
}

#********************
# RUNNING
# *******************

printf "\n"
check_circleci_token

# Get recently submitted pipelines (including finished and ongoing pipelines) on all branches.
fetch_pipeline_ids
# Save returned json object.
ids=$__
# Get IDs.
pipeline_ids=$(jq '. | .id' ${ids} | jq -r @sh)

# Check workflow status in each pipeline. Wait while workflow status is running or failing.
wait="30s"
IFS=$'\n'
for id in ${pipeline_ids}; do
  printf "********   \n"
  fetch_pipeline_number "${id}"
  pipeline_num=$__
  if should_skip_pipeline "${pipeline_num}"; then
    continue
  fi

  is_running=true
  waited_time=0
  printf "%s\n" "Polling pipeline \"${pipeline_num}\" while status is failing or running. Max wait time is ${max_time} seconds. Please wait..."
  while [[ "${is_running}" == "true" ]]; do
    # Getting pipeline's workflow status.
    fetch_workflow_status "${id}"
    status=$__

    if [[ -z "$status" ]]; then
      # $status is blank because this workflow name does not match $workflow_name variable.
      printf "%s\n" "Not workflow \"${workflow_name}\"." >&2
      break # Break out while loop. check next pipeline.
    fi

    # An active workflow has running or failing status.
    if [[ ("${status}" == "'running'") || ("${status}" == "'failing'") ]]; then
      printf "%s\n" "sleeping ${wait} seconds (workflow status is ${status})."
      sleep $sleep_time
      waited_time=$((sleep_time + waited_time))
    else
      printf "%s\n" "Finished polling for status of workflow \"${workflow_name}\" in pipeline_id: ${id}. It finished with status of ${status}!"
      is_running=false
    fi
    printf "%s\n" "Has been waiting for ${waited_time} seconds."
    if [ $waited_time -gt $max_time ]; then
      printf "\n\n%s\n\n" "***** Max wait time (${max_time} seconds) exceeded. Stop checking this workflow. *****"
      is_running=false
    fi
  done
  printf "\n"
done
unset IFS
printf "%s\n" "finished checking all pipelines."
