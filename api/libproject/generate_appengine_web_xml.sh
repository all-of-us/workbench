#!/bin/bash

for line in `awk '!/^ *#/ && NF' db/vars.env`; do
  IFS='=' read -r var val <<< "$line"

  if [ "$var" == "DB_HOST" ] && [ $OVERWRITE_WORKBENCH_DB_HOST ]; then
    export DB_HOST=localhost
    continue
  fi

  evaluatedString=$(echo $(eval "echo $val"))
  export $var=$evaluatedString
done

set -euo pipefail
IFS=$'\n\t'

envsubst < src/main/webapp/WEB-INF/appengine-web.xml.template \
  > src/main/webapp/WEB-INF/appengine-web.xml
