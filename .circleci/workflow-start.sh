#! /bin/bash

set -e
set -o pipefail
set -o functrace

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
  printf "Getting list of pipelines on master branch.\n"
  local get_result=$(get ${get_path})
  # Debug echo ${get_result} | jq .
  echo ${get_result} | jq '[.items[] | select(.vcs.branch=="master")][] | {created_at: .created_at, id: .id, number: .number}' > ${pipeline_json}
  echo "Found following pipelines:"
  cat ${pipeline_json}
  printf "\n"
}

fetch_pipeline_detail() {
  # Remove double or single quotes.
  local id=$(echo $1 | xargs echo)
  local get_path="pipeline/${id}"
  printf "Getting detail of pipeline_id: ${id}.\n"
  local get_result=$(get ${get_path})
  # Debug echo ${get_result} | jq .
  pipeline_num=$(echo ${get_result} | jq -r .number)
  printf "pipeline_num: ${pipeline_num}\n"
}

fetch_pipeline_workflow() {
  # Remove double or single quotes.
  local id=$(echo $1 | xargs echo)
  local get_path="pipeline/${id}/workflow"
  printf "Getting workflow_id in pipeline_id: ${id}\n"
  local get_result=$(get ${get_path})
  # Debug echo $get_result | jq .
  local workflow_id=$(echo ${get_result} | jq .items[].id | jq -r @sh)
  printf "workflow_id: ${workflow_id}\n\n"
  # https://circleci.com/docs/2.0/workflows/#states
  __=$(echo ${get_result} | jq -r '.items[] | .status')
}

# Get pipeline ids.
fetch_pipeline_ids
pipeline_ids=$(jq '. | .id' ${pipeline_json} | jq -r @sh)

# Check workflow for each pipeline.
IFS=$'\n'
for id in ${pipeline_ids}; do
  fetch_pipeline_detail ${id}
  # polling workflows status.
  is_running=true
  # max_loops (60) multiple wait (30 sec) is 30 minutes.
  max_retries=60
  counter=0
  wait="30s"
  printf "polling workflows' status. Please wait...\n"
  while [ $is_running ]; do
    if [[ $counter == $max_retries ]]; then
      echo "Maximum number of wait loops (${max_retries}) has been reached, Stopping querying for this workflow."
      break
    fi
    fetch_pipeline_workflow ${id}
    status=$__

    printf "workflow_status: ${status}\n"
    if [[ -z $status ]]; then
      printf '%s\n' "Fetch workflow status failed" >&2
      exit 1
    fi

    if [[ ( $status == "success" ) || ( $status == "failed" ) ]]; then
      is_running=false
      printf "pipeline_id: ${id} finished with status of ${status}!\n"
      break
    else
      sleep $wait
      printf "sleep ${wait} seconds...\n"
    fi
    ((counter+=1))
    printf "counter: ${counter}\n"
  done
done
unset IFS
echo "finished checking all pipelines."
exit 0
