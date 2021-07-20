#!/bin/bash

# Initialize test users' access timestamps to known fixed values
# to prepare for the e2e tests

PROJECT=$1
USER_ID=$2

# initially, require Test env
if [ "$PROJECT" != "all-of-us-workbench-test" ]; then
  echo "script called with project $PROJECT, but only 'all-of-us-workbench-test' is supported."
  exit 1
fi

# very basic SQL safety check: must be an integer
if ! [[ $USER_ID =~ ^[0-9]+$ ]]; then
  echo "called with a non-integer USER_ID: $USER_ID"
  exit 2
fi

PROXY_HOST="0.0.0.0"
PROXY_PORT=3307

# Start cloud proxy in the background
cloud_sql_proxy -instances ${PROJECT}:us-central1:workbenchmaindb=tcp:${PROXY_HOST}:${PROXY_PORT} &
# and wait for it
dockerize -wait tcp://${PROXY_HOST}:${PROXY_PORT} -timeout 2m

# Execute commands

declare -a COMMANDS=(
  "UPDATE user SET profile_last_confirmed_time = '2020-07-05' WHERE user_id = ${USER_ID};"
  "UPDATE user SET publications_last_confirmed_time = '2020-07-10' WHERE user_id = ${USER_ID};"
)

for SQL_COMMAND in "${COMMANDS[@]}"
do
  mysql --host=${PROXY_HOST} --port=${PROXY_PORT} \
    --user=workbench --database=workbench --password=${WORKBENCH_DB_WRITE_PASSWORD} \
    -e "${SQL_COMMAND}"
done
