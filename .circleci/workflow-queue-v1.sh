#! /bin/bash

set -e
set -o pipefail
set -o functrace

#********************
# GLOBAL VARIABLES
# *******************
API_ROOT="https://circleci.com/api/v1/"
# polling branch name. Other branches (PRs) are excluded from consideration.
BRANCH="master"
# Polling workflow name. Other workflow names (nightly-tests and deploy-staging) are excluded from consideration.
WORKFLOW_NAME="build-test-deploy"
PROJECT_SLUG="all-of-us/workbench"

# List of jobs in build-test-deploy workflow on master branch.
JOB_LIST=("puppeteer-test-2-1" "ui-deploy-to-test" "api-deploy-to-test" "api-integration-test" "api-local-test")
JOB_LIST+=("api-unit-test" "wait_until_previous_workflow_done" "ui-unit-test" "puppeteer-env-setup" "api-bigquery-test")


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
# Fetch list of builds on master branch that are running, pending or queued.
fetch_older_pipelines() {
  printf '%s\n' "Fetching workflow id (Older than ${1} on \"${BRANCH}\" branch that are running, pending or queued)."
  local get_path="project/${PROJECT_SLUG}/tree/${BRANCH}?filter=running&shallow=true"
  local curl_result=$(circle_get "${get_path}")
  if [[ ! "${curl_result}" ]]; then
    printf "Fetching all older pipelines failed."
    exit 1
  fi

  # Explanation:
  # .why=="github": Exclude jobs manually triggered via ssh by users.
  # .dont_build!="prs-only": Commits to github branch but a Pull Request has not been created.
  jq_filter=".branch==\"${BRANCH}\" "
  jq_filter+=" and .why==\"github\" and .dont_build!=\"prs-only\" "
  jq_filter+=" and .workflows.workflow_name==\"${WORKFLOW_NAME}\" and .workflows.workflow_id!=\"${CIRCLE_WORKFLOW_ID}\""

  __=$(echo "${curl_result}" | jq -r ".[] | select(${jq_filter}) | select(.start_time < \"${1}\") | [{workflow_id: .workflows.workflow_id}] | unique | .[] | .workflow_id")
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

fetch_running_jobs() {
  printf '%s\n' "Fetching running jobs in workflow_id \"${1}\" on \"${BRANCH}\" branch."
  local get_path="project/${PROJECT_SLUG}/tree/${BRANCH}?shallow=true"
  local curl_result=$(circle_get "${get_path}")

  if [[ ! "${curl_result}" ]]; then
    printf "Curl request failed. workflow_id \"${1}\"."
    exit 1
  fi

  jq_object="{ workflow_name: .workflows.workflow_name, workflow_id: .workflows.workflow_id, "
  jq_object+="job_name: .workflows.job_name, build_num, start_time, status, branch, build_url }"
  jq_filter=".branch==\"${BRANCH}\" and (.status | test(\"running|queued\")) "
  jq_filter+=" and .workflows.workflow_name==\"${WORKFLOW_NAME}\" and .workflows.workflow_id==\"${1}\""

  __=$(echo "${curl_result}" | jq -r ".[] | select(${jq_filter}) | ${jq_object}")
}

compare_arrays() {
  arg1=$1[@]
  array1=("${!arg1}")

  arg2=$2[@]
  array2=("${!arg2}")

  if [[ ${array1[*]} == ${array2[*]} ]] ; then
      return
  fi;
  false
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
if [[ ! "${current_pipeline_start_time}" ]]; then
  printf "Value of current_pipeline_start_time is not valid."
  exit 1
fi

fetch_older_pipelines "${current_pipeline_start_time}"
pipeline_workflow_ids=$__

# Exit if there are no running workflows on master branch.
if [[ -z $pipeline_workflow_ids ]]; then
  printf "%s\n" "No workflow currently running on master branch."
  exit 0
fi
# Filter out duplicate workflow id.
workflow_ids=$(echo $pipeline_workflow_ids | tr ' ' '\n' | sort --u)
printf "%s\n%s\n\n" "Currently running workflow_ids:" "${workflow_ids}"


# Max wait time until workflows have finished is 45 minutes because e2e tests may take a long time to finish.
# DISCLAIMER This max time may not be enough.
max_time=$((45 * 60))
is_running=true
waited_time=0
# sleep_time and time_counter must be same.
sleep_time="20s"
sleep_time_counter=20

while [[ "${is_running}" == "true" ]]; do
  printf "\n***\n"

  for id in ${workflow_ids}; do
    is_running=false

    # Find jobs that have been created in CircleCI (jobs listed in api response).
    # Created jobs are jobs with status running, queued, failed, success.
    fetch_jobs "${id}"
    created_jobs=$__
    printf "\n%s\n%s\n\n" "Jobs that have been created:" "${created_jobs}"

    # Find running/queued jobs only.
    running_jobs=$(echo ${created_jobs} | jq ". | select((.status | test(\"running|queued\")))")
    printf "\n%s\n%s\n\n" "Jobs that are running or queued:" "${running_jobs}"


    # Find just the running/queued jobs.
    # fetch_running_jobs "${id}"
    # running_jobs=$__
    # printf "\n%s\n%s\n" "Active workflow:" "${running_jobs}"

    # V1 "/project/" api response does not show jobs that have not been created.
    # We need to compare created job list against expected job list.
    # jobs=$(echo ${running_jobs} | jq .job_name)
    # printf "\n%s\n" "Jobs that have been created:" "${jobs}"

    # Find jobs that have not created in CircleCI.
    created_job_names=$(echo ${created_jobs} | jq -r ".job_name")
    printf "\n%s\n%s\n" "created_jobs_list:" "${created_job_names}"


    compare_arrays JOB_LIST created_job_names
    not_created_jobs=$__
    printf "\n%s\n" "Jobs that have not been created:" "${not_created_jobs}"

    # Wait while there is a job still is running or there is a job that has not been created.
    if [[ $running_jobs ]] && [[ $not_created_jobs ]]; then
      printf "\n%s\n" "Waiting for previously submitted pipelines to finish. sleep ${sleep_time}. Please wait..."
      sleep $sleep_time
      waited_time=$((sleep_time_counter + waited_time))
      is_running=true
      break
    fi
  done

  printf "%s\n" "Has been waiting for ${waited_time} seconds."
  if [ $waited_time -gt $max_time ]; then
    # Do not fail script.
    printf "\n\n%s\n\n" "***** WARNING: Max wait time (${max_time} seconds) exceeded. Stop waiting for running builds to complete."
    is_running=false
  fi
done

printf "\n%s\n" "Finished waiting. Final is_running=${is_running}";
