#! /bin/bash

set -e
set -o pipefail
set -o functrace

#********************
# VARIABLES
# *******************
api_root="https://circleci.com/api/v1/"
# polling branch name. Other branches (PRs) are excluded from consideration.
branch="master"
# Polling workflow name. Other workflow names (nightly-tests and deploy-staging) are excluded from consideration.
workflow_name="build-test-deploy"
project_slug="all-of-us/workbench"

# Max check time on a workflow is 45 minutes because e2e tests may take a long time to finish.
# DISCLAIMER This max time may not be enough.
max_time=$((45 * 60))
sleep_time=30

#********************
# FUNCTIONS
# *******************

# CIRCLECI_API_TOKEN is a env variable whose value is project token.
check_circleci_token() {
  if [[ ! $CIRCLECI_API_TOKEN ]]; then
    printf '%s\n' "Required env variable \"CIRCLECI_API_TOKEN\" is not found." >&2
    exit 1
  fi
}

check_circleci_build_num() {
  if [[ ! $CIRCLE_BUILD_NUM ]]; then
    printf '%s\n' "Required env variable \"CIRCLE_BUILD_NUM\" is not found." >&2
    exit 1
  fi
}

check_circleci_workflow_id() {
  if [[ ! $CIRCLE_WORKFLOW_ID ]]; then
    printf '%s\n' "Required env variable \"CIRCLE_WORKFLOW_ID\" is not found." >&2
    exit 1
  fi
}

circle_get() {
  local url="${api_root}${1}"
  printf "curl GET ${url}\n" >/dev/tty
  curl -X GET -s -S -f \
    -H "Content-Type: application/json" \
    -H "Circle-Token: ${CIRCLECI_API_TOKEN}" \
    "${url}"
}

fetch_current_pipeline_start_time() {
  printf '%s\n' "Fetch current pipeline start time"
  # local get_path="project/${project_slug}/tree/${branch}?filter=running&shallow=true"
  local get_path="project/${project_slug}?filter=running&shallow=true"
  local curl_result=$(circle_get "${get_path}")
  jq_filter=".build_num==$CIRCLE_BUILD_NUM and .workflows.workflow_name==\"${workflow_name}\" and .workflows.workflow_id==\"${CIRCLE_WORKFLOW_ID}\""
  # jq_filter="select(.build_num==$CIRCLE_BUILD_NUM and .workflows.workflow_name==\"${workflow_name}\") | select(.workflows.workflow_id==\"${CIRCLE_WORKFLOW_ID}\") | .start_time"
  __=$(echo "${curl_result}" | jq -r ".[] | select(${jq_filter}) | .start_time")
}

# Fetch list of builds on master branch that are running, pending or queued.
fetch_older_pipelines() {
  printf '%s\n' "Fetch previously submitted pipelines (older than ${1}) on \"${branch}\" branch that are running, pending or queued:"
  # local get_path="project/${project_slug}/tree/${branch}?filter=running&shallow=true"
  local get_path="project/${project_slug}?filter=running&shallow=true"
  local get_result=$(circle_get "${get_path}")
  if [[ ! "${get_result}" ]]; then
    printf "Fetch older pipelines curl failed."
    exit 1
  fi
  # jq_filter="select(.branch==\"${branch}\" and (.status | test(\"running|pending|queued\")))"
  jq_filter="(.status | test(\"running|pending|queued\")) and .workflows.workflow_name==\"${workflow_name}\" and .workflows.workflow_id!=\"${CIRCLE_WORKFLOW_ID}\""
  # jq_filter="select(.status | test(\"running|pending|queued\"))"
  # jq_filter+=" | select(.workflows.workflow_name==\"${workflow_name}\" and .workflows.workflow_id!=\"${CIRCLE_WORKFLOW_ID}\")"
  jq_object="{ workflow_name: .workflows.workflow_name, workflow_id: .workflows.workflow_id, job_name: .workflows.job_name, build_num: .build_num, start_time: .start_time, status: .status }"
  printf "%s\n\n" "${get_result}" | jq -r ".[] | select(${jq_filter}) | select(.start_time>=\"${1}\")"
  __=$(echo "${get_result}" | jq -r ".[] | select(${jq_filter}) | select(.start_time>=\"${1}\") | ${jq_object}")
}


#********************
# RUNNING
# *******************

check_circleci_token
check_circleci_build_num
check_circleci_workflow_id

# Get pipeline that is doing the polling.
printf "%s\n" "Current pipeline CIRCLE_BUILD_NUM: ${CIRCLE_BUILD_NUM}"
printf "%s\n\n" "Current pipeline CIRCLE_WORKFLOW_ID: ${CIRCLE_WORKFLOW_ID}"

fetch_current_pipeline_start_time
current_pipeline_start_time=$__
printf "%s\n\n" "Current pipeline start_at time: ${current_pipeline_start_time}"

# Wait as long as "pipelines" variable is not empty until max time has reached.
is_running=true
waited_time=0
wait="30s"

while [[ "${is_running}" == "true" ]]; do
  printf "\n***\n"
  fetch_older_pipelines "${current_pipeline_start_time}"
  pipelines=$__

  if [[ $pipelines ]]; then
    printf "%s\n%s\n" "Found older pipelines:" "${pipelines}"
    printf "%s\n" "Waiting for previously submitted pipelines to finish. sleep ${wait} seconds. Please wait..."
    sleep $sleep_time
    waited_time=$((sleep_time + waited_time))
  else
    printf "\n%s\n" "All previous submitted pipelines have finished."
    is_running=false
  fi

  printf "%s\n" "Has been waiting for ${waited_time} seconds."
  if [ $waited_time -gt $max_time ]; then
    printf "\n\n%s\n\n" "***** Max wait time (${max_time} seconds) exceeded. Stop waiting for running builds. *****"
    is_running=false
  fi
done
printf "\n%s\n" "Finished."
