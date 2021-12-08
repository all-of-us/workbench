#! /bin/bash

set -e
set -o pipefail
set -o functrace

#********************
# GLOBAL VARIABLES
# *******************
API_ROOT="https://circleci.com/api/v1/"
# polling branch name. Other branches (PRs) are excluded from consideration.
BRANCH="main"
# Polling workflow name. Other workflow names (nightly-tests and deploy-staging) are excluded from consideration.
WORKFLOW_NAME="build-test-deploy"
PROJECT_SLUG="all-of-us/workbench"

# List of jobs that have depend on upstream jobs in build-test-deploy workflow on main branch.
JOB_LIST=("puppeteer-test-2-1" "ui-deploy-to-test" "api-deploy-to-test")


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
  local url="${API_ROOT}${1}"
  printf "curl GET ${url}\n" >/dev/tty
  curl -X GET -s -S -f \
    -H "Content-Type: application/json" \
    -H "Circle-Token: ${CIRCLECI_API_TOKEN}" \
    "${url}"
}

# Function returns current pipeline's start_time. It is used for comparison of start_time values.
fetch_current_pipeline_start_time() {
  printf '%s\n' "Fetching current pipeline start_time."
  local get_path="project/${PROJECT_SLUG}?filter=running&shallow=true"
  local curl_result=$(circle_get "${get_path}")
  __=$(echo "${curl_result}" | jq -r ".[] | select(.build_num==$CIRCLE_BUILD_NUM) | .start_time")
}

# Function takes start_time parameter.
# Fetch list of builds on main branch that are running, pending or queued.
fetch_older_pipelines() {
  printf '%s\n' "Fetching workflow id (Older than ${1} on \"${BRANCH}\" branch that are running, pending or queued)."
  local get_path="project/${PROJECT_SLUG}/tree/${BRANCH}?filter=running&shallow=true"
  local curl_result=$(circle_get "${get_path}")
  if [[ ! "${curl_result}" ]]; then
    printf "Fetching all older pipelines failed."
    exit 1
  fi

  # .why=="github": Exclude jobs manually triggered via ssh by users.
  # .dont_build!="prs-only": Commits to github branch but a Pull Request has not been created.
  jq_filter=".branch==\"${BRANCH}\" and .why==\"github\" and .dont_build!=\"prs-only\" "
  jq_filter+=" and .workflows.workflow_name==\"${WORKFLOW_NAME}\" and .workflows.workflow_id!=\"${CIRCLE_WORKFLOW_ID}\""

  __=$(echo "${curl_result}" | jq -r ".[] | select(${jq_filter}) | select(.start_time < \"${1}\") | [{workflow_id: .workflows.workflow_id}] | .[] | .workflow_id")
}

fetch_jobs() {
  printf '%s\n' "Fetching created jobs in workflow_id \"${1}\" on \"${BRANCH}\" branch."
  local get_path="project/${PROJECT_SLUG}/tree/${BRANCH}?shallow=true"
  local curl_result=$(circle_get "${get_path}")

  if [[ ! "${curl_result}" ]]; then
    printf "Curl request failed. workflow_id \"${1}\"."
    exit 1
  fi

  jq_object="{ workflow_name: .workflows.workflow_name, workflow_id: .workflows.workflow_id, "
  jq_object+="job_name: .workflows.job_name, build_num, start_time, status, branch }"
  jq_filter=".branch==\"${BRANCH}\" and .workflows.workflow_name==\"${WORKFLOW_NAME}\" and .workflows.workflow_id==\"${1}\""

  __=$(echo "${curl_result}" | jq -r ".[] | select(${jq_filter}) | ${jq_object}")
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

if [[ ! "${current_pipeline_start_time}" ]]; then
  printf "%s\n\n" "Invalid (current) pipeline start_at time: ${current_pipeline_start_time}"
  exit 1
fi

fetch_older_pipelines "${current_pipeline_start_time}"
pipeline_workflow_ids=$__

# Exit if there are no running workflows on main branch.
if [[ -z $pipeline_workflow_ids ]]; then
  printf "%s\n" "No workflow currently running on main branch."
  exit 0
fi

# Filter out duplicate workflow id.
workflow_ids=$(echo "${pipeline_workflow_ids}" | tr ' ' '\n' | sort --u)
printf "%s\n%s\n\n" "Currently running workflow_ids:" "${workflow_ids}"


# Max wait time until a workflow has finished is 30 minutes.
# DISCLAIMER: This max time per workflow may not be enough.
max_time=$((30 * 60))
waited_time=0
# sleep 30 seconds. The sleep_time and time_counter must be same.
sleep_time="30s"
sleep_time_counter=30

for id in ${workflow_ids}; do
  is_running=true

  while [[ "${is_running}" == "true" ]]; do
    printf "\n\n%s\n" "*** Polling workflow id ${id} ***"
    is_running=false

    # Find jobs that have been created (listed jobs in api response):
    # Created jobs have status running, queued, failed, or success.
    fetch_jobs "${id}"
    created_jobs=$__
    created_job_names=$(echo "${created_jobs}" | jq -r ".job_name")

    # Find failed jobs only.
    jq_job_filter="(.status | test(\"failed\")) and (.job_name | test(\"ui-deploy-to-test\"|\"api-deploy-to-test\"))"
    failed_jobs=$(echo "${created_jobs}" | jq ". | select(${jq_job_filter})")
    if [[ $failed_jobs ]]; then
      printf "\n%s\n%s\n" ">>> Deploy has failed:" "${failed_jobs}"
      break
    fi

    # Find running/queued jobs only.
    running_jobs=$(echo "${created_jobs}" | jq ". | select((.status | test(\"running|queued\")))")

    # Find if any job has not been created.
    # V1 "/project/" api response does not show jobs that have not been created.
    # Get list of elements that appear in $JOB_LIST but are not in $created_job_names.
    not_created_jobs=$( \
      echo ${created_job_names[@]} ${JOB_LIST[@]} \
      | sed 's/ /\n/g' \
      | sort | uniq -d \
      | xargs echo ${JOB_LIST[@]} \
      | sed 's/ /\n/g' \
      | sort \
      | uniq -u )

    # Wait while some jobs in running/queued OR some jobs that have not been created.
    if [[ $running_jobs ]] || [[ $not_created_jobs ]]; then
      printf "\n%s\n" "Waiting for previously submitted pipelines to finish. sleep ${sleep_time}. Please wait..."
      printf "%s\n%s\n%s\n" "--------" "Running/queued jobs:" "${running_jobs}"
      printf "%s\n%s\n%s\n" "--------" "Not created jobs:" "${not_created_jobs}"
      sleep $sleep_time
      waited_time=$((sleep_time_counter + waited_time))
      is_running=true
    fi

    printf "\n%s\n" ">>> Has been waiting for ${waited_time} seconds."
    if [ $waited_time -gt $max_time ]; then
      # End wait but do not fail script.
      printf "\n\n%s\n\n" "****** Max wait time (${max_time} seconds) exceeded. Stop waiting for running builds to complete."
      break
    fi
  done

done

printf "\n%s\n%s\n" "Finished waiting." "is_running=${is_running}";
