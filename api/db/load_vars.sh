#!/usr/bin/env bash

# This script will load the necessary environment variables into your environment
# This needs to be run when starting our services without docker which has its own mechanisms
# for loading environment variables.

for line in $(awk '!/^ *#/ && NF' $WORKBENCH_DIR/api/db/vars.env); do
  IFS='=' read -r var val <<< "$line"

  if [[ "$var" == "DB_HOST" ]]; then
    export DB_HOST=localhost
    continue
  fi

  eval evaluatedString=$(echo $val) # this will expand the variable inside $val, for zsh compatibility
  export $var=$evaluatedString
done
